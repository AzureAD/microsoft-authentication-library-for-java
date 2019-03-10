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

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import org.slf4j.Logger;

import javax.net.ssl.SSLSocketFactory;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Abstract class containing common API methods and properties.
 */
abstract public class ClientApplicationBase {

    public static String DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common/";
    protected Logger log;
    protected ClientAuthentication clientAuthentication;
    protected String clientId;
    private String authority;
    protected AuthenticationAuthority authenticationAuthority;
    private boolean validateAuthority;
    private String correlationId;
    private boolean logPii;
    private ServiceBundle serviceBundle;

    /**
     * Returns Proxy configuration to be used by the context for all network communication.
     *
     * @return Proxy Object
     */
    public Proxy getProxy() { return this.serviceBundle.getProxy(); }

    /**
     * Returns SSLSocketFactory to be used by the context for all network communication.
     *
     * @return SSLSocketFactory object
     */

    public SSLSocketFactory getSslSocketFactory() {
        return this.serviceBundle.getSslSocketFactory();
    }

    /**
     * Gets the URL of the authority, or security token service (STS) from which MSAL will acquire security tokens
     * The return value of this property is either the value provided by the developer,
     * or otherwise the value of the DEFAULT_AUTHORITY {@link ClientApplicationBase#DEFAULT_AUTHORITY}
     *
     * @return String value
     */
    public String getAuthority() { return this.authority; }

    /**
     * Gets the Client ID (Application ID) of the application as registered in the application registration portal
     * (portal.azure.com) and as passed in the constructor of the application
     */
    public String getClientId() { return this.clientId; }

    /**
     * Returns logPii - boolean value, which determines
     * whether Pii (personally identifiable information) will be logged in
     *
     * @return boolean value of logPii
     */
    public boolean isLogPii() { return logPii; }

    /**
     * Returns the correlation id configured by the user or generated by the API(random UUID)
     * in case the user does not provide one.
     *
     * @return String value of the correlation id
     */
    public String getCorrelationId() { return correlationId; }

    /**
     * Returns a boolean value telling the application if the authority needs to be verified
     * against a list of known authorities.
     *
     * @return boolean value
     */
    public boolean isValidateAuthority() {
        return this.validateAuthority;
    }

    /**
     * Acquires security token from the authority using an authorization code
     * previously received.
     *
     * @param scopes scopes of the access request
     * @param authorizationCode The authorization code received from service authorization endpoint.
     * @param redirectUri (also known as Reply URI or Reply URL),
     *                    is the URI at which Azure AD will contact back the application with the tokens.
     *                    This redirect URI needs to be registered in the app registration portal.
     * @return A {@link Future} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     */

    public CompletableFuture<AuthenticationResult> acquireTokenByAuthorizationCode(
            Set<String> scopes,
            String authorizationCode,
            URI redirectUri) {

        validateNotBlank("authorizationCode", authorizationCode);
        validateNotBlank("redirectUri", authorizationCode);
        validateNotEmpty("scopes", scopes);

        AuthorizationCodeRequest authorizationCodeRequest =
                new AuthorizationCodeRequest(
                        scopes,
                        authorizationCode,
                        redirectUri,
                        clientAuthentication,
                        new RequestContext(clientId, correlationId));

        return this.InitializeRequest(authorizationCodeRequest);
    }

    /**
     * Acquires a security token from the authority using a Refresh Token
     * previously received.
     *
     * @param refreshToken
     *            Refresh Token to use in the refresh flow.
     * @param scopes scopes of the access request
     * @return A {@link CompletableFuture} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     */

    public CompletableFuture<AuthenticationResult> acquireTokenByRefreshToken(String refreshToken,
                                                                              Set<String> scopes) {
        validateNotBlank("refreshToken", refreshToken);
        validateNotEmpty("scopes", scopes);

        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(
                refreshToken,
                scopes,
                clientAuthentication,
                new RequestContext(clientId, correlationId));

        return this.InitializeRequest(refreshTokenRequest);
    }

    CompletableFuture<AuthenticationResult> InitializeRequest(
            MsalRequest msalRequest) {

        AuthenticationResultSupplier supplier = getAuthenticationResultSupplier(msalRequest);

        ExecutorService executorService = serviceBundle.getExecutorService();
        CompletableFuture<AuthenticationResult> future = executorService != null ?
                        CompletableFuture.supplyAsync(supplier, executorService) :
                        CompletableFuture.supplyAsync(supplier);
        return future;
    }

    AuthenticationResult acquireTokenCommon(MsalRequest msalRequest) throws Exception {
        ClientDataHttpHeaders headers = msalRequest.getHeaders();

        if(logPii) {
            log.debug(LogHelper.createMessage(
                    String.format("Using Client Http Headers: %s", headers),
                    headers.getHeaderCorrelationIdValue()));
        }

        this.authenticationAuthority.doInstanceDiscovery(
                validateAuthority,
                headers.getReadonlyHeaderMap(),
                this.serviceBundle);

        URL url = new URL(this.authenticationAuthority.getTokenUri());
        TokenEndpointRequest request = new TokenEndpointRequest(
                url,
                msalRequest,
                serviceBundle);

        return request.executeOAuthRequestAndProcessResponse();
    }

