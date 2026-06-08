// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResponseParsingTest {

    // ========== ErrorResponse ==========

    @Test
    void errorResponse_fromJson_allFieldsPopulated() throws IOException {
        String json = "{\"error\":\"invalid_grant\","
                + "\"error_description\":\"Token expired\","
                + "\"error_codes\":[50076,70008],"
                + "\"suberror\":\"basic_action\","
                + "\"trace_id\":\"abc-123\","
                + "\"timestamp\":\"2024-01-15 12:00:00Z\","
                + "\"correlation_id\":\"corr-456\","
                + "\"claims\":\"{\\\"access_token\\\":{\\\"nbf\\\":{\\\"essential\\\":true}}}\"}";

        ErrorResponse response;
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            response = ErrorResponse.fromJson(reader);
        }

        assertEquals("invalid_grant", response.error());
        assertEquals("Token expired", response.errorDescription());
        assertArrayEquals(new long[]{50076, 70008}, response.errorCodes());
        assertEquals("basic_action", response.subError());
        assertEquals("abc-123", response.traceId());
        assertEquals("2024-01-15 12:00:00Z", response.timestamp());
        assertEquals("corr-456", response.correlation_id());
        assertTrue(response.claims().contains("access_token"));
    }

    @Test
    void errorResponse_fromJson_minimalFields() throws IOException {
        String json = "{\"error\":\"server_error\"}";

        ErrorResponse response;
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            response = ErrorResponse.fromJson(reader);
        }

        assertEquals("server_error", response.error());
        assertNull(response.errorDescription());
        assertNull(response.errorCodes());
        assertNull(response.subError());
        assertNull(response.traceId());
        assertNull(response.timestamp());
        assertNull(response.correlation_id());
        assertNull(response.claims());
    }

    @Test
    void errorResponse_fromJson_unknownFieldsSkipped() throws IOException {
        String json = "{\"error\":\"invalid_request\",\"unknown_field\":\"value\",\"another\":123}";

        ErrorResponse response;
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            response = ErrorResponse.fromJson(reader);
        }

        assertEquals("invalid_request", response.error());
    }

    @Test
    void errorResponse_fromJson_emptyErrorCodes() throws IOException {
        String json = "{\"error\":\"test\",\"error_codes\":[]}";

        ErrorResponse response;
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            response = ErrorResponse.fromJson(reader);
        }

        assertNotNull(response.errorCodes());
        assertEquals(0, response.errorCodes().length);
    }

    @Test
    void errorResponse_settersAndGetters_fullCoverage() {
        ErrorResponse response = new ErrorResponse();

        response.statusCode(401);
        response.statusMessage("Unauthorized");
        response.error("invalid_token");
        response.errorDescription("The token is expired");
        response.errorCodes(new long[]{50013});
        response.subError("token_expired");
        response.traceId("trace-789");
        response.timestamp("2024-06-01");
        response.correlation_id("corr-id");
        response.claims("{\"id_token\":{}}");

        assertEquals(401, response.statusCode().intValue());
        assertEquals("Unauthorized", response.statusMessage());
        assertEquals("invalid_token", response.error());
        assertEquals("The token is expired", response.errorDescription());
        assertArrayEquals(new long[]{50013}, response.errorCodes());
        assertEquals("token_expired", response.subError());
        assertEquals("trace-789", response.traceId());
        assertEquals("2024-06-01", response.timestamp());
        assertEquals("corr-id", response.correlation_id());
        assertEquals("{\"id_token\":{}}", response.claims());
    }

    @Test
    void errorResponse_toJson_throwsDueToDoubleStartObject() {
        // Bug: ErrorResponse.toJson() calls writeStartObject() twice (lines 68, 70)
        // which causes an IllegalStateException from the JSON writer
        ErrorResponse response = new ErrorResponse();
        response.statusCode(400);
        response.error("invalid_grant");

        assertThrows(IllegalStateException.class, () -> writeToJson(response),
                "toJson has a double writeStartObject bug that causes IllegalStateException");
    }

    @Test
    void errorResponse_toJson_nullErrorCodes_throwsDueToDoubleStartObject() {
        // Same double writeStartObject bug affects all toJson calls
        ErrorResponse response = new ErrorResponse();
        response.statusCode(500);
        response.error("server_error");

        assertThrows(IllegalStateException.class, () -> writeToJson(response));
    }

    // ========== UserDiscoveryResponse ==========

    @Test
    void userDiscoveryResponse_fromJson_federatedAccount() throws IOException {
        String json = "{\"ver\":\"1.0\","
                + "\"account_type\":\"Federated\","
                + "\"federation_metadata_url\":\"https://adfs.example.com/metadata\","
                + "\"federation_protocol\":\"WSTrust\","
                + "\"federation_active_auth_url\":\"https://adfs.example.com/active\","
                + "\"cloud_audience_urn\":\"urn:federation:MicrosoftOnline\"}";

        UserDiscoveryResponse response;
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            response = UserDiscoveryResponse.fromJson(reader);
        }

        assertEquals(1.0f, response.version(), 0.01f);
        assertEquals("Federated", response.accountType());
        assertEquals("https://adfs.example.com/metadata", response.federationMetadataUrl());
        assertEquals("WSTrust", response.federationProtocol());
        assertEquals("https://adfs.example.com/active", response.federationActiveAuthUrl());
        assertEquals("urn:federation:MicrosoftOnline", response.cloudAudienceUrn());
        assertTrue(response.isAccountFederated());
        assertFalse(response.isAccountManaged());
    }

    @Test
    void userDiscoveryResponse_fromJson_managedAccount() throws IOException {
        String json = "{\"ver\":\"1.0\",\"account_type\":\"Managed\"}";

        UserDiscoveryResponse response;
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            response = UserDiscoveryResponse.fromJson(reader);
        }

        assertTrue(response.isAccountManaged());
        assertFalse(response.isAccountFederated());
    }

    @Test
    void userDiscoveryResponse_fromJson_unknownAccountType() throws IOException {
        String json = "{\"ver\":\"2.0\",\"account_type\":\"Unknown\"}";

        UserDiscoveryResponse response;
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            response = UserDiscoveryResponse.fromJson(reader);
        }

        assertFalse(response.isAccountFederated());
        assertFalse(response.isAccountManaged());
    }

    @Test
    void userDiscoveryResponse_isAccountFederated_nullAccountType() {
        // Default-constructed response has null accountType
        UserDiscoveryResponse response = new UserDiscoveryResponse();

        assertFalse(response.isAccountFederated());
        assertFalse(response.isAccountManaged());
    }

    @Test
    void userDiscoveryResponse_isAccountFederated_caseInsensitive() throws IOException {
        String json = "{\"ver\":\"1.0\",\"account_type\":\"fEdErAtEd\"}";

        UserDiscoveryResponse response;
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            response = UserDiscoveryResponse.fromJson(reader);
        }

        assertTrue(response.isAccountFederated());
    }

    @Test
    void userDiscoveryResponse_isAccountManaged_caseInsensitive() throws IOException {
        String json = "{\"ver\":\"1.0\",\"account_type\":\"MANAGED\"}";

        UserDiscoveryResponse response;
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            response = UserDiscoveryResponse.fromJson(reader);
        }

        assertTrue(response.isAccountManaged());
    }

    @Test
    void userDiscoveryResponse_toJson_roundTrip() throws IOException {
        String json = "{\"ver\":\"1.0\","
                + "\"account_type\":\"Federated\","
                + "\"federation_metadata_url\":\"https://adfs.example.com/metadata\","
                + "\"federation_protocol\":\"WSTrust\","
                + "\"federation_active_auth_url\":\"https://adfs.example.com/active\","
                + "\"cloud_audience_urn\":\"urn:federation:MicrosoftOnline\"}";

        UserDiscoveryResponse original;
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            original = UserDiscoveryResponse.fromJson(reader);
        }

        String serialized = writeToJson(original);

        UserDiscoveryResponse roundTripped;
        try (JsonReader reader = JsonProviders.createReader(new StringReader(serialized))) {
            roundTripped = UserDiscoveryResponse.fromJson(reader);
        }

        assertEquals(original.accountType(), roundTripped.accountType());
        assertEquals(original.federationProtocol(), roundTripped.federationProtocol());
        assertEquals(original.federationMetadataUrl(), roundTripped.federationMetadataUrl());
        assertEquals(original.federationActiveAuthUrl(), roundTripped.federationActiveAuthUrl());
        assertEquals(original.cloudAudienceUrn(), roundTripped.cloudAudienceUrn());
    }

    @Test
    void userDiscoveryResponse_fromJson_unknownFieldsSkipped() throws IOException {
        String json = "{\"ver\":\"1.0\",\"account_type\":\"Managed\",\"extra_field\":\"ignored\"}";

        UserDiscoveryResponse response;
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            response = UserDiscoveryResponse.fromJson(reader);
        }

        assertTrue(response.isAccountManaged());
    }

    // ========== OidcDiscoveryResponse ==========

    @Test
    void oidcDiscoveryResponse_fromJson_allEndpoints() throws IOException {
        String json = "{\"authorization_endpoint\":\"https://login.microsoftonline.com/common/oauth2/v2.0/authorize\","
                + "\"token_endpoint\":\"https://login.microsoftonline.com/common/oauth2/v2.0/token\","
                + "\"device_authorization_endpoint\":\"https://login.microsoftonline.com/common/oauth2/v2.0/devicecode\","
                + "\"issuer\":\"https://login.microsoftonline.com/{tenantid}/v2.0\"}";

        OidcDiscoveryResponse response;
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            response = OidcDiscoveryResponse.fromJson(reader);
        }

        assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/authorize", response.authorizationEndpoint());
        assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/token", response.tokenEndpoint());
        assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/devicecode", response.deviceCodeEndpoint());
        assertEquals("https://login.microsoftonline.com/{tenantid}/v2.0", response.issuer());
    }

    @Test
    void oidcDiscoveryResponse_fromJson_partialFields() throws IOException {
        String json = "{\"authorization_endpoint\":\"https://example.com/auth\","
                + "\"token_endpoint\":\"https://example.com/token\"}";

        OidcDiscoveryResponse response;
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            response = OidcDiscoveryResponse.fromJson(reader);
        }

        assertEquals("https://example.com/auth", response.authorizationEndpoint());
        assertEquals("https://example.com/token", response.tokenEndpoint());
        assertNull(response.deviceCodeEndpoint());
        assertNull(response.issuer());
    }

    @Test
    void oidcDiscoveryResponse_fromJson_unknownFieldsSkipped() throws IOException {
        String json = "{\"authorization_endpoint\":\"https://example.com/auth\","
                + "\"jwks_uri\":\"https://example.com/keys\","
                + "\"response_types_supported\":[\"code\"]}";

        OidcDiscoveryResponse response;
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            response = OidcDiscoveryResponse.fromJson(reader);
        }

        assertEquals("https://example.com/auth", response.authorizationEndpoint());
    }

    @Test
    void oidcDiscoveryResponse_toJson_doesNotIncludeIssuer() throws IOException {
        // Note: toJson() intentionally omits the issuer field — this test documents that behavior
        String json = "{\"authorization_endpoint\":\"https://example.com/auth\","
                + "\"token_endpoint\":\"https://example.com/token\","
                + "\"device_authorization_endpoint\":\"https://example.com/device\","
                + "\"issuer\":\"https://example.com/issuer\"}";

        OidcDiscoveryResponse response;
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            response = OidcDiscoveryResponse.fromJson(reader);
        }

        String output = writeToJson(response);

        assertTrue(output.contains("authorization_endpoint"));
        assertTrue(output.contains("token_endpoint"));
        assertTrue(output.contains("device_authorization_endpoint"));
        // toJson does not write the issuer field
        assertFalse(output.contains("\"issuer\""));
    }

    // ========== RequestedClaimAdditionalInfo ==========

    @Test
    void requestedClaimAdditionalInfo_constructorAndGetters() {
        List<String> values = Arrays.asList("val1", "val2");
        RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(true, "single", values);

        assertTrue(info.isEssential());
        assertEquals("single", info.getValue());
        assertEquals(Arrays.asList("val1", "val2"), info.getValues());
    }

    @Test
    void requestedClaimAdditionalInfo_setters() {
        RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(false, null, null);

        info.setEssential(true);
        info.setValue("updated");
        info.setValues(Arrays.asList("a", "b"));

        assertTrue(info.isEssential());
        assertEquals("updated", info.getValue());
        assertEquals(Arrays.asList("a", "b"), info.getValues());
    }

    @Test
    void requestedClaimAdditionalInfo_fromJson_allFields() throws IOException {
        // Wrap in outer object since fromJson expects to be positioned in a field context
        String json = "{\"essential\":true,\"value\":\"test-value\",\"values\":[\"v1\",\"v2\"]}";

        RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(false, null, null);
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            info = info.fromJson(reader);
        }

        assertTrue(info.isEssential());
        assertEquals("test-value", info.getValue());
        assertEquals(Arrays.asList("v1", "v2"), info.getValues());
    }

    @Test
    void requestedClaimAdditionalInfo_fromJson_essentialOnly() throws IOException {
        String json = "{\"essential\":true}";

        RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(false, null, null);
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            info = info.fromJson(reader);
        }

        assertTrue(info.isEssential());
        assertNull(info.getValue());
        assertNull(info.getValues());
    }

    @Test
    void requestedClaimAdditionalInfo_toJson_essentialTrue() throws IOException {
        RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(true, null, null);

        String output = writeToJson(info);

        assertTrue(output.contains("\"essential\":true"));
        assertFalse(output.contains("\"value\""));
        assertFalse(output.contains("\"values\""));
    }

    @Test
    void requestedClaimAdditionalInfo_toJson_essentialFalseOmitted() throws IOException {
        RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(false, "v", null);

        String output = writeToJson(info);

        // essential=false is omitted from serialization
        assertFalse(output.contains("essential"));
        assertTrue(output.contains("\"value\":\"v\""));
    }

    @Test
    void requestedClaimAdditionalInfo_toJson_withValues() throws IOException {
        RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(
                false, null, Arrays.asList("x", "y"));

        String output = writeToJson(info);

        assertTrue(output.contains("\"values\""));
        assertTrue(output.contains("\"x\""));
        assertTrue(output.contains("\"y\""));
    }

    @Test
    void requestedClaimAdditionalInfo_toJson_emptyValuesOmitted() throws IOException {
        RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(
                false, null, Arrays.asList());

        String output = writeToJson(info);

        // Empty list should be omitted (values != null but isEmpty())
        assertFalse(output.contains("values"));
    }

    // ========== RequestedClaim ==========

    @Test
    void requestedClaim_constructorAndGetter() {
        RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(true, "val", null);
        RequestedClaim claim = new RequestedClaim("acr", info);

        assertEquals("acr", claim.name);
        assertEquals(info, claim.getRequestedClaimAdditionalInfo());
    }

    @Test
    void requestedClaim_defaultConstructor() {
        RequestedClaim claim = new RequestedClaim();

        assertNull(claim.name);
        assertNull(claim.getRequestedClaimAdditionalInfo());
    }

    @Test
    void requestedClaim_setter() {
        RequestedClaim claim = new RequestedClaim();
        RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(false, null, null);

        claim.setRequestedClaimAdditionalInfo(info);

        assertEquals(info, claim.getRequestedClaimAdditionalInfo());
    }

    @Test
    void requestedClaim_any_returnsCorrectMap() {
        RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(true, "v", null);
        RequestedClaim claim = new RequestedClaim("email", info);

        Map<String, Object> result = claim.any();

        assertEquals(1, result.size());
        assertTrue(result.containsKey("email"));
        assertEquals(info, result.get("email"));
    }

    @Test
    void requestedClaim_any_nullName() {
        RequestedClaim claim = new RequestedClaim(null, null);

        Map<String, Object> result = claim.any();

        assertEquals(1, result.size());
        assertTrue(result.containsKey(null));
    }

    @Test
    void requestedClaim_toJson_withNameAndInfo_throwsDueToBadStringWrite() {
        // Bug: RequestedClaim.toJson() calls writeString(name) inside an object context,
        // which is invalid JSON (a raw string value where a field name is expected)
        RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(true, null, null);
        RequestedClaim claim = new RequestedClaim("sub", info);

        assertThrows(IllegalStateException.class, () -> writeToJson(claim),
                "toJson writes a raw string in object context, causing IllegalStateException");
    }

    @Test
    void requestedClaim_toJson_nullNameAndInfo_writesEmptyObject() throws IOException {
        RequestedClaim claim = new RequestedClaim(null, null);

        String output = writeToJson(claim);

        // When name or info is null, toJson skips writing the content
        assertEquals("{}", output);
    }

    // ========== Helper ==========

    private <T extends com.azure.json.JsonSerializable<T>> String writeToJson(T serializable)
            throws IOException {
        java.io.StringWriter sw = new java.io.StringWriter();
        try (com.azure.json.JsonWriter writer = JsonProviders.createWriter(sw)) {
            serializable.toJson(writer);
        }
        return sw.toString();
    }
}
