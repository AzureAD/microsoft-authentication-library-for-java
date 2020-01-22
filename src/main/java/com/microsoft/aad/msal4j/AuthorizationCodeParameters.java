// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.*;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;

/**
 * Object containing parameters for authorization code flow. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(AuthorizationCodeParameters)} or to
 * {@link ConfidentialClientApplication#acquireToken(AuthorizationCodeParameters)}
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthorizationCodeParameters {

    /**
     * Code verifier used for PKCE
     */
    private String codeVerifier;

    private Set<String> scopes;

    @NonNull
    private String authorizationCode;

    @NonNull
    private URI redirectUri;

    private static AuthorizationCodeParametersBuilder builder() {

        return new AuthorizationCodeParametersBuilder();
    }

    /**
     * Builder for {@link AuthorizationCodeParameters}
     * @param authorizationCode code received from the service authorization endpoint
     * @param redirectUri URI where authorization code was received
     * @return builder object that can be used to construct {@link AuthorizationCodeParameters}
     */
    public static AuthorizationCodeParametersBuilder builder(String authorizationCode, URI redirectUri) {

        validateNotBlank("authorizationCode", authorizationCode);

        return builder()
                .authorizationCode(authorizationCode)
                .redirectUri(redirectUri);
    }
}
