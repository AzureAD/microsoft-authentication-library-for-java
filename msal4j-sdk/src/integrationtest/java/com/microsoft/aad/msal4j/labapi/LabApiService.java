// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.labapi.LabServiceParameters.MFA;
import com.microsoft.aad.msal4j.labapi.LabServiceParameters.ProtectionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wrapper for lab service API interactions.
 */
class LabApiService {

    private static final Logger log = LoggerFactory.getLogger(LabApiService.class);

    private ConfidentialClientApplication labApp;
    private final ConcurrentHashMap<UserQueryHelper, LabResponse> userCache = new ConcurrentHashMap<>();

    /**
     * Get lab user data with caching support.
     *
     * @param query The UserQueryHelper to search for
     * @return LabResponse
     */
    LabResponse getLabUserData(UserQueryHelper query) {
        if (userCache.containsKey(query)) {
            LabResponse cached = userCache.get(query);
            log.debug("Lab cache hit: {}",
                    cached.getUser() != null ? cached.getUser().getUpn() : "N/A");
            return cached;
        }

        LabResponse response = getLabResponseFromApi(query);

        if (response == null) {
            log.error("No lab user found for query");
            throw new LabUserNotFoundException(query, "Found no users for the given query.");
        }

        log.info("Lab API returned user: {}",
                response.getUser() != null ? response.getUser().getUpn() : "N/A");

        userCache.put(query, response);
        return response;
    }

