// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.Serializable;

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
     * @return raw token type returned by the identity provider
     */
    default String tokenType() {
        return TokenType.BEARER.value();
    }

    /**
     * @return public diagnostics for the binding certificate, or null for bearer results
     */
    default BindingCertificate bindingCertificate() {
        return null;
    }

    /**
     * @return process-local capability for downstream mTLS, or null for bearer results
     */
    default IMtlsBindingContext mtlsBindingContext() {
        return null;
    }
}
