// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

class JsonHelper {

    private JsonHelper() {
    }

    /**
     * Throws exception if given String does not follow JSON syntax
     */
    static void validateJsonFormat(String jsonString) {
        try {
            new JSONObject(jsonString);
        } catch (Exception ex) {
            try{
                new JSONArray(jsonString);
            }catch(Exception e){
                throw new MsalClientException(e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
            }
        }
    }

    /**
     * Take a set of Strings and return a String representing a JSON object of the format:
     *  {
     *    "access_token": {
     *      "xms_cc": {
     *        "values": [ clientCapabilities ]
     *      }
     *    }
     *  }
     */
    public static String formCapabilitiesJson(Set<String> clientCapabilities) {
        if (clientCapabilities != null && !clientCapabilities.isEmpty()) {
            ClaimsRequest cr = new ClaimsRequest();
            RequestedClaimAdditionalInfo capabilitiesValues = new RequestedClaimAdditionalInfo(false, null, new ArrayList<>(clientCapabilities));
            cr.requestClaimInAccessToken("xms_cc", capabilitiesValues);

            return cr.formatAsJSONString();
        } else {
            return null;
        }
    }

    /**
     * Merges given JSONObject strings
     */
    static String mergeJSONString(String mainJsonString, String addJsonString) {

        JSONObject mainjsonObject = new JSONObject(mainJsonString);
        JSONObject addJsonObject = new JSONObject(addJsonString);
        if(addJsonObject.isEmpty() || mainjsonObject.isEmpty()) return addJsonString;
            try {

                mergeRemovals(mainjsonObject, addJsonObject);
                mergeUpdates(mainjsonObject, addJsonObject);
            } catch (JSONException e) {

                // RunttimeException: Constructs a new runtime exception with the specified detail message.
                // The cause is not initialized, and may subsequently be initialized by a call to initCause.
                throw new RuntimeException("JSON Exception" + e);
            }

            return mainjsonObject.toString();
        }

    private static void mergeRemovals(JSONObject old, JSONObject update) {
        Set<String> keySet = old.keySet();
        for (String key : keySet) {
            JSONObject oldEntries = old.has(key) ? (JSONObject) old.get(key) : null;
            JSONObject newEntries = update.has(key) ? (JSONObject) update.get(key) : null;
            if (oldEntries != null) {
                Iterator<String> iterator = oldEntries.keys();

                while (iterator.hasNext()) {
                    String innerKey = iterator.next();
                    if (newEntries != null && !newEntries.has(innerKey)) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    private static void mergeUpdates(JSONObject old, JSONObject update) {
        Iterator<String> fieldNames = update.keys();
        while (fieldNames.hasNext()) {
            String uKey = fieldNames.next();
            Object uValue = update.get(uKey);

            // add new property
            if (!old.has(uKey)) {
                if (uValue!=null &&
                        !(uValue instanceof JSONObject && ((JSONObject) uValue).isEmpty())) {
                    old.put(uKey, uValue);
                }
            }

            // merge old and new property
            else {
                Object oValue = old.get(uKey);
                if (uValue instanceof JSONObject) {
                    mergeUpdates((JSONObject) oValue, (JSONObject) uValue);
                } else {
                    old.put(uKey, uValue);
                }
            }
        }
    }

}
