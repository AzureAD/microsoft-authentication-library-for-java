// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.Set;

class JsonHelper {
    static ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    static <T> T convertJsonToObject(final String json, final Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new MsalClientException(e);
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
        } catch (JsonProcessingException e) {
            throw new MsalClientException(e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
        }

        mergeJSONNode(mainJson, addJson);

        return mainJson != null ? mainJson.toString() : null;
    }

    /**
     * Merges set of given JSON strings into one Jackson JsonNode object, which is returned as a String
     */
    static String mergeJSONString(Set<String> jsonStrings) {
        JsonNode mainJson = null;
        JsonNode addJson;

        Iterator<String> jsons = jsonStrings.iterator();
        try {
            if (jsons.hasNext()) {
                mainJson = mapper.readTree(jsons.next());
            }
        } catch (JsonProcessingException e) {
            throw new MsalClientException(e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
        }

        while (jsons.hasNext()) {
            try {
                addJson = mapper.readTree(jsons.next());
            } catch (JsonProcessingException e) {
                throw new MsalClientException(e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
            }
            mergeJSONNode(mainJson, addJson);
        }

        return mainJson != null ? mainJson.toString() : null;
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
