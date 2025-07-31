// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for silent requests. Can be used as parameter to
 * {@link PublicClientApplication#acquireTokenSilently(SilentParameters)} or to
 * {@link ConfidentialClientApplication#acquireTokenSilently(SilentParameters)}
 */
public class SilentParameters implements IAcquireTokenParameters {

    private Set<String> scopes;

    private IAccount account;

    private ClaimsRequest claims;

    private String authorityUrl;

    private boolean forceRefresh;

    private Map<String, String> extraHttpHeaders;

    private Map<String, String> extraQueryParameters;

    private String tenant;

    private PopParameters proofOfPossession;

    private SilentParameters(Set<String> scopes, IAccount account, ClaimsRequest claims, String authorityUrl, boolean forceRefresh, Map<String, String> extraHttpHeaders, Map<String, String> extraQueryParameters, String tenant, PopParameters proofOfPossession) {
        this.scopes = scopes;
        this.account = account;
        this.claims = claims;
        this.authorityUrl = authorityUrl;
        this.forceRefresh = forceRefresh;
        this.extraHttpHeaders = extraHttpHeaders;
        this.extraQueryParameters = extraQueryParameters;
        this.tenant = tenant;
        this.proofOfPossession = proofOfPossession;
    }

    private static SilentParametersBuilder builder() {

        return new SilentParametersBuilder();
    }

    /**
     * Builder for SilentParameters
     *
     * @param scopes  scopes application is requesting access to
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
     * Builder for SilentParameters for scenarios which involve no {@link IAccount}, such as some confidential client flows
     * or broker flows which support using the default OS account.
     *
     * @param scopes scopes application is requesting access to
     * @return builder object that can be used to construct SilentParameters flow.
     */
    public static SilentParametersBuilder builder(Set<String> scopes) {
        validateNotNull("scopes", scopes);

        return builder().scopes(removeEmptyScope(scopes));
    }

    private static Set<String> removeEmptyScope(Set<String> scopes) {
        // empty string is not a valid scope, but we currently accept it and can't remove support
        // for it yet as its a breaking change. This will be removed eventually (throwing
        // exception if empty scope is passed in).
        Set<String> updatedScopes = new HashSet<>();
        for (String scope : scopes) {
            if (!scope.equalsIgnoreCase(StringHelper.EMPTY_STRING)) {
                updatedScopes.add(scope.trim());
            }
        }
        return updatedScopes;
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
    public IAccount account() {
        return this.account;
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
    public String authorityUrl() {
        return this.authorityUrl;
    }

    /**
     * TODO: Add description
     */
    public boolean forceRefresh() {
        return this.forceRefresh;
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
    public PopParameters proofOfPossession() {
        return this.proofOfPossession;
    }

    public static class SilentParametersBuilder {

        private Set<String> scopes;
        private IAccount account;
        private ClaimsRequest claims;
        private String authorityUrl;
        private boolean forceRefresh;
        private Map<String, String> extraHttpHeaders;
        private Map<String, String> extraQueryParameters;
        private String tenant;
        private PopParameters proofOfPossession;

        SilentParametersBuilder() {
        }

        /**
         * Sets the PopParameters for this request, allowing the request to retrieve proof-of-possession tokens rather than bearer tokens
         * <p>
         * For more information, see {@link PopParameters} and https://aka.ms/msal4j-pop
         *
         * @param httpMethod a valid HTTP method, such as "GET" or "POST"
         * @param uri        URI to associate with the token
         * @param nonce      optional nonce value for the token, can be empty or null
         */
        public SilentParametersBuilder proofOfPossession(HttpMethod httpMethod, URI uri, String nonce) {
            this.proofOfPossession = new PopParameters(httpMethod, uri, nonce);

            return this;
        }

        /**
         * Scopes application is requesting access to.
         * <p>
         * Cannot be null.
         */
        public SilentParametersBuilder scopes(Set<String> scopes) {
            validateNotNull("scopes", scopes);

            this.scopes = scopes;
            return this;
        }

        /**
         * Account for which you are requesting a token for.
         */
        public SilentParametersBuilder account(IAccount account) {
            this.account = account;
            return this;
        }

        /**
         * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims.
         */
        public SilentParametersBuilder claims(ClaimsRequest claims) {
            this.claims = claims;
            return this;
        }

        /**
         * Authority for which the application is requesting tokens from.
         */
        public SilentParametersBuilder authorityUrl(String authorityUrl) {
            this.authorityUrl = authorityUrl;
            return this;
        }

        /**
         * Force MSAL to refresh the tokens in the cache, even if there is a valid access token.
         */
        public SilentParametersBuilder forceRefresh(boolean forceRefresh) {
            this.forceRefresh = forceRefresh;
            return this;
        }

        /**
         * Adds additional headers to the token request
         */
        public SilentParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
            this.extraHttpHeaders = extraHttpHeaders;
            return this;
        }

        /**
         * Adds additional query parameters to the token request
         */
        public SilentParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
            this.extraQueryParameters = extraQueryParameters;
            return this;
        }

        /**
         * Overrides the tenant value in the authority URL for this request
         */
        public SilentParametersBuilder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

    /**
     * Builds and returns the configured object.
     * 
     * @return built object instance
     */
        public SilentParameters build() {
            return new SilentParameters(this.scopes, this.account, this.claims, this.authorityUrl, this.forceRefresh, this.extraHttpHeaders, this.extraQueryParameters, this.tenant, this.proofOfPossession);
        }

    /**
     * Returns a string representation of the object.
     * 
     * @return string representation of this object
     */
        public String toString() {
            return "SilentParameters.SilentParametersBuilder(scopes=" + this.scopes + ", account=" + this.account + ", claims=" + this.claims + ", authorityUrl=" + this.authorityUrl + ", forceRefresh=" + this.forceRefresh + ", extraHttpHeaders=" + this.extraHttpHeaders + ", extraQueryParameters=" + this.extraQueryParameters + ", tenant=" + this.tenant + ", proofOfPossession=" + this.proofOfPossession + ")";
        }
    }
}
