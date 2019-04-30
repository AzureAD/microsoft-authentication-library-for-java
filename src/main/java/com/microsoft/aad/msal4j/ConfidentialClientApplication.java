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

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

public class ConfidentialClientApplication extends ClientApplicationBase {

    /**
     * Acquires security token from the authority.
     *
     * @param parameters#scopes scopes of the access request
     * @return A {@link CompletableFuture} object representing the
     * {@link AuthenticationResult} of the call. It contains Access
     * Token and the Access Token's expiration time. Refresh Token
     * property will be null for this overload.
     */
    public CompletableFuture<AuthenticationResult> acquireToken(ClientCredentialParameters parameters) {

        validateNotNull("parameters", parameters);

        ClientCredentialRequest clientCredentialRequest =
                new ClientCredentialRequest(
                        parameters,
                        this,
                        createRequestContext(AcquireTokenPublicApi.ACQUIRE_TOKEN_FOR_CLIENT));

        return this.executeRequest(clientCredentialRequest);
    }

    /**
     * Acquires an access token from the authority on behalf of a user. It
     * requires using a user token previously received.
     *
     * @param parameters#scopes        scopes of the access request
     * @param parameters#userAssertion userAssertion to use as Authorization grant
     * @return A {@link CompletableFuture} object representing the
     * {@link AuthenticationResult} of the call. It contains Access
     * Token and the Access Token's expiration time. Refresh Token
     * property will be null for this overload.
     * @throws AuthenticationException {@link AuthenticationException}
     */
    public CompletableFuture<AuthenticationResult> acquireToken(OnBehalfOfParameters parameters) {

        validateNotNull("parameters", parameters);

        OnBehalfOfRequest oboRequest = new OnBehalfOfRequest(
                parameters,
                this,
                createRequestContext(AcquireTokenPublicApi.ACQUIRE_TOKEN_ON_BEHALF_OF));

        return this.executeRequest(oboRequest);
    }

    private ConfidentialClientApplication(Builder builder) {
        super(builder);

        log = LoggerFactory.getLogger(ConfidentialClientApplication.class);

        initClientAuthentication(builder.clientCredential);
    }

    private void initClientAuthentication(IClientCredential clientCredential) {
        validateNotNull("clientCredential", clientCredential);

        if (clientCredential instanceof ClientSecret) {
            clientAuthentication = new ClientSecretPost(
                    new ClientID(clientId()),
                    new Secret(((ClientSecret) clientCredential).getClientSecret()));
        } else if (clientCredential instanceof AsymmetricKeyCredential) {
            ClientAssertion clientAssertion = JwtHelper.buildJwt(
                    clientId(),
                    (AsymmetricKeyCredential) clientCredential,
                    this.authenticationAuthority.selfSignedJwtAudience());

            clientAuthentication = createClientAuthFromClientAssertion(clientAssertion);
        } else {
            throw new IllegalArgumentException("Unsupported client credential");
        }
    }

    private ClientAuthentication createClientAuthFromClientAssertion(
            final ClientAssertion clientAssertion) {
        try {
            final Map<String, List<String>> map = new HashMap<>();
            map.put("client_assertion_type", Collections.singletonList(clientAssertion.getAssertionType()));
            map.put("client_assertion", Collections.singletonList(clientAssertion.getAssertion()));
            return PrivateKeyJWT.parse(map);
        } catch (final ParseException e) {
            throw new AuthenticationException(e);
        }
    }

    /**
     * Returns instance of Builder of ConfidentialClientApplication
     *
     * @param clientId         Client ID (Application ID) of the application as registered
     *                         in the application registration portal (portal.azure.com)
     * @param clientCredential The client credential to use for token acquisition.
     */
    public static Builder builder(String clientId, IClientCredential clientCredential) {

        return new Builder(clientId, clientCredential);
    }

    public static class Builder extends ClientApplicationBase.Builder<Builder> {

        private IClientCredential clientCredential;

        /**
         * Constructor to create instance of Builder of ConfidentialClientApplication
         *
         * @param clientId         Client ID (Application ID) of the application as registered
         *                         in the application registration portal (portal.azure.com)
         * @param clientCredential The client credential to use for token acquisition.
         */
        Builder(String clientId, IClientCredential clientCredential) {
            super(clientId);
            this.clientCredential = clientCredential;
        }

        @Override
        public ConfidentialClientApplication build() {

            return new ConfidentialClientApplication(this);
        }

        @Override
        protected ConfidentialClientApplication.Builder self() {
            return this;
        }
    }
}