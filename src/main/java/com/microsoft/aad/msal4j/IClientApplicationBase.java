// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.net.ssl.SSLSocketFactory;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Interface representing an application for which tokens can be acquired.
 */
interface IClientApplicationBase {

    String DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common/";

    /**
     * @return Client ID (Application ID) of the application as registered in the application registration portal
     * (portal.azure.com) and as passed in the constructor of the application
     */
    String clientId();

    /**
     * @return URL of the authority, or security token service (STS) from which MSAL will acquire security tokens.
     * Default value is {@link IClientApplicationBase#DEFAULT_AUTHORITY}
     */
    String authority();

    /**
     * @return a boolean value which determines whether the authority needs to be verified against a list of known authorities.
     */
    boolean validateAuthority();

    /**
     * @return Correlation Id which is used for diagnostics purposes, is attached to token service requests
     * Default value is random UUID
     */
    String correlationId();

    /**
     * @return a boolean value which determines whether Pii (personally identifiable information) will be logged in
     */
    boolean logPii();

    /**
     * @return proxy used by the application for all network communication.
     */
    Proxy proxy();

    /**
     * @return SSLSocketFactory used by the application for all network communication.
     */
    SSLSocketFactory sslSocketFactory();

    /**
     * @return Cache holding access tokens, refresh tokens, id tokens. It is maintained and used silently
     * if needed when calling {@link IClientApplicationBase#acquireTokenSilently(SilentParameters)}
     */
    ITokenCache tokenCache();

//    /**
//     * @return Telemetry consumer that will receive telemetry events emitted by the library.
//     */
//     java.util.function.Consumer<java.util.List<java.util.HashMap<String, String>>> telemetryConsumer();

    /**
     * Computes the URL of the authorization request letting the user sign-in and consent to the
     * application. The URL target the /authorize endpoint of the authority configured in the
     * application object.
     * <p>
     * Once the user successfully authenticates, the response should contain an authorization code,
     * which can then be passed in to{@link AbstractClientApplicationBase#acquireToken(AuthorizationCodeParameters)}
     * to be exchanged for a token
     *
     * @param parameters {@link AuthorizationRequestUrlParameters}
     * @return url of the authorization endpoint where the user can sign-in and consent to the application.
     */
    URL getAuthorizationRequestUrl(AuthorizationRequestUrlParameters parameters);

    /**
     * Acquires security token from the authority using an authorization code previously received.
     *
     * @param parameters {@link AuthorizationCodeParameters}
     * @return A {@link CompletableFuture} object representing the {@link IAuthenticationResult} of the call.
     */
    CompletableFuture<IAuthenticationResult> acquireToken(AuthorizationCodeParameters parameters);

    /**
     * Acquires a security token from the authority using a refresh token previously received.
     * Can be used in migration to MSAL from ADAL, and in various integration
     * scenarios where you have a refresh token available.
     *
     * @param parameters {@link RefreshTokenParameters}
     * @return A {@link CompletableFuture} object representing the {@link IAuthenticationResult} of the call.
     */
    CompletableFuture<IAuthenticationResult> acquireToken(RefreshTokenParameters parameters);

    /**
     * Returns tokens from cache if present and not expired or acquires new tokens from the authority
     * by using the refresh token present in cache.
     *
     * @param parameters instance of SilentParameters
     * @return A {@link CompletableFuture} object representing the {@link IAuthenticationResult} of the call.
     * @throws MalformedURLException if authorityUrl from parameters is malformed URL
     */
    CompletableFuture<IAuthenticationResult> acquireTokenSilently(SilentParameters parameters)
            throws MalformedURLException;

    /**
     * Returns accounts in the cache
     *
     * @return set of unique accounts from cache which can be used for silent acquire token call
     */
    CompletableFuture<Set<IAccount>> getAccounts();

    /**
     * Removes IAccount from the cache
     *
     * @param account instance of Account to be removed from cache
     * @return {@link CompletableFuture} object representing account removal task.
     */
    CompletableFuture removeAccount(IAccount account);
}
