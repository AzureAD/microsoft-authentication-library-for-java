// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

class ManagedIdentityParsingTest {

    // ========== ManagedIdentityResponse ==========

    @Test
    void managedIdentityResponse_fromJson_allFields() throws IOException {
        String json = "{"
                + "\"token_type\":\"Bearer\","
                + "\"access_token\":\"eyJ0eXAiOiJKV1QiLCJhbGciOi...\","
                + "\"expires_on\":\"1717430400\","
                + "\"resource\":\"https://management.azure.com/\","
                + "\"client_id\":\"00000000-0000-0000-0000-000000000000\""
                + "}";

        ManagedIdentityResponse response = parseJson(json, ManagedIdentityResponse::fromJson);

        assertEquals("Bearer", response.getTokenType());
        assertEquals("eyJ0eXAiOiJKV1QiLCJhbGciOi...", response.getAccessToken());
        assertEquals("1717430400", response.getExpiresOn());
        assertEquals("https://management.azure.com/", response.getResource());
        assertEquals("00000000-0000-0000-0000-000000000000", response.getClientId());
    }

    @Test
    void managedIdentityResponse_fromJson_partialFields() throws IOException {
        String json = "{\"access_token\":\"token\",\"expires_on\":\"123456\"}";

        ManagedIdentityResponse response = parseJson(json, ManagedIdentityResponse::fromJson);

        assertEquals("token", response.getAccessToken());
        assertEquals("123456", response.getExpiresOn());
        assertNull(response.getTokenType());
        assertNull(response.getResource());
        assertNull(response.getClientId());
    }

    @Test
    void managedIdentityResponse_fromJson_unknownFieldsSkipped() throws IOException {
        String json = "{\"access_token\":\"tok\",\"extra_field\":\"ignored\",\"nested\":{\"a\":1}}";

        ManagedIdentityResponse response = parseJson(json, ManagedIdentityResponse::fromJson);

        assertEquals("tok", response.getAccessToken());
    }

    @Test
    void managedIdentityResponse_toJson_allFields() throws IOException {
        ManagedIdentityResponse response = new ManagedIdentityResponse();
        response.tokenType = "Bearer";
        response.accessToken = "test-token";
        response.expiresOn = "9999999";
        response.resource = "https://vault.azure.net/";
        response.clientId = "client-123";

        String output = writeToJson(response);

        assertTrue(output.contains("\"token_type\":\"Bearer\""));
        assertTrue(output.contains("\"access_token\":\"test-token\""));
        assertTrue(output.contains("\"expires_on\":\"9999999\""));
        assertTrue(output.contains("\"resource\":\"https://vault.azure.net/\""));
        assertTrue(output.contains("\"client_id\":\"client-123\""));
    }

    @Test
    void managedIdentityResponse_toJson_roundTrip() throws IOException {
        String json = "{"
                + "\"token_type\":\"Bearer\","
                + "\"access_token\":\"round-trip-token\","
                + "\"expires_on\":\"12345\","
                + "\"resource\":\"https://api.example.com/\","
                + "\"client_id\":\"cid-456\""
                + "}";

        ManagedIdentityResponse original = parseJson(json, ManagedIdentityResponse::fromJson);
        String serialized = writeToJson(original);
        ManagedIdentityResponse roundTripped = parseJson(serialized, ManagedIdentityResponse::fromJson);

        assertEquals(original.getTokenType(), roundTripped.getTokenType());
        assertEquals(original.getAccessToken(), roundTripped.getAccessToken());
        assertEquals(original.getExpiresOn(), roundTripped.getExpiresOn());
        assertEquals(original.getResource(), roundTripped.getResource());
        assertEquals(original.getClientId(), roundTripped.getClientId());
    }

    @Test
    void managedIdentityResponse_defaultConstructor_allNull() {
        ManagedIdentityResponse response = new ManagedIdentityResponse();

        assertNull(response.getTokenType());
        assertNull(response.getAccessToken());
        assertNull(response.getExpiresOn());
        assertNull(response.getResource());
        assertNull(response.getClientId());
    }

    // ========== ManagedIdentityErrorResponse ==========

