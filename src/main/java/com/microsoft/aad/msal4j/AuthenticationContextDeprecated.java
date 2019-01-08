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
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.JWTBearerGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class representing the authority issuing tokens for resources. It
 * provides several ways to request access token, namely via Authorization Code,
 * Confidential Client and Client Certificate.
 */
public class AuthenticationContextDeprecated {

    final Logger log = LoggerFactory
            .getLogger(AuthenticationContextDeprecated.class);

    final AuthenticationAuthority authenticationAuthority;
    String correlationId;
    private String authority;
    private final ExecutorService service;
    private final boolean validateAuthority;

    Proxy proxy;
    SSLSocketFactory sslSocketFactory;

    private boolean logPii = false;

    /**
     * Returns logPii - boolean value, which determines
     * whether Pii (personally identifiable information) will be logged in
     *
     * @return boolean value of logPii
     */
    public boolean isLogPii() {
        return logPii;
    }

    /**
     * Set logPii - boolean value, which determines
     * whether Pii (personally identifiable information) will be logged in
     *
     * @param logPii boolean value
     */
    public void setLogPii(boolean logPii) {
        this.logPii = logPii;
    }

    /**
     * Constructor to create the context with the address of the authority.
     *
     * @param authority
     *            URL of the authenticating authority
     * @param validateAuthority
     *            flag to enable/disable authority validation.
     * @param service
     *            ExecutorService to be used to execute the requests. Developer
     *            is responsible for maintaining the lifetime of the
     *            ExecutorService.
     * @throws MalformedURLException
     *             thrown if URL is invalid
     */
    public AuthenticationContextDeprecated(final String authority,
            final boolean validateAuthority, final ExecutorService service)
            throws MalformedURLException {

        if (StringHelper.isBlank(authority)) {
            throw new IllegalArgumentException("authority is null or empty");
        }

        if (service == null) {
            throw new IllegalArgumentException("service is null");
        }
        this.service = service;
        this.validateAuthority = validateAuthority;
        this.authority = this.canonicalizeUri(authority);

        authenticationAuthority = new AuthenticationAuthority(new URL(
                this.getAuthority()));
    }

    /**
     * Returns Proxy configuration
     *
     * @return Proxy Object
     */
    public Proxy getProxy() {
        return proxy;
    }

    /**
     * Sets Proxy configuration to be used by the context for all network
     * communication. Default is null and system defined properties if any,
     * would be used.
     *
     * @param proxy
     *            Proxy configuration object
     */
    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    /**
     * Returns SSLSocketFactory configuration object.
     * 
     * @return SSLSocketFactory object
     */
    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    /**
     * Sets SSLSocketFactory object to be used by the context.
     * 
     * @param sslSocketFactory The SSL factory object to set
     */
    public void setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    private String canonicalizeUri(String authority) {
        if (!authority.endsWith("/")) {
            authority += "/";
        }
        return authority;
    }

    /**
     * Acquires a security token from the authority using a username/password flow.
     *
     * @param clientId
     *            Name or ID of the client requesting the token.
     * @param resource
     *            Identifier of the target resource that is the recipient of the
     *            requested token.
     * @param username
     *            Username of the managed or federated user.
     * @param password
     *            Password of the managed or federated user.
     *            If null, integrated authentication will be used.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     */
    // public - userName password
    // moved
    public Future<AuthenticationResult> acquireToken(final String resource,
            final String clientId, final String username,
            final String password, final AuthenticationCallback callback) {
        if (StringHelper.isBlank(resource)) {
            throw new IllegalArgumentException("resource is null or empty");
        }

        if (StringHelper.isBlank(clientId)) {
            throw new IllegalArgumentException("clientId is null or empty");
        }

        if (StringHelper.isBlank(username)) {
            throw new IllegalArgumentException("username is null or empty");
        }

        ClientAuthenticationPost clientAuth = new ClientAuthenticationPost(ClientAuthenticationMethod.NONE, new ClientID(clientId));

        if (password != null) {
            //return this.acquireToken(new MsalOAuthAuthorizationGrant(
              //      new ResourceOwnerPasswordCredentialsGrant(username, new Secret(
                //            password)), resource), clientAuth, callback);
        } else {
            //return this.acquireToken(new MsalIntegratedAuthorizationGrant(username, resource), clientAuth, callback);
        }
        return null;
    }

