// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.aad.msal4j.testcase.Act;
import com.microsoft.aad.msal4j.testcase.Arrange;
import com.microsoft.aad.msal4j.testcase.Assert;
import com.microsoft.aad.msal4j.testcase.Steps;
import com.microsoft.aad.msal4j.testcase.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for running MSAL4J test cases.
 * Provides functionality for creating applications, executing test steps, and validating results.
 */
public class RunnerHelper {
    private static final Logger LOG = LoggerFactory.getLogger(RunnerHelper.class);

    /**
     * Represents the result of attempting to create an application.
     * Can either hold the created application or an exception if creation failed.
     */
    static class AppCreationResult {
        private final IApplicationBase application;
        private final Exception exception;

        private AppCreationResult(IApplicationBase application) {
            this.application = application;
            this.exception = null;
        }

        private AppCreationResult(Exception exception) {
            this.application = null;
            this.exception = exception;
        }

        public boolean isSuccessful() {
            return application != null;
        }

        @SuppressWarnings("unchecked")
        public <T extends IApplicationBase> T getApplication() {
            return (T) application;
        }

        public Exception getException() {
            return exception;
        }

        public static AppCreationResult success(IApplicationBase application) {
            return new AppCreationResult(application);
        }

        public static AppCreationResult failure(Exception exception) {
            return new AppCreationResult(exception);
        }
    }

    /**
     * Creates Managed Identity applications from the test configuration.
     */
    static Map<String, AppCreationResult> createMangedIdentityAppsFromConfig(TestCase testCase) {
        Map<String, AppCreationResult> appResults = new HashMap<>();
        Arrange arrange = testCase.getArrange();
        IEnvironmentVariables env = setEnvironmentVariables(testCase);

        for (Map.Entry<String, Arrange.ObjectProperties> entry : arrange.getObjects().entrySet()) {
            String appName = entry.getKey();
            Arrange.ObjectProperties appObjectProperties = entry.getValue();

            if ("ManagedIdentityClient".equals(appObjectProperties.getType())) {
                try {
                    ManagedIdentityId identityId = createManagedIdentityId(appObjectProperties);
                    List<String> capabilities = extractClientCapabilities(appObjectProperties);

                    ManagedIdentityApplication app = ManagedIdentityApplication.builder(identityId)
                            .clientCapabilities(capabilities)
                            .build();

                    ManagedIdentityApplication.setEnvironmentVariables(env);

                    appResults.put(appName, AppCreationResult.success(app));
                    LOG.info("Created app: {}", appName);
                } catch (Exception e) {
                    LOG.info("Failed to create app {}: {}", appName, e.getMessage());
                    appResults.put(appName, AppCreationResult.failure(e));
                }
            }
        }
        return appResults;
    }

    /**
     * Creates Confidential Client applications from the test configuration.
     */
    static Map<String, AppCreationResult> createConfidentialClientAppsFromConfig(TestCase testCase) {
        Map<String, AppCreationResult> appResults = new HashMap<>();
        Arrange arrange = testCase.getArrange();

        for (Map.Entry<String, Arrange.ObjectProperties> entry : arrange.getObjects().entrySet()) {
            String appName = entry.getKey();
            Arrange.ObjectProperties appConfig = entry.getValue();

            if ("ConfidentialClientApplication".equals(appConfig.getType())) {
                try {
                    String clientId = (String) appConfig.getProperty("client_id");
                    String oidcAuthority = (String) appConfig.getProperty("oidc_authority");
                    IClientCredential credential = ClientCredentialFactory.createFromSecret(
                            (String) appConfig.getProperty("client_credential"));

                    ConfidentialClientApplication app = ConfidentialClientApplication.builder(clientId, credential)
                            .oidcAuthority(oidcAuthority)
                            .build();

                    appResults.put(appName, AppCreationResult.success(app));
                    LOG.info("Created app: {}", appName);
                } catch (Exception e) {
                    LOG.info("Failed to create app {}: {}", appName, e.getMessage());
                    appResults.put(appName, AppCreationResult.failure(e));
                }
            }
        }
        return appResults;
    }