    private AuthenticationResultSupplier getAuthenticationResultSupplier(MsalRequest msalRequest){

        AuthenticationResultSupplier supplier;
        if(msalRequest instanceof DeviceCodeRequest){
            supplier = new AcquireTokenDeviceCodeFlowSupplier(
                    (PublicClientApplication) this,
                    (DeviceCodeRequest) msalRequest);
        } else {
            supplier = new AcquireTokenByAuthorizationGrantSupplier(
                            this,
                            msalRequest);
        }
        return supplier;
    }

    protected static void validateNotBlank(String name, String value) {
        if (StringHelper.isBlank(value)) {
            throw new IllegalArgumentException(name + " is null or empty");
        }
    }

    protected static void validateNotNull(String name, Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException(name + " is null");
        }
    }

    protected static void validateNotEmpty(String name, Set<String> set) {
        if (set == null || set.isEmpty()) {
            throw new IllegalArgumentException(name + " is null or empty");
        }
    }

    ServiceBundle getServiceBundle(){
        return serviceBundle;
    }

    abstract static class Builder<T extends Builder<T>> {
        // Required parameters
        private String clientId;

        // Optional parameters - initialized to default values
        private String authority = DEFAULT_AUTHORITY;
        private AuthenticationAuthority authenticationAuthority;
        private boolean validateAuthority = true;
        private String correlationId = UUID.randomUUID().toString().replace("-", "");
        private boolean logPii = false;
        private ExecutorService executorService;
        private Proxy proxy;
        private SSLSocketFactory sslSocketFactory;

        /**
         * Constructor to create instance of Builder of client application
         * @param clientId Client ID (Application ID) of the application as registered
         *                 in the application registration portal (portal.azure.com)
         */
        public Builder(String clientId){
            validateNotBlank("clientId", clientId);
            this.clientId = clientId;
        }

        abstract T self();

        /**
         * Set URL of the authenticating authority or security token service (STS) from which MSAL
         * will acquire security tokens.
         * The default value is {@link ClientApplicationBase#DEFAULT_AUTHORITY}
         * @throws MalformedURLException
         */
        public T authority(String val) throws MalformedURLException {
            authority = canonicalizeUri(val);
            authenticationAuthority = new AuthenticationAuthority(new URL(authority));

            if(authenticationAuthority.detectAuthorityType() != AuthorityType.AAD){
                throw new IllegalArgumentException("Unsupported authority type");
            }
            return self();
        }

        /**
         * Set a boolean value telling the application if the authority needs to be verified
         * against a list of known authorities.
         * The default value is true.
         */
        public T validateAuthority(boolean val)
        {
            validateAuthority = val;
            return self();
        }

        /**
         *  Set optional correlation id to be used by the API.
         *  If not provided, the API generates a random UUID.
         */
        public T correlationId(String val)
        {
            validateNotBlank("correlationId", val);

            correlationId = val;
            return self();
        }

        /**
         * Set logPii - boolean value, which determines
         * whether Pii (personally identifiable information) will be logged in.
         * The default value is false.
         */
        public T logPii(boolean val)
        {
            logPii = val;
            return self();
        }

        /**
         *  Sets ExecutorService to be used to execute the requests.
         *  Developer is responsible for maintaining the lifecycle of the ExecutorService.
         */
        public T executorService(ExecutorService val)
        {
            validateNotNull("executorService", val);

            executorService = val;
            return self();
        }

        /**
         * Sets Proxy configuration to be used by the client application for all network communication.
         * Default is null and system defined properties if any, would be used.
         */
        public T proxy(Proxy val)
        {
            validateNotNull("proxy", val);

            proxy = val;
            return self();
        }

        /**
         * Sets SSLSocketFactory to be used by the client application for all network communication.
         *
         */
        public T sslSocketFactory(SSLSocketFactory val)
        {
            validateNotNull("sslSocketFactory", val);

            sslSocketFactory = val;
            return self();
        }

        abstract ClientApplicationBase build();

        private String canonicalizeUri(String authority) {
            if (!authority.endsWith("/")) {
                authority += "/";
            }
            return authority;
        }
    }

    ClientApplicationBase(Builder<?> builder) {
        clientId = builder.clientId;
        authority = builder.authority;
        authenticationAuthority = builder.authenticationAuthority;
        validateAuthority = builder.validateAuthority;
        correlationId = builder.correlationId;
        logPii = builder.logPii;
        // Telemetry will also go into serviceBundle
        serviceBundle = new ServiceBundle(builder.executorService, builder.proxy, builder.sslSocketFactory);
    }
}
