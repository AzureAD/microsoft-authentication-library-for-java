// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.*;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotEmpty;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for Username/Password flow. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(UserNamePasswordParameters)}
 * <p>
 * For more details, see https://aka.ms/msal4j-username-password
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserNamePasswordParameters implements IAcquireTokenParameters {

    /**
     * Scopes application is requesting access to
     */
    @NonNull
    private Set<String> scopes;

    /**
     * Username of the account
     */
    @NonNull
    private String username;

    /**
     * Char array containing credentials for the username
     */
    @NonNull
    private char[] password;

    /**
     * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
     */
    private ClaimsRequest claims;

    /**
     * Adds additional headers to the token request
     */
    private Map<String, String> extraHttpHeaders;

    /**
     * Overrides the tenant value in the authority URL for this request
     */
    private String tenant;

    private PopParameters proofOfPossession;

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

    public static class UserNamePasswordParametersBuilder {
        public UserNamePasswordParametersBuilder password(char[] password) {
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
        public UserNamePasswordParametersBuilder proofOfPossession(String httpMethod, URI uri, String nonce) {
            this.proofOfPossession = new PopParameters(httpMethod, uri, nonce);

            return this;
        }
    }
}
