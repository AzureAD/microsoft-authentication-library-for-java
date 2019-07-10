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

import java.util.concurrent.CompletableFuture;

public interface IConfidentialClientApplication extends IClientApplicationBase {

    /**
     * Acquires security token from the authority.
     *
     * @param parameters#scopes scopes of the access request
     * @return A {@link CompletableFuture} object representing the
     * {@link IAuthenticationResult} of the call. It contains Access
     * Token and the Access Token's expiration time. Refresh Token
     * property will be null for this overload.
     */
    CompletableFuture<IAuthenticationResult> acquireToken(ClientCredentialParameters parameters);

    /**
     * Acquires an access token from the authority on behalf of a user. It
     * requires using a user token previously received.
     *
     * @param parameters#scopes        scopes of the access request
     * @param parameters#userAssertion userAssertion to use as Authorization grant
     * @return A {@link CompletableFuture} object representing the
     * {@link IAuthenticationResult} of the call. It contains Access
     * Token and the Access Token's expiration time. Refresh Token
     * property will be null for this overload.
     * @throws MsalException {@link MsalException}
     */
    CompletableFuture<IAuthenticationResult> acquireToken(OnBehalfOfParameters parameters);
}