    /**
     * Acquires security token from the authority.
     *
     * @param resource
     *            Identifier of the target resource that is the recipient of the
     *            requested token.
     * @param clientAssertion
     *            The client assertion to use for client authentication.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token and the Access Token's expiration time. Refresh Token
     *         property will be null for this overload.
     */
    // confidential - client assertion is internal impl details
    // not needed
    public Future<AuthenticationResult> acquireToken(final String resource,
            final ClientAssertion clientAssertion,
            final AuthenticationCallback callback) {

        this.validateInput(resource, clientAssertion, true);
        final ClientAuthentication clientAuth = createClientAuthFromClientAssertion(clientAssertion);
        final MsalOAuthAuthorizationGrant authGrant = new MsalOAuthAuthorizationGrant(
                new ClientCredentialsGrant(), resource);
        //return this.acquireToken(authGrant, clientAuth, callback);
        return null;
    }

    /**
     * Acquires an access token from the authority on behalf of a user. It
     * requires using a user token previously received.
     *
     * @param scope
     *            Identifier of the target resource that is the recipient of the
     *            requested token.
     * @param userAssertion
     *            userAssertion to use as Authorization grant
     * @param credential
     *            The client credential to use for token acquisition.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token and the Access Token's expiration time. Refresh Token
     *         property will be null for this overload.
     * @throws AuthenticationException {@link AuthenticationException}
     */
    // confidential - client secret grant - OBO
    // moved
 /*   public Future<AuthenticationResult> acquireToken(final String scope,
            final UserAssertion userAssertion, final ClientSecret credential,
            final AuthenticationCallback callback) {

        this.validateOnBehalfOfRequestInput(scope, userAssertion, credential, true);
        final ClientAuthentication clientAuth = new ClientSecretPost(
                new ClientID(credential.getClientId()), new Secret(
                credential.getClientSecret()));
        return acquireTokenOnBehalfOf(scope, userAssertion, clientAuth, callback);
    }
    */

    /**
     * Acquires an access token from the authority on behalf of a user. It
     * requires using a user token previously received. Uses certificate to
     * authenticate client.
     *
    // * @param resource
     *            Identifier of the target resource that is the recipient of the
     *            requested token.
     * @param userAssertion
     *            userAssertion to use as Authorization grant
    // * @param credential
     *            The certificate based client credential to use for token acquisition.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token and the Access Token's expiration time. Refresh Token
     *         property will be null for this overload.
     * @throws AuthenticationException {@link AuthenticationException}
     */
    // confidential - client certificate grant - OBO
    // moved
    /*
    public Future<AuthenticationResult> acquireToken(final String resource,
                                                     final UserAssertion userAssertion,
                                                     final AsymmetricKeyCredential credential,
                                                     final AuthenticationCallback callback) {

        this.validateOnBehalfOfRequestInput(resource, userAssertion, credential, true);
        ClientAssertion clientAssertion =
                JwtHelper.buildJwt(credential, this.authenticationAuthority.getSelfSignedJwtAudience());
        final ClientAuthentication clientAuth = createClientAuthFromClientAssertion(clientAssertion);
        return acquireTokenOnBehalfOf(resource, userAssertion, clientAuth, callback);
    }
    */

    private Future<AuthenticationResult> acquireTokenOnBehalfOf(final String scope,
                                                     final UserAssertion userAssertion,
                                                     final ClientAuthentication clientAuthentication,
                                                     final AuthenticationCallback callback) {

        Map<String, String> params = new HashMap<String, String>();
        params.put("scope", scope);
        params.put("requested_token_use", "on_behalf_of");
        try {
            MsalOAuthAuthorizationGrant grant = new MsalOAuthAuthorizationGrant(
            new JWTBearerGrant(SignedJWT.parse(userAssertion.getAssertion())), params);

            //return this.acquireToken(grant, clientAuthentication, callback);
        }
        catch (final Exception e) {
            throw new AuthenticationException(e);
        }
        return null;
    }


