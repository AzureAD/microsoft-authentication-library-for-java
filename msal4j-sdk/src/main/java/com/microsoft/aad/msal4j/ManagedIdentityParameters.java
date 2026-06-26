// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Object containing parameters for managed identity flow. Can be used as parameter to
 * {@link ManagedIdentityApplication#acquireTokenForManagedIdentity(ManagedIdentityParameters)}
 */
public class ManagedIdentityParameters implements IAcquireTokenParameters {

    String resource;
    boolean forceRefresh;
    String claims;
    String clientClaims;
    String revokedTokenHash;

    // Generic extended cache key components. The hash of these components isolates cache
    // entries so that requests with different client-claims values do not collide.
    private SortedMap<String, String> cacheKeyComponents;

    // Memoized hash of cacheKeyComponents (computed once since parameters are immutable).
    private String extCacheKeyHashCache;

    private ManagedIdentityParameters(String resource, boolean forceRefresh, String claims, String clientClaims) {
        this.resource = resource;
        this.forceRefresh = forceRefresh;
        this.claims = claims;
        this.clientClaims = clientClaims;

        // Build cache key components from any parameters that require cache isolation.
        this.cacheKeyComponents = buildCacheKeyComponents();
    }

    @Override
    public Set<String> scopes() {
        return null;
    }

    @Override
    public ClaimsRequest claims() {
        if (claims == null || claims.isEmpty()) {
            return null;
        }

        try {
            return ClaimsRequest.formatAsClaimsRequest(claims);
        } catch (Exception ex) {
            // Log the exception if the claims JSON is invalid
            throw new MsalClientException("Failed to parse claims JSON: " + ex.getMessage(),
                                         AuthenticationErrorCode.INVALID_JSON);
        }
    }

    @Override
    public Map<String, String> extraHttpHeaders() {
        return null;
    }

    @Override
    public String tenant() {
        return Constants.MANAGED_IDENTITY_DEFAULT_TENTANT;
    }

    @Override
    public Map<String, String> extraQueryParameters() {
        return null;
    }

    private static ManagedIdentityParametersBuilder builder() {
        return new ManagedIdentityParametersBuilder();
    }

    /**
     * Builder for {@link ManagedIdentityParameters}
     * @param resource scopes application is requesting access to
     * @return builder that can be used to construct ManagedIdentityParameters
     */
    public static ManagedIdentityParametersBuilder builder(String resource) {
        return builder().resource(resource);
    }

    public boolean forceRefresh() {
        return this.forceRefresh;
    }

    public String resource() {
        return this.resource;
    }

    public String revokedTokenHash() {
        return this.revokedTokenHash;
    }

    /**
     * Client-originated claims set via {@link ManagedIdentityParametersBuilder#claimsFromClient(String)}.
     * Unlike {@link #claims()} (server-issued, cache-bypassing), these are cached and keyed on the
     * raw claims string as passed by the caller.
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
        if (!StringHelper.isNullOrBlank(clientClaims)) {
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

    public static class ManagedIdentityParametersBuilder {
        private String resource;
        private boolean forceRefresh;
        private String claims;
        private String clientClaims;

        ManagedIdentityParametersBuilder() {
        }

        public ManagedIdentityParametersBuilder resource(String resource) {
            this.resource = resource;
            return this;
        }

        public ManagedIdentityParametersBuilder forceRefresh(boolean forceRefresh) {
            this.forceRefresh = forceRefresh;
            return this;
        }

        /**
         * Instructs the SDK to bypass any token caches and to request new tokens with an additional claims challenge.
         * The claims challenge string is opaque to applications and should not be parsed.
         * The claims challenge string is issued either by the STS as part of an error response or by the resource,
         * as part of an HTTP 401 response, in the WWW-Authenticate header.
         * For more details see https://learn.microsoft.com/entra/identity-platform/app-resilience-continuous-access-evaluation?tabs=dotnet
         *
         * @param claims a valid JSON string representing additional claims
         * @return this builder instance
         */
        public ManagedIdentityParametersBuilder claims(String claims) {
            ParameterValidationUtils.validateNotBlank("claims", claims);

            this.claims = claims;
            return this;
        }

        /**
         * Specifies client-originated claims (a raw JSON object string) to forward to the identity
         * endpoint. Unlike {@link #claims(String)} (server-issued claims challenges, which bypass the
         * cache), tokens acquired with client claims are cached and the cache entry is keyed on the
         * claims value. Different claim values produce separate cache entries, so use stable,
         * non-dynamic values to avoid cache fragmentation.
         * <p>
         * Only IMDS-based managed identity is supported, and IMDS (MSIv1) only accepts the single
         * custom claim {@code xms_az_nwperimid}; any other source or claim key causes the request to
         * fail with an {@link MsalClientException}. A blank value is ignored.
         *
         * @param claimsJson a valid JSON object string containing the client claims
         * @return this builder instance
         */
        public ManagedIdentityParametersBuilder claimsFromClient(String claimsJson) {
            if (StringHelper.isNullOrBlank(claimsJson)) {
                return this;
            }

            JsonHelper.validateJsonObjectFormat(claimsJson);
            this.clientClaims = claimsJson;
            return this;
        }

        public ManagedIdentityParameters build() {
            return new ManagedIdentityParameters(this.resource, this.forceRefresh, this.claims, this.clientClaims);
        }

        public String toString() {
            return "ManagedIdentityParameters.ManagedIdentityParametersBuilder(resource=" + this.resource + ", forceRefresh=" + this.forceRefresh + ")";
        }
    }
}
