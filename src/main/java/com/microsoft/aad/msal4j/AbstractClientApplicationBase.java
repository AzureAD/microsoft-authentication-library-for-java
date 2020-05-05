// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;

import javax.net.ssl.SSLSocketFactory;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Abstract class containing common methods and properties to both {@link PublicClientApplication}
 * and {@link ConfidentialClientApplication}.
 */
abstract class AbstractClientApplicationBase implements IClientApplicationBase {

    protected Logger log;
    protected Authority authenticationAuthority;
    private ServiceBundle serviceBundle;

    @Accessors(fluent = true)
    @Getter
    private String clientId;

    @Accessors(fluent = true)
    @Getter
    private String authority;

    @Accessors(fluent = true)
    @Getter
    private boolean validateAuthority;

    @Accessors(fluent = true)
    @Getter
    private String correlationId;

    @Accessors(fluent = true)
    @Getter
    private boolean logPii;

    @Accessors(fluent = true)
    @Getter(AccessLevel.PACKAGE)
    private Consumer<List<HashMap<String, String>>> telemetryConsumer;

    @Accessors(fluent = true)
    @Getter
    private Proxy proxy;

    @Accessors(fluent = true)
    @Getter
    private SSLSocketFactory sslSocketFactory;

    @Accessors(fluent = true)
    @Getter
    protected TokenCache tokenCache;

    @Accessors(fluent = true)
    @Getter
    private String applicationName;

    @Accessors(fluent = true)
    @Getter
    private String applicationVersion;

    @Accessors(fluent = true)
    @Getter
    private AadInstanceDiscoveryResponse aadAadInstanceDiscoveryResponse;