    /**
     * Acquires security token from the authority.
     *
     * @param resource
     *            Identifier of the target resource that is the recipient of the
     *            requested token.
     * @param credential
     *            The client credential to use for token acquisition.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token and the Access Token's expiration time. Refresh Token
     *         property will be null for this overload.
     */
    // confidential - client secret grant
    // moved
    /*
    public Future<AuthenticationResult> acquireToken(final String resource,
            final ClientSecret credential,
            final AuthenticationCallback callback) {
        this.validateInput(resource, credential, true);
        final ClientAuthentication clientAuth = new ClientSecretPost(
                new ClientID(credential.getClientId()), new Secret(
                        credential.getClientSecret()));
        final MsalOAuthAuthorizationGrant authGrant = new MsalOAuthAuthorizationGrant(
                new ClientCredentialsGrant(), resource);
        return this.acquireToken(authGrant, clientAuth, callback);
    }
    */

    /**
     * Acquires security token from the authority.
     *
     * @param resource
     *            Identifier of the target resource that is the recipient of the
     *            requested token.
     * @param credential
     *            object representing Private Key to use for token acquisition.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token and the Access Token's expiration time. Refresh Token
     *         property will be null for this overload.
     * @throws AuthenticationException {@link AuthenticationException}
     */
    // confidential - client certificate grant
    // moved
    /*
    public Future<AuthenticationResult> acquireToken(final String resource,
            final AsymmetricKeyCredential credential,
            final AuthenticationCallback callback)
            throws AuthenticationException {
        return this.acquireToken(resource, JwtHelper.buildJwt(credential,
                this.authenticationAuthority.getSelfSignedJwtAudience()),
                callback);
    }
    */

    /**
     * Acquires security token from the authority using an authorization code
     * previously received.
     *
     * @param authorizationCode
     *            The authorization code received from service authorization
     * @param resource
     *            Identifier of the target resource that is the recipient of the
     *            requested token.
     * @param clientId
     *            The client assertion to use for token acquisition endpoint.
     * @param redirectUri
     *            The redirect address used for obtaining authorization code.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     */
    // public - authorization code grant
    // do we need to pass scopes here ?
    // moved
    public Future<AuthenticationResult> acquireTokenByAuthorizationCode(
            final String authorizationCode, final String resource,
            final String clientId, final URI redirectUri,
            final AuthenticationCallback callback) {

        final ClientAuthentication clientAuth = new ClientAuthenticationPost(
                ClientAuthenticationMethod.NONE, new ClientID(clientId));

        this.validateAuthCodeRequestInput(authorizationCode, redirectUri,
                clientAuth, resource);
        final MsalOAuthAuthorizationGrant authGrant = new MsalOAuthAuthorizationGrant(
                new AuthorizationCodeGrant(new AuthorizationCode(
                        authorizationCode), redirectUri), resource);
        //return this.acquireToken(authGrant, clientAuth, callback);
        return null;
    }

    /**
     * Acquires security token from the authority using an authorization code
     * previously received.
     *
     * @param authorizationCode
     *            The authorization code received from service authorization
     *            endpoint.
     * @param redirectUri
     *            The redirect address used for obtaining authorization code.
     * @param clientAssertion
     *            The client assertion to use for client authentication.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     */
    // confidential - authorization code grant
    // moved
    public Future<AuthenticationResult> acquireTokenByAuthorizationCode(
            final String authorizationCode, final URI redirectUri,
            final ClientAssertion clientAssertion,
            final AuthenticationCallback callback) {
        return acquireTokenByAuthorizationCode(authorizationCode,  redirectUri,
                clientAssertion, (String) null, callback);
    }

