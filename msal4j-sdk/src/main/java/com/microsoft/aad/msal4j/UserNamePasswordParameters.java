// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.*;

/**
 * Object containing parameters for Username/Password flow. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(UserNamePasswordParameters)}
 * <p>
 * <p><b>Deprecated:</b> This class supports the Resource Owner Password Credentials (ROPC) flow,
 * which is insecure and will be removed in a future release.</p>
 *
 * <p>See <a href="https://aka.ms/msal-ropc-migration">https://aka.ms/msal-ropc-migration</a> for migration guidance.</p>
 * For more details, see https://aka.ms/msal4j-username-password
 */
@Deprecated
public class UserNamePasswordParameters implements IAcquireTokenParameters {

    private Set<String> scopes;
    private String username;
    private char[] password;
    private ClaimsRequest claims;
    private Map<String, String> extraHttpHeaders;
    private Map<String, String> extraQueryParameters;
    private String tenant;
    private PopParameters proofOfPossession;

    private UserNamePasswordParameters(Set<String> scopes, String username, char[] password, ClaimsRequest claims, Map<String, String> extraHttpHeaders, Map<String, String> extraQueryParameters, String tenant, PopParameters proofOfPossession) {
        this.scopes = scopes;
        this.username = username;
        this.password = password;
        this.claims = claims;
        this.extraHttpHeaders = extraHttpHeaders;
        this.extraQueryParameters = extraQueryParameters;
        this.tenant = tenant;
        this.proofOfPossession = proofOfPossession;
    }

    public char[] password() {
        return password.clone();
    }

    private static UserNamePasswordParametersBuilder builder() {

        return new UserNamePasswordParametersBuilder();
    }

    /**
     * Builder for UserNameParameters
     *
     * @param scopes   scopes application is requesting access to
     * @param username username of the account
     * @param password char array containing credentials for the username
     * @return builder object that can be used to construct UserNameParameters
     */
    public static UserNamePasswordParametersBuilder builder
    (Set<String> scopes, String username, char[] password) {

        validateNotNull("scopes", scopes);
        validateNotBlank("username", username);
        validateNotEmpty("password", password);

        return builder()
                .scopes(scopes)
                .username(username)
                .password(password);
    }

    public Set<String> scopes() {
        return this.scopes;
    }

    public String username() {
        return this.username;
    }

    public ClaimsRequest claims() {
        return this.claims;
    }

    public Map<String, String> extraHttpHeaders() {
        return this.extraHttpHeaders;
    }

    public Map<String, String> extraQueryParameters() {
        return this.extraQueryParameters;
    }

    public String tenant() {
        return this.tenant;
    }

    public PopParameters proofOfPossession() {
        return this.proofOfPossession;
    }

    public static class UserNamePasswordParametersBuilder {
        private Set<String> scopes;
        private String username;
        private char[] password;
        private ClaimsRequest claims;
        private Map<String, String> extraHttpHeaders;
        private Map<String, String> extraQueryParameters;
        private String tenant;
        private PopParameters proofOfPossession;

        UserNamePasswordParametersBuilder() {
        }

        /**
         * Char array containing credentials for the username
         */
        public UserNamePasswordParametersBuilder password(char[] password) {
            validateNotNull("password", password);

            this.password = password.clone();
            return this;
        }

        /**
         * Sets the PopParameters for this request, allowing the request to retrieve proof-of-possession tokens rather than bearer tokens
         *
         * For more information, see {@link PopParameters} and https://aka.ms/msal4j-pop
         *
         * @param httpMethod a valid HTTP method, such as "GET" or "POST"
         * @param uri URI to associate with the token
         * @param nonce optional nonce value for the token, can be empty or null
         */
        public UserNamePasswordParametersBuilder proofOfPossession(HttpMethod httpMethod, URI uri, String nonce) {
            this.proofOfPossession = new PopParameters(httpMethod, uri, nonce);

            return this;
        }

        /**
         * Scopes application is requesting access to
         * <p>
         * Cannot be null.
         */
        public UserNamePasswordParametersBuilder scopes(Set<String> scopes) {
            validateNotNull("scopes", scopes);

            this.scopes = scopes;
            return this;
        }

        /**
         * Username of the account
         * <p>
         * Cannot be null.
         */
        public UserNamePasswordParametersBuilder username(String username) {
            validateNotNull("username", username);

            this.username = username;
            return this;
        }

        /**
         * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
         */
        public UserNamePasswordParametersBuilder claims(ClaimsRequest claims) {
            this.claims = claims;
            return this;
        }

        /**
         * Adds additional headers to the token request
         */
        public UserNamePasswordParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
            this.extraHttpHeaders = extraHttpHeaders;
            return this;
        }

        /**
         * Adds additional query parameters to the token request
         */
        public UserNamePasswordParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
            this.extraQueryParameters = extraQueryParameters;
            return this;
        }

        /**
         * Overrides the tenant value in the authority URL for this request
         */
        public UserNamePasswordParametersBuilder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        public UserNamePasswordParameters build() {
            return new UserNamePasswordParameters(this.scopes, this.username, this.password, this.claims, this.extraHttpHeaders, this.extraQueryParameters, this.tenant, this.proofOfPossession);
        }

        public String toString() {
            return "UserNamePasswordParameters.UserNamePasswordParametersBuilder(scopes=" + this.scopes + ", username=" + this.username + ", password=" + java.util.Arrays.toString(this.password) + ", claims=" + this.claims + ", extraHttpHeaders=" + this.extraHttpHeaders + ", extraQueryParameters=" + this.extraQueryParameters + ", tenant=" + this.tenant + ", proofOfPossession=" + this.proofOfPossession + ")";
        }
    }
}
