// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;

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

    private ClientCredentialParameters(Set<String> scopes, Boolean skipCache, ClaimsRequest claims, Map<String, String> extraHttpHeaders, Map<String, String> extraQueryParameters, String tenant, IClientCredential clientCredential, String fmiPath) {
        this.scopes = scopes;
        this.skipCache = skipCache;
        this.claims = claims;
        this.extraHttpHeaders = extraHttpHeaders;
        this.extraQueryParameters = extraQueryParameters;
        this.tenant = tenant;
        this.clientCredential = clientCredential;
        this.fmiPath = fmiPath;
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

    public static class ClientCredentialParametersBuilder {
        private Set<String> scopes;
        private Boolean skipCache = false;
        private ClaimsRequest claims;
        private Map<String, String> extraHttpHeaders;
        private Map<String, String> extraQueryParameters;
        private String tenant;
        private IClientCredential clientCredential;
        private String fmiPath;

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
            this.fmiPath = fmiPath;
            return this;
        }

        public ClientCredentialParameters build() {
            return new ClientCredentialParameters(this.scopes, this.skipCache, this.claims, this.extraHttpHeaders, this.extraQueryParameters, this.tenant, this.clientCredential, this.fmiPath);
        }

        public String toString() {
            return "ClientCredentialParameters.ClientCredentialParametersBuilder(scopes=" + this.scopes + ", skipCache=" + this.skipCache + ", claims=" + this.claims + ", extraHttpHeaders=" + this.extraHttpHeaders + ", extraQueryParameters=" + this.extraQueryParameters + ", tenant=" + this.tenant + ", clientCredential=" + this.clientCredential + ", fmiPath=" + this.fmiPath + ")";
        }
    }
}