    /**
     * Acquires security token from the authority using an authorization code
     * previously received.
     *
     * @param authorizationCode
     *            The authorization code received from service authorization
     *            endpoint.
     * @param redirectUri
     *            The redirect address used for obtaining authorization code.
     * @param clientAssertion
     *            The client assertion to use for client authentication.
     * @param resource
     *            Identifier of the target resource that is the recipient of the
     *            requested token. It can be null if provided earlier to acquire
     *            authorizationCode.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     */
    // confidential - authorization code grant
    // moved
    public Future<AuthenticationResult> acquireTokenByAuthorizationCode(
            final String authorizationCode, final URI redirectUri,
            final ClientAssertion clientAssertion, final String resource,
            final AuthenticationCallback callback) {

        this.validateAuthCodeRequestInput(authorizationCode, redirectUri,
                clientAssertion, resource);
        final ClientAuthentication clientAuth = createClientAuthFromClientAssertion(clientAssertion);
        final MsalOAuthAuthorizationGrant authGrant = new MsalOAuthAuthorizationGrant(
                new AuthorizationCodeGrant(new AuthorizationCode(
                        authorizationCode), redirectUri), resource);
        //return this.acquireToken(authGrant, clientAuth, callback);
        return null;
    }

    /**
     * Acquires security token from the authority using an authorization code
     * previously received.
     *
     * @param authorizationCode
     *            The authorization code received from service authorization
     *            endpoint.
     * @param redirectUri
     *            The redirect address used for obtaining authorization code.
     * @param credential
     *            The client credential to use for token acquisition.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     */
    // confidential - authorization code grant - client secret
    // moved
    public Future<AuthenticationResult> acquireTokenByAuthorizationCode(
            final String authorizationCode, final URI redirectUri,
            final ClientSecret credential,
            final AuthenticationCallback callback) {
        this.validateAuthCodeRequestInput(authorizationCode, redirectUri,
                credential, null);
        //return this.acquireTokenByAuthorizationCode(authorizationCode,
          //      redirectUri, credential, null, callback);
        return null;
    }

    /**
     * Acquires security token from the authority using an authorization code
     * previously received.
     *
     * @param authorizationCode
     *            The authorization code received from service authorization
     *            endpoint.
     * @param redirectUri
     *            The redirect address used for obtaining authorization code.
     * @param credential
     *            The client credential to use for token acquisition.
     * @param resource
     *            Identifier of the target resource that is the recipient of the
     *            requested token. It can be null if provided earlier to acquire
     *            authorizationCode.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     */
    // confidential - authorization code grant - client secret
    // moved
    /*
    public Future<AuthenticationResult> acquireTokenByAuthorizationCode(
            final String authorizationCode, final URI redirectUri,
            final ClientSecret credential, final String resource,
            final AuthenticationCallback callback) {

        this.validateAuthCodeRequestInput(authorizationCode, redirectUri,
                credential, resource);
        final ClientAuthentication clientAuth = new ClientSecretPost(
                new ClientID(credential.getClientId()), new Secret(
                        credential.getClientSecret()));
        final MsalOAuthAuthorizationGrant authGrant = new MsalOAuthAuthorizationGrant(
                new AuthorizationCodeGrant(new AuthorizationCode(
                        authorizationCode), redirectUri), resource);
        return this.acquireToken(authGrant, clientAuth, callback);

    }
    */

    /**
     * Acquires security token from the authority using an authorization code
     * previously received.
     *
     * @param authorizationCode
     *            The authorization code received from service authorization
     *            endpoint.
     * @param redirectUri
     *            The redirect address used for obtaining authorization code.
     * @param credential
     *            object representing Private Key to use for token acquisition.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     * @throws AuthenticationException
     *             thrown if {@link AsymmetricKeyCredential} fails to sign the
     *             JWT token.
     */
    // confidential - authorization code grant - client cert
    // moved
    public Future<AuthenticationResult> acquireTokenByAuthorizationCode(
            final String authorizationCode, final URI redirectUri,
            final AsymmetricKeyCredential credential,
            final AuthenticationCallback callback)
            throws AuthenticationException {
        //return this.acquireTokenByAuthorizationCode(authorizationCode,
          //      redirectUri, credential, null, callback);
        return null;
    }

