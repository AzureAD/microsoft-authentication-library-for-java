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
public abstract class AbstractClientApplicationBase extends AbstractApplicationBase implements IClientApplicationBase {

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
    private String applicationName;

    @Accessors(fluent = true)
    @Getter
    private String applicationVersion;

    @Accessors(fluent = true)
    @Getter
    private AadInstanceDiscoveryResponse aadAadInstanceDiscoveryResponse;

    protected abstract ClientAuthentication clientAuthentication();

    @Accessors(fluent = true)
    @Getter
    private String clientCapabilities;

    @Accessors(fluent = true)
    @Getter
    private boolean autoDetectRegion;

    @Accessors(fluent = true)
    @Getter
    protected String azureRegion;

    @Accessors(fluent = true)
    @Getter
    private boolean instanceDiscovery;

    @Override
    public TokenCache tokenCache() {
        return super.tokenCache;
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(AuthorizationCodeParameters parameters) {

        validateNotNull("parameters", parameters);

        RequestContext context = new RequestContext(
                this,
                PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE,
                parameters);

        AuthorizationCodeRequest authorizationCodeRequest = new AuthorizationCodeRequest(
                parameters,
                this,
                context);

        return this.executeRequest(authorizationCodeRequest);
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(RefreshTokenParameters parameters) {

        validateNotNull("parameters", parameters);

        RequestContext context = new RequestContext(
                this,
                PublicApi.ACQUIRE_TOKEN_BY_REFRESH_TOKEN,
                parameters);

        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(
                parameters,
                this,
                context);

        return executeRequest(refreshTokenRequest);
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireTokenSilently(SilentParameters parameters)
            throws MalformedURLException {

        validateNotNull("parameters", parameters);

        RequestContext context;
        if (parameters.account() != null) {
            context = new RequestContext(
                    this,
                    PublicApi.ACQUIRE_TOKEN_SILENTLY,
                    parameters,
                    UserIdentifier.fromHomeAccountId(parameters.account().homeAccountId()));

        } else {
            context = new RequestContext(
                    this,
                    PublicApi.ACQUIRE_TOKEN_SILENTLY,
                    parameters);
        }

        SilentRequest silentRequest = new SilentRequest(
                parameters,
                this,
                context,
                null);

        return executeRequest(silentRequest);
    }

    public CompletableFuture<Set<IAccount>> getAccounts() {

        RequestContext context = new RequestContext(this, PublicApi.GET_ACCOUNTS, null);
        MsalRequest msalRequest =
                new MsalRequest(this, null, context) {
                };

        AccountsSupplier supplier = new AccountsSupplier(this, msalRequest);

        return super.serviceBundle().getExecutorService() != null ?
                CompletableFuture.supplyAsync(supplier, super.serviceBundle().getExecutorService()) :
                CompletableFuture.supplyAsync(supplier);
    }

    public CompletableFuture<Void> removeAccount(IAccount account) {
        RequestContext context = new RequestContext(this, PublicApi.REMOVE_ACCOUNTS, null);
        MsalRequest msalRequest = new MsalRequest(this, null, context) {
        };

        RemoveAccountRunnable runnable = new RemoveAccountRunnable(msalRequest, account);

        return super.serviceBundle().getExecutorService() != null ?
                CompletableFuture.runAsync(runnable, super.serviceBundle().getExecutorService()) :
                CompletableFuture.runAsync(runnable);
    }

    @Override
    public URL getAuthorizationRequestUrl(AuthorizationRequestUrlParameters parameters) {

        validateNotNull("parameters", parameters);

        parameters.requestParameters.put("client_id", Collections.singletonList(this.clientId));

        //If the client application has any client capabilities set, they must be merged into the claims parameter
        if (this.clientCapabilities != null) {
            if (parameters.requestParameters.containsKey("claims")) {
                String claims = String.valueOf(parameters.requestParameters.get("claims").get(0));
                String mergedClaimsCapabilities = JsonHelper.mergeJSONString(claims, this.clientCapabilities);
                parameters.requestParameters.put("claims", Collections.singletonList(mergedClaimsCapabilities));
            } else {
                parameters.requestParameters.put("claims", Collections.singletonList(this.clientCapabilities));
            }
        }

        return parameters.createAuthorizationURL(
                this.authenticationAuthority,
                parameters.requestParameters());
    }

    public abstract static class Builder<T extends Builder<T>> extends AbstractApplicationBase.Builder<T> {
        // Required parameters
        private String clientId;

        // Optional parameters - initialized to default values
        private String authority = DEFAULT_AUTHORITY;
        private Authority authenticationAuthority = createDefaultAADAuthority();
        private boolean validateAuthority = true;
        private String applicationName;
        private String applicationVersion;
        private ITokenCacheAccessAspect tokenCacheAccessAspect;
        private AadInstanceDiscoveryResponse aadInstanceDiscoveryResponse;
        private String clientCapabilities;
        private boolean autoDetectRegion;
        private String azureRegion;
        protected boolean isInstanceDiscoveryEnabled = true;

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
            authority = Authority.enforceTrailingSlash(val);

            URL authorityURL = new URL(authority);


            switch (Authority.detectAuthorityType(authorityURL)) {
                case AAD:
                    authenticationAuthority = new AADAuthority(authorityURL);
                    break;
                case ADFS:
                    authenticationAuthority = new ADFSAuthority(authorityURL);
                    break;
                case CIAM:
                    authenticationAuthority = new CIAMAuthority(authorityURL);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported authority type.");
            }

            Authority.validateAuthority(authenticationAuthority.canonicalAuthorityUrl());

            return self();
        }

        /**
         * Set URL of the authenticating B2C authority from which MSAL will acquire tokens
         *
         * Valid B2C authorities should look like: https://&lt;something.b2clogin.com/&lt;tenant&gt;/&lt;policy&gt;
         *
         * MSAL Java also supports a legacy B2C authority format, which looks like: https://&lt;host&gt;/tfp/&lt;tenant&gt;/&lt;policy&gt;
         *
         * However, MSAL Java will eventually stop supporting the legacy format. See here for information on how to migrate to the new format: https://aka.ms/msal4j-b2c
         *
         * @param val a boolean value for validateAuthority
         * @return instance of the Builder on which method was called
         */
        public T b2cAuthority(String val) throws MalformedURLException {
            authority = Authority.enforceTrailingSlash(val);

            URL authorityURL = new URL(authority);
            Authority.validateAuthority(authorityURL);

            if (Authority.detectAuthorityType(authorityURL) != AuthorityType.B2C) {
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
         * <p>
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
         * <p>
         * Note that authority validation is not done even if {@link AbstractClientApplicationBase#validateAuthority}
         * is set to true.
         * <p>
         * For more information, see
         * https://aka.ms/msal4j-instance-discovery
         *
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
            } catch (Exception e) {
                throw new MsalClientException(e);
            }
            return authority;
        }

        public T clientCapabilities(Set<String> capabilities) {
            clientCapabilities = JsonHelper.formCapabilitiesJson(capabilities);

            return self();
        }

        /**
         * Indicates that the library should attempt to discover the Azure region the application is running in when
         * fetching the instance discovery metadata. Regions can only be detected when running in an Azure environment,
         * such as an Azure VM or other service, or if the environment has environment variable named REGION_NAME configured.
         *
         * Although you can enable both autodetection here and a specific region with {@link AbstractClientApplicationBase#azureRegion} at the same time,
         * the region set with {@link AbstractClientApplicationBase#azureRegion} will take priority if there is a mismatch.
         *
         * See here for more information about supported scenarios: https://aka.ms/msal4j-azure-regions
         *
         * @param val boolean (default is false)
         * @return instance of the Builder on which method was called
         */
        public T autoDetectRegion(boolean val) {
            autoDetectRegion = val;
            return self();
        }

        /**
         * Set the region that the library will use to format authorities in token requests. If given a valid Azure region,
         * the library will attempt to make token requests at a regional ESTS-R endpoint rather than the global ESTS endpoint.
         *
         * Regions must be valid Azure regions and their short names should be used, such as 'westus' for the West US Azure region,
         * 'centralus' for the Central US Azure region, etc.
         *
         * Although you can set a specific region here and enable autodetection with {@link AbstractClientApplicationBase#autoDetectRegion} at the same time
         * the specific region set here will take priority over the autodetected region if there is a mismatch.
         *
         * See here for more information about supported scenarios: https://aka.ms/msal4j-azure-regions
         *
         * @param val String region name
         * @return instance of the Builder on which method was called
         */
        public T azureRegion(String val) {
            azureRegion = val;
            return self();
        }

        /** Historically, MSAL would connect to a central endpoint located at
            ``https://login.microsoftonline.com`` to acquire some metadata, especially when using an unfamiliar authority.
        This behavior is known as Instance Discovery.
        This parameter defaults to true, which enables the Instance Discovery.
        If you do not know some authorities beforehand,
        yet still want MSAL to accept any authority that you will provide,
        you can use a ``False`` to unconditionally disable Instance Discovery. */
        public T instanceDiscovery(boolean val) {
            isInstanceDiscoveryEnabled = val;
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
            return super.logPii(val);
        }

        /**
         * Sets the connect timeout value used in HttpsURLConnection connections made by {@link DefaultHttpClient},
         * and is not needed if using a custom HTTP client
         *
         * @param val timeout value in milliseconds
         * @return instance of the Builder on which method was called
         */
        public T connectTimeoutForDefaultHttpClient(Integer val) {
            return super.connectTimeoutForDefaultHttpClient(val);
        }

        /**
         * Sets the read timeout value used in HttpsURLConnection connections made by {@link DefaultHttpClient},
         * and is not needed if using a custom HTTP client
         *
         * @param val timeout value in milliseconds
         * @return instance of the Builder on which method was called
         */
        public T readTimeoutForDefaultHttpClient(Integer val) {
            return super.readTimeoutForDefaultHttpClient(val);
        }

        /**
         * Sets HTTP client to be used by the client application for all HTTP requests. Allows for fine
         * grained configuration of HTTP client.
         *
         * @param val Implementation of {@link IHttpClient}
         * @return instance of the Builder on which method was called
         */
        public T httpClient(IHttpClient val) {
            return super.httpClient(val);
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
            return super.sslSocketFactory(val);
        }

        /**
         * Sets ExecutorService to be used to execute the requests.
         * Developer is responsible for maintaining the lifecycle of the ExecutorService.
         *
         * @param val an instance of ExecutorService
         * @return instance of the Builder on which method was called
         */
        public T executorService(ExecutorService val) {
            return super.executorService(val);
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
            return super.proxy(val);
        }

        /**
         * Set optional correlation id to be used by the API.
         * If not provided, the API generates a random UUID.
         *
         * @param val a string value of correlation id
         * @return instance of the Builder on which method was called
         */
        public T correlationId(String val) {
            return super.correlationId(val);
        }

        abstract AbstractClientApplicationBase build();
    }

    AbstractClientApplicationBase(Builder<?> builder) {
        super(builder);
        clientId = builder.clientId;
        authority = builder.authority;
        validateAuthority = builder.validateAuthority;
        applicationName = builder.applicationName;
        applicationVersion = builder.applicationVersion;
        authenticationAuthority = builder.authenticationAuthority;
        super.tokenCache = new TokenCache(builder.tokenCacheAccessAspect);
        aadAadInstanceDiscoveryResponse = builder.aadInstanceDiscoveryResponse;
        clientCapabilities = builder.clientCapabilities;
        autoDetectRegion = builder.autoDetectRegion;
        azureRegion = builder.azureRegion;
        instanceDiscovery = builder.isInstanceDiscoveryEnabled;
        super.serviceBundle = new ServiceBundle(
                builder.executorService,
                new TelemetryManager(telemetryConsumer, builder.onlySendFailureTelemetry),
                new HttpHelper(builder.httpClient == null ?
                        new DefaultHttpClient(builder.proxy, builder.sslSocketFactory, builder.connectTimeoutForDefaultHttpClient, builder.readTimeoutForDefaultHttpClient) :
                        builder.httpClient)
        );

        if (aadAadInstanceDiscoveryResponse != null) {
            AadInstanceDiscoveryProvider.cacheInstanceDiscoveryResponse(
                    authenticationAuthority.host,
                    aadAadInstanceDiscoveryResponse);
        }
    }
}