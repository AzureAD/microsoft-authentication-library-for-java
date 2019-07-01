// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotEmpty;

/**
 * Object containing parameters for refresh token request. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(RefreshTokenParameters)} or to
 * {@link ConfidentialClientApplication#acquireToken(RefreshTokenParameters)}
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RefreshTokenParameters {

    @NonNull
    private Set<String> scopes;

    @NonNull
    private String refreshToken;

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