    /**
     * Acquires security token from the authority using an authorization code
     * previously received.
     *
     * @param authorizationCode
     *            The authorization code received from service authorization
     *            endpoint.
     * @param redirectUri
     *            The redirect address used for obtaining authorization code.
     * @param credential
     *            object representing Private Key to use for token acquisition.
     * @param resource
     *            Identifier of the target resource that is the recipient of the
     *            requested token. It can be null if provided earlier to acquire
     *            authorizationCode.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     * @throws AuthenticationException
     *             thrown if {@link AsymmetricKeyCredential} fails to sign the
     *             JWT token.
     */
    // confidential - authorization code grant - client cert
    // moved
    /*
    public Future<AuthenticationResult> acquireTokenByAuthorizationCode(
            final String authorizationCode, final URI redirectUri,
            final AsymmetricKeyCredential credential, final String resource,
            final AuthenticationCallback callback)
            throws AuthenticationException {
        this.validateAuthCodeRequestInput(authorizationCode, redirectUri,
                credential, resource);
        return this.acquireTokenByAuthorizationCode(authorizationCode,
                redirectUri, JwtHelper
                        .buildJwt(credential, this.authenticationAuthority
                                .getSelfSignedJwtAudience()), resource,
                callback);
    }
    */

    /**
     * Acquires a device code from the authority
     *
     * @param clientId Identifier of the client requesting the token
     * @param resource Identifier of the target resource that is the recipient of the
     *                 requested token.
     * @param callback optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the {@link DeviceCode} of the call.
     * It contains device code, user code, its expiration date,
     * message which should be displayed to the user.
     * @throws AuthenticationException thrown if the device code is not acquired successfully
     */
    // public - get device code
    /*
    public Future<DeviceCode> acquireDeviceCode(final String clientId, final String resource,
                                                final AuthenticationCallback<DeviceCode> callback) {
        validateDeviceCodeRequestInput(clientId, resource);
        return service.submit(
                new AcquireDeviceCodeCallable(this, clientId, resource, callback));
    }
    */

    /**
     * Acquires security token from the authority using an device code previously received.
     *
     * @param deviceCode The device code result received from calling acquireDeviceCode.
     * @param callback   optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the {@link AuthenticationResult} of the call.
     * It contains AccessToken, Refresh Token and the Access Token's expiration time.
     * @throws AuthenticationException thrown if authorization is pending or another error occurred.
     *                                 If the errorCode of the exception is AdalErrorCode.AUTHORIZATION_PENDING,
     *                                 the call needs to be retried until the AccessToken is returned.
     *                                 DeviceCode.interval - The minimum amount of time in seconds that the client
     *                                 SHOULD wait between polling requests to the token endpoin
     */
    // public - device code grant
    public Future<AuthenticationResult> acquireTokenByDeviceCode(
            final DeviceCode deviceCode, final AuthenticationCallback callback)
            throws AuthenticationException {

        final ClientAuthentication clientAuth = new ClientAuthenticationPost(
                ClientAuthenticationMethod.NONE, new ClientID(deviceCode.getClientId()));

        this.validateDeviceCodeRequestInput(deviceCode, clientAuth, deviceCode.getScopes());

        final MsalDeviceCodeAuthorizationGrant deviceCodeGrant = new MsalDeviceCodeAuthorizationGrant(deviceCode, deviceCode.getScopes());

        //return this.acquireToken(deviceCodeGrant, clientAuth, callback);
        return null;
    }

    /**
     * Acquires a security token from the authority using a Refresh Token
     * previously received.
     *
     * @param refreshToken
     *            Refresh Token to use in the refresh flow.
     * @param clientId
     *            Name or ID of the client requesting the token.
     * @param clientAssertion
     *            The client assertion to use for client authentication.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     */
    // confidential - refresh token grant
    // moved
    public Future<AuthenticationResult> acquireTokenByRefreshToken(
            final String refreshToken, final String clientId,
            final ClientAssertion clientAssertion,
            final AuthenticationCallback callback) {
        return acquireTokenByRefreshToken(refreshToken, clientId, clientAssertion,
                null, callback);
    }

