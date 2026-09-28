// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for client credential flow. Can be used as parameter to
 * {@link ConfidentialClientApplication#acquireToken(ClientCredentialParameters)}
 */
public class ClientCredentialParameters implements IAcquireTokenParameters {

    private Set<String> scopes;

    private Boolean skipCache = false;

    private ClaimsRequest claims;

    private Map<String, String> extraHttpHeaders;

    private Map<String, String> extraQueryParameters;

    private String tenant;

    private IClientCredential clientCredential;

    private String fmiPath;

    private String clientClaims;

    private boolean mtlsProofOfPossession;

    // Generic extended cache key. Any optional or flow-specific parameters that should influence
    // token cache isolation are contributed via buildCacheKeyComponents(); the hash of those
    // components is used as part of the cache key in relevant scenarios.
    private final ExtendedCacheKey extendedCacheKey;

    private ClientCredentialParameters(Set<String> scopes, Boolean skipCache, ClaimsRequest claims, Map<String, String> extraHttpHeaders, Map<String, String> extraQueryParameters, String tenant, IClientCredential clientCredential, String fmiPath, String clientClaims, boolean mtlsProofOfPossession) {
        this.scopes = scopes;
        this.skipCache = skipCache;
        this.claims = claims;
        this.extraHttpHeaders = extraHttpHeaders;
        this.extraQueryParameters = extraQueryParameters;
        this.tenant = tenant;
        this.clientCredential = clientCredential;
        this.fmiPath = fmiPath;
        this.clientClaims = clientClaims;
        this.mtlsProofOfPossession = mtlsProofOfPossession;

        // Build cache key components from any parameters that require cache isolation.
        this.extendedCacheKey = new ExtendedCacheKey(buildCacheKeyComponents());
    }

    private static ClientCredentialParametersBuilder builder() {

        return new ClientCredentialParametersBuilder();
    }

    /**
     * Builder for {@link ClientCredentialParameters}
     *
     * @param scopes scopes application is requesting access to
     * @return builder that can be used to construct ClientCredentialParameters
     */
    public static ClientCredentialParametersBuilder builder(Set<String> scopes) {
        validateNotNull("scopes", scopes);

        return builder().scopes(scopes);
    }

    public Set<String> scopes() {
        return this.scopes;
    }

    public Boolean skipCache() {
        return this.skipCache;
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

    public IClientCredential clientCredential() {
        return this.clientCredential;
    }

    /**
     * @return whether this acquisition requests a certificate-bound mTLS PoP token
     */
    public boolean mtlsProofOfPossession() {
        return mtlsProofOfPossession;
    }

    /**
     * Gets the FMI (Federated Managed Identity) path for agent identity scenarios.
     * When set, {@code fmi_path} is sent as a body parameter in the client credentials token request,
     * which scopes the resulting token to a specific agent identity.
     *
     * @return the FMI path, or null if not set
     */
    public String fmiPath() {
        return this.fmiPath;
    }

    /**
     * Client-originated claims set via {@link ClientCredentialParametersBuilder#claimsFromClient(String)}.
     * Forwarded to the token endpoint as the OAuth {@code claims} parameter and used as part of the
     * extended cache key so that distinct claim values are cached separately.
     */
    @Override
    public String clientClaims() {
        return this.clientClaims;
    }

    /**
     * Builds the sorted map of cache key components from the parameters that require
     * cache isolation. Returns null if no components are present.
     * <p>
     * This is the single place where parameters contribute to the extended cache key.
     * To add a new cache key component, add an entry here.
     */
    private SortedMap<String, String> buildCacheKeyComponents() {
        TreeMap<String, String> components = null;
        if (!StringHelper.isBlank(fmiPath)) {
            components = new TreeMap<>();
            components.put("fmi_path", fmiPath);
        }
        if (!StringHelper.isBlank(clientClaims)) {
            if (components == null) {
                components = new TreeMap<>();
            }
            components.put("client_claims", clientClaims);
        }
        return components;
    }

    /**
     * Computes the extended cache key hash from all cache key components.
     * Returns an empty string if no components are present.
     * <p>
     * The result is memoized since ClientCredentialParameters is immutable after construction.
     * Used by both cache writes ({@link TokenCache}) and cache reads (silent lookup).
     */
    @Override
    public String computeExtCacheKeyHash() {
        return extendedCacheKey.computeHash();
    }

    public static class ClientCredentialParametersBuilder {
        private Set<String> scopes;
        private Boolean skipCache = false;
        private ClaimsRequest claims;
        private Map<String, String> extraHttpHeaders;
        private Map<String, String> extraQueryParameters;
        private String tenant;
        private IClientCredential clientCredential;
        private String fmiPath;
        private String clientClaims;
        private boolean mtlsProofOfPossession;

        ClientCredentialParametersBuilder() {
        }

        /**
         * Scopes application is requesting access to.
         * <p>
         * Cannot be null.
         */
        public ClientCredentialParametersBuilder scopes(Set<String> scopes) {
            validateNotNull("scopes", scopes);

            this.scopes = scopes;
            return this;
        }

        /**
         * Indicates whether the request should skip looking into the token cache. Be default it is
         * set to false.
         */
        public ClientCredentialParametersBuilder skipCache(Boolean skipCache) {
            this.skipCache = skipCache;
            return this;
        }

        /**
         * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
         */
        public ClientCredentialParametersBuilder claims(ClaimsRequest claims) {
            this.claims = claims;
            return this;
        }

        /**
         * Adds additional headers to the token request
         */
        public ClientCredentialParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
            this.extraHttpHeaders = extraHttpHeaders;
            return this;
        }

        /**
         * Adds additional query parameters to the token request
         * @deprecated Not recommended for production scenarios. It will be removed in a future release, and the behavior may be replaced by a new API.
         */
        @Deprecated
        public ClientCredentialParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
            this.extraQueryParameters = extraQueryParameters;
            return this;
        }

        /**
         * Overrides the tenant value in the authority URL for this request
         */
        public ClientCredentialParametersBuilder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        /**
         * Overrides the client credentials for this request
         */
        public ClientCredentialParametersBuilder clientCredential(IClientCredential clientCredential) {
            this.clientCredential = clientCredential;
            return this;
        }

        /**
         * Sets the FMI (Federated Managed Identity) path for agent identity scenarios.
         * When set, {@code fmi_path} is sent as a body parameter in the client credentials token request,
         * which tells Entra ID to scope the resulting token to a specific agent identity.
         * The token is also cached with an extended cache key to prevent collisions between
         * tokens for different agent identities.
         *
         * @param fmiPath the FMI path value (typically the agent application ID)
         * @return builder that can be used to construct ClientCredentialParameters
         */
        public ClientCredentialParametersBuilder fmiPath(String fmiPath) {
            ParameterValidationUtils.validateNotBlank("fmiPath", fmiPath);
            this.fmiPath = fmiPath;
            return this;
        }

        /**
         * Specifies client-originated claims (a raw JSON object string) to forward to the token
         * endpoint as the OAuth {@code claims} request parameter. Unlike {@link #claims(ClaimsRequest)}
         * (server-issued claims challenges, which bypass the cache), tokens acquired with client claims
         * are cached and the cache entry is keyed on the claims value, so distinct claim values produce
         * separate cache entries. Use stable, non-dynamic values to avoid cache fragmentation. Send the identical value on every
         * request for a given token; because the raw value is part of the cache key, changing or
         * omitting it routes the request to a different cache partition.
         * A blank value is ignored; an invalid JSON object throws {@link MsalClientException}.
         *
         * @param claimsJson a valid JSON object string containing the client claims
         * @return builder that can be used to construct ClientCredentialParameters
         */
        public ClientCredentialParametersBuilder claimsFromClient(String claimsJson) {
            if (StringHelper.isBlank(claimsJson)) {
                return this;
            }

            JsonHelper.validateJsonObjectFormat(claimsJson);
            this.clientClaims = claimsJson;
            return this;
        }

        /**
         * Requests an mTLS Proof-of-Possession token for this client-credentials acquisition.
         *
         * <p>This option requires an RSA certificate credential and a concrete tenanted Microsoft
         * Entra AAD authority. Tenantless aliases such as {@code common}, {@code organizations},
         * and {@code consumers} are rejected. A configured custom HTTP client must implement
         * {@link IMtlsCapableHttpClient} and honor the request-specific TLS context and redirect
         * policy. The default HTTP client cannot combine an application-configured
         * {@link javax.net.ssl.SSLSocketFactory} with the binding key, so that combination is
         * rejected rather than silently replacing the configured trust policy.</p>
         *
         * <p>A successful result exposes a process-local {@link IMtlsBindingContext} whose
         * configured {@link javax.net.ssl.SSLContext} can be reused for downstream mTLS calls.
         * The private key is not exposed. Because that capability is process-local, serialized
         * mTLS PoP results fail closed when deserialized; ordinary bearer results remain
         * serialization-compatible.</p>
         */
        public ClientCredentialParametersBuilder mtlsProofOfPossession() {
            this.mtlsProofOfPossession = true;
            return this;
        }

        public ClientCredentialParameters build() {
            return new ClientCredentialParameters(this.scopes, this.skipCache, this.claims, this.extraHttpHeaders, this.extraQueryParameters, this.tenant, this.clientCredential, this.fmiPath, this.clientClaims, this.mtlsProofOfPossession);
        }

        public String toString() {
            return "ClientCredentialParameters.ClientCredentialParametersBuilder(scopes=" + this.scopes + ", skipCache=" + this.skipCache + ", claims=" + this.claims + ", extraHttpHeaders=" + this.extraHttpHeaders + ", extraQueryParameters=" + this.extraQueryParameters + ", tenant=" + this.tenant + ", clientCredential=" + this.clientCredential + ", fmiPath=" + this.fmiPath + ")";
        }
    }
}
