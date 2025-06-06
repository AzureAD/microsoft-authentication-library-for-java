package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.aad.msal4j.ManagedIdentitySourceType.SERVICE_FABRIC;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.aad.msal4j.Shortcuts.TestConfig;
import com.microsoft.aad.msal4j.Shortcuts.TestObject;
import com.microsoft.aad.msal4j.Shortcuts.TestAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunnerHelper {
    private static final Logger LOG = LoggerFactory.getLogger(RunnerHelper.class);

    /**
     * Create Managed Identity applications from the test configuration.
     * This method processes the "arrange" section of the test configuration.
     */
    static Map<String, ManagedIdentityApplication> createAppsFromConfig(TestConfig config) {
        Map<String, ManagedIdentityApplication> apps = new HashMap<>();

        for (String appName : config.getAllArrangeObjects()) {
            TestObject appObject = config.getArrangeObject(appName);
            if ("ManagedIdentityClient".equals(appObject.getType())) {
                ManagedIdentityId identityId = createManagedIdentityId(appObject);
                List<String> capabilities = extractClientCapabilities(appObject);
                IEnvironmentVariables envVars = createEnvironmentVariables(config);
                // TODO: other application properties

                ManagedIdentityApplication app = ManagedIdentityApplication.builder(identityId)
                        .clientCapabilities(capabilities)
                        .build();

                ManagedIdentityApplication.setEnvironmentVariables(envVars);

                apps.put(appName, app);
            } //TODO: Confidential and public clients
        }

        return apps;
    }

    /**
     * Execute an action and return the result
     * This method uses the "act" section of the test configuration.
     */
    static IAuthenticationResult executeAction(ManagedIdentityApplication app, TestAction action) throws Exception {
        if (action.getMethodName().equals("AcquireTokenForManagedIdentity")) {
            LOG.info(String.format("Executing action: %s", action.getMethodName()));

            ManagedIdentityParameters params = buildManagedIdentityParameters(action);

            IAuthenticationResult result = app.acquireTokenForManagedIdentity(params).get();

            LOG.info("Action result:");
            LOG.info(String.format("Access Token: %s", result.accessToken()));
            LOG.info(String.format("ID Token    : %s", result.idToken()));
            LOG.info(String.format("Account     : %s", result.account()));
            LOG.info(String.format("Token Source: %s", result.metadata().tokenSource()));

            return result;
        } else {
            //TODO: other token calls and apps
            throw new UnsupportedOperationException("Unsupported action: " + action.getMethodName());
        }
    }

    /**
     * Validate assertions against a result.
     * This method uses the "assert" section of the test configuration.
     */
    static void validateAssertions(IAuthenticationResult result, Map<String, JsonNode> assertions) {
        assertions.forEach((key, value) -> {
            switch (key) {
                case "token_source":
                    LOG.info("Validating token source");
                    validateTokenSource(value.asText(), result);
                    break;
                //TODO: other assertions
                default:
                    // Optional: Handle unknown assertion types
                    break;
            }
        });
    }

    /**
     * Create managed identity ID from test object
     */
    static ManagedIdentityId createManagedIdentityId(TestObject appObject) {
        String idType = appObject.getProperty("managed_identity").get("ManagedIdentityIdType").asText();

        if ("SystemAssigned".equals(idType)) {
            return ManagedIdentityId.systemAssigned();
        } else {
            // TODO: handle user assertions
            return null;
        }
    }

    /**
     * Extract client capabilities from test object
     */
    static List<String> extractClientCapabilities(TestObject testObject) {
        List<String> capabilities = new ArrayList<>();
        JsonNode capabilitiesNode = testObject.getProperty("client_capabilities");

        if (capabilitiesNode != null && capabilitiesNode.isArray()) {
            capabilitiesNode.forEach(node -> capabilities.add(node.asText()));
        }

        LOG.info(String.format("Extracted client capabilities: %s", capabilities));

        return capabilities;
    }

    //TODO: Re-used from other Managed Identity tests, specific to this proof-of-concept but should be more generic
    static IEnvironmentVariables createEnvironmentVariables(TestConfig config) {
        return new EnvironmentVariablesHelper(
                SERVICE_FABRIC,
                config.getEnvironmentVariable("IDENTITY_ENDPOINT"));
    }

    /**
     * Build parameters for token acquisition
     */
    static ManagedIdentityParameters buildManagedIdentityParameters(TestAction action) {
        String resource = action.getParameter("resource").asText();

        LOG.info(String.format("Building ManagedIdentityParameters with resource: %s", resource));

        ManagedIdentityParameters.ManagedIdentityParametersBuilder builder =
                ManagedIdentityParameters.builder(resource);

        // Add optional claims challenge
        if (action.hasParameter("claims_challenge")) {
            builder.claims(action.getParameter("claims_challenge").asText());
        }

        //TODO: other parameters

        return builder.build();
    }

    /**
     * Validate token source assertion, either cache or identity provider
     */
    static void validateTokenSource(String expectedSource, IAuthenticationResult result) {
        TokenSource expected = "identity_provider".equals(expectedSource) ?
                TokenSource.IDENTITY_PROVIDER : TokenSource.CACHE;
        LOG.info(String.format("Expected token source: %s", expected));
        LOG.info(String.format("Actual token source  : %s", result.metadata().tokenSource()));

        assertEquals(expected, result.metadata().tokenSource());
    }
}
