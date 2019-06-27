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

import java.net.URI;
import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;

/**
 * Object containing parameters for authorization code flow. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(AuthorizationCodeParameters)} or to
 * {@link ConfidentialClientApplication#acquireToken(AuthorizationCodeParameters)}
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthorizationCodeParameters {

    private Set<String> scopes;

    @NonNull
    private String authorizationCode;

    @NonNull
    private URI redirectUri;

    private static AuthorizationCodeParametersBuilder builder() {

        return new AuthorizationCodeParametersBuilder();
    }

    /**
     * Builder for {@link AuthorizationCodeParameters}
     * @param authorizationCode code received from the service authorization endpoint
     * @param redirectUri URI where authorization code was received
     * @return builder object that can be used to construct {@link AuthorizationCodeParameters}
     */
    public static AuthorizationCodeParametersBuilder builder(String authorizationCode, URI redirectUri) {

        validateNotBlank("authorizationCode", authorizationCode);

        return builder()
                .authorizationCode(authorizationCode)
                .redirectUri(redirectUri);
    }
}
