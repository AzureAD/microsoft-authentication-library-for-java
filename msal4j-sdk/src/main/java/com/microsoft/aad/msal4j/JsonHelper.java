// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.ReadValueCallback;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

class JsonHelper {
    private static final Logger LOG = LoggerFactory.getLogger(JsonHelper.class);

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
            LOG.error(String.format("Error converting JSON string into %s: %s", tClass, e.getMessage()));
            throw new MsalJsonParsingException(e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
        }
    }

    static IdToken createIdTokenFromEncodedTokenString(String token) {
        return JsonHelper.convertJsonToObject(getTokenPayloadClaims(token), IdToken.class);
    }

    static String getTokenPayloadClaims(String token) {
        try {
            return new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        } catch (ArrayIndexOutOfBoundsException e) {
            LOG.error("Error parsing ID token, missing payload section.");
            throw new MsalClientException("Error parsing ID token, missing payload section.",
                    AuthenticationErrorCode.INVALID_JWT);
        }
    }

    //Converts a generic JSON string to a Map<String, Object> with relevant types
    static Map<String, Object> parseJsonToMap(String jsonString) {
        if (StringHelper.isBlank(jsonString)) {
            return new HashMap<>();
        }

        try (JsonReader jsonReader = JsonProviders.createReader(jsonString)) {
            jsonReader.nextToken();
            return parseJsonObject(jsonReader);
        } catch (IOException e) {
            LOG.error("JSON parsing error when attempting to convert JSON into a Map.");
            throw new MsalJsonParsingException(e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
        }
    }

    private static Map<String, Object> parseJsonObject(JsonReader jsonReader) throws IOException {
        Map<String, Object> object = new HashMap<>();

        while (jsonReader.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jsonReader.getFieldName();
            Object value = parseValue(jsonReader);
            object.put(fieldName, handleSpecialFields(fieldName, value));
        }

        return object;
    }

    //Due to the old usage of com.nimbusds for JWT parsing, customers may be relying on certain fields being treated as specific types.
    // This method handles those special cases to help ensure backwards compatibility.
    private static Object handleSpecialFields(String fieldName, Object value) {
        //nimbus always treated the "aud" field as an ArrayList, even when it was a single string
        if ("aud".equals(fieldName) && value instanceof String) {
            ArrayList<String> list = new ArrayList<>();
            list.add((String) value);
            return list;
        }

        //nimbus converted certain unix timestamps to Date objects
        if (isTimestampField(fieldName) && value instanceof Number) {
            // Convert seconds to milliseconds for Date constructor
            return new Date(((Number) value).longValue() * 1000);
        }

        return value;
    }

    private static boolean isTimestampField(String fieldName) {
        return "exp".equals(fieldName) || "iat".equals(fieldName) ||
                "nbf".equals(fieldName);
    }

    private static Object parseValue(JsonReader jsonReader) throws IOException {
        JsonToken token = jsonReader.currentToken();

        switch (token) {
            case STRING:
                return jsonReader.getString();
            case NUMBER:
                try {
                    return jsonReader.getLong();
                } catch (ArithmeticException e) {
                    return jsonReader.getDouble();
                }
            case BOOLEAN:
                return jsonReader.getBoolean();
            case NULL:
                return null;
            case START_ARRAY:
                return jsonReader.readArray(JsonReader::readUntyped);
            case START_OBJECT:
                return parseJsonObject(jsonReader);
            default:
                jsonReader.skipChildren();
                return null;
        }
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
