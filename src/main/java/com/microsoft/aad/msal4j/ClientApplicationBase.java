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

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import org.slf4j.Logger;

import javax.net.ssl.SSLSocketFactory;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Abstract class containing common API methods and properties.
 */
abstract public class ClientApplicationBase {
    protected Logger log;
    protected ClientAuthentication clientAuthentication;

    /**
     * Constructor to create the client application with the address of the authority.
     *
     * @param authority URL of the authenticating authority
     * @param clientId Client ID (Application ID) of the application as registered
     *                 in the application registration portal (portal.azure.com)
     * @throws MalformedURLException thrown if URL is invalid
     */
    protected ClientApplicationBase(String authority, String clientId)
            throws MalformedURLException {

        validateNotBlank("authority", authority);
        validateNotBlank("clientId", clientId);

        this.authority = this.canonicalizeUri(authority);
        this.clientId = clientId;

        authenticationAuthority = new AuthenticationAuthority(new URL(authority));

        if(authenticationAuthority.detectAuthorityType() != AuthorityType.AAD){
            validateAuthority = false;
        }
    }

    final AuthenticationAuthority authenticationAuthority;

    protected Proxy proxy;

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

    SSLSocketFactory sslSocketFactory;

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

    protected ExecutorService service;

    /**
     * Sets ExecutorService to be used to execute the requests.
     * Developer is responsible for maintaining the lifecycle of the ExecutorService.
     *
     * @param executorService object
     */
    public void setExecutorService(ExecutorService executorService) {

        validateNotNull("service", service);
        this.service = executorService;
    }

    protected boolean logPii = false;
    protected static String DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common/";

    /**
     * Gets the URL of the authority, or security token service (STS) from which MSAL will acquire security tokens
     * The return value of this property is either the value provided by the developer in the constructor of the application,
     * or otherwise the value of the DEFAULT_AUTHORITY {@link ClientApplicationBase#DEFAULT_AUTHORITY}
     */
    private String authority;

    /**
     * Authority associated with the client application instance
     *
     * @return String value
     */
    public String getAuthority() {
        return this.authority;
    }

    /**
     * Gets the Client ID (Application ID) of the application as registered in the application registration portal
     * (portal.azure.com) and as passed in the constructor of the application
     */
    protected String clientId;

    protected String correlationId;

    /**
     * Set optional correlation id to be used by the API. If not provided, the API generates a random id.
     *
     * @param correlationId String value
     */
    public void setCorrelationId(final String correlationId) {
        this.correlationId = correlationId;
    }

    /**
     * Returns the correlation id configured by the user or generated by the API in case the user does not provide one.
     *
     * @return String value of the correlation id
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * A boolean value telling the application if the authority needs to be verified against a list of known authorities.
     * The default value is true.
     */
    private boolean validateAuthority = true;

    /**
     * Sets validateAuthority boolean value for the client application instance
     *
     * @param validateAuthority
     */
    public void setValidateAuthority(boolean validateAuthority)
    {
        if (authenticationAuthority.authorityType != AuthorityType.AAD && validateAuthority) {
            throw new IllegalArgumentException(
                    AuthenticationErrorMessage.UNSUPPORTED_AUTHORITY_VALIDATION);
        }
        this.validateAuthority = validateAuthority;
    }

    /**
     * Returns validateAuthority boolean value for the client application instance
     *
     * @return boolean value
     */
    public boolean isValidateAuthority() {
        return this.validateAuthority;
    }

    private String canonicalizeUri(String authority) {
        if (!authority.endsWith("/")) {
            authority += "/";
        }
        return authority;
    }

