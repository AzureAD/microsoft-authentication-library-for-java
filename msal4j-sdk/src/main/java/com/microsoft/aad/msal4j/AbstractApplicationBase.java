// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;

import javax.net.ssl.SSLSocketFactory;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
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

    @Accessors(fluent = true)
    @Getter(AccessLevel.PACKAGE)
    protected TokenCache tokenCache;

    @Accessors(fluent = true)
    @Getter
    private String correlationId;

    @Accessors(fluent = true)
    @Getter
    private boolean logPii;

    @Accessors(fluent = true)
    @Getter
    private Proxy proxy;

    @Accessors(fluent = true)
    @Getter
    private SSLSocketFactory sslSocketFactory;

    @Accessors(fluent = true)
    @Getter(AccessLevel.PACKAGE)
    private ServiceBundle serviceBundle;

    @Accessors(fluent = true)
    @Getter(AccessLevel.PACKAGE)
    private Consumer<List<HashMap<String, String>>> telemetryConsumer;

    @Accessors(fluent = true)
    @Getter
    private IHttpClient httpClient;

    @Accessors(fluent = true)
    @Getter
    private Integer connectTimeoutForDefaultHttpClient;

    @Accessors(fluent = true)
    @Getter
    private Integer readTimeoutForDefaultHttpClient;

    //The following fields are set in only some applications and/or set internally by the library. To avoid excessive
    // type casting throughout the library they are defined here as package-private, but will not be part of this class's Builder
    @Accessors(fluent = true)
    @Getter(AccessLevel.PACKAGE)
    private boolean validateAuthority;

    @Accessors(fluent = true)
    @Getter(AccessLevel.PACKAGE)
    private String clientId;

    @Accessors(fluent = true)
    @Getter(AccessLevel.PACKAGE)
    private String authority;

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

    public abstract static class Builder<T extends Builder<T>> {
        // Optional parameters - initialized to default values
        private String correlationId;
        private boolean logPii = false;
        private ExecutorService executorService;
        private Proxy proxy;
        private SSLSocketFactory sslSocketFactory;
        private IHttpClient httpClient;
        private Consumer<List<HashMap<String, String>>> telemetryConsumer;
        private Boolean onlySendFailureTelemetry = false;
        private Integer connectTimeoutForDefaultHttpClient;
        private Integer readTimeoutForDefaultHttpClient;
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
        serviceBundle = new ServiceBundle(
                builder.executorService,
                builder.httpClient == null ?
                        new DefaultHttpClient(builder.proxy, builder.sslSocketFactory, builder.connectTimeoutForDefaultHttpClient, builder.readTimeoutForDefaultHttpClient) :
                        builder.httpClient,
                new TelemetryManager(telemetryConsumer, builder.onlySendFailureTelemetry));
        authenticationAuthority = builder.authenticationAuthority;
        clientId = builder.clientId;
    }
}
