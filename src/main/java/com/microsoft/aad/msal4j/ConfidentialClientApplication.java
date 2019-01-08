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

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ConfidentialClientApplication extends ClientApplicationBase {
    /**
     * Constructor to create the client application with the address of the authority.
     *
     * @param authority         URL of the authenticating authority
     * @param clientId Client ID (Application ID) of the application as registered
     *                 in the application registration portal (portal.azure.com)
     * @param clientCredential The client credential to use for token acquisition.
     * @throws MalformedURLException thrown if URL is invalid
     */
    public ConfidentialClientApplication(String authority, String clientId, IClientCredential clientCredential)
            throws MalformedURLException {

        super(authority, clientId);

        log = LoggerFactory.getLogger(ConfidentialClientApplication.class);

        initClientAuthentication(clientCredential);
    }

    /**
     * Constructor to create the client application with the address of the authority.
     *
     * @param clientId Client ID (Application ID) of the application as registered
     *                 in the application registration portal (portal.azure.com)
     * @param clientCredential The client credential to use for token acquisition.
     * @throws MalformedURLException thrown if URL is invalid
     */
    public ConfidentialClientApplication(String clientId, IClientCredential clientCredential)
            throws MalformedURLException {
        this(DEFAULT_AUTHORITY, clientId, clientCredential);
    }

    private void initClientAuthentication(IClientCredential clientCredential){
        validateNotNull("clientCredential", clientCredential);

        if(clientCredential instanceof ClientSecret){
            clientAuthentication = new ClientSecretPost(new ClientID(clientId),
                    new Secret(((ClientSecret)clientCredential).getClientSecret()));
        }
        else if(clientCredential instanceof AsymmetricKeyCredential){
            ClientAssertion clientAssertion = JwtHelper.buildJwt(clientId,
                    (AsymmetricKeyCredential)clientCredential, this.authenticationAuthority.getSelfSignedJwtAudience());

            clientAuthentication = createClientAuthFromClientAssertion(clientAssertion);
        }
        else{
            throw new IllegalArgumentException("Unsupported client credential");
        }
    }

    private ClientAuthentication createClientAuthFromClientAssertion(
            final ClientAssertion clientAssertion) {

        try {
            final Map<String, String> map = new HashMap<>();
            map.put("client_assertion_type", clientAssertion.getAssertionType());
            map.put("client_assertion", clientAssertion.getAssertion());
            return PrivateKeyJWT.parse(map);
        }
        catch (final ParseException e) {
            throw new AuthenticationException(e);
        }
    }

    /**
     * Acquires security token from the authority.
     *
     * @param scopes scopes of the access request
     * @return A {@link CompletableFuture} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token and the Access Token's expiration time. Refresh Token
     *         property will be null for this overload.
     */
    public CompletableFuture<AuthenticationResult> acquireToken(final String scopes) {
        validateNotBlank("scopes", scopes);

        MsalOAuthAuthorizationGrant authGrant = new MsalOAuthAuthorizationGrant(
                new ClientCredentialsGrant(), scopes);

        return this.acquireToken(authGrant, clientAuthentication);
    }

    /**
     * Acquires an access token from the authority on behalf of a user. It
     * requires using a user token previously received.
     *
     * @param scopes scopes of the access request
     * @param userAssertion
     *            userAssertion to use as Authorization grant
     * @return A {@link CompletableFuture} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token and the Access Token's expiration time. Refresh Token
     *         property will be null for this overload.
     * @throws AuthenticationException {@link AuthenticationException}
     */
    public CompletableFuture<AuthenticationResult> acquireTokenOnBehalfOf(String scopes, UserAssertion userAssertion) {
        validateNotNull("userAssertion", userAssertion);
        validateNotBlank("scopes", scopes);

        return acquireTokenOnBehalfOf(scopes, userAssertion, clientAuthentication);
    }

    private CompletableFuture<AuthenticationResult> acquireTokenOnBehalfOf
            (String scopes, UserAssertion userAssertion, ClientAuthentication clientAuthentication) {
        Map<String, String> params = new HashMap<>();
        params.put("scope", scopes);
        params.put("requested_token_use", "on_behalf_of");
        try {
            MsalOAuthAuthorizationGrant grant = new MsalOAuthAuthorizationGrant(
                    new JWTBearerGrant(SignedJWT.parse(userAssertion.getAssertion())), params);

            return this.acquireToken(grant, clientAuthentication);
        }
        catch (final Exception e) {
            throw new AuthenticationException(e);
        }
    }
}
