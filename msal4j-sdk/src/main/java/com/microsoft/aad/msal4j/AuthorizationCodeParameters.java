// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.*;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.Map;
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
public class AuthorizationCodeParameters implements IAcquireTokenParameters {

    /**
     * Authorization code acquired in the first step of OAuth2.0 authorization code flow. For more
     * details, see https://aka.ms/msal4j-authorization-code-flow
     */
    @NonNull
    private String authorizationCode;

    /**
     * Redirect URI registered in the Azure portal, and which was used in the first step of OAuth2.0
     * authorization code flow. For more details, see https://aka.ms/msal4j-authorization-code-flow
     */
    @NonNull
    private URI redirectUri;

    /**
     * Scopes to which the application is requesting access
     */
    private Set<String> scopes;

    /**
     * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
     */
    private ClaimsRequest claims;

    /**
     * Code verifier used for PKCE. For more details, see https://tools.ietf.org/html/rfc7636
     */
    private String codeVerifier;

    /**
     * Adds additional headers to the token request
     */
    private Map<String, String> extraHttpHeaders;

    /**
     * Adds additional query parameters to the token request
     */
    private Map<String, String> extraQueryParameters;

    /**
     * Overrides the tenant value in the authority URL for this request
     */
    private String tenant;

    private static AuthorizationCodeParametersBuilder builder() {

        return new AuthorizationCodeParametersBuilder();
    }

    /**
     * Builder for {@link AuthorizationCodeParameters}
     *
     * @param authorizationCode code received from the service authorization endpoint
     * @param redirectUri       URI where authorization code was received
     * @return builder object that can be used to construct {@link AuthorizationCodeParameters}
     */
    public static AuthorizationCodeParametersBuilder builder(String authorizationCode, URI redirectUri) {

        validateNotBlank("authorizationCode", authorizationCode);

        return builder()
                .authorizationCode(authorizationCode)
                .redirectUri(redirectUri);
    }
}
