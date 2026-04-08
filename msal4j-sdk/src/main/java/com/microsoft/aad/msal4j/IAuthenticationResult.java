// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.Serializable;
import java.security.cert.X509Certificate;

/**
 * Interface representing the results of token acquisition operation.
 */
public interface IAuthenticationResult extends Serializable {

    /**
     * @return access token
     */
    String accessToken();

    /**
     * @return id token
     */
    String idToken();

    /**
     * @return user account
     */
    IAccount account();

    /**
     * @return tenant profile
     */
    ITenantProfile tenantProfile();

    /**
     * @return environment
     */
    String environment();

    /**
     * @return granted scopes values returned by the service
     */
    String scopes();

    /**
     * @return access token expiration date
     */
    java.util.Date expiresOnDate();

    /**
     * @return various metadata relating to this authentication result
     */
    default AuthenticationResultMetadata metadata() {
        return AuthenticationResultMetadata.builder().build();
    }

    /**
     * Returns the token type. For standard flows this is {@code "Bearer"}. For mTLS
     * Proof-of-Possession flows this is {@code "mtls_pop"}.
     *
     * @return the token type string, or {@code null} if the server did not return one
     */
    default String tokenType() {
        return null;
    }

    /**
     * For mTLS Proof-of-Possession tokens, returns the X.509 certificate that was used for
     * the mTLS handshake and to which the token is cryptographically bound (via the
     * {@code cnf.x5t#S256} claim). Returns {@code null} for standard Bearer tokens.
     *
     * @return the binding certificate, or {@code null} for Bearer tokens
     */
    default X509Certificate bindingCertificate() {
        return null;
    }
}
