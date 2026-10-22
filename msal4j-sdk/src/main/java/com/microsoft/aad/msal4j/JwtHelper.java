// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class JwtHelper {

    static ClientAssertion buildJwt(String clientId, final IClientCertificate credential,
                                    final String jwtAudience, boolean sendX5c,
                                    boolean useSha1) throws MsalClientException {

        ParameterValidationUtils.validateNotBlank("clientId", clientId);
        ParameterValidationUtils.validateNotNull("credential", credential);

        try {
            String certificateHash256 = credential.publicCertificateHash256();
            return buildJwt(
                    clientId,
                    credential.privateKey(),
                    sendX5c
                            ? credential.getEncodedPublicKeyCertificateChain()
                            : Collections.emptyList(),
                    useSha1 || certificateHash256 == null
                            ? credential.publicCertificateHash()
                            : null,
                    certificateHash256,
                    jwtAudience,
                    sendX5c,
                    useSha1);
        } catch (final Exception e) {
            throw new MsalClientException(e);
        }
    }

    static ClientAssertion buildJwt(String clientId, final MtlsBindingContext context,
                                    final String jwtAudience, boolean sendX5c,
                                    boolean useSha1) throws MsalClientException {

        ParameterValidationUtils.validateNotBlank("clientId", clientId);
        ParameterValidationUtils.validateNotNull("context", context);

        try {
            String certificateHash256 = context.publicCertificateHash256();
            return buildJwt(
                    clientId,
                    context.privateKey(),
                    sendX5c
                            ? context.getEncodedPublicKeyCertificateChain()
                            : Collections.emptyList(),
                    useSha1 || certificateHash256 == null
                            ? context.publicCertificateHash()
                            : null,
                    certificateHash256,
                    jwtAudience,
                    sendX5c,
                    useSha1);
        } catch (final Exception e) {
            throw new MsalClientException(e);
        }
    }

    private static ClientAssertion buildJwt(
            String clientId,
            PrivateKey privateKey,
            List<String> certificateChain,
            String certificateHash,
            String certificateHash256,
            String jwtAudience,
            boolean sendX5c,
            boolean useSha1) throws Exception {
        final long time = System.currentTimeMillis();

        // Build header
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "RS256");
        header.put("typ", "JWT");

        if (sendX5c) {
            header.put("x5c", new ArrayList<>(certificateChain));
        }

        //SHA-256 is preferred, however certain flows still require SHA-1 due to what is supported server-side. If SHA-256
        // is not supported or the IClientCredential.publicCertificateHash256() method is not implemented, the library will default to SHA-1.
        if (useSha1 || certificateHash256 == null) {
            header.put("x5t", certificateHash);
        } else {
            header.put("x5t#S256", certificateHash256);
        }

        // Build payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("aud", jwtAudience);
        payload.put("iss", clientId);
        payload.put("jti", UUID.randomUUID().toString());
        payload.put("nbf", time / 1000);
        payload.put("exp", time / 1000 + Constants.AAD_JWT_TOKEN_LIFETIME_SECONDS);
        payload.put("sub", clientId);

        // Concatenate header and payload
        String jsonHeader = JsonHelper.writeJsonMap(header);
        String jsonPayload = JsonHelper.writeJsonMap(payload);

        String encodedHeader = base64UrlEncode(jsonHeader.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64UrlEncode(jsonPayload.getBytes(StandardCharsets.UTF_8));

        // Create signature
        String dataToSign = encodedHeader + "." + encodedPayload;

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(dataToSign.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = sig.sign();

        String encodedSignature = base64UrlEncode(signatureBytes);

        // Build the JWT
        String jwt = dataToSign + "." + encodedSignature;

        return new ClientAssertion(jwt);
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}