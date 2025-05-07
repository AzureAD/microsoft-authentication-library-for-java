// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.ReadValueCallback;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

class JsonHelper {
    static ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private JsonHelper() {
    }

    static <T> T convertJsonToObject(final String json, final Class<T> tClass) {
        try {
            return mapper.readValue(json, tClass);
        } catch (Exception e) {
            throw new MsalJsonParsingException(e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
        }
    }

    static IdToken createIdTokenFromEncodedTokenString(String token) {
        String idTokenJson;
        try {
            idTokenJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new MsalClientException("Error parsing ID token, missing payload section.",
                    AuthenticationErrorCode.INVALID_JWT);
        }

        return JsonHelper.convertJsonToObject(idTokenJson, IdToken.class);
    }

    //This method is used to convert a JSON string to an object which implements the JsonSerializable interface from com.azure.json
    static <T extends JsonSerializable<T>> T convertJsonStringToJsonSerializableObject(String jsonResponse, ReadValueCallback<JsonReader, T> readFunction) {
        try (JsonReader jsonReader = JsonProviders.createReader(jsonResponse)) {
            return readFunction.read(jsonReader);
        } catch (Exception e) {
            throw new MsalJsonParsingException(e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
        }
    }

    //Converts a JSON string to a Map<String, String>
    static Map<String, String> convertJsonToMap(String jsonString) {
        try (JsonReader reader = JsonProviders.createReader(jsonString)) {
            reader.nextToken();
            return reader.readMap(JsonReader::getString);
        } catch (IOException e) {
            throw new MsalClientException("Could not parse JSON from HttpResponse body: " + e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
        }
    }

    /**
     * Throws exception if given String does not follow JSON syntax
     */
    static void validateJsonFormat(String jsonString) {
        try {
            mapper.readTree(jsonString);
        } catch (IOException e) {
            throw new MsalClientException(e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
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
     * Merges given JSON strings into one Jackson JSONNode object, which is returned as a String
     */
    static String mergeJSONString(String mainJsonString, String addJsonString) {
        JsonNode mainJson;
        JsonNode addJson;

        try {
            mainJson = mapper.readTree(mainJsonString);
            addJson = mapper.readTree(addJsonString);
        } catch (IOException e) {
            throw new MsalClientException(e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
        }

        mergeJSONNode(mainJson, addJson);

        return mainJson.toString();
    }

    /**
     * Merges given Jackson JsonNode object into another JsonNode
     */
    static void mergeJSONNode(JsonNode mainNode, JsonNode addNode) {
        if (addNode == null) {
            return;
        }

        Iterator<String> fieldNames = addNode.fieldNames();
        while (fieldNames.hasNext()) {

            String fieldName = fieldNames.next();
            JsonNode jsonNode = mainNode.get(fieldName);

            if (jsonNode != null && jsonNode.isObject()) {
                mergeJSONNode(jsonNode, addNode.get(fieldName));
            } else {
                if (mainNode instanceof ObjectNode) {
                    JsonNode value = addNode.get(fieldName);
                    ((ObjectNode) mainNode).put(fieldName, value);
                }
            }
        }
    }
}