    protected CompletableFuture<AuthenticationResult> acquireToken(
            final AbstractMsalAuthorizationGrant authGrant,
            final ClientAuthentication clientAuth) {

        Supplier<AuthenticationResult> supplier = () ->
        {
            AcquireTokenCallable callable =
                    new AcquireTokenCallable(this, authGrant, clientAuth);

            AuthenticationResult result;
            try {
                result = callable.execute();
                callable.logResult(result, callable.headers);
            } catch (Exception ex) {
                log.error(LogHelper.createMessage("Execution of " + this.getClass() + " failed.",
                        callable.headers.getHeaderCorrelationIdValue()), ex);

                throw new CompletionException(ex);
            }
            return result;
        };

        CompletableFuture<AuthenticationResult> future =
                service != null ? CompletableFuture.supplyAsync(supplier, service)
                                : CompletableFuture.supplyAsync(supplier);
        return future;

        //return service.submit(
          //      new AcquireTokenCallable(this, authGrant, clientAuth, callback));
    }

    protected void validateNotBlank(String name, String value) {
        if (StringHelper.isBlank(value)) {
            throw new IllegalArgumentException(name + " is null or empty");
        }
    }

    protected void validateNotNull(String name, Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException(name + " is null");
        }
    }

    /**
     * Acquires security token from the authority using an authorization code
     * previously received.
     *
     * @param authorizationCode The authorization code received from service authorization endpoint.
     * @param redirectUri (also known as Reply URI or Reply URL),
     *                    is the URI at which Azure AD will contact back the application with the tokens.
     *                    This redirect URI needs to be registered in the app registration portal.
     * @param scopes scopes of the access request
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     */
    public Future<AuthenticationResult> acquireTokenByAuthorizationCode(String authorizationCode,
                                                                        URI redirectUri, String scopes)
    {
        validateNotBlank("authorizationCode", authorizationCode);
        validateNotBlank("redirectUri", authorizationCode);

        MsalOAuthAuthorizationGrant authGrant = new MsalOAuthAuthorizationGrant(
                new AuthorizationCodeGrant(new AuthorizationCode(authorizationCode), redirectUri), scopes);

        return this.acquireToken(authGrant, clientAuthentication);
    }

    /**
     * Acquires security token from the authority using an authorization code
     * previously received.
     *
     * @param authorizationCode The authorization code received from service authorization endpoint.
     * @param redirectUri (also known as Reply URI or Reply URL),
     *                    is the URI at which Azure AD will contact back the application with the tokens.
     *                    This redirect URI needs to be registered in the app registration portal.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     */
    public Future<AuthenticationResult> acquireTokenByAuthorizationCode(String authorizationCode, URI redirectUri)
    {
        return acquireTokenByAuthorizationCode(authorizationCode, redirectUri, null);
    }

    /**
     * Acquires a security token from the authority using a Refresh Token
     * previously received.
     *
     * @param refreshToken
     *            Refresh Token to use in the refresh flow.
     * @param scopes scopes of the access request
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     */
    public Future<AuthenticationResult> acquireTokenByRefreshToken(String refreshToken, String scopes) {
        validateNotBlank("refreshToken", refreshToken);

        final MsalOAuthAuthorizationGrant authGrant = new MsalOAuthAuthorizationGrant(
                new RefreshTokenGrant(new RefreshToken(refreshToken)), scopes);

        return this.acquireToken(authGrant, clientAuthentication);
    }

    AuthenticationResult acquireTokenCommon(AbstractMsalAuthorizationGrant authGrant, ClientAuthentication clientAuth,
                                            ClientDataHttpHeaders headers) throws Exception {
        if(logPii) {
            log.debug(LogHelper.createMessage(
                    String.format("Using Client Http Headers: %s", headers),
                    headers.getHeaderCorrelationIdValue()));
        }


        this.authenticationAuthority.doInstanceDiscovery(validateAuthority,
                headers.getReadonlyHeaderMap(), this.proxy,
                this.sslSocketFactory);
        URL url = new URL(this.authenticationAuthority.getTokenUri());
        AdalTokenRequest request = new AdalTokenRequest(url, clientAuth,
                authGrant, headers.getReadonlyHeaderMap(), this.proxy,
                this.sslSocketFactory);
        AuthenticationResult result = request
                .executeOAuthRequestAndProcessResponse();
        return result;
    }
}
