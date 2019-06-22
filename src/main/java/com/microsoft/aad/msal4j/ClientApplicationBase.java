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
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;

import javax.net.ssl.SSLSocketFactory;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Abstract class containing common API methods and properties.
 */
abstract class ClientApplicationBase implements IClientApplicationBase {

    protected Logger log;
    protected ClientAuthentication clientAuthentication;

    @Accessors(fluent = true)
    @Getter
    private String clientId;

    @Accessors(fluent = true)
    @Getter
    private String authority;

    protected Authority authenticationAuthority;

    @Accessors(fluent = true)
    @Getter
    private boolean validateAuthority;

    @Accessors(fluent = true)
    @Getter
    private String correlationId;

    @Accessors(fluent = true)
    @Getter
    private boolean logPii;

    private ServiceBundle serviceBundle;

    @Accessors(fluent = true)
    @Getter
    private Consumer<List<HashMap<String, String>>> telemetryConsumer;

    @Override
    public Proxy proxy() {
        return this.serviceBundle.getProxy();
    }

    @Override
    public SSLSocketFactory sslSocketFactory() {
        return this.serviceBundle.getSslSocketFactory();
    }

    @Accessors(fluent = true)
    @Getter
    protected TokenCache tokenCache;

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(AuthorizationCodeParameters parameters) {

        validateNotNull("parameters", parameters);

        AuthorizationCodeRequest authorizationCodeRequest = new AuthorizationCodeRequest(
                parameters,
                this,
                createRequestContext(PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        return this.executeRequest(authorizationCodeRequest);
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(RefreshTokenParameters parameters) {

        validateNotNull("parameters", parameters);

        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(
                parameters,
                this,
                createRequestContext(PublicApi.ACQUIRE_TOKEN_BY_REFRESH_TOKEN));

        return executeRequest(refreshTokenRequest);
    }

    CompletableFuture<IAuthenticationResult> executeRequest(
            MsalRequest msalRequest) {

        AuthenticationResultSupplier supplier = getAuthenticationResultSupplier(msalRequest);

        ExecutorService executorService = serviceBundle.getExecutorService();
        CompletableFuture<IAuthenticationResult> future = executorService != null ?
                CompletableFuture.supplyAsync(supplier, executorService) :
                CompletableFuture.supplyAsync(supplier);

        return future;
    }

    AuthenticationResult acquireTokenCommon(MsalRequest msalRequest, Authority requestAuthority)
            throws Exception {

        ClientDataHttpHeaders headers = msalRequest.headers();

        if (logPii) {
            log.debug(LogHelper.createMessage(
                    String.format("Using Client Http Headers: %s", headers),
                    headers.getHeaderCorrelationIdValue()));
        }

        TokenRequest request = new TokenRequest(requestAuthority, msalRequest, serviceBundle);

        AuthenticationResult result = request.executeOauthRequestAndProcessResponse();

        if(authenticationAuthority.authorityType.equals(AuthorityType.B2C)){
            tokenCache.saveTokens(request, result, authenticationAuthority.host);
        } else {
            InstanceDiscoveryMetadataEntry instanceDiscoveryMetadata =
                    AadInstanceDiscovery.GetMetadataEntry
                            (requestAuthority.canonicalAuthorityUrl(), validateAuthority, msalRequest, serviceBundle);

            tokenCache.saveTokens(request, result, instanceDiscoveryMetadata.preferredCache);
        }

        return result;
    }

    private AuthenticationResultSupplier getAuthenticationResultSupplier(MsalRequest msalRequest) {

        AuthenticationResultSupplier supplier;
        if (msalRequest instanceof DeviceCodeFlowRequest) {
            supplier = new AcquireTokenByDeviceCodeFlowSupplier(
                    (PublicClientApplication) this,
                    (DeviceCodeFlowRequest) msalRequest);
        } else if (msalRequest instanceof SilentRequest) {
            supplier = new AcquireTokenSilentSupplier(this, (SilentRequest) msalRequest);
        } else {
            supplier = new AcquireTokenByAuthorizationGrantSupplier(
                    this,
                    msalRequest, null);
        }
        return supplier;
    }

    RequestContext createRequestContext(PublicApi publicApi) {
        return new RequestContext(
                clientId,
                correlationId(),
                publicApi);
    }

    ServiceBundle getServiceBundle() {
        return serviceBundle;
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireTokenSilently(SilentParameters parameters)
            throws MalformedURLException {

        validateNotNull("parameters", parameters);

        SilentRequest silentRequest = new SilentRequest(
                parameters,
                this,
                createRequestContext(PublicApi.ACQUIRE_TOKEN_SILENTLY));

        return executeRequest(silentRequest);
    }

    @Override
    public CompletableFuture<Set<IAccount>> getAccounts() {
        MsalRequest msalRequest =
                new MsalRequest(this, null,
                        createRequestContext(PublicApi.GET_ACCOUNTS)){};

        AccountsSupplier supplier = new AccountsSupplier(this, msalRequest);

        CompletableFuture<Set<IAccount>> future =
                serviceBundle.getExecutorService() != null ? CompletableFuture.supplyAsync(supplier, serviceBundle.getExecutorService())
                        : CompletableFuture.supplyAsync(supplier);
        return future;
    }

    @Override
    public CompletableFuture removeAccount(IAccount account) {
        RemoveAccountRunnable runnable = new RemoveAccountRunnable(this, account);

        CompletableFuture<Void> future =
                serviceBundle.getExecutorService() != null ? CompletableFuture.runAsync(runnable, serviceBundle.getExecutorService())
                        : CompletableFuture.runAsync(runnable);
        return future;
    }

    protected static String canonicalizeUrl(String authority) {
        authority = authority.toLowerCase();

        if (!authority.endsWith("/")) {
            authority += "/";
        }
        return authority;
    }

    abstract static class Builder<T extends Builder<T>> {
        // Required parameters
        private String clientId;

        // Optional parameters - initialized to default values
        private String authority = DEFAULT_AUTHORITY;
        private Authority authenticationAuthority = createDefaultAADAuthority();
        private boolean validateAuthority = true;
        private String correlationId = UUID.randomUUID().toString();
        private boolean logPii = false;
        private ExecutorService executorService;
        private Proxy proxy;
        private SSLSocketFactory sslSocketFactory;
        private Consumer<List<HashMap<String, String>>> telemetryConsumer;
        private Boolean onlySendFailureTelemetry = false;
        private ITokenCacheAccessAspect tokenCacheAccessAspect;

        /**
         * Constructor to create instance of Builder of client application
         *
         * @param clientId Client ID (Application ID) of the application as registered
         *                 in the application registration portal (portal.azure.com)
         */
        public Builder(String clientId) {
            validateNotBlank("clientId", clientId);
            this.clientId = clientId;
        }

        abstract T self();

        /**
         * Set URL of the authenticating authority or security token service (STS) from which MSAL
         * will acquire security tokens.
         * The default value is {@link ClientApplicationBase#DEFAULT_AUTHORITY}
         *
         * @param val a string value of authority
         * @return instance of the Builder on which method was called
         * @throws MalformedURLException if val is malformed URL
         */

        public T authority(String val) throws MalformedURLException {
            authority = canonicalizeUrl(val);

            if (Authority.detectAuthorityType(new URL(authority)) != AuthorityType.AAD) {
                throw new IllegalArgumentException("Unsupported authority type. Please use AAD authority");
            }

            authenticationAuthority = new AADAuthority(new URL(authority));

            return self();
        }

        public T b2cAuthority(String val) throws MalformedURLException{
            authority = canonicalizeUrl(val);

            if(Authority.detectAuthorityType(new URL(authority)) != AuthorityType.B2C){
                throw new IllegalArgumentException("Unsupported authority type. Please use B2C authority");
            }
            authenticationAuthority = new B2CAuthority(new URL(authority));

            validateAuthority = false;
            return self();
        }

        /**
         * Set a boolean value telling the application if the authority needs to be verified
         * against a list of known authorities.
         * The default value is true.
         *
         * @param val a boolean value for validateAuthority
         * @return instance of the Builder on which method was called
         */
        public T validateAuthority(boolean val) {
            validateAuthority = val;
            return self();
        }

        /**
         * Set optional correlation id to be used by the API.
         * If not provided, the API generates a random UUID.
         *
         * @param val a string value of correlation id
         * @return instance of the Builder on which method was called
         */
        public T correlationId(String val) {
            validateNotBlank("correlationId", val);

            correlationId = val;
            return self();
        }

        /**
         * Set logPii - boolean value, which determines
         * whether Pii (personally identifiable information) will be logged in.
         * The default value is false.
         *
         * @param val a boolean value for logPii
         * @return instance of the Builder on which method was called
         */
        public T logPii(boolean val) {
            logPii = val;
            return self();
        }

        /**
         * Sets ExecutorService to be used to execute the requests.
         * Developer is responsible for maintaining the lifecycle of the ExecutorService.
         *
         * @param val an instance of ExecutorService
         * @return instance of the Builder on which method was called
         */
        public T executorService(ExecutorService val) {
            validateNotNull("executorService", val);

            executorService = val;
            return self();
        }

        /**
         * Sets Proxy configuration to be used by the client application for all network communication.
         * Default is null and system defined properties if any, would be used.
         *
         * @param val an instance of Proxy
         * @return instance of the Builder on which method was called
         */
        public T proxy(Proxy val) {
            validateNotNull("proxy", val);

            proxy = val;
            return self();
        }

        /**
         * Sets SSLSocketFactory to be used by the client application for all network communication.
         *
         * @param val an instance of SSLSocketFactory
         * @return instance of the Builder on which method was called
         */
        public T sslSocketFactory(SSLSocketFactory val) {
            validateNotNull("sslSocketFactory", val);

            sslSocketFactory = val;
            return self();
        }

        public T telemetryConsumer(Consumer<List<HashMap<String, String>>> val) {
            validateNotNull("telemetryConsumer", val);

            telemetryConsumer = val;
            return self();
        }

        public T onlySendFailureTelemetry(Boolean val) {

            onlySendFailureTelemetry = val;
            return self();
        }

        /**
         * Sets ITokenCacheAccessAspect to be used for cache_data persistence.
         *
         * @param val an instance of ITokenCacheAccessAspect
         * @return instance of the Builder on which method was called
         */
        public T setTokenCacheAccessAspect(ITokenCacheAccessAspect val) {
            validateNotNull("tokenCacheAccessAspect", val);

            tokenCacheAccessAspect = val;
            return self();
        }

        private static Authority createDefaultAADAuthority() {
            Authority authority;
            try {
                authority = new AADAuthority(new URL(DEFAULT_AUTHORITY));
            } catch(Exception e){
                throw new AuthenticationException(e);
            }
            return authority;
        }

        abstract ClientApplicationBase build();
    }

    ClientApplicationBase(Builder<?> builder) {
        clientId = builder.clientId;
        authority = builder.authority;
        validateAuthority = builder.validateAuthority;
        correlationId = builder.correlationId;
        logPii = builder.logPii;
        telemetryConsumer = builder.telemetryConsumer;
        serviceBundle = new ServiceBundle(
                builder.executorService,
                builder.proxy,
                builder.sslSocketFactory,
                new TelemetryManager(telemetryConsumer, builder.onlySendFailureTelemetry));
        authenticationAuthority = builder.authenticationAuthority;
        tokenCache = new TokenCache(builder.tokenCacheAccessAspect);
    }
}