// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.microsoft.aad.msal4j.IManagedIdentityMtlsProvider;
import com.microsoft.aad.msal4j.ManagedIdentityMtlsBinding;
import com.microsoft.aad.msal4j.ManagedIdentityMtlsRequest;
import com.microsoft.aad.msal4j.MtlsBindingStrength;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Windows KeyGuard/MAA implementation of the managed identity mTLS provider SPI.
 */
public final class KeyGuardManagedIdentityMtlsProvider
        implements IManagedIdentityMtlsProvider {

    private static final long ROTATION_BUFFER_MILLIS = 24L * 60L * 60L * 1000L;
    private static final AttestationTokenCache ATTESTATION_CACHE =
            new AttestationTokenCache();
    private static final Map<String, BindingGeneration> CURRENT =
            new ConcurrentHashMap<>();
    private static final Map<String, List<BindingGeneration>> RETIRED =
            new ConcurrentHashMap<>();
    private static final Map<String, Object> LOCKS = new ConcurrentHashMap<>();

    @Override
    public ManagedIdentityMtlsBinding getOrCreateBinding(
            ManagedIdentityMtlsRequest request) {
        validateRequest(request);
        String cacheKey = request.bindingCacheKey();
        BindingGeneration cached = CURRENT.get(cacheKey);
        if (canReturnWithoutCleanup(
                cached,
                RETIRED.containsKey(cacheKey))) {
            return cached.binding;
        }

        Object lock = LOCKS.computeIfAbsent(cacheKey, ignored -> new Object());
        synchronized (lock) {
            cleanupRetired(cacheKey);
            cached = CURRENT.get(cacheKey);
            if (isCurrent(cached)) {
                return cached.binding;
            }
            BindingGeneration created = createBinding(request);
            BindingGeneration previous = CURRENT.put(cacheKey, created);
            if (previous != null) {
                RETIRED.computeIfAbsent(cacheKey, ignored -> new ArrayList<>())
                        .add(previous);
            }
            return created.binding;
        }
    }

    @Override
    public MtlsBindingStrength getMaxSupportedBindingStrength(
            ManagedIdentityMtlsRequest request) {
        validateRequest(request);
        ImdsV2Client.PlatformMetadata metadata =
                ImdsV2Client.getPlatformMetadata(request);
        validateSelectedIdentity(request, metadata);
        CngRsaPrivateKey privateKey = CngKeyGuard.getOrCreateKey(
                keyName(request, metadata));
        try {
            CngKeyGuard.exportPublicKey(privateKey.nativeHandle());
            return MtlsBindingStrength.KEY_GUARD;
        } finally {
            privateKey.close();
        }
    }

    private static BindingGeneration createBinding(
            ManagedIdentityMtlsRequest request) {
        ImdsV2Client.PlatformMetadata metadata =
                ImdsV2Client.getPlatformMetadata(request);
        validateSelectedIdentity(request, metadata);

        CngRsaPrivateKey privateKey = CngKeyGuard.getOrCreateKey(
                keyName(request, metadata));
        try {
            BigInteger[] publicKey =
                    CngKeyGuard.exportPublicKey(privateKey.nativeHandle());
            String attestationKeyId = hashBytes(
                    CngKeyGuard.exportPublicKeyBytes(privateKey.nativeHandle()));
            String csr = Pkcs10Builder.generate(
                    privateKey.nativeHandle(),
                    publicKey[0],
                    publicKey[1].intValue(),
                    metadata.clientId,
                    metadata.tenantId,
                    metadata.vmId,
                    metadata.vmssId);
            String attestationToken = request.attestationEnabled()
                    ? ATTESTATION_CACHE.getOrAttest(
                            metadata.attestationEndpoint,
                            attestationKeyId,
                            () -> CngKeyGuard.getAttestationToken(
                                    privateKey.nativeHandle(),
                                    metadata.attestationEndpoint,
                                    metadata.clientId))
                    : null;
            ImdsV2Client.CredentialResponse credential =
                    ImdsV2Client.issueCredential(request, csr, attestationToken);
            validateCredential(metadata, request, credential);

            X509Certificate certificate = parseCertificate(credential.certificate);
            validateCertificateMatchesKey(certificate, publicKey);
            KeyGuardMtlsBindingContext context =
                    new KeyGuardMtlsBindingContext(privateKey, certificate);
            String endpoint = trimTrailingSlash(
                    credential.mtlsAuthenticationEndpoint)
                    + "/" + trimSlashes(credential.tenantId)
                    + "/oauth2/v2.0/token";
            ManagedIdentityMtlsBinding binding = new ManagedIdentityMtlsBinding(
                    context,
                    credential.clientId,
                    endpoint);
            return new BindingGeneration(binding, context, certificate.getNotAfter().getTime());
        } catch (RuntimeException e) {
            privateKey.close();
            throw e;
        }
    }

    private static void validateRequest(ManagedIdentityMtlsRequest request) {
        if (request == null
                || request.httpClient() == null
                || isBlank(request.bindingCacheKey())
                || isBlank(request.correlationId())) {
            throw new MtlsMsiException(
                    "Managed identity mTLS provider request is incomplete.");
        }
        if ((request.identityQueryParameter() == null)
                != (request.identityQueryValue() == null)) {
            throw new MtlsMsiException(
                    "Managed identity selector name and value must be supplied together.");
        }
    }

    private static void validateSelectedIdentity(
            ManagedIdentityMtlsRequest request,
            ImdsV2Client.PlatformMetadata metadata) {
        if ("client_id".equals(request.identityQueryParameter())
                && !metadata.clientId.equalsIgnoreCase(request.identityQueryValue())) {
            throw new MtlsMsiException(
                    "IMDS returned a different managed identity than the requested client ID.");
        }
    }

    private static void validateCredential(
            ImdsV2Client.PlatformMetadata metadata,
            ManagedIdentityMtlsRequest request,
            ImdsV2Client.CredentialResponse credential) {
        if (!metadata.clientId.equalsIgnoreCase(credential.clientId)
                || !metadata.tenantId.equalsIgnoreCase(credential.tenantId)) {
            throw new MtlsMsiException(
                    "IMDS issuecredential identity does not match platform metadata.");
        }
        boolean systemAssigned = request.identityQueryParameter() == null;
        String expectedIdentityType =
                systemAssigned ? "SystemAssigned" : "UserAssigned";
        if (!expectedIdentityType.equalsIgnoreCase(credential.identityType)) {
            throw new MtlsMsiException(
                    "IMDS issuecredential returned unexpected identity_type '"
                            + credential.identityType + "'.");
        }
    }

    private static X509Certificate parseCertificate(String encoded) {
        try {
            byte[] der = Base64.getDecoder().decode(encoded);
            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(der));
        } catch (Exception e) {
            throw new MtlsMsiException(
                    "Unable to parse the IMDS binding certificate.", e);
        }
    }

    private static void validateCertificateMatchesKey(
            X509Certificate certificate,
            BigInteger[] publicKey) {
        if (!(certificate.getPublicKey() instanceof RSAPublicKey)) {
            throw new MtlsMsiException(
                    "IMDS binding certificate does not contain an RSA public key.");
        }
        RSAPublicKey certificateKey = (RSAPublicKey) certificate.getPublicKey();
        if (!publicKey[0].equals(certificateKey.getModulus())
                || !publicKey[1].equals(certificateKey.getPublicExponent())) {
            throw new MtlsMsiException(
                    "IMDS binding certificate does not match the KeyGuard key.");
        }
    }

    private static boolean isCurrent(BindingGeneration generation) {
        return generation != null
                && isCertificateCurrent(
                        generation.notAfterMillis,
                        System.currentTimeMillis());
    }

    static boolean canReturnWithoutCleanup(
            BindingGeneration generation,
            boolean hasRetiredGenerations) {
        return isCurrent(generation) && !hasRetiredGenerations;
    }

    static boolean isCertificateCurrent(long notAfterMillis, long nowMillis) {
        return nowMillis < notAfterMillis - ROTATION_BUFFER_MILLIS;
    }

    private static void cleanupRetired(String cacheKey) {
        List<BindingGeneration> generations = RETIRED.get(cacheKey);
        if (generations == null) {
            return;
        }
        List<BindingGeneration> retained = cleanupRetiredGenerations(
                generations,
                System.currentTimeMillis());
        if (retained.isEmpty()) {
            RETIRED.remove(cacheKey);
        } else {
            RETIRED.put(cacheKey, retained);
        }
    }

    static List<BindingGeneration> cleanupRetiredGenerations(
            List<BindingGeneration> generations,
            long now) {
        List<BindingGeneration> retained = new ArrayList<>();
        for (BindingGeneration generation : generations) {
            if (now >= generation.notAfterMillis) {
                generation.context.closeNativeKey();
            } else {
                retained.add(generation);
            }
        }
        return retained;
    }

    private static String shortHash(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(hash).substring(0, 32);
        } catch (Exception e) {
            throw new MtlsMsiException("Unable to derive the KeyGuard key name.", e);
        }
    }

    private static String keyName(
            ManagedIdentityMtlsRequest request,
            ImdsV2Client.PlatformMetadata metadata) {
        return "MSALJavaMtls_" + shortHash(
                request.bindingCacheKey() + "|" + metadata.cuId());
    }

    private static String hashBytes(byte[] value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception e) {
            throw new MtlsMsiException(
                    "Unable to derive the KeyGuard attestation cache key.", e);
        }
    }

    private static String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String trimSlashes(String value) {
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        return trimTrailingSlash(value);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static final class BindingGeneration {
        final ManagedIdentityMtlsBinding binding;
        final KeyGuardMtlsBindingContext context;
        final long notAfterMillis;

        BindingGeneration(
                ManagedIdentityMtlsBinding binding,
                KeyGuardMtlsBindingContext context,
                long notAfterMillis) {
            this.binding = binding;
            this.context = context;
            this.notAfterMillis = notAfterMillis;
        }
    }
}
