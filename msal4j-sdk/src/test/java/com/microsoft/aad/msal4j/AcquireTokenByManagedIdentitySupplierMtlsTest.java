// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AcquireTokenByManagedIdentitySupplierMtlsTest {

    @Test
    void providerRuntimeFailureIsNormalized() {
        RuntimeException providerFailure =
                new RuntimeException("extension-specific failure");
        IManagedIdentityMtlsProvider provider = request -> {
            throw providerFailure;
        };

        MsalClientException exception = assertThrows(
                MsalClientException.class,
                () -> AcquireTokenByManagedIdentitySupplier
                        .getMtlsProviderBinding(provider, request()));

        assertEquals(MsalError.MANAGED_IDENTITY_MTLS_REQUEST_FAILED,
                exception.errorCode());
        assertSame(providerFailure, exception.getCause());
    }

    @Test
    void providerMsalFailureIsPreserved() {
        MsalClientException providerFailure = new MsalClientException(
                "known failure",
                MsalError.MANAGED_IDENTITY_MTLS_PROVIDER_UNAVAILABLE);
        IManagedIdentityMtlsProvider provider = request -> {
            throw providerFailure;
        };

        assertSame(providerFailure, assertThrows(
                MsalClientException.class,
                () -> AcquireTokenByManagedIdentitySupplier
                        .getMtlsProviderBinding(provider, request())));
    }

    @Test
    void minimumBindingStrengthFailsClosed() {
        ManagedIdentityMtlsBinding softwareBinding =
                binding(MtlsBindingStrength.SOFTWARE);

        MsalClientException exception = assertThrows(
                MsalClientException.class,
                () -> AcquireTokenByManagedIdentitySupplier
                        .validateMinimumBindingStrength(
                                softwareBinding,
                                MtlsBindingStrength.KEY_GUARD));

        assertEquals(
                MsalError.MANAGED_IDENTITY_MTLS_MINIMUM_STRENGTH_NOT_MET,
                exception.errorCode());
    }

    @Test
    void minimumBindingStrengthAcceptsStrongerBinding() {
        AcquireTokenByManagedIdentitySupplier.validateMinimumBindingStrength(
                binding(MtlsBindingStrength.KEY_GUARD),
                MtlsBindingStrength.SOFTWARE);
    }

    @Test
    void imdsCallbackUsesImdsRetryPolicyForHttp410() throws Exception {
        DefaultHttpClient httpClient = mock(DefaultHttpClient.class);
        HttpResponse gone = response(HttpStatus.HTTP_GONE, "updating");
        HttpResponse success = response(HttpStatus.HTTP_OK, "{}");
        when(httpClient.send(any(HttpRequest.class)))
                .thenReturn(gone, success);

        IMDSRetryPolicy.setRetryDelayMs(0);
        try {
            ManagedIdentityApplication application =
                    ManagedIdentityApplication
                            .builder(ManagedIdentityId.systemAssigned())
                            .httpClient(httpClient)
                            .build();
            ManagedIdentityParameters parameters =
                    ManagedIdentityParameters
                            .builder("https://vault.azure.net")
                            .withMtlsProofOfPossession()
                            .build();
            RequestContext context = new RequestContext(
                    application,
                    PublicApi.ACQUIRE_TOKEN_BY_SYSTEM_ASSIGNED_MANAGED_IDENTITY,
                    parameters);
            HttpHelper imdsHelper =
                    new HttpHelper(httpClient, new IMDSRetryPolicy());
            ServiceBundle serviceBundle = new ServiceBundle(
                    null,
                    new TelemetryManager(null, false),
                    imdsHelper);
            IManagedIdentityMtlsHttpClient callback =
                    AcquireTokenByManagedIdentitySupplier
                            .createMtlsProviderHttpClient(
                                    imdsHelper,
                                    serviceBundle,
                                    context);

            ManagedIdentityMtlsHttpResponse result = callback.execute(
                    new ManagedIdentityMtlsHttpRequest(
                            "GET",
                            "http://169.254.169.254/metadata/identity/getplatformmetadata",
                            Collections.singletonMap("Metadata", "true"),
                            null));

            assertEquals(HttpStatus.HTTP_OK, result.statusCode());
            verify(httpClient,
                    org.mockito.Mockito.times(2)).send(any(HttpRequest.class));
        } finally {
            IMDSRetryPolicy.resetToDefaults();
        }
    }

    @Test
    void persistedMtlsTokenIsReloadedOnlyAfterBindingIsResolved() throws Exception {
        TokenCache original = ManagedIdentityApplication.sharedTokenCache;
        try {
            ManagedIdentityApplication application =
                    ManagedIdentityApplication
                            .builder(ManagedIdentityId.systemAssigned())
                            .build();
            ManagedIdentityParameters parameters =
                    ManagedIdentityParameters
                            .builder("https://vault.azure.net")
                            .withMtlsProofOfPossession()
                            .build();
            ManagedIdentityMtlsBinding binding =
                    binding(MtlsBindingStrength.KEY_GUARD);
            String extCacheKeyHash = parameters.computeMtlsExtCacheKeyHash(
                    binding.bindingContext().keyId());

            TokenCache persisted = new TokenCache();
            AccessTokenCacheEntity accessToken = new AccessTokenCacheEntity();
            accessToken.credentialType(
                    CredentialTypeEnum.ACCESS_TOKEN_EXTENDED.value());
            accessToken.environment(application.authenticationAuthority.host());
            accessToken.clientId(application.clientId());
            accessToken.realm(Constants.MANAGED_IDENTITY_DEFAULT_TENTANT);
            accessToken.target(parameters.resource());
            accessToken.secret("persisted-mtls-token");
            accessToken.cachedAt(Long.toString(
                    System.currentTimeMillis() / 1000));
            accessToken.expiresOn(Long.toString(
                    System.currentTimeMillis() / 1000 + 3600));
            accessToken.extCacheKeyHash(extCacheKeyHash);
            persisted.accessTokens.put(accessToken.getKey(), accessToken);

            TokenCache reloaded = new TokenCache();
            reloaded.deserialize(persisted.serialize());
            ManagedIdentityApplication.sharedTokenCache = reloaded;
            application.tokenCache = reloaded;

            RequestContext context = new RequestContext(
                    application,
                    PublicApi.ACQUIRE_TOKEN_BY_SYSTEM_ASSIGNED_MANAGED_IDENTITY,
                    parameters);
            ManagedIdentityRequest request =
                    new ManagedIdentityRequest(application, context);
            AtomicInteger bindingResolutions = new AtomicInteger();
            AcquireTokenByManagedIdentitySupplier supplier =
                    new AcquireTokenByManagedIdentitySupplier(
                            application,
                            request,
                            () -> {
                                bindingResolutions.incrementAndGet();
                                return binding;
                            });

            AuthenticationResult result = supplier.execute();

            assertEquals(1, bindingResolutions.get());
            assertEquals("persisted-mtls-token", result.accessToken());
            assertEquals("mtls_pop", result.tokenType());
            assertEquals(TokenSource.CACHE, result.metadata().tokenSource());
            assertSame(binding.bindingContext(), result.mtlsBindingContext());
            assertNotNull(result.bindingCertificate());
        } finally {
            ManagedIdentityApplication.sharedTokenCache = original;
        }
    }

    private static ManagedIdentityMtlsRequest request() {
        return new ManagedIdentityMtlsRequest(
                null,
                null,
                "binding",
                "correlation",
                httpRequest -> new ManagedIdentityMtlsHttpResponse(
                        HttpStatus.HTTP_OK,
                        "{}",
                        Collections.emptyMap()));
    }

    private static ManagedIdentityMtlsBinding binding(
            MtlsBindingStrength strength) {
        X509Certificate certificate = mock(X509Certificate.class);
        IMtlsBindingContext context = new IMtlsBindingContext() {
            @Override
            public MtlsBindingStrength bindingStrength() {
                return strength;
            }

            @Override
            public SSLContext sslContext() {
                return null;
            }

            @Override
            public javax.net.ssl.X509ExtendedKeyManager keyManager() {
                return null;
            }

            @Override
            public X509Certificate bindingCertificate() {
                return certificate;
            }

            @Override
            public String keyId() {
                return "key";
            }
        };
        return new ManagedIdentityMtlsBinding(
                context,
                "client",
                "https://login.example/token");
    }

    private static HttpResponse response(int status, String body) {
        HttpResponse response = new HttpResponse();
        response.statusCode(status);
        response.body(body);
        return response;
    }
}
