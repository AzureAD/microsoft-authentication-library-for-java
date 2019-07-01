// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotEmpty;

/**
 * Object containing parameters for Username/Password flow. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(UserNamePasswordParameters)}
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserNamePasswordParameters {

    @NonNull
    private Set<String> scopes;

    @NonNull
    private String username;

    @NonNull
    private char[] password;

    public char[] password(){
        return password.clone();
    }

    private static UserNamePasswordParametersBuilder builder() {

        return new UserNamePasswordParametersBuilder();
    }

    /**
     * Builder for UserNameParameters
     * @param scopes scopes application is requesting access to
     * @param username username of the account
     * @param password char array containing credentials for the username
     * @return builder object that can be used to construct UserNameParameters
     */
    public static UserNamePasswordParametersBuilder builder
            (Set<String> scopes, String username, char[] password) {

        validateNotEmpty("scopes", scopes);
        validateNotBlank("username", username);
        validateNotEmpty("password", password);

        return builder()
                .scopes(scopes)
                .username(username)
                .password(password);
    }

    public static class UserNamePasswordParametersBuilder{
        public UserNamePasswordParametersBuilder password(char[] password) {
            this.password = password.clone();
            return this;
        }
    }
}
