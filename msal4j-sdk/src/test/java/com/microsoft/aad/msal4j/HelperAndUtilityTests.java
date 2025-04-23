package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class HelperAndUtilityTests {

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
}
