// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
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
            final long time = System.currentTimeMillis();

            // Build header
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "RS256");
            header.put("typ", "JWT");

            if (sendX5c) {
                List<String> certs = new ArrayList<>();
                for (String cert : credential.getEncodedPublicKeyCertificateChain()) {
                    certs.add(cert);
                }
                header.put("x5c", certs);
            }

            //SHA-256 is preferred, however certain flows still require SHA-1 due to what is supported server-side. If SHA-256
            // is not supported or the IClientCredential.publicCertificateHash256() method is not implemented, the library will default to SHA-1.
            String hash256 = credential.publicCertificateHash256();
            if (useSha1 || hash256 == null) {
                header.put("x5t", credential.publicCertificateHash());
            } else {
                header.put("x5t#S256", hash256);
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
            String jsonHeader = JsonHelper.mapper.writeValueAsString(header);
            String jsonPayload = JsonHelper.mapper.writeValueAsString(payload);

            String encodedHeader = base64UrlEncode(jsonHeader.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = base64UrlEncode(jsonPayload.getBytes(StandardCharsets.UTF_8));

            // Create signature
            String dataToSign = encodedHeader + "." + encodedPayload;

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(credential.privateKey());
            sig.update(dataToSign.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = sig.sign();

            String encodedSignature = base64UrlEncode(signatureBytes);

            // Build the JWT
            String jwt = dataToSign + "." + encodedSignature;

            return new ClientAssertion(jwt);
        } catch (final Exception e) {
            throw new MsalClientException(e);
        }
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}