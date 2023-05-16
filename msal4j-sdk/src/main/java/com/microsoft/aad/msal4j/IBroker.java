// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.jwt.JWTParser;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Used to define the basic set of methods that all Brokers must implement
 * <p>
 * All methods are marked as default so they can be referenced by MSAL Java without an implementation,
 * and most will simply throw an exception if not overridden by an IBroker implementation
 */
public interface IBroker {

    /**
     * Acquire a token silently, i.e. without direct user interaction
     * <p>
     * This may be accomplished by returning tokens from a token cache, using cached refresh tokens to get new tokens,
     * or via any authentication flow where a user is not prompted to enter credentials
     */
    default CompletableFuture<IAuthenticationResult> acquireToken(PublicClientApplication application, SilentParameters requestParameters) {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    /**
     * Acquire a token interactively, by prompting users to enter their credentials in some way
     */
    default CompletableFuture<IAuthenticationResult> acquireToken(PublicClientApplication application, InteractiveRequestParameters parameters) {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    /**
     * Acquire a token silently, i.e. without direct user interaction, using username/password authentication
     */
    default CompletableFuture<IAuthenticationResult> acquireToken(PublicClientApplication application, UserNamePasswordParameters parameters) {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    default void removeAccount(PublicClientApplication application, IAccount account) throws MsalClientException {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    /**
     * Returns whether a broker is available and ready to use on this machine, allowing the use of the methods
     * in this interface and other broker-only features in MSAL Java
     */
    default boolean isBrokerAvailable() {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    /**
     * MSAL Java's AuthenticationResult requires several package-private classes that a broker implementation can't access,
     * so this helper method can be used to create AuthenticationResults from within the MSAL Java package
     */
    default IAuthenticationResult parseBrokerAuthResult(String authority, String idToken, String accessToken,
                                                        String accountId, String clientInfo,
                                                        long accessTokenExpirationTime,
                                                        boolean isPopAuthorization) {

        AuthenticationResult.AuthenticationResultBuilder builder = AuthenticationResult.builder();

        try {
            if (idToken != null) {
                builder.idToken(idToken);
                if (accountId != null) {
                    String idTokenJson =
                            JWTParser.parse(idToken).getParsedParts()[1].decodeToString();
                    //TODO: need to figure out if 'policy' field is relevant for brokers
                    builder.accountCacheEntity(AccountCacheEntity.create(clientInfo,
                            Authority.createAuthority(new URL(authority)), JsonHelper.convertJsonToObject(idTokenJson,
                                    IdToken.class), null));
                }
            }
            if (accessToken != null) {
                builder.accessToken(accessToken);
                builder.expiresOn(accessTokenExpirationTime);
            }

            builder.isPopAuthorization(isPopAuthorization);

        } catch (Exception e) {
            throw new MsalClientException(String.format("Exception when converting broker result to MSAL Java AuthenticationResult: %s", e.getMessage()), AuthenticationErrorCode.MSALJAVA_BROKERS_ERROR);
        }
        return builder.build();
    }
}