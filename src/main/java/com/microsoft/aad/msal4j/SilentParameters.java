// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
public class SilentParameters implements IAcquireTokenParameters {

    /**
     * Scopes application is requesting access to.
     */
    @NonNull
    private Set<String> scopes;

    /**
     * Account for which you are requesting a token for.
     */
    private IAccount account;

    /**
     * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims.
     */
    private ClaimsRequest claims;

    /**
     * Authority for which the application is requesting tokens from.
     */
    private String authorityUrl;

    /**
     * Force MSAL to refresh the tokens in the cache, even if there is a valid access token.
     */
    private boolean forceRefresh;

    /**
     * Adds additional headers to the token request
     */
    private Map<String, String> extraHttpHeaders;

    /**
     * Overrides the tenant value in the authority URL for this request
     */
    private String tenant;

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
        validateNotNull("scopes", scopes);

        return builder()
                .scopes(removeEmptyScope(scopes))
                .account(account);
    }

    /**
     * Builder for SilentParameters
     *
     * @deprecated This method was used for using cached tokens in client credentials or On-behalf-of
     * flow. Those flows will now by default attempt to use cached the cached tokens, so there is
     * no need to call acquireTokenSilently. This overload will be removed in the next major version.
     *
     * @param scopes scopes application is requesting access to
     * @return builder object that can be used to construct SilentParameters
     */
    @Deprecated
    public static SilentParametersBuilder builder(Set<String> scopes) {
        validateNotNull("scopes", scopes);

        return builder().scopes(removeEmptyScope(scopes));
    }

    private static Set<String> removeEmptyScope(Set<String> scopes){
        // empty string is not a valid scope, but we currently accept it and can't remove support
        // for it yet as its a breaking change. This will be removed eventually (throwing
        // exception if empty scope is passed in).
        Set<String> updatedScopes = new HashSet<>();
        for(String scope: scopes){
            if(!scope.equalsIgnoreCase(StringHelper.EMPTY_STRING)){
                updatedScopes.add(scope.trim());
            }
        }
        return updatedScopes;
    }
}