    /**
     * Returns a test user account for use in testing.
     *
     * @param query Any and all parameters that the returned user should satisfy
     * @return LabResponse with user that matches the query
     */
    private LabResponse getLabResponseFromApi(UserQueryHelper query) {
        log.debug("Querying lab API for user with parameters: {}", query);

        try {
            String result = runQuery(query);

            if (result == null || result.trim().isEmpty()) {
                log.error("No lab user found for query");
                throw new LabUserNotFoundException(query,
                        "No lab user with specified parameters exists");
            }
            log.debug("Lab API returned {} characters of data", result.length());

            LabResponse response = createLabResponseFromResultString(result);

            if (response != null && response.getUser() != null) {
                log.info("Successfully retrieved lab user: {}", response.getUser().getUpn());
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to get lab response: {}", e.getMessage());
            throw new RuntimeException("Failed to get lab response", e);
        }
    }

    /**
     * Create a LabResponse object from JSON result string.
     *
     * @param result JSON string containing lab user array
     * @return Populated LabResponse
     */
    LabResponse createLabResponseFromResultString(String result) {
        try {
            log.debug("Parsing lab user JSON response");

            List<LabUser> userResponses;
            try (JsonReader jsonReader = JsonProviders.createReader(result)) {
                userResponses = jsonReader.readArray(LabUser::fromJson);
            }

            if (userResponses.isEmpty()) {
                log.error("Lab API returned empty user array");
                throw new RuntimeException("Lab API returned empty user array");
            }

            LabUser user = userResponses.get(0);
            log.debug("Parsed lab user: {}, fetching app and lab info", user.getUpn());

            // Fetch app and lab info
            String appEndpoint = LabConstants.LAB_APP_ENDPOINT + user.getAppId();
            log.debug("Fetching app info from: {}", appEndpoint);
            String appResponse = getLabResponse(appEndpoint);

            List<LabApp> labApps;
            try (JsonReader appReader = JsonProviders.createReader(appResponse)) {
                labApps = appReader.readArray(LabApp::fromJson);
            }

            if (labApps.isEmpty()) {
                log.error("No lab app found for appId: {}", user.getAppId());
                throw new RuntimeException("No lab app found for appId: " + user.getAppId());
            }
            log.debug("Successfully parsed lab app: {}", labApps.get(0).getAppId());

            String labInfoEndpoint = LabConstants.LAB_INFO_ENDPOINT + user.getLabName();
            log.debug("Fetching lab info from: {}", labInfoEndpoint);
            String labInfoResponse = getLabResponse(labInfoEndpoint);

            List<Lab> labs;
            try (JsonReader labReader = JsonProviders.createReader(labInfoResponse)) {
                labs = labReader.readArray(Lab::fromJson);
            }

            if (labs.isEmpty()) {
                log.error("No lab info found for labName: {}", user.getLabName());
                throw new RuntimeException("No lab info found for labName: " + user.getLabName());
            }
            log.debug("Successfully parsed lab info for: {}", user.getLabName());

            LabResponse response = new LabResponse();
            response.setUser(user);
            response.setApp(labApps.get(0));
            response.setLab(labs.get(0));

            log.info("Successfully created LabResponse for user: {}", user.getUpn());
            return response;

        } catch (Exception e) {
            log.error("Failed to create LabResponse from API result: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create LabResponse from API result", e);
        }
    }

    /**
     * Execute a query against the lab API.
     *
     * @param query UserQueryHelper parameters
     * @return JSON response string
     */
    private String runQuery(UserQueryHelper query) {
        Map<String, String> queryDict = new HashMap<>();

        // Building user query - required parameters set to defaults if not supplied
        queryDict.put(
                LabConstants.MULTI_FACTOR_AUTHENTICATION,
                MFA.NONE.toString()
        );

        queryDict.put(
                LabConstants.PROTECTION_POLICY,
                ProtectionPolicy.NONE.toString()
        );

        if (query.getUserType() != null) {
            queryDict.put(LabConstants.USER_TYPE, query.getUserType().toString());
        }

        if (query.getB2cIdentityProvider() != null) {
            queryDict.put(LabConstants.B2C_PROVIDER,
                    query.getB2cIdentityProvider().toString());
        }

        if (query.getFederationProvider() != null) {
            queryDict.put(LabConstants.FEDERATION_PROVIDER,
                    query.getFederationProvider().toString());
        }

        if (query.getSignInAudience() != null) {
            queryDict.put(LabConstants.SIGN_IN_AUDIENCE,
                    query.getSignInAudience().toString());
        }

        if (query.getAzureEnvironment() != null) {
            queryDict.put(LabConstants.AZURE_ENVIRONMENT,
                    query.getAzureEnvironment());
        }

        return sendLabRequest(LabConstants.LAB_ENDPOINT, queryDict);
    }

    /**
     * Send HTTP request to lab API with query parameters.
     *
     * @param requestUrl Base URL for the request
     * @param queryDict  Query parameters
     * @return Response string
     */
    private String sendLabRequest(String requestUrl, Map<String, String> queryDict) {
        try {
            log.debug("Acquiring lab access token");
            String accessToken = getLabAccessToken();
            log.debug("Sending lab request to: {} with {} query parameters", requestUrl, queryDict.size());
            String response = LabHttpHelper.sendRequestToLab(requestUrl, queryDict, accessToken);
            log.debug("Lab request successful, received {} characters", response.length());
            return response;
        } catch (IOException e) {
            log.error("Failed to send lab request to {}: {}", requestUrl, e.getMessage());
            throw new RuntimeException("Failed to send lab request", e);
        }
    }

    private void initLabApp() {
        if (labApp == null) {
            log.debug("Initializing lab app");
            KeyVaultSecretsProvider keyVaultSecretsProvider = new KeyVaultSecretsProvider();

            String appID = keyVaultSecretsProvider.getSecretByName("LabVaultAppID").getValue();
            log.debug("Retrieved lab app ID from Key Vault");

            try {
                labApp = ConfidentialClientApplication.builder(
                                appID,
                                keyVaultSecretsProvider.getClientCredentialFromKeyStore())
                        .authority(LabConstants.MICROSOFT_AUTHORITY)
                        .build();
                log.debug("Lab app initialized successfully");
            } catch (Exception e) {
                log.error("Error initializing lab app: {}", e.getMessage(), e);
                throw new RuntimeException("Error initializing lab app: " + e.getMessage());
            }
        }
    }

    private String getLabAccessToken() {
        try {
            initLabApp();

            log.debug("Acquiring lab access token");
            IAuthenticationResult result = labApp.acquireToken(
                    ClientCredentialParameters
                            .builder(Collections.singleton(LabConstants.LAB_API_SCOPE))
                            .build()
            ).get();

            log.debug("Successfully acquired lab access token");
            return result.accessToken();

        } catch (Exception e) {
            log.error("Error acquiring lab access token: {}", e.getMessage(), e);
            throw new RuntimeException("Error acquiring lab access token: " + e.getMessage());
        }
    }

    /**
     * Execute HTTP GET request to lab API endpoint.
     *
     * @param address Full URL to request
     * @return Response body as string
     */
    String getLabResponse(String address) {
        try {
            log.debug("Getting lab response from: {}", address);
            String accessToken = getLabAccessToken();
            String response = LabHttpHelper.sendRequestToLab(address, accessToken);
            log.debug("Successfully retrieved lab response: {} characters", response.length());
            return response;
        } catch (IOException e) {
            log.error("Failed to get lab response from {}: {}", address, e.getMessage());
            throw new RuntimeException("Failed to get lab response from: " + address, e);
        }
    }
}