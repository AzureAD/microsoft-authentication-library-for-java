// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.microsoft.aad.msal4j.ManagedIdentityMtlsBinding;
import com.microsoft.aad.msal4j.ManagedIdentityMtlsHttpResponse;
import com.microsoft.aad.msal4j.ManagedIdentityMtlsRequest;
import com.sun.jna.Pointer;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.*;

class KeyGuardManagedIdentityMtlsProviderTest {

    @Test
    void forceRefreshRemintsCertificateButReusesKeyAndFreshAttestation() throws Exception {
        try (BindingFixture fixture = new BindingFixture(true)) {
            ManagedIdentityMtlsBinding first =
                    fixture.provider.getOrCreateBinding(fixture.request(false));
            assertSame(first, fixture.provider.getOrCreateBinding(fixture.request(false)));

            ManagedIdentityMtlsBinding replacement =
                    fixture.provider.getOrCreateBinding(fixture.request(true));

            assertNotEquals(first.bindingContext().keyId(), replacement.bindingContext().keyId());
            assertNotSame(first.bindingContext().sslContext(), replacement.bindingContext().sslContext());
            assertSame(first.bindingContext().rotatingSslContext(),
                    replacement.bindingContext().rotatingSslContext());
            assertEquals(first.bindingContext().bindingCertificate().getPublicKey(),
                    replacement.bindingContext().bindingCertificate().getPublicKey());
            assertSame(replacement, fixture.provider.getOrCreateBinding(fixture.request(false)));
            assertEquals(2, fixture.issuances.get());
            assertEquals(2, fixture.computeReads.get());
            assertEquals(2, fixture.keyOpens.get());
            assertEquals(1, fixture.keyNames.size());
            assertEquals(1, fixture.attestations.get());
            assertEquals("https://westus2.attestation.microsoft.com", fixture.attestationEndpoint);
            assertEquals("tenant", fixture.attestationClientId);
            assertEquals("client", replacement.clientId());
            assertEquals("https://login.example/tenant/oauth2/v2.0/token",
                    replacement.tokenEndpoint());
            assertEquals(0, fixture.keyReleases.get(),
                    "Existing result contexts must retain their key handles.");
        }
    }

    @Test
    void failedForcedRefreshCannotRestoreTheEvictedBinding() throws Exception {
        try (BindingFixture fixture = new BindingFixture(false)) {
            ManagedIdentityMtlsBinding first =
                    fixture.provider.getOrCreateBinding(fixture.request(false));
            fixture.failIssuance = true;

            assertThrows(MtlsMsiException.class,
                    () -> fixture.provider.getOrCreateBinding(fixture.request(true)));
            assertEquals(1, fixture.keyReleases.get());
            fixture.failIssuance = false;

            ManagedIdentityMtlsBinding replacement =
                    fixture.provider.getOrCreateBinding(fixture.request(false));

            assertNotEquals(first.bindingContext().keyId(), replacement.bindingContext().keyId());
            assertEquals(3, fixture.issuances.get());
            assertEquals(0, fixture.computeReads.get());
            assertEquals(0, fixture.attestations.get());
        }
    }

    @Test
    void forcedFirstAcquisitionIssuesOnlyOneCertificate() throws Exception {
        try (BindingFixture fixture = new BindingFixture(false)) {
            ManagedIdentityMtlsBinding binding =
                    fixture.provider.getOrCreateBinding(fixture.request(true));

            assertSame(binding, fixture.provider.getOrCreateBinding(fixture.request(false)));
            assertEquals(1, fixture.issuances.get());
            assertEquals(0, fixture.computeReads.get());
            assertEquals(0, fixture.attestations.get());
        }
    }

    @Test
    void certificateRotatesInsideTwentyFourHourWindow() {
        long now = Instant.now().toEpochMilli();

        assertTrue(KeyGuardManagedIdentityMtlsProvider.isCertificateCurrent(
                now + Duration.ofHours(25).toMillis(), now));
        assertFalse(KeyGuardManagedIdentityMtlsProvider.isCertificateCurrent(
                now + Duration.ofHours(24).toMillis(), now));
        assertFalse(KeyGuardManagedIdentityMtlsProvider.isCertificateCurrent(
                now + Duration.ofHours(1).toMillis(), now));
    }