    /**
     * Acquires a security token from the authority using a Refresh Token
     * previously received.
     *
     * @param refreshToken
     *            Refresh Token to use in the refresh flow.
     * @param clientId
     *            Name or ID of the client requesting the token.
     * @param clientAssertion
     *            The client assertion to use for client authentication.
     * @param resource
     *            Identifier of the target resource that is the recipient of the
     *            requested token. If null, token is requested for the same
     *            resource refresh token was originally issued for. If passed,
     *            resource should match the original resource used to acquire
     *            refresh token unless token service supports refresh token for
     *            multiple resources.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     */
    // confidential - refresh token grant
    // moved
    public Future<AuthenticationResult> acquireTokenByRefreshToken(
            final String refreshToken, final String clientId,
            final ClientAssertion clientAssertion, final String resource,
            final AuthenticationCallback callback) {
        this.validateRefreshTokenRequestInput(refreshToken, clientId,
                clientAssertion);
        final ClientAuthentication clientAuth = createClientAuthFromClientAssertion(clientAssertion);
        final MsalOAuthAuthorizationGrant authGrant = new MsalOAuthAuthorizationGrant(
                new RefreshTokenGrant(new RefreshToken(refreshToken)), resource);
        //return this.acquireToken(authGrant, clientAuth, callback);
        return null;
    }

    /**
     * Acquires a security token from the authority using a Refresh Token
     * previously received.
     *
     * @param refreshToken
     *            Refresh Token to use in the refresh flow.
     * @param credential
     *            The client credential used for token acquisition.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     */
    // confidential - refresh token grant
    // moved
    public Future<AuthenticationResult> acquireTokenByRefreshToken(
            final String refreshToken, final ClientSecret credential,
            final AuthenticationCallback callback) {
        //return acquireTokenByRefreshToken(refreshToken, credential,
          //      (String) null, callback);
        return null;
    }

    /**
     * Acquires a security token from the authority using a Refresh Token
     * previously received.
     *
     * @param refreshToken
     *            Refresh Token to use in the refresh flow.
     * @param credential
     *            The client credential used for token acquisition.
     * @param resource
     *            Identifier of the target resource that is the recipient of the
     *            requested token. If null, token is requested for the same
     *            resource refresh token was originally issued for. If passed,
     *            resource should match the original resource used to acquire
     *            refresh token unless token service supports refresh token for
     *            multiple resources.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     */
    // confidential - refresh token grant
    // moved
    /*
    public Future<AuthenticationResult> acquireTokenByRefreshToken(
            final String refreshToken, final ClientSecret credential,
            final String resource, final AuthenticationCallback callback) {

        final ClientAuthentication clientAuth = new ClientSecretPost(
                new ClientID(credential.getClientId()), new Secret(
                        credential.getClientSecret()));
        final MsalOAuthAuthorizationGrant authGrant = new MsalOAuthAuthorizationGrant(
                new RefreshTokenGrant(new RefreshToken(refreshToken)), resource);
        return this.acquireToken(authGrant, clientAuth, callback);
    }
    */

    /**
     * Acquires a security token from the authority using a Refresh Token
     * previously received.
     *
     * @param refreshToken
     *            Refresh Token to use in the refresh flow.
     * @param credential
     *            object representing Private Key to use for token acquisition.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     * @throws AuthenticationException
     *             thrown if {@link AsymmetricKeyCredential} fails to sign the
     *             JWT token.
     */
    // confidential - refresh token grant
    // moved
    public Future<AuthenticationResult> acquireTokenByRefreshToken(
            final String refreshToken,
            final AsymmetricKeyCredential credential,
            final AuthenticationCallback callback)
            throws AuthenticationException {
        //return acquireTokenByRefreshToken(refreshToken, credential,
          //      (String) null, callback);
        return null;
    }

