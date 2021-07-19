// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for refresh token request. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(RefreshTokenParameters)} or to
 * {@link ConfidentialClientApplication#acquireToken(RefreshTokenParameters)}
 *
 *  RefreshTokenParameters should only be used for migration scenarios (when moving from ADAL to
 *  MSAL). To acquire tokens silently, use {@link AbstractClientApplicationBase#acquireTokenSilently(SilentParameters)}
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RefreshTokenParameters implements IAcquireTokenParameters {

    /**
     * Scopes the application is requesting access to
     */
    @NonNull
    private Set<String> scopes;

    /**
     * Refresh token received from the STS
     */
    @NonNull
    private String refreshToken;

    /**
     * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
     */
    private ClaimsRequest claims;

    /**
     * Adds additional headers to the token request
     */
    private Map<String, String> extraHttpHeaders;

    private static RefreshTokenParametersBuilder builder() {

        return new RefreshTokenParametersBuilder();
    }

    /**
     * Builder for {@link RefreshTokenParameters}
     * @param scopes scopes application is requesting access to
     * @param refreshToken refresh token received form the STS
     * @return builder object that can be used to construct {@link RefreshTokenParameters}
     */
    public static RefreshTokenParametersBuilder builder(Set<String> scopes, String refreshToken) {
        
        validateNotBlank("refreshToken", refreshToken);

        return builder()
                .scopes(scopes)
                .refreshToken(refreshToken);
    }
}
