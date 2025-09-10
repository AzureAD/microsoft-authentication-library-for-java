// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class JwtHelper {

    static ClientAssertion buildJwt(String clientId, final ClientCertificate credential,
                                    final String jwtAudience, boolean sendX5c,
                                    boolean useSha1) throws MsalClientException {

        ParameterValidationUtils.validateNotBlank("clientId", clientId);
        ParameterValidationUtils.validateNotNull("credential", clientId);

        try {
            // First try with PS256 (preferred)
            return generatePS256Jwt(clientId, credential, jwtAudience, sendX5c, useSha1);
        } catch (InvalidKeyException e) {
            // If the key isn't compatible with PSS, fall back to RS256.
            // This is for backwards compatibility, as the Signature instance created with SHA256withRSA
            // accepted key types that weren't RSAPrivateKey but the RSASSA-PSS signature does not.
            try {
                return generateRs256Jwt(clientId, credential, jwtAudience, sendX5c, useSha1);
            } catch (Exception fallbackException) {
                throw new MsalClientException(fallbackException);
            }
        } catch (Exception e) {
            throw new MsalClientException(e);
        }
    }

    /**
     * Generates a JWT signed using the PS256 algorithm (RSASSA-PSS with SHA-256).
     *
     * @param clientId     The client ID to use as the issuer and subject
     * @param credential   The certificate credential used for signing
     * @param jwtAudience  The audience claim for the JWT
     * @param sendX5c      Whether to include the x5c header with certificate chain
     * @param useSha1      Whether to use SHA-1 hash for thumbprint instead of SHA-256
     * @return A ClientAssertion containing the signed JWT
     * @throws Exception If JWT creation or signing fails
     */
    private static ClientAssertion generatePS256Jwt(String clientId, ClientCertificate credential,
                                                    String jwtAudience, boolean sendX5c,
                                                    boolean useSha1) throws Exception {
        // Build header with PS256 algorithm
        Map<String, Object> header = createHeader(credential, sendX5c, useSha1, "PS256");

        // Build payload
        Map<String, Object> payload = createPayload(clientId, jwtAudience, System.currentTimeMillis());

        // Encode header and payload
        String jsonHeader = JsonHelper.writeJsonMap(header);
        String jsonPayload = JsonHelper.writeJsonMap(payload);
        String encodedHeader = base64UrlEncode(jsonHeader.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64UrlEncode(jsonPayload.getBytes(StandardCharsets.UTF_8));
        String dataToSign = encodedHeader + "." + encodedPayload;

        // Sign with PS256
        byte[] signatureBytes = signWithPS256(credential, dataToSign);
        String encodedSignature = base64UrlEncode(signatureBytes);

        // Build the JWT
        String jwt = dataToSign + "." + encodedSignature;
        return new ClientAssertion(jwt);
    }

    /**
     * Generates a JWT signed using the RS256 algorithm (RSASSA-PKCS1-v1_5 with SHA-256).
     * This is used as a fallback when PS256 is not supported by the private key.
     *
     * @param clientId     The client ID to use as the issuer and subject
     * @param credential   The certificate credential used for signing
     * @param jwtAudience  The audience claim for the JWT
     * @param sendX5c      Whether to include the x5c header with certificate chain
     * @param useSha1      Whether to use SHA-1 hash for thumbprint instead of SHA-256
     * @return A ClientAssertion containing the signed JWT
     * @throws Exception If JWT creation or signing fails
     */
    private static ClientAssertion generateRs256Jwt(String clientId, ClientCertificate credential,
                                                    String jwtAudience, boolean sendX5c,
                                                    boolean useSha1) throws Exception {
        // Build header with RS256 algorithm
        Map<String, Object> header = createHeader(credential, sendX5c, useSha1, "RS256");

        // Build payload
        Map<String, Object> payload = createPayload(clientId, jwtAudience, System.currentTimeMillis());

        // Encode header and payload
        String jsonHeader = JsonHelper.writeJsonMap(header);
        String jsonPayload = JsonHelper.writeJsonMap(payload);
        String encodedHeader = base64UrlEncode(jsonHeader.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64UrlEncode(jsonPayload.getBytes(StandardCharsets.UTF_8));
        String dataToSign = encodedHeader + "." + encodedPayload;

        // Sign with RS256
        byte[] signatureBytes = signWithRS256(credential, dataToSign);
        String encodedSignature = base64UrlEncode(signatureBytes);

        // Build the JWT
        String jwt = dataToSign + "." + encodedSignature;
        return new ClientAssertion(jwt);
    }

    /**
     * Creates the JWT header with the specified algorithm and certificate information.
     *
     * @param credential The certificate credential containing thumbprint and chain
     * @param sendX5c    Whether to include the x5c header with certificate chain
     * @param useSha1    Whether to use SHA-1 hash for thumbprint instead of SHA-256
     * @param algorithm  The signing algorithm to specify in the header (PS256 or RS256)
     * @return A map containing the JWT header claims
     * @throws Exception If certificate operations fail
     */
    private static Map<String, Object> createHeader(ClientCertificate credential, boolean sendX5c,
                                                    boolean useSha1, String algorithm) throws Exception {
        Map<String, Object> header = new HashMap<>();
        header.put("alg", algorithm);
        header.put("typ", "JWT");

        if (sendX5c) {
            List<String> certs = new ArrayList<>(credential.getEncodedPublicKeyCertificateChain());
            header.put("x5c", certs);
        }

        // SHA-256 is preferred, however certain flows still require SHA-1
        String hash256 = credential.publicCertificateHash256();
        if (useSha1 || hash256 == null) {
            header.put("x5t", credential.publicCertificateHash());
        } else {
            header.put("x5t#S256", hash256);
        }

        return header;
    }

    /**
     * Creates the JWT payload with standard claims.
     *
     * @param clientId The client ID to use as the issuer and subject
     * @param audience The audience claim for the JWT
     * @param time     The current time in milliseconds
     * @return A map containing the JWT payload claims
     */
    private static Map<String, Object> createPayload(String clientId, String audience, long time) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("aud", audience);
        payload.put("iss", clientId);
        payload.put("jti", UUID.randomUUID().toString());
        payload.put("nbf", time / 1000);
        payload.put("exp", time / 1000 + Constants.AAD_JWT_TOKEN_LIFETIME_SECONDS);
        payload.put("sub", clientId);
        return payload;
    }

    /**
     * Signs data using the PS256 algorithm (RSASSA-PSS with SHA-256).
     *
     * @param credential The certificate credential containing the private key
     * @param dataToSign The data to sign
     * @return The signature bytes
     * @throws Exception If signing fails
     */
    private static byte[] signWithPS256(ClientCertificate credential, String dataToSign) throws Exception {
        Signature sig = Signature.getInstance("RSASSA-PSS");
        sig.setParameter(new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
        sig.initSign(credential.privateKey());
        sig.update(dataToSign.getBytes(StandardCharsets.UTF_8));
        return sig.sign();
    }

    /**
     * Signs data using the RS256 algorithm (RSASSA-PKCS1-v1_5 with SHA-256).
     *
     * @param credential The certificate credential containing the private key
     * @param dataToSign The data to sign
     * @return The signature bytes
     * @throws Exception If signing fails
     */
    private static byte[] signWithRS256(ClientCertificate credential, String dataToSign) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(credential.privateKey());
        sig.update(dataToSign.getBytes(StandardCharsets.UTF_8));
        return sig.sign();
    }

    /**
     * Encodes bytes using Base64URL encoding without padding.
     *
     * @param data The data to encode
     * @return The Base64URL encoded string
     */
    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}