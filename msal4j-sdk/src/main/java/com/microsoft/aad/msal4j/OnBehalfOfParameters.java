// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for On-Behalf-Of flow. Can be used as parameter to
 * {@link ConfidentialClientApplication#acquireToken(OnBehalfOfParameters)}
 * <p>
 * For more details, see https://aka.ms/msal4j-on-behalf-of
 */
public class OnBehalfOfParameters implements IAcquireTokenParameters {

    private Set<String> scopes;
    private boolean skipCache;
    private IUserAssertion userAssertion;
    private ClaimsRequest claims;
    private Map<String, String> extraHttpHeaders;
    private Map<String, String> extraQueryParameters;
    private String tenant;
    private String clientClaims;

    // Generic extended cache key components. The hash of these components isolates cache
    // entries so that requests with different client-claims values do not collide.
    private SortedMap<String, String> cacheKeyComponents;

    // Memoized hash of cacheKeyComponents (computed once since parameters are immutable).
    private String extCacheKeyHashCache;

    private OnBehalfOfParameters(Set<String> scopes, Boolean skipCache, IUserAssertion userAssertion, ClaimsRequest claims, Map<String, String> extraHttpHeaders, Map<String, String> extraQueryParameters, String tenant, String clientClaims) {
        this.scopes = scopes;
        //Legacy code that made the public parameter take the Boolean class instead of the primitive, so we need a null check
        this.skipCache = skipCache != null && skipCache;
        this.userAssertion = userAssertion;
        this.claims = claims;
        this.extraHttpHeaders = extraHttpHeaders;
        this.extraQueryParameters = extraQueryParameters;
        this.tenant = tenant;
        this.clientClaims = clientClaims;

        // Build cache key components from any parameters that require cache isolation.
        this.cacheKeyComponents = buildCacheKeyComponents();
    }

    private static OnBehalfOfParametersBuilder builder() {

        return new OnBehalfOfParametersBuilder();
    }

    /**
     * Builder for {@link OnBehalfOfParameters}
     *
     * @param scopes        scopes application is requesting access to
     * @param userAssertion {@link UserAssertion} created from access token received
     * @return builder that can be used to construct OnBehalfOfParameters
     */
    public static OnBehalfOfParametersBuilder builder(Set<String> scopes, UserAssertion userAssertion) {

        validateNotNull("scopes", scopes);

        return builder()
                .scopes(scopes)
                .userAssertion(userAssertion);
    }

    public Set<String> scopes() {
        return this.scopes;
    }

    public Boolean skipCache() {
        return this.skipCache;
    }

    public IUserAssertion userAssertion() {
        return this.userAssertion;
    }

    public ClaimsRequest claims() {
        return this.claims;
    }

    public Map<String, String> extraHttpHeaders() {
        return this.extraHttpHeaders;
    }

    /**
     * @deprecated Not recommended for production scenarios. It will be removed in a future release, and the behavior may be replaced by a new API.
     */
    @Deprecated
    public Map<String, String> extraQueryParameters() {
        return this.extraQueryParameters;
    }

    public String tenant() {
        return this.tenant;
    }

    /**
     * Client-originated claims set via {@link OnBehalfOfParametersBuilder#claimsFromClient(String)}.
     * Forwarded to the token endpoint as the OAuth {@code claims} parameter and used as part of the
     * extended cache key so that distinct claim values are cached separately.
     */
    @Override
    public String clientClaims() {
        return this.clientClaims;
    }

    /**
     * Builds the sorted map of cache key components from the parameters that require cache isolation.
     * Returns null if no components are present.
     */
    private SortedMap<String, String> buildCacheKeyComponents() {
        TreeMap<String, String> components = null;
        if (!StringHelper.isBlank(clientClaims)) {
            components = new TreeMap<>();
            components.put("client_claims", clientClaims);
        }
        return components;
    }

    /**
     * Returns the extended cache key components for this request, if any.
     * Used by {@link TokenCache} for both cache writes and reads.
     */
    SortedMap<String, String> cacheKeyComponents() {
        return this.cacheKeyComponents;
    }

    /**
     * Computes the extended cache key hash from all cache key components, or an empty string when
     * there are none. The result is memoized since the parameters are immutable after construction.
     */
    @Override
    public String computeExtCacheKeyHash() {
        if (extCacheKeyHashCache != null) {
            return extCacheKeyHashCache;
        }
        extCacheKeyHashCache = StringHelper.computeExtCacheKeyHash(cacheKeyComponents);
        return extCacheKeyHashCache;
    }

    public static class OnBehalfOfParametersBuilder {
        private Set<String> scopes;
        private Boolean skipCache;
        private IUserAssertion userAssertion;
        private ClaimsRequest claims;
        private Map<String, String> extraHttpHeaders;
        private Map<String, String> extraQueryParameters;
        private String tenant;
        private String clientClaims;

        OnBehalfOfParametersBuilder() {
        }

        public OnBehalfOfParametersBuilder scopes(Set<String> scopes) {
            validateNotNull("scopes", scopes);

            this.scopes = scopes;
            return this;
        }

        /**
         * Indicates whether the request should skip looking into the token cache. Be default it is set to false.
         */
        public OnBehalfOfParametersBuilder skipCache(Boolean skipCache) {
            this.skipCache = skipCache;
            return this;
        }

        public OnBehalfOfParametersBuilder userAssertion(IUserAssertion userAssertion) {
            validateNotNull("userAssertion", userAssertion);

            this.userAssertion = userAssertion;
            return this;
        }

        /**
         * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
         */
        public OnBehalfOfParametersBuilder claims(ClaimsRequest claims) {
            this.claims = claims;
            return this;
        }

        /**
         * Adds additional headers to the token request
         */
        public OnBehalfOfParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
            this.extraHttpHeaders = extraHttpHeaders;
            return this;
        }

        /**
         * Adds additional parameters to the token request
         * @deprecated Not recommended for production scenarios. It will be removed in a future release, and the behavior may be replaced by a new API.
         */
        @Deprecated
        public OnBehalfOfParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
            this.extraQueryParameters = extraQueryParameters;
            return this;
        }

        /**
         * Overrides the tenant value in the authority URL for this request
         */
        public OnBehalfOfParametersBuilder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        /**
         * Specifies client-originated claims (a raw JSON object string) to forward to the token
         * endpoint as the OAuth {@code claims} request parameter. Unlike {@link #claims(ClaimsRequest)}
         * (server-issued claims challenges, which bypass the cache), tokens acquired with client claims
         * are cached and the cache entry is keyed on the claims value, so distinct claim values produce
         * separate cache entries. Use stable, non-dynamic values to avoid cache fragmentation.
         * A blank value is ignored; an invalid JSON object throws {@link MsalClientException}.
         *
         * @param claimsJson a valid JSON object string containing the client claims
         * @return this builder instance
         */
        public OnBehalfOfParametersBuilder claimsFromClient(String claimsJson) {
            if (StringHelper.isBlank(claimsJson)) {
                return this;
            }

            JsonHelper.validateJsonObjectFormat(claimsJson);
            this.clientClaims = claimsJson;
            return this;
        }

        public OnBehalfOfParameters build() {
            return new OnBehalfOfParameters(this.scopes, this.skipCache, this.userAssertion, this.claims, this.extraHttpHeaders, this.extraQueryParameters, this.tenant, this.clientClaims);
        }

        public String toString() {
            return "OnBehalfOfParameters.OnBehalfOfParametersBuilder(scopes=" + this.scopes + ", skipCache$value=" + this.skipCache + ", userAssertion=" + this.userAssertion + ", claims=" + this.claims + ", extraHttpHeaders=" + this.extraHttpHeaders + ", extraQueryParameters=" + this.extraQueryParameters + ", tenant=" + this.tenant + ")";
        }
    }
}
