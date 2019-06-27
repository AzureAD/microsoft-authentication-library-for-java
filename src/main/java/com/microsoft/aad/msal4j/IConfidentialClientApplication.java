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

/**
 * Interface representing a confidential client application (Web App, Web API, Daemon App).
 * Confidential client applications are trusted to safely store application secrets, and therefore
 * can be used to acquire tokens in then name of either the application or an user
 */
public interface IConfidentialClientApplication extends IClientApplicationBase {

    /**
     * Acquires tokens from the authority configured in the application, for the confidential client
     * itself (in the name of no user)
     * @param parameters instance of {@link ClientCredentialParameters}
     * @return {@link CompletableFuture} containing an {@link IAuthenticationResult}
     */
    CompletableFuture<IAuthenticationResult> acquireToken(ClientCredentialParameters parameters);

    /**
     * Acquires an access token for this application (usually a Web API) from the authority configured
     * in the application, in order to access another downstream protected Web API on behalf of a user
     * using the On-Behalf-Of flow. This confidential client application was itself called with a token
     * which will be provided in the {@link UserAssertion} to the {@link OnBehalfOfParameters}
     * @param parameters instance of {@link OnBehalfOfParameters}
     * @return {@link CompletableFuture} containing an {@link IAuthenticationResult}
     */
    CompletableFuture<IAuthenticationResult> acquireToken(OnBehalfOfParameters parameters);
}