    @Test
    void capabilityProbeAndBindingCreationShareTheSameKeyLock() throws Exception {
        KeyGuardManagedIdentityMtlsProvider provider =
                new KeyGuardManagedIdentityMtlsProvider();
        CountDownLatch probeEntered = new CountDownLatch(1);
        CountDownLatch releaseProbe = new CountDownLatch(1);
        CountDownLatch acquisitionStarted = new CountDownLatch(1);
        AtomicInteger httpCalls = new AtomicInteger();
        ManagedIdentityMtlsRequest request = new ManagedIdentityMtlsRequest(
                null, null, UUID.randomUUID().toString(), "correlation",
                ignored -> {
                    if (httpCalls.incrementAndGet() == 1) {
                        probeEntered.countDown();
                        try {
                            if (!releaseProbe.await(5, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("Timed out releasing capability probe.");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                    }
                    throw new MtlsMsiException("Stop before accessing native keys.");
                }, false);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> probe = executor.submit(
                    () -> provider.getMaxSupportedBindingStrength(request));
            assertTrue(probeEntered.await(5, TimeUnit.SECONDS));
            Future<?> acquisition = executor.submit(() -> {
                acquisitionStarted.countDown();
                return provider.getOrCreateBinding(request);
            });
            assertTrue(acquisitionStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,
                    () -> acquisition.get(250, TimeUnit.MILLISECONDS));
            assertEquals(1, httpCalls.get());

            releaseProbe.countDown();

            assertInstanceOf(MtlsMsiException.class,
                    assertThrows(ExecutionException.class,
                            () -> probe.get(5, TimeUnit.SECONDS)).getCause());
            assertInstanceOf(MtlsMsiException.class,
                    assertThrows(ExecutionException.class,
                            () -> acquisition.get(5, TimeUnit.SECONDS)).getCause());
            assertEquals(2, httpCalls.get());
        } finally {
            releaseProbe.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void retiredGenerationPreventsUnlockedCacheHit() {
        long now = Instant.now().toEpochMilli();
        KeyGuardManagedIdentityMtlsProvider.BindingGeneration current =
                new KeyGuardManagedIdentityMtlsProvider.BindingGeneration(
                        null,
                        null,
                        now + Duration.ofHours(25).toMillis());

        assertTrue(KeyGuardManagedIdentityMtlsProvider
                .canReturnWithoutCleanup(current, false));
        assertFalse(KeyGuardManagedIdentityMtlsProvider
                .canReturnWithoutCleanup(current, true));
    }

    @Test
    void cleanupClosesOnlyExpiredRetiredGenerations() throws Exception {
        long now = Instant.now().toEpochMilli();
        AtomicInteger expiredReleases = new AtomicInteger();
        AtomicInteger activeReleases = new AtomicInteger();
        KeyGuardManagedIdentityMtlsProvider.BindingGeneration expired =
                generation(now - 1, expiredReleases);
        KeyGuardManagedIdentityMtlsProvider.BindingGeneration active =
                generation(now + Duration.ofHours(1).toMillis(), activeReleases);

        List<KeyGuardManagedIdentityMtlsProvider.BindingGeneration> retained =
                KeyGuardManagedIdentityMtlsProvider.cleanupRetiredGenerations(
                        Arrays.asList(expired, active),
                        now);

        assertEquals(1, retained.size());
        assertSame(active, retained.get(0));
        assertEquals(1, expiredReleases.get());
        assertEquals(0, activeReleases.get());
    }

    @Test
    void providerSupportsBindingInvalidationForBoundedRetry() throws Exception {
        KeyGuardManagedIdentityMtlsProvider provider =
                new KeyGuardManagedIdentityMtlsProvider();
        ManagedIdentityMtlsRequest request = new ManagedIdentityMtlsRequest(
                null,
                null,
                "missing-binding",
                "correlation",
                ignored -> new ManagedIdentityMtlsHttpResponse(
                        500,
                        "",
                        java.util.Collections.emptyMap()),
                false);

        assertTrue(provider.invalidateBinding(
                request,
                generation(
                        Instant.now().plus(Duration.ofHours(48)).toEpochMilli(),
                        new AtomicInteger()).binding));
    }

    @Test
    void unexpiredRejectedGenerationsRemainUsable() throws Exception {
        AtomicInteger oldestReleases = new AtomicInteger();
        AtomicInteger middleReleases = new AtomicInteger();
        AtomicInteger newestReleases = new AtomicInteger();
        List<KeyGuardManagedIdentityMtlsProvider.BindingGeneration> generations =
                new java.util.ArrayList<>(Arrays.asList(
                        generation(
                                Instant.now().plus(Duration.ofHours(48)).toEpochMilli(),
                                oldestReleases),
                        generation(
                                Instant.now().plus(Duration.ofHours(48)).toEpochMilli(),
                                middleReleases),
                        generation(
                                Instant.now().plus(Duration.ofHours(48)).toEpochMilli(),
                                newestReleases)));

        List<KeyGuardManagedIdentityMtlsProvider.BindingGeneration> retained =
                KeyGuardManagedIdentityMtlsProvider.cleanupRetiredGenerations(
                        generations,
                        Instant.now().toEpochMilli());

        assertEquals(3, retained.size());
        assertEquals(0, oldestReleases.get());
        assertEquals(0, middleReleases.get());
        assertEquals(0, newestReleases.get());
    }

    private static KeyGuardManagedIdentityMtlsProvider.BindingGeneration generation(
            long notAfterMillis,
            AtomicInteger releases) throws Exception {
        CngRsaPrivateKey key = new CngRsaPrivateKey(
                Pointer.createConstant(notAfterMillis),
                BigInteger.valueOf(17),
                65537,
                releases::incrementAndGet);
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getEncoded()).thenReturn(
                Long.toString(notAfterMillis).getBytes("UTF-8"));
        KeyGuardMtlsBindingContext context =
                new KeyGuardMtlsBindingContext(key, certificate);
        return new KeyGuardManagedIdentityMtlsProvider.BindingGeneration(
                new ManagedIdentityMtlsBinding(
                        context,
                        "client",
                        "https://login.example/token"),
                context,
                notAfterMillis);
    }

    private static final class BindingFixture implements AutoCloseable {
        final KeyGuardManagedIdentityMtlsProvider provider =
                new KeyGuardManagedIdentityMtlsProvider();
        final AtomicInteger issuances = new AtomicInteger();
        final AtomicInteger computeReads = new AtomicInteger();
        final AtomicInteger keyOpens = new AtomicInteger();
        final AtomicInteger keyReleases = new AtomicInteger();
        final AtomicInteger attestations = new AtomicInteger();
        final Set<String> keyNames = new HashSet<>();
        final String cacheKey = UUID.randomUUID().toString();
        final boolean attested;
        final KeyPair keyPair;
        final MockedStatic<CngKeyGuard> nativeKey;
        boolean failIssuance;
        String attestationEndpoint;
        String attestationClientId;

        BindingFixture(boolean attested) throws Exception {
            this.attested = attested;
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair = generator.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            nativeKey = Mockito.mockStatic(CngKeyGuard.class);
            nativeKey.when(() -> CngKeyGuard.getOrCreateKey(anyString()))
                    .thenAnswer(call -> {
                        keyNames.add(call.getArgument(0));
                        return new CngRsaPrivateKey(
                                Pointer.createConstant(keyOpens.incrementAndGet()),
                                publicKey.getModulus(),
                                publicKey.getPublicExponent().intValue(),
                                keyReleases::incrementAndGet);
                    });
            nativeKey.when(() -> CngKeyGuard.exportPublicKey(any()))
                    .thenReturn(new BigInteger[]{
                            publicKey.getModulus(), publicKey.getPublicExponent()});
            nativeKey.when(() -> CngKeyGuard.exportPublicKeyBytes(any()))
                    .thenReturn(publicKey.getEncoded());
            nativeKey.when(() -> CngKeyGuard.signPss(any(), any(), anyString(), anyInt()))
                    .thenReturn(new byte[256]);
            nativeKey.when(() -> CngKeyGuard.getAttestationToken(
                            any(), anyString(), anyString(), anyString()))
                    .thenAnswer(call -> {
                        attestations.incrementAndGet();
                        attestationEndpoint = call.getArgument(1);
                        attestationClientId = call.getArgument(2);
                        return "header." + Base64.getUrlEncoder().withoutPadding().encodeToString(
                                ("{\"exp\":" + Instant.now().plusSeconds(3600).getEpochSecond() + "}")
                                        .getBytes(StandardCharsets.UTF_8)) + ".signature";
                    });
        }

        ManagedIdentityMtlsRequest request(boolean forceRefresh) {
            return new ManagedIdentityMtlsRequest(
                    null, null, cacheKey, "correlation",
                    http -> {
                        String body;
                        if (http.url().contains("getplatformmetadata")) {
                            body = "{\"clientId\":\"client\",\"tenantId\":\"tenant\","
                                    + "\"cuId\":{\"vmId\":\"vm\"},"
                                    + "\"attestationEndpoint\":\"https://platform.example\"}";
                        } else if (http.url().contains("/instance/compute")) {
                            computeReads.incrementAndGet();
                            body = "{\"azEnvironment\":\"AzurePublicCloud\",\"location\":\"westus2\"}";
                        } else {
                            assertTrue(http.url().contains("issuecredential"));
                            int serial = issuances.incrementAndGet();
                            if (failIssuance) {
                                return new ManagedIdentityMtlsHttpResponse(
                                        500, "", Collections.emptyMap());
                            }
                            assertEquals(attested, http.body().contains("attestation_token"));
                            body = "{\"certificate\":\"" + certificate(serial) + "\","
                                    + "\"mtls_authentication_endpoint\":\"https://login.example\","
                                    + "\"client_id\":\"client\",\"tenant_id\":\"tenant\","
                                    + "\"identity_type\":\"SystemAssigned\"}";
                        }
                        return new ManagedIdentityMtlsHttpResponse(
                                200, body, Collections.singletonMap(
                                        "Server", Collections.singletonList("IMDS")));
                    }, attested, forceRefresh);
        }

        private String certificate(int serial) {
            try {
                byte[] algorithm = Base64.getDecoder().decode("MA0GCSqGSIb3DQEBCwUA");
                byte[] name = new X500Principal("CN=client,DC=tenant").getEncoded();
                byte[] validity = Pkcs10Builder.derSequence(join(
                        new byte[]{0x18, 15}, "20250101000000Z".getBytes(StandardCharsets.US_ASCII),
                        new byte[]{0x18, 15}, "20990101000000Z".getBytes(StandardCharsets.US_ASCII)));
                byte[] tbs = Pkcs10Builder.derSequence(join(
                        new byte[]{2, 1, (byte) serial}, algorithm, name,
                        validity, name, keyPair.getPublic().getEncoded()));
                Signature signer = Signature.getInstance("SHA256withRSA");
                signer.initSign(keyPair.getPrivate());
                signer.update(tbs);
                return Base64.getEncoder().encodeToString(Pkcs10Builder.derSequence(
                        join(tbs, algorithm, Pkcs10Builder.derBitString(signer.sign()))));
            } catch (Exception e) {
                throw new AssertionError("Unable to issue the test binding certificate.", e);
            }
        }

        private static byte[] join(byte[]... parts) throws java.io.IOException {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            for (byte[] part : parts) {
                bytes.write(part);
            }
            return bytes.toByteArray();
        }

        @Override
        public void close() {
            nativeKey.close();
        }
    }
}