    /**
     * Executes all steps in a test case using the created applications.
     */
    static void executeTestCase(TestCase testCase, Map<String, AppCreationResult> appResults) throws Exception {
        Map<String, List<Steps.Step>> stepsByApp = groupStepsByApplication(testCase);

        for (String appId : stepsByApp.keySet()) {
            AppCreationResult appResult = appResults.get(appId);
            if (appResult == null) {
                throw new IllegalStateException("No app result found for ID: " + appId);
            }

            if (appResult.isSuccessful()) {
                executeStepsForSuccessfulApp(appResult.getApplication(), stepsByApp.get(appId));
            } else {
                validateFailedAppResults(appResult.getException(), stepsByApp.get(appId));
            }
        }
    }

    /**
     * Groups test steps by the application they target.
     */
    private static Map<String, List<Steps.Step>> groupStepsByApplication(TestCase testCase) {
        Map<String, List<Steps.Step>> stepsByApp = new HashMap<>();

        for (Steps.Step step : testCase.getSteps().getSteps()) {
            String appId = step.getAction().getTargetObject();
            if (!stepsByApp.containsKey(appId)) {
                stepsByApp.put(appId, new ArrayList<>());
            }
            stepsByApp.get(appId).add(step);
        }

        return stepsByApp;
    }

    /**
     * Executes test steps for a successfully created application.
     */
    private static void executeStepsForSuccessfulApp(IApplicationBase app, List<Steps.Step> steps) throws Exception {
        if (app instanceof ManagedIdentityApplication) {
            executeTestSteps((ManagedIdentityApplication) app, steps);
        } else if (app instanceof ConfidentialClientApplication) {
            executeTestSteps((ConfidentialClientApplication) app, steps);
        } else {
            throw new UnsupportedOperationException("Unsupported application type: " + app.getClass().getName());
        }
    }

    /**
     * Validates test assertions for each step when application creation failed.
     */
    private static void validateFailedAppResults(Exception exception, List<Steps.Step> steps) {
        LOG.info("Validating app creation exception");
        for (Steps.Step step : steps) {
            validateAssertions(null, exception, step.getAssertion());
        }
    }

    /**
     * Execute all test steps for a ManagedIdentityApplication.
     */
    static void executeTestSteps(ManagedIdentityApplication app, List<Steps.Step> steps) throws Exception {
        // Clear token cache once at the start for this app
        app.tokenCache.accessTokens.clear();

        for (Steps.Step step : steps) {
            LOG.info("Executing step for ManagedIdentityApplication");
            executeStep(app, step);
        }
    }

    /**
     * Execute all test steps for a ConfidentialClientApplication.
     */
    static void executeTestSteps(ConfidentialClientApplication app, List<Steps.Step> steps) throws Exception {
        // Clear token cache once at the start for this app
        app.tokenCache.accessTokens.clear();

        for (Steps.Step step : steps) {
            LOG.info("Executing step for ConfidentialClientApplication");
            executeStep(app, step);
        }
    }

    /**
     * Executes a single test step and validates its assertions.
     */
    private static <T extends IApplicationBase> void executeStep(T app, Steps.Step step) throws Exception {
        try {
            IAuthenticationResult result;
            if (app instanceof ManagedIdentityApplication) {
                result = executeAction((ManagedIdentityApplication) app, step.getAction());
            } else if (app instanceof ConfidentialClientApplication) {
                result = executeAction((ConfidentialClientApplication) app, step.getAction());
            } else {
                throw new UnsupportedOperationException("Unsupported application type: " + app.getClass().getName());
            }

            validateAssertions(result, null, step.getAssertion());
        } catch (Exception e) {
            LOG.info("Exception executing action: {}", e.getMessage());
            validateAssertions(null, e, step.getAssertion());
        }
    }

    /**
     * Execute an action on a ManagedIdentityApplication and return the result.
     */
    static IAuthenticationResult executeAction(ManagedIdentityApplication app, Act action) throws Exception {
        if (action.getMethodName().equals("AcquireTokenForManagedIdentity")) {
            LOG.info("Executing action: {}", action.getMethodName());
            ManagedIdentityParameters params = buildManagedIdentityParameters(action);
            IAuthenticationResult result = app.acquireTokenForManagedIdentity(params).get();
            logAuthResult(result);
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported action: " + action.getMethodName());
        }
    }

