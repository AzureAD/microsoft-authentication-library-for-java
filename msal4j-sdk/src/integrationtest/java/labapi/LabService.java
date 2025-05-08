// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.azure.json.*;
import com.microsoft.aad.msal4j.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class LabService {

    static ConfidentialClientApplication labApp;

    static void initLabApp() throws MalformedURLException {
        KeyVaultSecretsProvider keyVaultSecretsProvider = new KeyVaultSecretsProvider();

        String appID = keyVaultSecretsProvider.getSecret(LabConstants.APP_ID_KEY_VAULT_SECRET);

        labApp = ConfidentialClientApplication.builder(
                appID, keyVaultSecretsProvider.getClientCredentialFromKeyStore()).
                authority(TestConstants.MICROSOFT_AUTHORITY).
                build();
    }

    static String getLabAccessToken() throws MalformedURLException, ExecutionException, InterruptedException {
        if (labApp == null) {
            initLabApp();
        }
        return labApp.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(TestConstants.MSIDLAB_DEFAULT_SCOPE))
                .build()).
                get().accessToken();
    }

    User getUser(UserQueryParameters query) {
        try {
            Map<String, String> queryMap = query.parameters;
            String result = HttpClientHelper.sendRequestToLab(
                    LabConstants.LAB_USER_ENDPOINT, queryMap, getLabAccessToken());

            User[] users = parseUserArray(result);
            User user = users[0];
            if (user.getUserType().equals("Guest")) {
                String secretId = user.getHomeDomain().split("\\.")[0];
                user.setPassword(getSecret(secretId));
            } else {
                user.setPassword(getSecret(user.getLabName()));
            }
            user.setFederationProvider(query.parameters.getOrDefault(UserQueryParameters.FEDERATION_PROVIDER, FederationProvider.NONE));
            return user;
        } catch (Exception ex) {
            throw new RuntimeException("Error getting user from lab: " + ex.getMessage());
        }
    }

    public static App getApp(String appId) {
        try {
            String result = HttpClientHelper.sendRequestToLab(
                    LabConstants.LAB_APP_ENDPOINT, appId, getLabAccessToken());
            App[] apps = parseAppArray(result);
            return apps[0];
        } catch (Exception ex) {
            throw new RuntimeException("Error getting app from lab: " + ex.getMessage());
        }
    }

    public static Lab getLab(String labId) {
        String result;
        try {
            result = HttpClientHelper.sendRequestToLab(
                    LabConstants.LAB_LAB_ENDPOINT, labId, getLabAccessToken());
            Lab[] labs = parseLabArray(result);
            return labs[0];
        } catch (Exception ex) {
            throw new RuntimeException("Error getting lab from lab: " + ex.getMessage());
        }
    }

    public static String getSecret(String labName) {
        String result;
        try {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("secret", labName);
            result = HttpClientHelper.sendRequestToLab(
                    LabConstants.LAB_USER_SECRET_ENDPOINT, queryMap, getLabAccessToken());

            UserSecret userSecret = parseUserSecret(result);
            return userSecret.value;
        } catch (Exception ex) {
            throw new RuntimeException("Error getting user secret from lab: " + ex.getMessage());
        }
    }

    // Helper methods for parsing JSON responses into specific objects
    private static User[] parseUserArray(String json) {
        try (JsonReader reader = JsonProviders.createReader(json)) {
            reader.nextToken();
            return reader.readArray(User::fromJson).toArray(new User[0]);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing User array: " + e.getMessage(), e);
        }
    }

    private static App[] parseAppArray(String json) {
        try (JsonReader reader = JsonProviders.createReader(json)) {
            reader.nextToken();
            return reader.readArray(App::fromJson).toArray(new App[0]);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing App array: " + e.getMessage(), e);
        }
    }

    private static Lab[] parseLabArray(String json) {
        try (JsonReader reader = JsonProviders.createReader(json)) {
            reader.nextToken();
            return reader.readArray(Lab::fromJson).toArray(new Lab[0]);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing Lab array: " + e.getMessage(), e);
        }
    }

    private static UserSecret parseUserSecret(String json) {
        try (JsonReader reader = JsonProviders.createReader(json)) {
            return UserSecret.fromJson(reader);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing UserSecret: " + e.getMessage(), e);
        }
    }
}