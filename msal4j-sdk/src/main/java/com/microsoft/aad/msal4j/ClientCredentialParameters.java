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

    private boolean mtlsProofOfPossession;

    // Generic extended cache key components. Any optional or flow-specific parameters 
    // that should influence token cache isolation adds an entry here. The hash of these
    // components is used as part of the cache key in relevant scenarios entries.
    private volatile SortedMap<String, String> cacheKeyComponents;

    // Lazily memoized hash of cacheKeyComponents. Invalidated (set to null) whenever the
    // components change (e.g. bindingCertificateKeyId adds the cert KeyId) so it is recomputed.
    private volatile String extCacheKeyHashCache;

    private ClientCredentialParameters(Set<String> scopes, Boolean skipCache, ClaimsRequest claims, Map<String, String> extraHttpHeaders, Map<String, String> extraQueryParameters, String tenant, IClientCredential clientCredential, String fmiPath, boolean mtlsProofOfPossession) {
        this.scopes = scopes;
        this.skipCache = skipCache;
        this.claims = claims;
        this.extraHttpHeaders = extraHttpHeaders;
        this.extraQueryParameters = extraQueryParameters;
        this.tenant = tenant;
        this.clientCredential = clientCredential;
        this.fmiPath = fmiPath;
        this.mtlsProofOfPossession = mtlsProofOfPossession;

        // Build cache key components from any parameters that require cache isolation.
        this.cacheKeyComponents = buildCacheKeyComponents();
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
     * Indicates whether this request should acquire a mutual-TLS Proof-of-Possession (mTLS PoP) token,
     * where the client certificate is presented on the TLS handshake to the token endpoint and the
     * resulting token is cryptographically bound to that certificate.
     *
     * @return true if mTLS Proof-of-Possession was requested, false for a standard Bearer token
     */
    public boolean mtlsProofOfPossession() {
        return this.mtlsProofOfPossession;
    }

    /**
     * Stamps the resolved binding-certificate KeyId ({@code x5t#S256}) onto this request so the access
     * token is cache-isolated by certificate, in addition to the {@code token_type} dimension.
     * <p>
     * Called once (before the silent cache lookup) so both cache reads and writes observe the same
     * components. This params instance may be reused across concurrent acquireToken calls, so the
     * update is copy-on-write and synchronized, and clears the memoized hash so it is recomputed.
     */
    void bindingCertificateKeyId(String keyId) {
        if (StringHelper.isBlank(keyId)) {
            return;
        }
        synchronized (this) {
            // Copy-on-write: never mutate a map that another thread may be reading. Swap in a fresh
            // map and invalidate the memoized hash so it is recomputed with the added component.
            TreeMap<String, String> updated = this.cacheKeyComponents == null
                    ? new TreeMap<>()
                    : new TreeMap<>(this.cacheKeyComponents);
            updated.put("cert_kid", keyId);
            this.cacheKeyComponents = updated;
            this.extCacheKeyHashCache = null;
        }
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
        if (mtlsProofOfPossession) {
            if (components == null) {
                components = new TreeMap<>();
            }
            components.put("token_type", TokenType.MTLS_POP.value());
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
     * Computes the extended cache key hash from all cache key components.
     * Returns an empty string if no components are present.
     * <p>
     * The result is lazily memoized and invalidated whenever the cache key components change
     * (see {@link #bindingCertificateKeyId(String)}).
     * Used by both cache writes ({@link TokenCache}) and cache reads (silent lookup).
     */
    String computeExtCacheKeyHash() {
        String cached = extCacheKeyHashCache;
        if (cached != null) {
            return cached;
        }
        String computed = StringHelper.computeExtCacheKeyHash(cacheKeyComponents);
        extCacheKeyHashCache = computed;
        return computed;
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
         * Requests a mutual-TLS Proof-of-Possession (mTLS PoP) token instead of a Bearer token.
         * <p>
         * When set, the app's client certificate (a Subject-Name/Issuer cert configured on the
         * {@link ConfidentialClientApplication}, or a certificate configured via
         * {@link ConfidentialClientApplication.Builder#mtlsBindingCertificate(IClientCertificate)} for
         * assertion-authenticated apps) is presented as the client TLS certificate in the mutual-TLS
         * handshake to the token endpoint. Entra ID returns a token that is cryptographically bound to
         * that certificate ({@code cnf}/{@code x5t#S256}), and {@code token_type=mtls_pop}.
         * <p>
         * Requirements: the authority must be tenanted (not {@code /common} or {@code /organizations}),
         * a binding certificate must be available, and the cloud must support mTLS PoP (public cloud
         * today). A region is optional — when omitted, the global {@code mtlsauth.microsoft.com} endpoint
         * is used. This mirrors MSAL.NET's {@code WithMtlsProofOfPossession()}. For more details, see
         * https://aka.ms/msal4j-pop
         *
         * @return builder that can be used to construct ClientCredentialParameters
         */
        public ClientCredentialParametersBuilder mtlsProofOfPossession() {
            this.mtlsProofOfPossession = true;
            return this;
        }

        public ClientCredentialParameters build() {
            return new ClientCredentialParameters(this.scopes, this.skipCache, this.claims, this.extraHttpHeaders, this.extraQueryParameters, this.tenant, this.clientCredential, this.fmiPath, this.mtlsProofOfPossession);
        }

        public String toString() {
            return "ClientCredentialParameters.ClientCredentialParametersBuilder(scopes=" + this.scopes + ", skipCache=" + this.skipCache + ", claims=" + this.claims + ", extraHttpHeaders=" + this.extraHttpHeaders + ", extraQueryParameters=" + this.extraQueryParameters + ", tenant=" + this.tenant + ", clientCredential=" + this.clientCredential + ", fmiPath=" + this.fmiPath + ", mtlsProofOfPossession=" + this.mtlsProofOfPossession + ")";
        }
    }
}
