// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        ErrorResponse response = TestHelper.parseJson(json, ErrorResponse::fromJson);

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

        ErrorResponse response = TestHelper.parseJson(json, ErrorResponse::fromJson);

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

        ErrorResponse response = TestHelper.parseJson(json, ErrorResponse::fromJson);

        assertEquals("invalid_request", response.error());
    }

    @Test
    void errorResponse_fromJson_emptyErrorCodes() throws IOException {
        String json = "{\"error\":\"test\",\"error_codes\":[]}";

        ErrorResponse response = TestHelper.parseJson(json, ErrorResponse::fromJson);

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

        assertThrows(IllegalStateException.class, () -> TestHelper.writeToJson(response),
                "toJson has a double writeStartObject bug that causes IllegalStateException");
    }

    @Test
    void errorResponse_toJson_nullErrorCodes_throwsDueToDoubleStartObject() {
        // Same double writeStartObject bug affects all toJson calls
        ErrorResponse response = new ErrorResponse();
        response.statusCode(500);
        response.error("server_error");

        assertThrows(IllegalStateException.class, () -> TestHelper.writeToJson(response));
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

        UserDiscoveryResponse response = TestHelper.parseJson(json, UserDiscoveryResponse::fromJson);

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

        UserDiscoveryResponse response = TestHelper.parseJson(json, UserDiscoveryResponse::fromJson);

        assertTrue(response.isAccountManaged());
        assertFalse(response.isAccountFederated());
    }

    @Test
    void userDiscoveryResponse_fromJson_unknownAccountType() throws IOException {
        String json = "{\"ver\":\"2.0\",\"account_type\":\"Unknown\"}";

        UserDiscoveryResponse response = TestHelper.parseJson(json, UserDiscoveryResponse::fromJson);

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

        UserDiscoveryResponse response = TestHelper.parseJson(json, UserDiscoveryResponse::fromJson);

        assertTrue(response.isAccountFederated());
    }

    @Test
    void userDiscoveryResponse_isAccountManaged_caseInsensitive() throws IOException {
        String json = "{\"ver\":\"1.0\",\"account_type\":\"MANAGED\"}";

        UserDiscoveryResponse response = TestHelper.parseJson(json, UserDiscoveryResponse::fromJson);

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

        UserDiscoveryResponse original = TestHelper.parseJson(json, UserDiscoveryResponse::fromJson);

        String serialized = TestHelper.writeToJson(original);

        UserDiscoveryResponse roundTripped = TestHelper.parseJson(serialized, UserDiscoveryResponse::fromJson);

        assertEquals(original.accountType(), roundTripped.accountType());
        assertEquals(original.federationProtocol(), roundTripped.federationProtocol());
        assertEquals(original.federationMetadataUrl(), roundTripped.federationMetadataUrl());
        assertEquals(original.federationActiveAuthUrl(), roundTripped.federationActiveAuthUrl());
        assertEquals(original.cloudAudienceUrn(), roundTripped.cloudAudienceUrn());
    }

    @Test
    void userDiscoveryResponse_fromJson_unknownFieldsSkipped() throws IOException {
        String json = "{\"ver\":\"1.0\",\"account_type\":\"Managed\",\"extra_field\":\"ignored\"}";

        UserDiscoveryResponse response = TestHelper.parseJson(json, UserDiscoveryResponse::fromJson);

        assertTrue(response.isAccountManaged());
    }

    // ========== OidcDiscoveryResponse ==========

    @Test
    void oidcDiscoveryResponse_fromJson_allEndpoints() throws IOException {
        String json = "{\"authorization_endpoint\":\"https://login.microsoftonline.com/common/oauth2/v2.0/authorize\","
                + "\"token_endpoint\":\"https://login.microsoftonline.com/common/oauth2/v2.0/token\","
                + "\"device_authorization_endpoint\":\"https://login.microsoftonline.com/common/oauth2/v2.0/devicecode\","
                + "\"issuer\":\"https://login.microsoftonline.com/{tenantid}/v2.0\"}";

        OidcDiscoveryResponse response = TestHelper.parseJson(json, OidcDiscoveryResponse::fromJson);

        assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/authorize", response.authorizationEndpoint());
        assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/token", response.tokenEndpoint());
        assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/devicecode", response.deviceCodeEndpoint());
        assertEquals("https://login.microsoftonline.com/{tenantid}/v2.0", response.issuer());
    }

    @Test
    void oidcDiscoveryResponse_fromJson_partialFields() throws IOException {
        String json = "{\"authorization_endpoint\":\"https://example.com/auth\","
                + "\"token_endpoint\":\"https://example.com/token\"}";

        OidcDiscoveryResponse response = TestHelper.parseJson(json, OidcDiscoveryResponse::fromJson);

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

        OidcDiscoveryResponse response = TestHelper.parseJson(json, OidcDiscoveryResponse::fromJson);

        assertEquals("https://example.com/auth", response.authorizationEndpoint());
    }

    @Test
    void oidcDiscoveryResponse_toJson_doesNotIncludeIssuer() throws IOException {
        // Note: toJson() intentionally omits the issuer field — this test documents that behavior
        String json = "{\"authorization_endpoint\":\"https://example.com/auth\","
                + "\"token_endpoint\":\"https://example.com/token\","
                + "\"device_authorization_endpoint\":\"https://example.com/device\","
                + "\"issuer\":\"https://example.com/issuer\"}";

        OidcDiscoveryResponse response = TestHelper.parseJson(json, OidcDiscoveryResponse::fromJson);

        String output = TestHelper.writeToJson(response);

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

        RequestedClaimAdditionalInfo info = TestHelper.parseJson(json,
                reader -> new RequestedClaimAdditionalInfo(false, null, null).fromJson(reader));

        assertTrue(info.isEssential());
        assertEquals("test-value", info.getValue());
        assertEquals(Arrays.asList("v1", "v2"), info.getValues());
    }

    @Test
    void requestedClaimAdditionalInfo_fromJson_essentialOnly() throws IOException {
        String json = "{\"essential\":true}";

        RequestedClaimAdditionalInfo info = TestHelper.parseJson(json,
                reader -> new RequestedClaimAdditionalInfo(false, null, null).fromJson(reader));

        assertTrue(info.isEssential());
        assertNull(info.getValue());
        assertNull(info.getValues());
    }

    @Test
    void requestedClaimAdditionalInfo_toJson_essentialTrue() throws IOException {
        RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(true, null, null);

        String output = TestHelper.writeToJson(info);

        assertTrue(output.contains("\"essential\":true"));
        assertFalse(output.contains("\"value\""));
        assertFalse(output.contains("\"values\""));
    }

    @Test
    void requestedClaimAdditionalInfo_toJson_essentialFalseOmitted() throws IOException {
        RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(false, "v", null);

        String output = TestHelper.writeToJson(info);

        // essential=false is omitted from serialization
        assertFalse(output.contains("essential"));
        assertTrue(output.contains("\"value\":\"v\""));
    }

    @Test
    void requestedClaimAdditionalInfo_toJson_withValues() throws IOException {
        RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(
                false, null, Arrays.asList("x", "y"));

        String output = TestHelper.writeToJson(info);

        assertTrue(output.contains("\"values\""));
        assertTrue(output.contains("\"x\""));
        assertTrue(output.contains("\"y\""));
    }

    @Test
    void requestedClaimAdditionalInfo_toJson_emptyValuesOmitted() throws IOException {
        RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(
                false, null, Arrays.asList());

        String output = TestHelper.writeToJson(info);

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

        assertThrows(IllegalStateException.class, () -> TestHelper.writeToJson(claim),
                "toJson writes a raw string in object context, causing IllegalStateException");
    }

    @Test
    void requestedClaim_toJson_nullNameAndInfo_writesEmptyObject() throws IOException {
        RequestedClaim claim = new RequestedClaim(null, null);

        String output = TestHelper.writeToJson(claim);

        // When name or info is null, toJson skips writing the content
        assertEquals("{}", output);
    }

    // ========== ClientInfo ==========

    @Test
    void clientInfo_createFromJson_validBase64() {
        String json = "{\"uid\":\"user-id\",\"utid\":\"tenant-id\"}";
        String base64 = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        ClientInfo info = ClientInfo.createFromJson(base64);

        assertNotNull(info);
        assertEquals("user-id", info.getUniqueIdentifier());
        assertEquals("tenant-id", info.getUniqueTenantIdentifier());
        assertEquals("user-id.tenant-id", info.toAccountIdentifier());
    }

    @Test
    void clientInfo_createFromJson_blankInput_returnsNull() {
        assertNull(ClientInfo.createFromJson(null));
        assertNull(ClientInfo.createFromJson(""));
        assertNull(ClientInfo.createFromJson("   "));
    }

    @Test
    void clientInfo_toJson_roundTrip() throws IOException {
        String json = "{\"uid\":\"uid-1\",\"utid\":\"utid-1\"}";
        ClientInfo original = TestHelper.parseJson(json, ClientInfo::fromJson);

        String serialized = TestHelper.writeToJson(original);

        assertTrue(serialized.contains("\"uid\":\"uid-1\""));
        assertTrue(serialized.contains("\"utid\":\"utid-1\""));

        ClientInfo roundTripped = TestHelper.parseJson(serialized, ClientInfo::fromJson);
        assertEquals(original.getUniqueIdentifier(), roundTripped.getUniqueIdentifier());
        assertEquals(original.getUniqueTenantIdentifier(), roundTripped.getUniqueTenantIdentifier());
    }

    @Test
    void clientInfo_fromJson_unknownFieldsSkipped() throws IOException {
        String json = "{\"uid\":\"u\",\"utid\":\"t\",\"extra\":\"ignored\"}";

        ClientInfo info = TestHelper.parseJson(json, ClientInfo::fromJson);

        assertEquals("u", info.getUniqueIdentifier());
        assertEquals("t", info.getUniqueTenantIdentifier());
    }

    // ========== MsalServiceException ==========

    @Test
    void msalServiceException_errorResponseConstructor() {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.statusCode(401);
        errorResponse.statusMessage("Unauthorized");
        errorResponse.error("invalid_token");
        errorResponse.errorDescription("Token is expired");
        errorResponse.subError("token_expired");
        errorResponse.correlation_id("corr-123");
        errorResponse.claims("{\"access_token\":{}}");

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("WWW-Authenticate", Collections.singletonList("Bearer"));

        MsalServiceException ex = new MsalServiceException(errorResponse, headers);

        assertEquals(401, ex.statusCode().intValue());
        assertEquals("Unauthorized", ex.statusMessage());
        assertEquals("invalid_token", ex.errorCode());
        assertEquals("Token is expired", ex.getMessage());
        assertEquals("token_expired", ex.subError());
        assertEquals("corr-123", ex.correlationId());
        assertEquals("{\"access_token\":{}}", ex.claims());
        assertNotNull(ex.headers());
        assertTrue(ex.headers().containsKey("WWW-Authenticate"));
    }

    @Test
    void msalServiceException_discoveryResponseConstructor() throws IOException {
        String json = "{\"error\":\"invalid_instance\","
                + "\"error_description\":\"AADSTS50049: Unknown instance.\","
                + "\"correlation_id\":\"disc-corr\"}";

        AadInstanceDiscoveryResponse discoveryResponse =
                TestHelper.parseJson(json, AadInstanceDiscoveryResponse::fromJson);

        MsalServiceException ex = new MsalServiceException(discoveryResponse);

        assertEquals("invalid_instance", ex.errorCode());
        assertEquals("AADSTS50049: Unknown instance.", ex.getMessage());
        assertEquals("disc-corr", ex.correlationId());
        assertNull(ex.statusCode());
        assertNull(ex.headers());
    }

    @Test
    void msalServiceException_managedIdentityConstructor() {
        MsalServiceException ex = new MsalServiceException(
                "MI error", "managed_identity_error",
                ManagedIdentitySourceType.APP_SERVICE);

        assertEquals("MI error", ex.getMessage());
        assertEquals("managed_identity_error", ex.errorCode());
        assertEquals("APP_SERVICE", ex.managedIdentitySource());
    }

    // ========== MsalServiceExceptionFactory ==========

    @Test
    void msalServiceExceptionFactory_blankBody_returnsUnknownError() {
        IHttpResponse response = mock(IHttpResponse.class);
        when(response.body()).thenReturn("");
        when(response.statusCode()).thenReturn(500);

        MsalServiceException ex = MsalServiceExceptionFactory.fromHttpResponse(response);

        assertEquals(AuthenticationErrorCode.UNKNOWN, ex.errorCode());
        assertTrue(ex.getMessage().contains("500"));
        assertTrue(ex.getMessage().contains("no response body"));
    }

    @Test
    void msalServiceExceptionFactory_nullBody_returnsUnknownError() {
        IHttpResponse response = mock(IHttpResponse.class);
        when(response.body()).thenReturn(null);
        when(response.statusCode()).thenReturn(503);

        MsalServiceException ex = MsalServiceExceptionFactory.fromHttpResponse(response);

        assertEquals(AuthenticationErrorCode.UNKNOWN, ex.errorCode());
        assertTrue(ex.getMessage().contains("503"));
    }

    @Test
    void msalServiceExceptionFactory_invalidGrant_interactionRequired() {
        String body = "{\"error\":\"invalid_grant\","
                + "\"error_description\":\"AADSTS50076: needs MFA\","
                + "\"suberror\":\"basic_action\"}";

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json"));

        IHttpResponse response = mock(IHttpResponse.class);
        when(response.body()).thenReturn(body);
        when(response.statusCode()).thenReturn(400);
        when(response.headers()).thenReturn(headers);

        MsalServiceException ex = MsalServiceExceptionFactory.fromHttpResponse(response);

        assertTrue(ex instanceof MsalInteractionRequiredException,
                "invalid_grant with UI-required suberror should return MsalInteractionRequiredException");
    }

    @Test
    void msalServiceExceptionFactory_invalidGrant_clientMismatch_notInteractionRequired() {
        String body = "{\"error\":\"invalid_grant\","
                + "\"error_description\":\"Client mismatch\","
                + "\"suberror\":\"client_mismatch\"}";

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json"));

        IHttpResponse response = mock(IHttpResponse.class);
        when(response.body()).thenReturn(body);
        when(response.statusCode()).thenReturn(400);
        when(response.headers()).thenReturn(headers);

        MsalServiceException ex = MsalServiceExceptionFactory.fromHttpResponse(response);

        assertFalse(ex instanceof MsalInteractionRequiredException,
                "client_mismatch suberror should NOT return MsalInteractionRequiredException");
        assertEquals(400, ex.statusCode().intValue());
    }

    @Test
    void msalServiceExceptionFactory_invalidGrant_protectionPolicyRequired_notInteractionRequired() {
        String body = "{\"error\":\"invalid_grant\","
                + "\"error_description\":\"Protection policy required\","
                + "\"suberror\":\"protection_policy_required\"}";

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json"));

        IHttpResponse response = mock(IHttpResponse.class);
        when(response.body()).thenReturn(body);
        when(response.statusCode()).thenReturn(400);
        when(response.headers()).thenReturn(headers);

        MsalServiceException ex = MsalServiceExceptionFactory.fromHttpResponse(response);

        assertFalse(ex instanceof MsalInteractionRequiredException,
                "protection_policy_required suberror should NOT return MsalInteractionRequiredException");
    }

    @Test
    void msalServiceExceptionFactory_noErrorInBody_returnsUnknownError() {
        String body = "{\"some_field\":\"some_value\"}";

        IHttpResponse response = mock(IHttpResponse.class);
        when(response.body()).thenReturn(body);
        when(response.statusCode()).thenReturn(500);

        MsalServiceException ex = MsalServiceExceptionFactory.fromHttpResponse(response);

        assertEquals(AuthenticationErrorCode.UNKNOWN, ex.errorCode());
        assertTrue(ex.getMessage().contains("500"));
    }

}
