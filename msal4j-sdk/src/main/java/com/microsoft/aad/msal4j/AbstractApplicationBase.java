// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;

import javax.net.ssl.SSLSocketFactory;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Abstract class containing common methods and properties for {@link PublicClientApplication},
 * {@link ConfidentialClientApplication}, and {@link ManagedIdentityApplication}
 */
public abstract class AbstractApplicationBase implements IApplicationBase {

    protected Logger log;
    protected Authority authenticationAuthority;
    private String correlationId;
    private boolean logPii;
    private Proxy proxy;
    private SSLSocketFactory sslSocketFactory;
    private IHttpClient httpClient;
    private Integer connectTimeoutForDefaultHttpClient;
    private Integer readTimeoutForDefaultHttpClient;
    private boolean retryDisabled;
    String tenant;

    //The following fields are set in only some applications and/or set internally by the library. To avoid excessive
    // type casting throughout the library they are defined here as package-private, but will not be part of this class's Builder
    private boolean validateAuthority;
    private String clientId;
    private String authority;
    ServiceBundle serviceBundle;
    Consumer<List<HashMap<String, String>>> telemetryConsumer;
    protected TokenCache tokenCache;

    CompletableFuture<IAuthenticationResult> executeRequest(
            MsalRequest msalRequest) {

        AuthenticationResultSupplier supplier = getAuthenticationResultSupplier(msalRequest);

        ExecutorService executorService = serviceBundle.getExecutorService();
        return executorService != null ?
                CompletableFuture.supplyAsync(supplier, executorService) :
                CompletableFuture.supplyAsync(supplier);

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

        if (authenticationAuthority.authorityType.equals(AuthorityType.AAD)) {
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
        } else if (msalRequest instanceof InteractiveRequest) {
            supplier = new AcquireTokenByInteractiveFlowSupplier(
                    (PublicClientApplication) this,
                    (InteractiveRequest) msalRequest);
        } else if (msalRequest instanceof ClientCredentialRequest) {
            supplier = new AcquireTokenByClientCredentialSupplier(
                    (ConfidentialClientApplication) this,
                    (ClientCredentialRequest) msalRequest);
        } else if (msalRequest instanceof OnBehalfOfRequest) {
            supplier = new AcquireTokenByOnBehalfOfSupplier(
                    (ConfidentialClientApplication) this,
                    (OnBehalfOfRequest) msalRequest);
        } else if (msalRequest instanceof ManagedIdentityRequest) {
            supplier = new AcquireTokenByManagedIdentitySupplier(
                    (ManagedIdentityApplication) this,
                    (ManagedIdentityRequest) msalRequest);
        } else {
            supplier = new AcquireTokenByAuthorizationGrantSupplier(
                    this,
                    msalRequest, null);
        }
        return supplier;
    }

    /**
     * Gets the correlation ID used to correlate requests and responses.
     * 
     * @return the correlation ID
     */
    public String correlationId() {
        return this.correlationId;
    }

    /**
     * Gets whether PII (personally identifiable information) should be logged.
     * 
     * @return true if PII should be logged, false otherwise
     */
    public boolean logPii() {
        return this.logPii;
    }

    /**
     * Gets the proxy configuration for HTTP requests.
     * 
     * @return the proxy configuration, or null if no proxy is configured
     */
    public Proxy proxy() {
        return this.proxy;
    }

    /**
     * TODO: Add description
     */
    public SSLSocketFactory sslSocketFactory() {
        return this.sslSocketFactory;
    }

    /**
     * TODO: Add description
     */
    public IHttpClient httpClient() {
        return this.httpClient;
    }

    /**
     * TODO: Add description
     */
    public Integer connectTimeoutForDefaultHttpClient() {
        return this.connectTimeoutForDefaultHttpClient;
    }

    /**
     * TODO: Add description
     */
    public Integer readTimeoutForDefaultHttpClient() {
        return this.readTimeoutForDefaultHttpClient;
    }

