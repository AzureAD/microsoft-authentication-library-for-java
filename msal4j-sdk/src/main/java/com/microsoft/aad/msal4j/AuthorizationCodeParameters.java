// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for authorization code flow. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(AuthorizationCodeParameters)} or to
 * {@link ConfidentialClientApplication#acquireToken(AuthorizationCodeParameters)}
 */
public class AuthorizationCodeParameters implements IAcquireTokenParameters {

    private String authorizationCode;

    private URI redirectUri;

    private Set<String> scopes;

    private ClaimsRequest claims;

    private String codeVerifier;

    private Map<String, String> extraHttpHeaders;

    private Map<String, String> extraQueryParameters;

    private String tenant;

    private AuthorizationCodeParameters(String authorizationCode, URI redirectUri,
                                        Set<String> scopes, ClaimsRequest claims,
                                        String codeVerifier, Map<String, String> extraHttpHeaders,
                                        Map<String, String> extraQueryParameters, String tenant) {
        this.authorizationCode = authorizationCode;
        this.redirectUri = redirectUri;
        this.scopes = scopes;
        this.claims = claims;
        this.codeVerifier = codeVerifier;
        this.extraHttpHeaders = extraHttpHeaders;
        this.extraQueryParameters = extraQueryParameters;
        this.tenant = tenant;
    }

    private static AuthorizationCodeParametersBuilder builder() {

        return new AuthorizationCodeParametersBuilder();
    }

    /**
     * Builder for {@link AuthorizationCodeParameters}
     *
     * @param authorizationCode code received from the service authorization endpoint
     * @param redirectUri       URI where authorization code was received
     * @return builder object that can be used to construct {@link AuthorizationCodeParameters}
     */
    public static AuthorizationCodeParametersBuilder builder(String authorizationCode, URI redirectUri) {

        validateNotBlank("authorizationCode", authorizationCode);

        return builder()
                .authorizationCode(authorizationCode)
                .redirectUri(redirectUri);
    }

    /**
     * TODO: Add description
     */
    public String authorizationCode() {
        return this.authorizationCode;
    }

    /**
     * TODO: Add description
     */
    public URI redirectUri() {
        return this.redirectUri;
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
    public ClaimsRequest claims() {
        return this.claims;
    }

    /**
     * TODO: Add description
     */
    public String codeVerifier() {
        return this.codeVerifier;
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

    public static class AuthorizationCodeParametersBuilder {
        private String authorizationCode;
        private URI redirectUri;
        private Set<String> scopes;
        private ClaimsRequest claims;
        private String codeVerifier;
        private Map<String, String> extraHttpHeaders;
        private Map<String, String> extraQueryParameters;
        private String tenant;

        AuthorizationCodeParametersBuilder() {
        }

        /**
         * Authorization code acquired in the first step of OAuth2.0 authorization code flow. For more
         * details, see https://aka.ms/msal4j-authorization-code-flow
         * <p>
         * Cannot be null.
         */
        public AuthorizationCodeParametersBuilder authorizationCode(String authorizationCode) {
            validateNotNull("authorizationCode", authorizationCode);

            this.authorizationCode = authorizationCode;
            return this;
        }

        /**
         * Redirect URI registered in the Azure portal, and which was used in the first step of OAuth2.0
         * authorization code flow. For more details, see https://aka.ms/msal4j-authorization-code-flow
         * <p>
         * Cannot be null.
         */
        public AuthorizationCodeParametersBuilder redirectUri(URI redirectUri) {
            validateNotNull("redirectUri", redirectUri);

            this.redirectUri = redirectUri;
            return this;
        }

        /**
         * Scopes to which the application is requesting access
         */
        public AuthorizationCodeParametersBuilder scopes(Set<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        /**
         * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
         */
        public AuthorizationCodeParametersBuilder claims(ClaimsRequest claims) {
            this.claims = claims;
            return this;
        }

        /**
         * Code verifier used for PKCE. For more details, see https://tools.ietf.org/html/rfc7636
         */
        public AuthorizationCodeParametersBuilder codeVerifier(String codeVerifier) {
            this.codeVerifier = codeVerifier;
            return this;
        }

        /**
         * Adds additional headers to the token request
         */
        public AuthorizationCodeParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
            this.extraHttpHeaders = extraHttpHeaders;
            return this;
        }

        /**
         * Adds additional query parameters to the token request
         */
        public AuthorizationCodeParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
            this.extraQueryParameters = extraQueryParameters;
            return this;
        }

        /**
         * Overrides the tenant value in the authority URL for this request
         */
        public AuthorizationCodeParametersBuilder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

    /**
     * Builds and returns the configured object.
     * 
     * @return built object instance
     */
        public AuthorizationCodeParameters build() {
            return new AuthorizationCodeParameters(this.authorizationCode, this.redirectUri, this.scopes, this.claims, this.codeVerifier, this.extraHttpHeaders, this.extraQueryParameters, this.tenant);
        }

    /**
     * Returns a string representation of the object.
     * 
     * @return string representation of this object
     */
        public String toString() {
            return "AuthorizationCodeParameters.AuthorizationCodeParametersBuilder(authorizationCode=" + this.authorizationCode + ", redirectUri=" + this.redirectUri + ", scopes=" + this.scopes + ", claims=" + this.claims + ", codeVerifier=" + this.codeVerifier + ", extraHttpHeaders=" + this.extraHttpHeaders + ", extraQueryParameters=" + this.extraQueryParameters + ", tenant=" + this.tenant + ")";
        }
    }
}
