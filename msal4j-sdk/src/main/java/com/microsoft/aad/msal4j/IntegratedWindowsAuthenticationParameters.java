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
 * Object containing parameters for Integrated Windows Authentication. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(IntegratedWindowsAuthenticationParameters)}`
 * <p>
 * For more details, see https://aka.ms/msal4j-iwa
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IntegratedWindowsAuthenticationParameters implements IAcquireTokenParameters {

    /**
     * Scopes that the application is requesting access to
     */
    @NonNull
    private Set<String> scopes;

    /**
     * Identifier of user account for which to acquire tokens for
     */
    @NonNull
    private String username;

    /**
     * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
     */
    private ClaimsRequest claims;

    /**
     * Adds additional headers to the token request
     */
    private Map<String, String> extraHttpHeaders;

    /**
     * Overrides the tenant value in the authority URL for this request
     */
    private String tenant;

    private static IntegratedWindowsAuthenticationParametersBuilder builder() {

        return new IntegratedWindowsAuthenticationParametersBuilder();
    }

    /**
     * Builder for {@link IntegratedWindowsAuthenticationParameters}
     *
     * @param scopes   scopes application is requesting access to
     * @param username identifier of user account for which to acquire token for. Usually in UPN format,
     *                 e.g. john.doe@contoso.com.
     * @return builder that can be used to construct IntegratedWindowsAuthenticationParameters
     */
    public static IntegratedWindowsAuthenticationParametersBuilder builder(Set<String> scopes, String username) {

        validateNotNull("scopes", scopes);
        validateNotBlank("username", username);

        return builder()
                .scopes(scopes)
                .username(username);
    }
}