    protected abstract ClientAuthentication clientAuthentication();

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(AuthorizationCodeParameters parameters) {

        validateNotNull("parameters", parameters);

        AuthorizationCodeRequest authorizationCodeRequest = new AuthorizationCodeRequest(
                parameters,
                this,
                createRequestContext(PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        return this.executeRequest(authorizationCodeRequest);
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(RefreshTokenParameters parameters) {

        validateNotNull("parameters", parameters);

        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(
                parameters,
                this,
                createRequestContext(PublicApi.ACQUIRE_TOKEN_BY_REFRESH_TOKEN, parameters));

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

    @Override
    public CompletableFuture<IAuthenticationResult> acquireTokenSilently(SilentParameters parameters)
            throws MalformedURLException {

        validateNotNull("parameters", parameters);

        SilentRequest silentRequest = new SilentRequest(
                parameters,
                this,
                createRequestContext(PublicApi.ACQUIRE_TOKEN_SILENTLY, parameters));

        return executeRequest(silentRequest);
    }

    @Override
    public CompletableFuture<Set<IAccount>> getAccounts() {
        MsalRequest msalRequest =
                new MsalRequest(this, null,
                        createRequestContext(PublicApi.GET_ACCOUNTS, null)){};

        AccountsSupplier supplier = new AccountsSupplier(this, msalRequest);

        CompletableFuture<Set<IAccount>> future =
                serviceBundle.getExecutorService() != null ? CompletableFuture.supplyAsync(supplier, serviceBundle.getExecutorService())
                        : CompletableFuture.supplyAsync(supplier);
        return future;
    }

    @Override
    public CompletableFuture removeAccount(IAccount account) {
        MsalRequest msalRequest = new MsalRequest(this, null,
                        createRequestContext(PublicApi.REMOVE_ACCOUNTS, null)){};

        RemoveAccountRunnable runnable = new RemoveAccountRunnable(msalRequest, account);

        CompletableFuture<Void> future =
                serviceBundle.getExecutorService() != null ? CompletableFuture.runAsync(runnable, serviceBundle.getExecutorService())
                        : CompletableFuture.runAsync(runnable);
        return future;
    }

    @Override
    public URL getAuthorizationRequestUrl(AuthorizationRequestUrlParameters parameters) {

        validateNotNull("parameters", parameters);

        parameters.requestParameters.put("client_id", Collections.singletonList(this.clientId));

        return parameters.createAuthorizationURL(
                this.authenticationAuthority,
                parameters.requestParameters());
    }

    AuthenticationResult acquireTokenCommon(MsalRequest msalRequest, Authority requestAuthority)
            throws Exception {

        HttpHeaders headers = msalRequest.headers();

        if (logPii) {
            log.debug(LogHelper.createMessage(
                    String.format("Using Client Http Headers: %s", headers),
                    headers.getHeaderCorrelationIdValue()));
        }

        TokenRequestExecutor requestExecutor = new TokenRequestExecutor(
                requestAuthority,
                msalRequest,
                serviceBundle);

        AuthenticationResult result = requestExecutor.executeTokenRequest();

        if(authenticationAuthority.authorityType.equals(AuthorityType.AAD)){
            InstanceDiscoveryMetadataEntry instanceDiscoveryMetadata =
                    AadInstanceDiscoveryProvider.getMetadataEntry(
                            requestAuthority.canonicalAuthorityUrl(),
                            validateAuthority,
                            msalRequest,
                            serviceBundle);

            tokenCache.saveTokens(requestExecutor, result, instanceDiscoveryMetadata.preferredCache);
        } else {
            tokenCache.saveTokens(requestExecutor, result, authenticationAuthority.host);
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
        } else if(msalRequest instanceof  InteractiveRequest){
            supplier = new AcquireTokenByInteractiveFlowSupplier(
                    (PublicClientApplication) this,
                    (InteractiveRequest) msalRequest);
        } else {
            supplier = new AcquireTokenByAuthorizationGrantSupplier(
                    this,
                    msalRequest, null);
        }
        return supplier;
    }

    RequestContext createRequestContext(PublicApi publicApi, IApiParameters apiParameters) {
        return new RequestContext(this, publicApi, apiParameters);
    }

    ServiceBundle getServiceBundle() {
        return serviceBundle;
    }

    protected static String enforceTrailingSlash(String authority) {
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
        private String correlationId;
        private boolean logPii = false;
        private ExecutorService executorService;
        private Proxy proxy;
        private SSLSocketFactory sslSocketFactory;
        private IHttpClient httpClient;
        private Consumer<List<HashMap<String, String>>> telemetryConsumer;
        private Boolean onlySendFailureTelemetry = false;
        private String applicationName;
        private String applicationVersion;
        private ITokenCacheAccessAspect tokenCacheAccessAspect;
        private AadInstanceDiscoveryResponse aadInstanceDiscoveryResponse;

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
         * The default value is {@link AbstractClientApplicationBase#DEFAULT_AUTHORITY}
         *
         * @param val a string value of authority
         * @return instance of the Builder on which method was called
         * @throws MalformedURLException if val is malformed URL
         */
        public T authority(String val) throws MalformedURLException {
            authority = enforceTrailingSlash(val);

            URL authorityURL = new URL(authority);
            Authority.validateAuthority(authorityURL);

            switch (Authority.detectAuthorityType(authorityURL)) {
                case AAD:
                    authenticationAuthority = new AADAuthority(authorityURL);
                    break;
                case ADFS:
                    authenticationAuthority = new ADFSAuthority(authorityURL);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported authority type.");
            }

            return self();
        }

        public T b2cAuthority(String val) throws MalformedURLException{
            authority = enforceTrailingSlash(val);

            URL authorityURL = new URL(authority);
            Authority.validateAuthority(authorityURL);

            if(Authority.detectAuthorityType(authorityURL) != AuthorityType.B2C){
                throw new IllegalArgumentException("Unsupported authority type. Please use B2C authority");
            }
            authenticationAuthority = new B2CAuthority(authorityURL);

            validateAuthority = false;
            return self();
        }

        /**
         * Set a boolean value telling the application if the authority needs to be verified
         * against a list of known authorities. Authority is only validated when:
         * 1 - It is an Azure Active Directory authority (not B2C or ADFS)
         * 2 - Instance discovery metadata is not set via {@link AbstractClientApplicationBase#aadAadInstanceDiscoveryResponse}
         *
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
         * Sets Proxy configuration to be used by the client application (MSAL4J by default uses
         * {@link javax.net.ssl.HttpsURLConnection}) for all network communication.
         * If no proxy value is passed in, system defined properties are used. If HTTP client is set on
         * the client application (via ClientApplication.builder().httpClient()),
         * proxy configuration should be done on the HTTP client object being passed in,
         * and not through this method.
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
         * Sets HTTP client to be used by the client application for all HTTP requests. Allows for fine
         * grained configuration of HTTP client.
         *
         * @param val Implementation of {@link IHttpClient}
         * @return instance of the Builder on which method was called
         */
        public T httpClient(IHttpClient val){
            validateNotNull("httpClient", val);

            httpClient = val;
            return self();
        }

        /**
         * Sets SSLSocketFactory to be used by the client application for all network communication.
         * If HTTP client is set on the client application (via ClientApplication.builder().httpClient()),
         * any configuration of SSL should be done on the HTTP client and not through this method.
         *
         * @param val an instance of SSLSocketFactory
         * @return instance of the Builder on which method was called
         */
        public T sslSocketFactory(SSLSocketFactory val) {
            validateNotNull("sslSocketFactory", val);

            sslSocketFactory = val;
            return self();
        }

        T telemetryConsumer(Consumer<List<HashMap<String, String>>> val) {
            validateNotNull("telemetryConsumer", val);

            telemetryConsumer = val;
            return self();
        }

        T onlySendFailureTelemetry(Boolean val) {

            onlySendFailureTelemetry = val;
            return self();
        }

        /**
         * Sets application name for telemetry purposes
         *
         * @param val application name
         * @return instance of the Builder on which method was called
         */
        public T applicationName(String val) {
            validateNotNull("applicationName", val);

            applicationName = val;
            return self();
        }

        /**
         * Sets application version for telemetry purposes
         * 
         * @param val application version
         * @return instance of the Builder on which method was called
         */
        public T applicationVersion(String val) {
            validateNotNull("applicationVersion", val);

            applicationVersion = val;
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

        /**
         * Sets instance discovery response data which will be used for determining tenant discovery
         * endpoint and authority aliases.
         *
         * Note that authority validation is not done even if {@link AbstractClientApplicationBase#validateAuthority}
         * is set to true.
         *
         * For more information, see
         * https://aka.ms/msal4j-instance-discovery
         * @param val JSON formatted value of response from AAD instance discovery endpoint
         * @return instance of the Builder on which method was called
         */
        public T aadInstanceDiscoveryResponse(String val) {
            validateNotNull("aadInstanceDiscoveryResponse", val);

            aadInstanceDiscoveryResponse =
                    AadInstanceDiscoveryProvider.parseInstanceDiscoveryMetadata(val);

            return self();
        }

        private static Authority createDefaultAADAuthority() {
            Authority authority;
            try {
                authority = new AADAuthority(new URL(DEFAULT_AUTHORITY));
            } catch(Exception e){
                throw new MsalClientException(e);
            }
            return authority;
        }

        abstract AbstractClientApplicationBase build();
    }

    AbstractClientApplicationBase(Builder<?> builder) {
        clientId = builder.clientId;
        authority = builder.authority;
        validateAuthority = builder.validateAuthority;
        correlationId = builder.correlationId;
        logPii = builder.logPii;
        applicationName = builder.applicationName;
        applicationVersion = builder.applicationVersion;
        telemetryConsumer = builder.telemetryConsumer;
        proxy = builder.proxy;
        sslSocketFactory = builder.sslSocketFactory;
        serviceBundle = new ServiceBundle(
                builder.executorService,
                builder.httpClient == null ?
                        new DefaultHttpClient(builder.proxy, builder.sslSocketFactory) :
                        builder.httpClient,
                new TelemetryManager(telemetryConsumer, builder.onlySendFailureTelemetry));
        authenticationAuthority = builder.authenticationAuthority;
        tokenCache = new TokenCache(builder.tokenCacheAccessAspect);
        aadAadInstanceDiscoveryResponse = builder.aadInstanceDiscoveryResponse;

        if(aadAadInstanceDiscoveryResponse != null){
            AadInstanceDiscoveryProvider.cacheInstanceDiscoveryMetadata(
                    authenticationAuthority.host,
                    aadAadInstanceDiscoveryResponse);
        }
    }
}