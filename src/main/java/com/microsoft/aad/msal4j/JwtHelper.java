// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.msal4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSHeader.Builder;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 *
 */
final class JwtHelper {
    /**
     * Builds JWT object.
     * 
     * @param credential
     * @return
     * @throws AuthenticationException
     */
    static ClientAssertion buildJwt(String clientId, final AsymmetricKeyCredential credential,
            final String jwtAudience) throws AuthenticationException {
        if (StringHelper.isBlank(clientId)) {
            throw new IllegalArgumentException("clientId is null or empty");
        }

        if (credential == null) {
            throw new IllegalArgumentException("credential is null");
        }

        final long time = System.currentTimeMillis();

        final JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .audience(Collections.singletonList(jwtAudience))
                .issuer(clientId)
                .jwtID(UUID.randomUUID().toString())
                .notBeforeTime(new Date(time))
                .expirationTime(new Date(time
                                + Constants.AAD_JWT_TOKEN_LIFETIME_SECONDS
                                * 1000))
                .subject(clientId)
                .build();

        SignedJWT jwt;
        try {
            JWSHeader.Builder builder = new Builder(JWSAlgorithm.RS256);
            List<Base64> certs = new ArrayList<Base64>();
            certs.add(new Base64(credential.getPublicCertificate()));
            builder.x509CertChain(certs);
            builder.x509CertThumbprint(new Base64URL(credential
                    .getPublicCertificateHash()));
            jwt = new SignedJWT(builder.build(), claimsSet);
            final RSASSASigner signer = new RSASSASigner(credential.getKey());

            jwt.sign(signer);
        }
        catch (final Exception e) {
            throw new AuthenticationException(e);
        }

        return new ClientAssertion(jwt.serialize());
    }
}
