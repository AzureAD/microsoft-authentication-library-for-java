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

import javax.net.ssl.SSLSocketFactory;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public interface IClientApplicationBase {

    String DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common/";

    /**
     * Client ID (Application ID) of the application as registered in the application registration portal
     * (portal.azure.com) and as passed in the constructor of the application
     */
    String clientId();

    /**
     * URL of the authority, or security token service (STS) from which MSAL will acquire security tokens.
     * Default value is {@link this#DEFAULT_AUTHORITY}
     */
    String authority();

    /**
     * Determine whether the authority needs to be verified against a list of known authorities.
     */
    boolean validateAuthority();

    /**
     * Correlation Id is to be used for diagnostics purposes, is attached to token service requests
     * Default value is random UUID
     */
    String correlationId();

    /**
     * Determine whether Pii (personally identifiable information) will be logged in
     */
    boolean logPii();

    /**
     * Proxy used by the application for all network communication.
     */
    Proxy proxy();

    /**
     * SSLSocketFactory used by the application for all network communication.
     */
    SSLSocketFactory sslSocketFactory();


    ITokenCache tokenCache();

    /**
     * Telemetry consumer that will receive telemetry events emitted by the library.
     */
    java.util.function.Consumer<java.util.List<java.util.HashMap<String, String>>> telemetryConsumer();


    /**
     * Acquires security token from the authority using an authorization code previously received.
     *
     * @param parameters#authorizationCode The authorization code received from service authorization endpoint.
     * @param parameters#redirectUri       (also known as Reply URI or Reply URL),
     *                                     is the URI at which Azure AD will contact back the application with the tokens.
     *                                     This redirect URI needs to be registered in the app registration portal.
     * @return A {@link Future} object representing the
     * {@link IAuthenticationResult} of the call. It contains Access
     * Token, Refresh Token and the Access Token's expiration time.
     */
    CompletableFuture<IAuthenticationResult> acquireToken(AuthorizationCodeParameters parameters);

    /**
     * Returning tokens from cache or requesting new one using previously cached refresh tokens
     */
    CompletableFuture<IAuthenticationResult> acquireTokenSilently(SilentParameters parameters)
            throws MalformedURLException;

    /**
     * Returns unique accounts from cache which can be used for silent acquire token call
     */
    CompletableFuture<Set<IAccount>> getAccounts();

    /**
     * Remove account from the cache
     */
    CompletableFuture removeAccount(IAccount account);
}
