// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for Integrated Windows Authentication. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(IntegratedWindowsAuthenticationParameters)}`
 * <p>
 * For more details, see https://aka.ms/msal4j-iwa
 */
public class IntegratedWindowsAuthenticationParameters implements IAcquireTokenParameters {

    private Set<String> scopes;
    private String username;
    private ClaimsRequest claims;
    private Map<String, String> extraHttpHeaders;
    private Map<String, String> extraQueryParameters;
    private String tenant;

    private IntegratedWindowsAuthenticationParameters(Set<String> scopes, String username, ClaimsRequest claims, Map<String, String> extraHttpHeaders, Map<String, String> extraQueryParameters, String tenant) {
        this.scopes = scopes;
        this.username = username;
        this.claims = claims;
        this.extraHttpHeaders = extraHttpHeaders;
        this.extraQueryParameters = extraQueryParameters;
        this.tenant = tenant;
    }

    private static IntegratedWindowsAuthenticationParametersBuilder builder() {

        return new IntegratedWindowsAuthenticationParametersBuilder();
    }

    /**
     * Builder for {@link IntegratedWindowsAuthenticationParameters}
     *
     * @param scopes   scopes application is requesting access to
     * @param username identifier of user account for which to acquire token for. Usually in UPN format,
     *                 e.g. john.doe@contoso.com.
     * @return builder that can be used to construct IntegratedWindowsAuthenticationParameters
     */
    public static IntegratedWindowsAuthenticationParametersBuilder builder(Set<String> scopes, String username) {

        validateNotNull("scopes", scopes);
        validateNotBlank("username", username);

        return builder()
                .scopes(scopes)
                .username(username);
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
    public String username() {
        return this.username;
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

    public static class IntegratedWindowsAuthenticationParametersBuilder {
        private Set<String> scopes;
        private String username;
        private ClaimsRequest claims;
        private Map<String, String> extraHttpHeaders;
        private Map<String, String> extraQueryParameters;
        private String tenant;

        IntegratedWindowsAuthenticationParametersBuilder() {
        }

        /**
         * Scopes that the application is requesting access to
         */
        public IntegratedWindowsAuthenticationParametersBuilder scopes(Set<String> scopes) {
            validateNotNull("scopes", scopes);

            this.scopes = scopes;
            return this;
        }

        /**
         * Identifier of user account for which to acquire tokens for
         */
        public IntegratedWindowsAuthenticationParametersBuilder username(String username) {
            validateNotNull("username", username);

            this.username = username;
            return this;
        }

        /**
         * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
         */
        public IntegratedWindowsAuthenticationParametersBuilder claims(ClaimsRequest claims) {
            this.claims = claims;
            return this;
        }

        /**
         * Adds additional headers to the token request
         */
        public IntegratedWindowsAuthenticationParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
            this.extraHttpHeaders = extraHttpHeaders;
            return this;
        }

        /**
         * Adds additional parameters to the token request
         */
        public IntegratedWindowsAuthenticationParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
            this.extraQueryParameters = extraQueryParameters;
            return this;
        }

        /**
         * Overrides the tenant value in the authority URL for this request
         */
        public IntegratedWindowsAuthenticationParametersBuilder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

    /**
     * Builds and returns the configured object.
     * 
     * @return built object instance
     */
        public IntegratedWindowsAuthenticationParameters build() {
            return new IntegratedWindowsAuthenticationParameters(this.scopes, this.username, this.claims, this.extraHttpHeaders, this.extraQueryParameters, this.tenant);
        }

    /**
     * Returns a string representation of the object.
     * 
     * @return string representation of this object
     */
        public String toString() {
            return "IntegratedWindowsAuthenticationParameters.IntegratedWindowsAuthenticationParametersBuilder(scopes=" + this.scopes + ", username=" + this.username + ", claims=" + this.claims + ", extraHttpHeaders=" + this.extraHttpHeaders + ", extraQueryParameters=" + this.extraQueryParameters + ", tenant=" + this.tenant + ")";
        }
    }
}