    boolean isRetryDisabled() {
        return this.retryDisabled;
    }

    String tenant() {
        return this.tenant;
    }

    boolean validateAuthority() {
        return this.validateAuthority;
    }

    String clientId() {
        return this.clientId;
    }

    String authority() {
        return this.authority;
    }

    ServiceBundle serviceBundle() {
        return this.serviceBundle;
    }

    Consumer<List<HashMap<String, String>>> telemetryConsumer() {
        return this.telemetryConsumer;
    }

    TokenCache tokenCache() {
        return this.tokenCache;
    }

    public abstract static class Builder<T extends Builder<T>> {
        // Optional parameters - initialized to default values
        private String correlationId;
        private boolean logPii = false;
        ExecutorService executorService;
        Proxy proxy;
        SSLSocketFactory sslSocketFactory;
        IHttpClient httpClient;
        private Consumer<List<HashMap<String, String>>> telemetryConsumer;
        Boolean onlySendFailureTelemetry = false;
        Integer connectTimeoutForDefaultHttpClient;
        Integer readTimeoutForDefaultHttpClient;
        boolean disableInternalRetries;
        private String clientId;
        private Authority authenticationAuthority = createDefaultAADAuthority();

        public Builder() {
        }

        public Builder(String clientId) {
            validateNotBlank("clientId", clientId);
            this.clientId = clientId;
        }

        abstract T self();

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
        public T httpClient(IHttpClient val) {
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

        /**
         * Sets the connect timeout value used in HttpsURLConnection connections made by {@link DefaultHttpClient},
         * and is not needed if using a custom HTTP client
         *
         * @param val timeout value in milliseconds
         * @return instance of the Builder on which method was called
         */
        public T connectTimeoutForDefaultHttpClient(Integer val) {
            validateNotNull("connectTimeoutForDefaultHttpClient", val);

            connectTimeoutForDefaultHttpClient = val;
            return self();
        }

        /**
         * Sets the read timeout value used in HttpsURLConnection connections made by {@link DefaultHttpClient},
         * and is not needed if using a custom HTTP client
         *
         * @param val timeout value in milliseconds
         * @return instance of the Builder on which method was called
         */
        public T readTimeoutForDefaultHttpClient(Integer val) {
            validateNotNull("readTimeoutForDefaultHttpClient", val);

            readTimeoutForDefaultHttpClient = val;
            return self();
        }

        /**
         * The library has a number of policies for retrying HTTP calls in different scenarios.
         * <p>
         * This will disable all internal retry behavior, allowing customized retry behavior via your own implementation of {@link IHttpClient}
         *
         * @return instance of the Builder on which method was called
         */
        public T disableInternalRetries() {
            disableInternalRetries = true;
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

        private static Authority createDefaultAADAuthority() {
            Authority authority;
            try {
                authority = new AADAuthority(new URL(DEFAULT_AUTHORITY));
            } catch (Exception e) {
                throw new MsalClientException(e);
            }
            return authority;
        }


        abstract AbstractApplicationBase build();
    }

    AbstractApplicationBase(Builder<?> builder) {
        correlationId = builder.correlationId;
        logPii = builder.logPii;
        telemetryConsumer = builder.telemetryConsumer;
        proxy = builder.proxy;
        sslSocketFactory = builder.sslSocketFactory;
        connectTimeoutForDefaultHttpClient = builder.connectTimeoutForDefaultHttpClient;
        readTimeoutForDefaultHttpClient = builder.readTimeoutForDefaultHttpClient;
        authenticationAuthority = builder.authenticationAuthority;
        clientId = builder.clientId;
        retryDisabled = builder.disableInternalRetries;

        if (builder.httpClient == null) {
            httpClient = new DefaultHttpClient(
                    builder.proxy,
                    builder.sslSocketFactory,
                    builder.connectTimeoutForDefaultHttpClient,
                    builder.readTimeoutForDefaultHttpClient);
        } else {
            httpClient = builder.httpClient;
        }
    }
}
