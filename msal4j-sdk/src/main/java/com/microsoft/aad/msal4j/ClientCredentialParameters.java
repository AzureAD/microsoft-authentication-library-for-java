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

    private ClientCredentialParameters(Set<String> scopes, Boolean skipCache, ClaimsRequest claims, Map<String, String> extraHttpHeaders, Map<String, String> extraQueryParameters, String tenant, IClientCredential clientCredential) {
        this.scopes = scopes;
        this.skipCache = skipCache;
        this.claims = claims;
        this.extraHttpHeaders = extraHttpHeaders;
        this.extraQueryParameters = extraQueryParameters;
        this.tenant = tenant;
        this.clientCredential = clientCredential;
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

    /**
     * TODO: Add description
     */
    public Set<String> scopes() {
        return this.scopes;
    }

    /**
     * TODO: Add description
     */
    public Boolean skipCache() {
        return this.skipCache;
    }

    /**
     * TODO: Add description
     */
    public ClaimsRequest claims() {
        return this.claims;
    }

    /**
     * TODO: Add description
     */
    public Map<String, String> extraHttpHeaders() {
        return this.extraHttpHeaders;
    }

    /**
     * TODO: Add description
     */
    public Map<String, String> extraQueryParameters() {
        return this.extraQueryParameters;
    }

    /**
     * TODO: Add description
     */
    public String tenant() {
        return this.tenant;
    }

    /**
     * TODO: Add description
     */
    public IClientCredential clientCredential() {
        return this.clientCredential;
    }

    public static class ClientCredentialParametersBuilder {
        private Set<String> scopes;
        private Boolean skipCache = false;
        private ClaimsRequest claims;
        private Map<String, String> extraHttpHeaders;
        private Map<String, String> extraQueryParameters;
        private String tenant;
        private IClientCredential clientCredential;

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
         */
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
     * Builds and returns the configured object.
     * 
     * @return built object instance
     */
        public ClientCredentialParameters build() {
            return new ClientCredentialParameters(this.scopes, this.skipCache, this.claims, this.extraHttpHeaders, this.extraQueryParameters, this.tenant, this.clientCredential);
        }

    /**
     * Returns a string representation of the object.
     * 
     * @return string representation of this object
     */
        public String toString() {
            return "ClientCredentialParameters.ClientCredentialParametersBuilder(scopes=" + this.scopes + ", skipCache=" + this.skipCache + ", claims=" + this.claims + ", extraHttpHeaders=" + this.extraHttpHeaders + ", extraQueryParameters=" + this.extraQueryParameters + ", tenant=" + this.tenant + ", clientCredential=" + this.clientCredential + ")";
        }
    }
}
