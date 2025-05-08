// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonProviders;
import com.azure.json.JsonWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClaimsTest {

    @Test
    void testClaimsRequest_Format() {

        List<String> values = new ArrayList<>();
        values.add("urn:mace:incommon:iap:silver");
        values.add("urn:mace:incommon:iap:bronze");

        ClaimsRequest cr = new ClaimsRequest();
        cr.requestClaimInAccessToken("given_name", new RequestedClaimAdditionalInfo(true, null, null));
        cr.requestClaimInAccessToken("email", null);
        cr.requestClaimInIdToken("acr", new RequestedClaimAdditionalInfo(false, null, values));
        cr.requestClaimInIdToken("sub", new RequestedClaimAdditionalInfo(true, "248289761001", null));
        cr.requestClaimInIdToken("auth_time", new RequestedClaimAdditionalInfo(false, null, null));

        assertEquals(TestConfiguration.CLAIMS_REQUEST, cr.formatAsJSONString());
    }

    @Test
    void testClaimsRequest_MergeWithClientCapabilitiesAndClaimsChallenge() throws URISyntaxException {

        ClaimsRequest cr = new ClaimsRequest();
        cr.requestClaimInAccessToken("nbf", new RequestedClaimAdditionalInfo(true, "1701477303", null));

        Set<String> capabilities = new HashSet<>();
        capabilities.add("cp1");

        PublicClientApplication pca = PublicClientApplication.builder(
                "client_id").
                clientCapabilities(capabilities).
                build();

        InteractiveRequestParameters parameters = InteractiveRequestParameters.builder(new URI("http://localhost:8080"))
                .claimsChallenge(TestConfiguration.CLAIMS_CHALLENGE)
                .claims(cr)
                .scopes(Collections.singleton(""))
                .build();

        String clientCapabilities = pca.clientCapabilities();
        String claimsChallenge = parameters.claimsChallenge();
        String claimsRequest = parameters.claims().formatAsJSONString();
        String mergedClaimsAndCapabilities = JsonHelper.mergeJSONString(claimsRequest, clientCapabilities);
        String mergedClaimsAndChallenge = JsonHelper.mergeJSONString(claimsChallenge, claimsRequest);
        String mergedAll = JsonHelper.mergeJSONString(claimsChallenge, mergedClaimsAndCapabilities);

        assertEquals(TestConfiguration.CLIENT_CAPABILITIES, clientCapabilities);
        assertEquals(TestConfiguration.CLAIMS_CHALLENGE, claimsChallenge);
        assertEquals(TestConfiguration.MERGED_CLAIMS_AND_CAPABILITIES, mergedClaimsAndCapabilities);
        assertEquals(TestConfiguration.MERGED_CLAIMS_AND_CHALLENGE, mergedClaimsAndChallenge);
        assertEquals(TestConfiguration.MERGED_CLAIMS_CAPABILITIES_AND_CHALLENGE, mergedAll);
    }

    @Test
    void testClaimsRequest_StringToClaimsRequest() {
        ClaimsRequest cr = ClaimsRequest.formatAsClaimsRequest(TestConfiguration.CLAIMS_CHALLENGE);

        assertEquals(TestConfiguration.CLAIMS_CHALLENGE, cr.formatAsJSONString());
    }

    @Test
    void testClaimsRequest_SerializationWithNullAdditionalInfo() {
        // Setup a claims request with null additional info
        ClaimsRequest request = new ClaimsRequest();
        request.requestClaimInIdToken("email", null);

        // Convert to JSON string
        String jsonString = request.formatAsJSONString();

        // Verify the output format
        assertNotNull(jsonString);
        assertEquals("{\"id_token\":{\"email\":null}}", jsonString);
    }

    @Test
    void testClaimsRequest_RoundTrip() {
        // Create original request with various claims
        ClaimsRequest originalRequest = new ClaimsRequest();
        originalRequest.requestClaimInIdToken("email", new RequestedClaimAdditionalInfo(true, null, null));
        originalRequest.requestClaimInUserInfo("name", new RequestedClaimAdditionalInfo(false, "John", null));

        List<String> groups = Arrays.asList("admin", "user");
        originalRequest.requestClaimInAccessToken("groups", new RequestedClaimAdditionalInfo(false, null, groups));

        // Convert to JSON string
        String jsonString = originalRequest.formatAsJSONString();

        // Parse back to a ClaimsRequest
        ClaimsRequest parsedRequest = ClaimsRequest.formatAsClaimsRequest(jsonString);

        // Verify the claims were preserved
        assertEquals(1, parsedRequest.getIdTokenRequestedClaims().size());
        assertEquals("email", parsedRequest.getIdTokenRequestedClaims().get(0).name);
        assertTrue(parsedRequest.getIdTokenRequestedClaims().get(0).getRequestedClaimAdditionalInfo().isEssential());

        // Check userinfo claims
        List<RequestedClaim> userInfoClaims = parsedRequest.userInfoRequestedClaims;
        assertEquals(1, userInfoClaims.size());
        assertEquals("name", userInfoClaims.get(0).name);
        assertEquals("John", userInfoClaims.get(0).getRequestedClaimAdditionalInfo().getValue());

        // Check access token claims
        List<RequestedClaim> accessTokenClaims = parsedRequest.accessTokenRequestedClaims;
        assertEquals(1, accessTokenClaims.size());
        assertEquals("groups", accessTokenClaims.get(0).name);
        assertEquals(2, accessTokenClaims.get(0).getRequestedClaimAdditionalInfo().getValues().size());
        assertTrue(accessTokenClaims.get(0).getRequestedClaimAdditionalInfo().getValues().contains("admin"));
    }

    @Test
    void testRequestedClaimAdditionalInfo_Serialization() {
        RequestedClaimAdditionalInfo info1 = new RequestedClaimAdditionalInfo(true, null, null);
        String json1 = serializeAdditionalInfo(info1);
        assertTrue(json1.contains("\"essential\":true"));
        assertFalse(json1.contains("\"value\""));
        assertFalse(json1.contains("\"values\""));

        RequestedClaimAdditionalInfo info2 = new RequestedClaimAdditionalInfo(false, "test", null);
        String json2 = serializeAdditionalInfo(info2);
        assertFalse(json2.contains("\"essential\""));
        assertTrue(json2.contains("\"value\":\"test\""));

        List<String> valuesList = Arrays.asList("one", "two");
        RequestedClaimAdditionalInfo info3 = new RequestedClaimAdditionalInfo(false, null, valuesList);
        String json3 = serializeAdditionalInfo(info3);
        assertTrue(json3.contains("\"values\":[\"one\",\"two\"]"));
    }

    @Test
    void testInvalidJsonHandling() {
        try {
            ClaimsRequest.formatAsClaimsRequest("{invalid json}");
            fail("Should have thrown MsalClientException");
        } catch (MsalClientException e) {
            assertTrue(e.getMessage().contains("Could not convert string to ClaimsRequest"));
            assertEquals(AuthenticationErrorCode.INVALID_JSON, e.errorCode());
        }
    }

    // Helper method to serialize RequestedClaimAdditionalInfo for testing
    private String serializeAdditionalInfo(RequestedClaimAdditionalInfo info) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             JsonWriter jsonWriter = JsonProviders.createWriter(outputStream)) {

            info.toJson(jsonWriter);
            jsonWriter.flush();
            return outputStream.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize", e);
        }
    }
}