    /**
     * Execute an action on a ConfidentialClientApplication and return the result.
     */
    static IAuthenticationResult executeAction(ConfidentialClientApplication app, Act action) throws Exception {
        if (action.getMethodName().equals("AcquireTokenForClient")) {
            LOG.info("Executing action: {}", action.getMethodName());
            //TODO: Handle other acquire token methods
            ClientCredentialParameters params = buildClientCredentialParameters(action);
            IAuthenticationResult result = app.acquireToken(params).get();
            logAuthResult(result);
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported action: " + action.getMethodName());
        }
    }

    /**
     * Logs the details of an authentication result.
     */
    private static void logAuthResult(IAuthenticationResult result) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Action result:");
            LOG.debug("Access Token: {}", result.accessToken());
            LOG.debug("ID Token    : {}", result.idToken());
            LOG.debug("Account     : {}", result.account());
            LOG.debug("Token Source: {}", result.metadata().tokenSource());
        }
    }

    /**
     * Validate assertions against a result.
     */
    static void validateAssertions(IAuthenticationResult result, Exception exception, Assert assertion) {
        if (assertion == null) {
            LOG.info("No assertions to validate");
            return;
        }

        Map<String, Object> assertions = assertion.getAssertions();
        assertions.forEach((key, value) -> {
            switch (key) {
                case "token_source":
                    LOG.info("Validating token source");
                    validateTokenSource((String) value, result);
                    break;
                case "error":
                    LOG.info("Validating exception");
                    validateException((String) value, exception);
                    break;
                default:
                    LOG.info("Unknown assertion type: {}", key);
                    break;
            }
        });
    }

    /**
     * Validates that an exception matches expected criteria.
     */
    static void validateException(String expectedExceptionType, Exception actualException) {
        if (StringHelper.isNullOrBlank(expectedExceptionType)) {
            assertEquals(null, actualException, "Exception was not expected but one was thrown");
            return;
        }

        if (actualException == null) {
            throw new AssertionError("Expected exception of type " + expectedExceptionType + " but none was thrown");
        }

        ExceptionExpectation expectation = getExceptionExpectation(expectedExceptionType);

        LOG.info("Validating exception - Expected type: {}, Actual: {}",
                expectation.type, actualException.getClass().getSimpleName());

        if (expectation.type != null) {
            boolean matchesType = actualException.getClass().getSimpleName().equals(expectation.type);
            assertEquals(true, matchesType,
                    "Expected exception of type " + expectation.type + " but got " + actualException.getClass().getSimpleName());
        }

        if (expectation.message != null && actualException.getMessage() != null) {
            boolean containsMessage = actualException.getMessage().contains(expectation.message);
            assertEquals(true, containsMessage,
                    "Expected exception message to contain '" + expectation.message + "' but got: " + actualException.getMessage());
        }

        if (expectation.errorCode != null && actualException instanceof MsalClientException) {
            MsalClientException msalException = (MsalClientException) actualException;
            assertEquals(expectation.errorCode, msalException.errorCode(),
                    "Expected error code '" + expectation.errorCode + "' but got: " + msalException.errorCode());
        }
    }

    /**
     * Maps exception type identifiers to expected exception details.
     */
    private static class ExceptionExpectation {
        final String type;
        final String message;
        final String errorCode;

        ExceptionExpectation(String type, String message, String errorCode) {
            this.type = type;
            this.message = message;
            this.errorCode = errorCode;
        }
    }

    /**
     * Gets the expected exception details for a given exception type identifier.
     */
    private static ExceptionExpectation getExceptionExpectation(String exceptionType) {
        switch (exceptionType) {
            case "invalid_issuer":
                return new ExceptionExpectation(
                        "MsalClientException",
                        "Invalid issuer from OIDC discovery",
                        "issuer_validation");
            // Add more mappings as needed for different test cases
            default:
                LOG.info("Unknown exception type mapping: {}", exceptionType);
                return new ExceptionExpectation(null, null, null);
        }
    }

    /**
     * Creates managed identity ID from test object properties.
     */
    static ManagedIdentityId createManagedIdentityId(Arrange.ObjectProperties appObjectProperties) {
        Map<String, Object> managedIdentityMap = (Map<String, Object>) appObjectProperties.getProperty("managed_identity");
        String idType = (String) managedIdentityMap.get("ManagedIdentityIdType");
        String id = (String) managedIdentityMap.get("Id");

        switch (idType) {
            case "SystemAssigned":
                return ManagedIdentityId.systemAssigned();
            case "ClientId":
                return ManagedIdentityId.userAssignedClientId(id);
            case "ObjectId":
                return ManagedIdentityId.userAssignedObjectId(id);
            case "ResourceId":
                return ManagedIdentityId.userAssignedResourceId(id);
            default:
                throw new IllegalArgumentException("Unsupported ManagedIdentityIdType: " + idType);
        }
    }

    /**
     * Extracts client capabilities from application object properties.
     */
    static List<String> extractClientCapabilities(Arrange.ObjectProperties testObjectProperties) {
        List<String> capabilities = new ArrayList<>();
        Object capabilitiesObj = testObjectProperties.getProperty("client_capabilities");

        if (capabilitiesObj instanceof List) {
            List<?> capabilitiesList = (List<?>) capabilitiesObj;
            for (Object capability : capabilitiesList) {
                capabilities.add((String) capability);
            }
        }

        return capabilities;
    }

    /**
     * Creates a provider for environment variables using the test configuration.
     */
    static IEnvironmentVariables setEnvironmentVariables(TestCase testCase) {
        final Map<String, Object> envVarsObj = testCase.getEnv().getVariables();
        final Map<String, String> envVars = new HashMap<>();

        // Convert Object values to String
        for (Map.Entry<String, Object> entry : envVarsObj.entrySet()) {
            envVars.put(entry.getKey(), entry.getValue().toString());
        }

        LOG.info("Configured environment variables: {}", envVars.keySet());

        return new IEnvironmentVariables() {
            @Override
            public String getEnvironmentVariable(String envVariable) {
                return envVars.get(envVariable);
            }
        };
    }

    /**
     * Builds parameters for managed identity token acquisition
     */
    static ManagedIdentityParameters buildManagedIdentityParameters(Act action) {
        String resource = (String) action.getParameters().get("resource");
        LOG.info("Building ManagedIdentityParameters with resource: {}", resource);

        ManagedIdentityParameters.ManagedIdentityParametersBuilder builder =
                ManagedIdentityParameters.builder(resource);

        // Add optional claims challenge
        if (action.getParameters().containsKey("claims_challenge")) {
            String claimsChallenge = (String) action.getParameters().get("claims_challenge");

            // Convert simple string to JSON format if needed
            if (!claimsChallenge.startsWith("{") && !claimsChallenge.endsWith("}")) {
                claimsChallenge = "{\"" + claimsChallenge + "\":\"" + claimsChallenge + "\"}";
                LOG.info("Converting simple string claim to JSON format: {}", claimsChallenge);
            }

            builder.claims(claimsChallenge);
        }

        return builder.build();
    }

    /**
     * Builds parameters for client credential token acquisition.
     */
    static ClientCredentialParameters buildClientCredentialParameters(Act action) {
        Set<String> scopes = new HashSet<>();
        if (action.getParameters().containsKey("scopes")) {
            Object scopesObj = action.getParameters().get("scopes");
            if (scopesObj instanceof List) {
                List<?> scopesList = (List<?>) scopesObj;
                for (Object scope : scopesList) {
                    scopes.add((String) scope);
                }
            }
        }

        LOG.info("Building ClientCredentialParameters with scopes: {}", scopes);
        return ClientCredentialParameters.builder(scopes).build();
    }

    /**
     * Validates token source assertion, either cache or identity provider.
     */
    static void validateTokenSource(String expectedSource, IAuthenticationResult result) {
        TokenSource expected = "identity_provider".equals(expectedSource) ?
                TokenSource.IDENTITY_PROVIDER : TokenSource.CACHE;

        LOG.info("Token source - Expected: {}, Actual: {}", expected, result.metadata().tokenSource());
        assertEquals(expected, result.metadata().tokenSource());
    }
}