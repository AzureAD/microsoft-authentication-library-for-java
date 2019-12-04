// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class LabService {

    User getUser(UserQueryParameters query){
        try {
            Map<String, String> queryMap = query.parameters;
            String result = HttpClientHelper.sendRequestToLab(LabConstants.LAB_USER_ENDPOINT, queryMap);

            User[] users = new Gson().fromJson(result, User[].class);
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
            String result = HttpClientHelper.sendRequestToLab(LabConstants.LAB_APP_ENDPOINT, appId);
            App[] apps = new Gson().fromJson(result, App[].class);
            return apps[0];
        } catch (Exception ex) {
            throw new RuntimeException("Error getting app from lab: " + ex.getMessage());
        }
    }

    public static Lab getLab(String labId){
        String result;
        try {
            result = HttpClientHelper.sendRequestToLab(LabConstants.LAB_LAB_ENDPOINT, labId);
            Lab[] labs = new Gson().fromJson(result, Lab[].class);
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
            result = HttpClientHelper.sendRequestToLab(LabConstants.LAB_USER_SECRET_ENDPOINT, queryMap);

            return new Gson().fromJson(result, UserSecret.class).value;
        } catch (Exception ex) {
            throw new RuntimeException("Error getting user secret from lab: " + ex.getMessage());
        }
    }
}
