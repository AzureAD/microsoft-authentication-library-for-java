// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.aad.msal4j.*;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class LabService {

    static ConfidentialClientApplication labApp;

    static ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    static <T> T convertJsonToObject(final String json, final Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JsonProcessingException: " + e.getMessage(), e);
        }
    }

    static void initLabApp() throws MalformedURLException {
        KeyVaultSecretsProvider keyVaultSecretsProvider = new KeyVaultSecretsProvider();

        String appID = keyVaultSecretsProvider.getSecret(LabConstants.APP_ID_KEY_VAULT_SECRET);
        String appSecret = keyVaultSecretsProvider.getSecret(LabConstants.APP_PASSWORD_KEY_VAULT_SECRET);

        labApp = ConfidentialClientApplication.builder(
                appID, ClientCredentialFactory.createFromSecret(appSecret)).
                authority(TestConstants.MICROSOFT_AUTHORITY).
                build();
    }

    static String getLabAccessToken() throws MalformedURLException, ExecutionException, InterruptedException {
        if(labApp == null){
            initLabApp();
        }
        return labApp.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(TestConstants.MSIDLAB_DEFAULT_SCOPE))
                .build()).
                get().accessToken();
    }

    User getUser(UserQueryParameters query){
        try {
            Map<String, String> queryMap = query.parameters;
            String result = HttpClientHelper.sendRequestToLab(
                    LabConstants.LAB_USER_ENDPOINT, queryMap, getLabAccessToken());

            User[] users = convertJsonToObject(result, User[].class);
            User user = users[0];
            user.setPassword(getUserSecret(user.getLabName()));
            if (query.parameters.containsKey(UserQueryParameters.FEDERATION_PROVIDER)) {
                user.setFederationProvider(query.parameters.get(UserQueryParameters.FEDERATION_PROVIDER));
            } else {
                user.setFederationProvider(FederationProvider.NONE);
            }
            return user;
        } catch (Exception ex) {
            throw new RuntimeException("Error getting user from lab: " + ex.getMessage());
        }
    }

    public static App getApp(String appId){
        try {
            String result = HttpClientHelper.sendRequestToLab(
                    LabConstants.LAB_APP_ENDPOINT, appId, getLabAccessToken());
            App[] apps = convertJsonToObject(result, App[].class);
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
            Lab[] labs = convertJsonToObject(result, Lab[].class);
            return labs[0];
        } catch (Exception ex) {
            throw new RuntimeException("Error getting lab from lab: " + ex.getMessage());
        }
    }

    private String getUserSecret(String labName){
        String result;
        try {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("secret", labName);
            result = HttpClientHelper.sendRequestToLab(
                    LabConstants.LAB_USER_SECRET_ENDPOINT, queryMap, getLabAccessToken());

            return convertJsonToObject(result, UserSecret.class).value;
        } catch (Exception ex) {
            throw new RuntimeException("Error getting user secret from lab: " + ex.getMessage());
        }
    }
}
