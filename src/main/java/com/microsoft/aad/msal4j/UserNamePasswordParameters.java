// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

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
