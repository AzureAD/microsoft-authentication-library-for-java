// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class HelperAndUtilityTests {

    @Test
    void StringHelper_serializeQueryParameters_ValidUrlQueryStrings() {
        //Empty map
        Map<String, String> params = new LinkedHashMap<>();
        String result = StringHelper.serializeQueryParameters(params);
        assertEquals("", result);

        //Null map
        result = StringHelper.serializeQueryParameters(null);
        assertEquals("", result);

        //Basic parameters
        params = new LinkedHashMap<>();
        params.put("client_id", "abc123");
        params.put("scope", "openid profile");
        params.put("redirect_uri", "https://example.com/auth");

        result = StringHelper.serializeQueryParameters(params);
        assertEquals("client_id=abc123&scope=openid+profile&redirect_uri=https%3A%2F%2Fexample.com%2Fauth", result);

        //(Unrealistic) parameters with special characters
        params = new LinkedHashMap<>();
        params.put("client_id", "special@client");
        params.put("scope", "openid offline_access");
        params.put("redirect_uri", "https://example.com/query?key=value");

        result = StringHelper.serializeQueryParameters(params);
        assertEquals("client_id=special%40client&scope=openid+offline_access&redirect_uri=https%3A%2F%2Fexample.com%2Fquery%3Fkey%3Dvalue", result);

        //Null values in map
        params = new LinkedHashMap<>();
        params.put("client_id", "abc123");
        params.put("scope", null);
        params.put("redirect_uri", "https://example.com/auth");

        result = StringHelper.serializeQueryParameters(params);
        assertEquals("client_id=abc123&redirect_uri=https%3A%2F%2Fexample.com%2Fauth", result);
    }

    @Test
    void StringHelper_convertToMultiValueMap() {
        //Historically, much of the library once used Map<String, List<String>> to represent URL query params, though it now uses Map<String, String>.
        // AuthorizationRequestUrlParameters unfortunately has a public API returning Map<String, List<String>>,
        // so this test helps ensure we still return an equivalent map to what we're using internally.
        AuthorizationRequestUrlParameters params = new AuthorizationRequestUrlParameters.Builder()
                .redirectUri("https://myapp.com/callback")
                .scopes(Collections.singleton("openid profile email"))
                .state("state123")
                .nonce("nonce123")
                .loginHint("user@example.com")
                .correlationId("correlation-id-123").build();

        Map<String, String> internalRequestParams = params.requestParameters;
        Map<String, List<String>> convertedInternalMap = StringHelper.convertToMultiValueMap(internalRequestParams);

        // Get the map returned by the method
        Map<String, List<String>> methodReturnedMap = params.requestParameters();

        // Assert
        assertNotNull(convertedInternalMap, "Converted map should not be null");
        assertNotNull(methodReturnedMap, "Method returned map should not be null");

        assertEquals(convertedInternalMap.size(), methodReturnedMap.size(), "Maps should have the same size");

        for (String key : convertedInternalMap.keySet()) {
            assertTrue(methodReturnedMap.containsKey(key), "Method returned map should contain key: " + key);

            List<String> convertedValues = convertedInternalMap.get(key);
            List<String> methodValues = methodReturnedMap.get(key);

            assertEquals(convertedValues, methodValues,
                    "Values for key '" + key + "' should be equal");
        }
    }

    @Test
    void JwtHelper_buildJwt_ValidSha1AndSha256Assertions() throws MsalClientException, CertificateEncodingException, NoSuchAlgorithmException {
        ClientCertificate clientCertificateMock = mock(ClientCertificate.class);
        when(clientCertificateMock.privateKey()).thenReturn(TestHelper.getPrivateKey());
        when(clientCertificateMock.publicCertificateHash()).thenReturn("certificateHash");
        when(clientCertificateMock.publicCertificateHash256()).thenReturn("certificateHash256");
        when(clientCertificateMock.getEncodedPublicKeyCertificateChain()).thenReturn(Arrays.asList("cert1", "cert2"));

        String clientId = "clientId";
        String audience = "https://login.microsoftonline.com/common/oauth2/v2.0/token";

        //Sha256 assertion
        ClientAssertion clientAssertion = JwtHelper.buildJwt(clientId, clientCertificateMock, audience, true, false);

        assertNotNull(clientAssertion);
        assertNotNull(clientAssertion.assertion());

        // Verify JWT structure (header.payload.signature)
        String jwt = clientAssertion.assertion();
        String[] jwtParts = jwt.split("\\.");
        assertEquals(3, jwtParts.length, "JWT should have three parts");

        // Decode and verify headers
        String headerJson = new String(Base64.getUrlDecoder().decode(jwtParts[0]));
        assertTrue(headerJson.contains("\"alg\":\"RS256\""), "Header should specify RS256 algorithm");
        assertTrue(headerJson.contains("\"typ\":\"JWT\""), "Header should specify JWT type");
        assertTrue(headerJson.contains("\"x5t#S256\":\"certificateHash256\""), "Header should contain x5t#S256");
        assertTrue(headerJson.contains("\"x5c\":[\"cert1\",\"cert2\"]"), "Header should contain x5c");

        // Decode and verify payload
        String payloadJson = new String(Base64.getUrlDecoder().decode(jwtParts[1]));
        assertTrue(payloadJson.contains("\"aud\":\"" + audience + "\""), "Payload should contain correct audience");
        assertTrue(payloadJson.contains("\"iss\":\"" + clientId + "\""), "Payload should contain correct issuer");
        assertTrue(payloadJson.contains("\"sub\":\"" + clientId + "\""), "Payload should contain correct subject");
        assertTrue(payloadJson.contains("\"nbf\":"), "Payload should contain nbf claim");
        assertTrue(payloadJson.contains("\"exp\":"), "Payload should contain exp claim");
        assertTrue(payloadJson.contains("\"jti\":"), "Payload should contain jti claim");

        // Verify certificate parameters were accessed
        verify(clientCertificateMock).privateKey();
        verify(clientCertificateMock).publicCertificateHash256();
        verify(clientCertificateMock).getEncodedPublicKeyCertificateChain();

        // Sha1 assertion, used in certain legacy flows
        clientAssertion = JwtHelper.buildJwt(clientId, clientCertificateMock, audience, true, true);

        jwt = clientAssertion.assertion();
        jwtParts = jwt.split("\\.");

        // Verify header uses SHA1 hash/x5t header and not SHA256/x5t#S256
        headerJson = new String(Base64.getUrlDecoder().decode(jwtParts[0]));
        assertTrue(headerJson.contains("\"x5t\":\"certificateHash\""), "Header should contain x5t (SHA1)");
        assertFalse(headerJson.contains("\"x5t#S256\""), "Header should not contain x5t#S256");

        // Verify the correct certificate hash method was called
        verify(clientCertificateMock).publicCertificateHash();
    }

    @Test
    void JsonHelper_createIdTokenFromEncodedTokenString_Base64URLCharacters() {
        HashMap<String, String> tokenParameters = new HashMap<>();
        tokenParameters.put("preferred_username", "~nameWith~specialChars");
        String encodedIDToken = TestHelper.createIdToken(tokenParameters);

        try {
            //TestHelper.createIdToken() should use Base64URL encoding, so first we prove that the encoded token cannot be decoded with Base64 decoder
            Base64.getDecoder().decode(encodedIDToken);

            fail("IllegalArgumentException was expected but not thrown.");
        } catch (IllegalArgumentException e) {
            //Encoded token should have some "-" characters in it
            assertTrue(e.getMessage().contains("Illegal base64 character 2e"));
        }

        // Act
        IdToken idToken = JsonHelper.createIdTokenFromEncodedTokenString(encodedIDToken);

        // Assert
        assertNotNull(idToken);
        assertEquals("~nameWith~specialChars", idToken.preferredUsername);
    }

    @Test
    void JsonHelper_createIdTokenFromEncodedTokenString_InvalidJsonInToken() {
        // Arrange
        String invalidPayload = "{not-valid-json}";
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(invalidPayload.getBytes(StandardCharsets.UTF_8));
        String invalidToken = "header." + encodedPayload + ".signature";

        // Act & Assert
        MsalJsonParsingException exception = assertThrows(MsalJsonParsingException.class,
                () -> JsonHelper.createIdTokenFromEncodedTokenString(invalidToken));

        assertEquals(AuthenticationErrorCode.INVALID_JSON, exception.errorCode());
    }
}