    /**
     * Acquires a security token from the authority using a Refresh Token
     * previously received.
     *
     * @param refreshToken
     *            Refresh Token to use in the refresh flow.
     * @param credential
     *            object representing Private Key to use for token acquisition.
     * @param resource
     *            Identifier of the target resource that is the recipient of the
     *            requested token. If null, token is requested for the same
     *            resource refresh token was originally issued for. If passed,
     *            resource should match the original resource used to acquire
     *            refresh token unless token service supports refresh token for
     *            multiple resources.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     * @throws AuthenticationException
     *             thrown if {@link AsymmetricKeyCredential} fails to sign the
     *             JWT token.
     */
    // confidential - refresh token grant
    // moved
    /*
    public Future<AuthenticationResult> acquireTokenByRefreshToken(
            final String refreshToken,
            final AsymmetricKeyCredential credential, final String resource,
            final AuthenticationCallback callback)
            throws AuthenticationException {

        return acquireTokenByRefreshToken(
                refreshToken,
                credential.getClientId(),
                JwtHelper.buildJwt(credential,
                        this.authenticationAuthority.getSelfSignedJwtAudience()),
                resource, callback);
    }
    */

    /**
     * Acquires a security token from the authority using a Refresh Token
     * previously received. This method is suitable for the daemon OAuth2
     * flow when a client secret is not possible.
     *
     * @param refreshToken
     *            Refresh Token to use in the refresh flow.
     * @param clientId
     *            Name or ID of the client requesting the token.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     * @throws AuthenticationException
     *             thrown if the access token is not refreshed successfully
     */
    // public - refresh token grant
    // moved
    public Future<AuthenticationResult> acquireTokenByRefreshToken(
            final String refreshToken, final String clientId,
            final AuthenticationCallback callback) {

        return acquireTokenByRefreshToken(refreshToken, clientId, (String)null, callback);
    }
    /*

    private Future<AuthenticationResult> acquireToken(
            final MsalAuthorizationGrant authGrant,
            final ClientAuthentication clientAuth,
            final AuthenticationCallback<AuthenticationResult> callback) {

        return service.submit(
                new AcquireTokenCallable(this, authGrant, clientAuth, callback));
    }
    */

    /**
     * Acquires a security token from the authority using a Refresh Token
     * previously received. This method is suitable for the daemon OAuth2
     * flow when a client secret is not possible.
     *
     * @param refreshToken
     *            Refresh Token to use in the refresh flow.
     * @param clientId
     *            Name or ID of the client requesting the token.
     * @param resource
     *            Identifier of the target resource that is the recipient of the
     *            requested token. If null, token is requested for the same
     *            resource refresh token was originally issued for. If passed,
     *            resource should match the original resource used to acquire
     *            refresh token unless token service supports refresh token for
     *            multiple resources.
     * @param callback
     *            optional callback object for non-blocking execution.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     * @throws AuthenticationException
     *             thrown if the access token is not refreshed successfully
     */
    // public - refresh token grant
    // moved
    public Future<AuthenticationResult> acquireTokenByRefreshToken(
            final String refreshToken, final String clientId,
            final String resource, final AuthenticationCallback callback) {

        final ClientAuthentication clientAuth = new ClientAuthenticationPost(
                ClientAuthenticationMethod.NONE, new ClientID(clientId));
        final MsalOAuthAuthorizationGrant authGrant = new MsalOAuthAuthorizationGrant(
                new RefreshTokenGrant(new RefreshToken(refreshToken)), resource);
        //return this.acquireToken(authGrant, clientAuth, callback);
        return null;
    }

    private void validateRefreshTokenRequestInput(final String refreshToken,
            final String clientId, final Object credential) {

        if (StringHelper.isBlank(refreshToken)) {
            throw new IllegalArgumentException("refreshToken is null or empty");
        }

        if (StringHelper.isBlank(clientId)) {
            throw new IllegalArgumentException("clientId is null or empty");
        }
        this.validateInput(null, credential, false);
    }

