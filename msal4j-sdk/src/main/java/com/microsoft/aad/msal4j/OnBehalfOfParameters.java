// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;

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

    private OnBehalfOfParameters(Set<String> scopes, Boolean skipCache, IUserAssertion userAssertion, ClaimsRequest claims, Map<String, String> extraHttpHeaders, Map<String, String> extraQueryParameters, String tenant) {
        this.scopes = scopes;
        //Legacy code that made the public parameter take the Boolean class instead of the primitive, so we need a null check
        this.skipCache = skipCache != null && skipCache;
        this.userAssertion = userAssertion;
        this.claims = claims;
        this.extraHttpHeaders = extraHttpHeaders;
        this.extraQueryParameters = extraQueryParameters;
        this.tenant = tenant;
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
    public IUserAssertion userAssertion() {
        return this.userAssertion;
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

    public static class OnBehalfOfParametersBuilder {
        private Set<String> scopes;
        private Boolean skipCache;
        private IUserAssertion userAssertion;
        private ClaimsRequest claims;
        private Map<String, String> extraHttpHeaders;
        private Map<String, String> extraQueryParameters;
        private String tenant;

        OnBehalfOfParametersBuilder() {
        }

    /**
     * TODO: Add description
     */
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

    /**
     * TODO: Add description
     */
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
         */
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
     * Builds and returns the configured object.
     * 
     * @return built object instance
     */
        public OnBehalfOfParameters build() {
            return new OnBehalfOfParameters(this.scopes, this.skipCache, this.userAssertion, this.claims, this.extraHttpHeaders, this.extraQueryParameters, this.tenant);
        }

    /**
     * Returns a string representation of the object.
     * 
     * @return string representation of this object
     */
        public String toString() {
            return "OnBehalfOfParameters.OnBehalfOfParametersBuilder(scopes=" + this.scopes + ", skipCache$value=" + this.skipCache + ", userAssertion=" + this.userAssertion + ", claims=" + this.claims + ", extraHttpHeaders=" + this.extraHttpHeaders + ", extraQueryParameters=" + this.extraQueryParameters + ", tenant=" + this.tenant + ")";
        }
    }
}
