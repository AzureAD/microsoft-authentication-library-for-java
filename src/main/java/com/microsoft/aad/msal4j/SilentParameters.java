// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotEmpty;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for silent requests. Can be used as parameter to
 * {@link PublicClientApplication#acquireTokenSilently(SilentParameters)} or to
 * {@link ConfidentialClientApplication#acquireTokenSilently(SilentParameters)}
 *
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SilentParameters implements IApiParameters {

    @NonNull
    private Set<String> scopes;

    private IAccount account;

    /**
     * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
     */
    private ClaimsRequest withClaims;

    private String authorityUrl;

    private boolean forceRefresh;

    private static SilentParametersBuilder builder() {

        return new SilentParametersBuilder();
    }

    /**
     * Builder for SilentParameters
     * @param scopes scopes application is requesting access to
     * @param account {@link IAccount} for which to acquire a token for
     * @return builder object that can be used to construct SilentParameters
     */
    public static SilentParametersBuilder builder(Set<String> scopes, IAccount account) {

        validateNotNull("account", account);
        validateNotEmpty("scopes", scopes);

        return builder()
                .scopes(scopes)
                .account(account);
    }

    /**
     * Builder for SilentParameters
     * @param scopes scopes application is requesting access to
     * @return builder object that can be used to construct SilentParameters
     */
    public static SilentParametersBuilder builder(Set<String> scopes) {
        validateNotEmpty("scopes", scopes);

        return builder().scopes(scopes);
    }
}
