// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.jwt.JWTParser;

import java.net.URL;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Used to define the basic set of methods that all Brokers must implement
 *
 * All methods are marked as default so they can be referenced by MSAL Java without an implementation,
 *  and most will simply throw an exception if not overridden by an IBroker implementation
 */
public interface IBroker {

    /**
     * Checks if an IBroker implementation is accessible by MSAL Java
     */
    default boolean isAvailable(){
        return false;
    }

    /**
     * Acquire a token silently, i.e. without direct user interaction
     *
     * This may be accomplished by returning tokens from a token cache, using cached refresh tokens to get new tokens,
     * or via any authentication flow where a user is not prompted to enter credentials
     *
     * @param requestParameters MsalRequest object which contains everything needed for the broker implementation to make a request
     * @return IBroker implementations will return an AuthenticationResult object
     */
    default IAuthenticationResult acquireToken(PublicClientApplication application, SilentParameters requestParameters) throws Exception {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    /**
     * Acquire a token interactively, by prompting users to enter their credentials in some way
     *
     * @param requestParameters MsalRequest object which contains everything needed for the broker implementation to make a request
     * @return IBroker implementations will return an AuthenticationResult object
     */
    default IAuthenticationResult acquireToken(PublicClientApplication application, InteractiveRequestParameters requestParameters) throws Exception {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    /**
     * Acquire a token silently, i.e. without direct user interaction, using username/password authentication
     *
     * @param requestParameters MsalRequest object which contains everything needed for the broker implementation to make a request
     * @return IBroker implementations will return an AuthenticationResult object
     */
    default IAuthenticationResult acquireToken(PublicClientApplication application, UserNamePasswordParameters requestParameters) throws Exception {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    default void removeAccount(PublicClientApplication application, IAccount account) throws Exception {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    //TODO: Any better place to put this helper method? This feels wrong
    //AuthenticationResult requires many package-private classes that broker package can't access, so helper methods will be
    //  made for each broker to create the sort of AuthenticationResult that the rest of MSAL Java expects
    default IAuthenticationResult parseBrokerAuthResult(String authority, String idToken, String accessToken,
                                                            String accountId, String clientInfo,
                                                            long accessTokenExpirationTime) {

        //TODO: need to either make AuthenticationResult public or implement IAuthenticationResult here in the interop layer
        AuthenticationResult.AuthenticationResultBuilder builder =  AuthenticationResult.builder();

        try {
            if (idToken != null) {
                builder.idToken(idToken);
                if (accountId!= null) {
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
        } catch (Exception e) {
            //TODO: throw new msalexception. Could a valid broker result be an invalid MSAL Java result?
        }
        return builder.build();
    }
}