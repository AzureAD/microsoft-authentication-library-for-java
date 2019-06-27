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
 * Object containing parameters for Integrated Windows Authentication. Can be used as parameter to
 *  {@link PublicClientApplication#acquireToken(IntegratedWindowsAuthenticationParameters)}
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IntegratedWindowsAuthenticationParameters {

    @NonNull
    private Set<String> scopes;

    @NonNull
    private String username;

    private static IntegratedWindowsAuthenticationParametersBuilder builder() {

        return new IntegratedWindowsAuthenticationParametersBuilder();
    }

    /**
     * Builder for {@link IntegratedWindowsAuthenticationParameters}
     * @param scopes scopes application is requesting access to
     * @param username identifier of user account for which to acquire token for. Usually in UPN format,
     *                 e.g. john.doe@contoso.com.
     * @return builder that can be used to construct IntegratedWindowsAuthenticationParameters
     */
    public static IntegratedWindowsAuthenticationParametersBuilder builder(Set<String> scopes, String username) {

        validateNotEmpty("scopes", scopes);
        validateNotBlank("username", username);

        return builder()
                .scopes(scopes)
                .username(username);
    }
}