    @Test
    void managedIdentityErrorResponse_fromJson_simpleError() throws IOException {
        String json = "{"
                + "\"error\":\"invalid_resource\","
                + "\"error_description\":\"The resource requested is invalid.\","
                + "\"message\":\"Identity not found\","
                + "\"correlationId\":\"corr-789\""
                + "}";

        ManagedIdentityErrorResponse response = parseJson(json, ManagedIdentityErrorResponse::fromJson);

        assertEquals("invalid_resource", response.getError());
        assertEquals("The resource requested is invalid.", response.getErrorDescription());
        assertEquals("Identity not found", response.getMessage());
        assertEquals("corr-789", response.getCorrelationId());
    }

    @Test
    void managedIdentityErrorResponse_fromJson_nestedErrorObject() throws IOException {
        // Some MI endpoints return error as a nested JSON object with code/message
        String json = "{"
                + "\"error\":{\"code\":\"ManagedIdentityCredential\",\"message\":\"Managed identity unavailable\"},"
                + "\"correlationId\":\"corr-nested\""
                + "}";

        ManagedIdentityErrorResponse response = parseJson(json, ManagedIdentityErrorResponse::fromJson);

        assertEquals("ManagedIdentityCredential", response.getError());
        assertEquals("Managed identity unavailable", response.getMessage());
        assertEquals("corr-nested", response.getCorrelationId());
    }

    @Test
    void managedIdentityErrorResponse_fromJson_errorAsString() throws IOException {
        String json = "{\"error\":\"unauthorized_client\"}";

        ManagedIdentityErrorResponse response = parseJson(json, ManagedIdentityErrorResponse::fromJson);

        assertEquals("unauthorized_client", response.getError());
    }

    @Test
    void managedIdentityErrorResponse_fromJson_unknownFieldsSkipped() throws IOException {
        String json = "{\"error\":\"test\",\"unknown_field\":42,\"nested_unknown\":{\"a\":1}}";

        ManagedIdentityErrorResponse response = parseJson(json, ManagedIdentityErrorResponse::fromJson);

        assertEquals("test", response.getError());
    }

    @Test
    void managedIdentityErrorResponse_fromJson_emptyObject() throws IOException {
        String json = "{}";

        ManagedIdentityErrorResponse response = parseJson(json, ManagedIdentityErrorResponse::fromJson);

        assertNull(response.getError());
        assertNull(response.getErrorDescription());
        assertNull(response.getMessage());
        assertNull(response.getCorrelationId());
    }

    @Test
    void managedIdentityErrorResponse_toJson_allFields() throws IOException {
        String json = "{"
                + "\"message\":\"Identity not found\","
                + "\"correlationId\":\"c-1\","
                + "\"error\":\"not_found\","
                + "\"error_description\":\"desc\""
                + "}";

        ManagedIdentityErrorResponse response = parseJson(json, ManagedIdentityErrorResponse::fromJson);
        String output = writeToJson(response);

        assertTrue(output.contains("\"message\":\"Identity not found\""));
        assertTrue(output.contains("\"correlationId\":\"c-1\""));
        assertTrue(output.contains("\"error\":\"not_found\""));
        assertTrue(output.contains("\"error_description\":\"desc\""));
    }

    @Test
    void managedIdentityErrorResponse_toJson_roundTrip() throws IOException {
        String json = "{"
                + "\"message\":\"msg\","
                + "\"correlationId\":\"corr\","
                + "\"error\":\"err\","
                + "\"error_description\":\"desc\""
                + "}";

        ManagedIdentityErrorResponse original = parseJson(json, ManagedIdentityErrorResponse::fromJson);
        String serialized = writeToJson(original);
        ManagedIdentityErrorResponse roundTripped = parseJson(serialized, ManagedIdentityErrorResponse::fromJson);

        assertEquals(original.getError(), roundTripped.getError());
        assertEquals(original.getErrorDescription(), roundTripped.getErrorDescription());
        assertEquals(original.getMessage(), roundTripped.getMessage());
        assertEquals(original.getCorrelationId(), roundTripped.getCorrelationId());
    }

    // ========== Helpers ==========

    @FunctionalInterface
    interface JsonParser<T> {
        T parse(JsonReader reader) throws IOException;
    }

    private <T> T parseJson(String json, JsonParser<T> parser) throws IOException {
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            return parser.parse(reader);
        }
    }

    private <T extends com.azure.json.JsonSerializable<T>> String writeToJson(T serializable)
            throws IOException {
        StringWriter sw = new StringWriter();
        try (com.azure.json.JsonWriter writer = JsonProviders.createWriter(sw)) {
            serializable.toJson(writer);
        }
        return sw.toString();
    }
}