    AuthenticationResult acquireTokenCommon(
            final AbstractMsalAuthorizationGrant authGrant,
            final ClientAuthentication clientAuth,
            final ClientDataHttpHeaders headers) throws Exception {
        if(logPii) {
            log.debug(LogHelper.createMessage(
                    String.format("Using Client Http Headers: %s", headers),
                    headers.getHeaderCorrelationIdValue()));
        }

        this.authenticationAuthority.doInstanceDiscovery(validateAuthority,
                headers.getReadonlyHeaderMap(), this.proxy,
                this.sslSocketFactory);
        final URL url = new URL(this.authenticationAuthority.getTokenUri());
        final AdalTokenRequest request = new AdalTokenRequest(url, clientAuth,
                authGrant, headers.getReadonlyHeaderMap(), this.proxy,
                this.sslSocketFactory);
        AuthenticationResult result = request
                .executeOAuthRequestAndProcessResponse();
        return result;
    }

    private ClientAuthentication createClientAuthFromClientAssertion(
            final ClientAssertion clientAssertion) {

        try {
            final Map<String, String> map = new HashMap<String, String>();
            map.put("client_assertion_type", clientAssertion.getAssertionType());
            map.put("client_assertion", clientAssertion.getAssertion());
            return PrivateKeyJWT.parse(map);
        }
        catch (final ParseException e) {
            throw new AuthenticationException(e);
        }
    }

    /**
     * Returns the correlation id configured by the user. It does not return the
     * id automatically generated by the API in case the user does not provide
     * one.
     *
     * @return String value of the correlation id
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Set optional correlation id to be used by the API. If not provided, the
     * API generates a random id.
     *
     * @param correlationId
     *            String value
     */
    public void setCorrelationId(final String correlationId) {
        this.correlationId = correlationId;
    }

    /**
     * Returns validateAuthority boolean value passed as a constructor
     * parameter.
     *
     * @return boolean value
     */
    public boolean shouldValidateAuthority() {
        return this.validateAuthority;
    }

    /**
     * Authority associated with the context instance
     *
     * @return String value
     */
    public String getAuthority() {
        return this.authority;
    }

    private void validateInput(final String resource, final Object credential,
                                    final boolean validateResource) {
        if (validateResource && StringHelper.isBlank(resource)) {
            throw new IllegalArgumentException("resource is null or empty");
        }
        if (credential == null) {
            throw new IllegalArgumentException("credential is null");
        }
    }

    private void validateAuthCodeRequestInput(final String authorizationCode,
            final URI redirectUri, final Object clientCredential,
            final String resource) {
        if (StringHelper.isBlank(authorizationCode)) {
            throw new IllegalArgumentException(
                    "authorization code is null or empty");
        }
        if (redirectUri == null) {
            throw new IllegalArgumentException("redirect uri is null");
        }
        this.validateInput(resource, clientCredential, false);
    }

  private void validateDeviceCodeRequestInput(String clientId, String resource) {
        if (StringHelper.isBlank(clientId)) {
            throw new IllegalArgumentException("clientId is null or empty");
        }

        if (StringHelper.isBlank(resource)) {
            throw new IllegalArgumentException("resource is null or empty");
        }

        if (AuthorityType.ADFS.equals(authenticationAuthority.getAuthorityType())){
            throw new IllegalArgumentException(
                    "Invalid authority type. Device Flow is not supported by ADFS authority");
        }
    }

    private void validateDeviceCodeRequestInput(final DeviceCode deviceCode,
                                                final Object credential,
                                                final String resource) {
        if (StringHelper.isBlank(deviceCode.getDeviceCode())) {
            throw new IllegalArgumentException("device code is null or empty");
        }
        if (StringHelper.isBlank(deviceCode.getCorrelationId())) {
            throw new IllegalArgumentException("correlation id in device code is null or empty");
        }
        this.validateInput(resource, credential, true);
    }

    private void validateOnBehalfOfRequestInput(final String resource, final UserAssertion userAssertion,
                                                final Object clientCredential, final boolean validateResource) {
        if (userAssertion == null) {
            throw new IllegalArgumentException("userAssertion is null");
        }
        this.validateInput(resource, clientCredential, validateResource);
    }
}
