// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import com.azure.json.ReadValueCallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

class JsonHelper {

    private JsonHelper() {
    }

    static IdToken createIdTokenFromEncodedTokenString(String token) {
        return convertJsonStringToJsonSerializableObject(getTokenPayloadClaims(token), IdToken::fromJson);
    }

    static String getTokenPayloadClaims(String token) {
        try {
            return new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new MsalClientException("Error parsing ID token, missing payload section.",
                    AuthenticationErrorCode.INVALID_JWT);
        }
    }

    static Map<String, Object> parseJsonToMap(String jsonString) {
        if (StringHelper.isBlank(jsonString)) {
            return new HashMap<>();
        }

        try (JsonReader jsonReader = JsonProviders.createReader(jsonString)) {
            jsonReader.nextToken();
            return parseJsonObject(jsonReader);
        } catch (IOException e) {
            throw new MsalJsonParsingException(e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
        }
    }

    private static List<Object> parseJsonArray(JsonReader jsonReader) throws IOException {
        List<Object> array = new ArrayList<>();

        while (jsonReader.nextToken() != JsonToken.END_ARRAY) {
            array.add(parseValue(jsonReader));
        }

        return array;
    }

    private static Map<String, Object> parseJsonObject(JsonReader jsonReader) throws IOException {
        Map<String, Object> object = new HashMap<>();

        while (jsonReader.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jsonReader.getFieldName();
            object.put(fieldName, parseValue(jsonReader));
        }

        return object;
    }

    private static Object parseValue(JsonReader jsonReader) throws IOException {
        JsonToken token = jsonReader.currentToken();

        switch (token) {
            case STRING: return jsonReader.getString();
            case NUMBER:
                try {
                    return jsonReader.getLong();
                } catch (ArithmeticException e) {
                    return jsonReader.getDouble();
                }
            case BOOLEAN: return jsonReader.getBoolean();
            case NULL: return null;
            case START_ARRAY: return parseJsonArray(jsonReader);
            case START_OBJECT: return parseJsonObject(jsonReader);
            default:
                jsonReader.skipChildren();
                return null;
        }
    }

    static <T extends JsonSerializable<T>> T convertJsonStringToJsonSerializableObject(String jsonResponse, ReadValueCallback<JsonReader, T> readFunction) {
        try (JsonReader jsonReader = JsonProviders.createReader(jsonResponse)) {
            return readFunction.read(jsonReader);
        } catch (Exception e) {
            throw new MsalJsonParsingException(e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
        }
    }

    static <T extends JsonSerializable<T>> String convertJsonSerializableObjectToString(T jsonSerializable) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JsonWriter jsonWriter = JsonProviders.createWriter(outputStream);

            jsonSerializable.toJson(jsonWriter);
            jsonWriter.flush();

            return outputStream.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new MsalClientException("Error serializing object to JSON: " + e.getMessage(),
                    AuthenticationErrorCode.INVALID_JSON);
        }
    }

    static Map<String, String> convertJsonToMap(String jsonString) {
        try (JsonReader reader = JsonProviders.createReader(jsonString)) {
            reader.nextToken();
            return reader.readMap(JsonReader::getString);
        } catch (IOException e) {
            throw new MsalClientException("Could not parse JSON from HttpResponse body: " + e.getMessage(),
                    AuthenticationErrorCode.INVALID_JSON);
        }
    }

    static void validateJsonFormat(String jsonString) {
        try (JsonReader reader = JsonProviders.createReader(jsonString)) {
            while (reader.nextToken() != JsonToken.END_DOCUMENT) {
                reader.skipChildren();
            }
        } catch (IOException e) {
            throw new MsalClientException(e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
        }
    }

    public static String formCapabilitiesJson(Set<String> clientCapabilities) {
        if (clientCapabilities == null || clientCapabilities.isEmpty()) {
            return null;
        }

        ClaimsRequest cr = new ClaimsRequest();
        RequestedClaimAdditionalInfo capabilitiesValues = new RequestedClaimAdditionalInfo(
                false, null, new ArrayList<>(clientCapabilities));
        cr.requestClaimInAccessToken("xms_cc", capabilitiesValues);

        return cr.formatAsJSONString();
    }

    static String mergeJSONString(String mainJsonString, String addJsonString) {
        try {
            Map<String, Object> mainMap = parseJsonToMap(mainJsonString);
            Map<String, Object> addMap = parseJsonToMap(addJsonString);

            mergeJsonMaps(mainMap, addMap);

            return writeJsonMap(mainMap);
        } catch (IOException e) {
            throw new MsalClientException(e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
        }
    }

    @SuppressWarnings("unchecked")
    private static void mergeJsonMaps(Map<String, Object> mainMap, Map<String, Object> addMap) {
        if (addMap == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : addMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (mainMap.containsKey(key) && mainMap.get(key) instanceof Map && value instanceof Map) {
                mergeJsonMaps((Map<String, Object>) mainMap.get(key), (Map<String, Object>) value);
            } else {
                mainMap.put(key, value);
            }
        }
    }

    static String writeJsonMap(Map<String, Object> map) throws IOException {
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = JsonProviders.createWriter(stringWriter)) {

            jsonWriter.writeStartObject();

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                jsonWriter.writeUntypedField(entry.getKey(), entry.getValue());
            }

            jsonWriter.writeEndObject();
            jsonWriter.flush();

            return stringWriter.toString();
        } catch (Exception e) {
            throw new MsalClientException("Error writing JSON map to string: " + e.getMessage(),
                    AuthenticationErrorCode.INVALID_JSON);
        }
    }
}
