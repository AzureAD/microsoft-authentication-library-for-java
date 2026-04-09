// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Acquires and caches the mTLS PoP binding certificate for a managed identity.
 *
 * <p>The binding certificate is issued by IMDS's {@code /issuecredential} endpoint and
 * ties the KeyGuard CNG key to the managed identity. It is valid for several hours;
 * this class caches the binding and refreshes it 5 minutes before expiry.</p>
 *
 * <p>The full flow mirrors msal-go's {@code buildMtlsBindingInfo()}:</p>
 * <ol>
 *   <li>GET {@code /getplatformmetadata} → clientId, tenantId, cuId, attestationEndpoint</li>
 *   <li>Get or create KeyGuard CNG key (persisted in Software KSP, USER scope)</li>
 *   <li>Generate PKCS#10 CSR with the CNG key (RSASSA-PSS SHA-256)</li>
 *   <li>If {@code attestationEndpoint} present: call {@code AttestationClientLib.dll} for MAA JWT</li>
 *   <li>POST CSR (+ MAA JWT) to {@code /issuecredential} → DER certificate (base64)</li>
 *   <li>Parse certificate, build {@link MtlsBindingInfo}</li>
 * </ol>
 */
final class MtlsBindingCertManager {

    private static final Map<String, MtlsBindingInfo> CACHE     = new HashMap<>();
    private static final Object                        CACHE_LOCK = new Object();

    private MtlsBindingCertManager() {}

    /**
     * Returns a valid (non-expired) {@link MtlsBindingInfo} for the managed identity on
     * this VM, fetching/refreshing from IMDS as needed.
     *
     * @param withAttestation whether to request a MAA attestation JWT and include it in the
     *                        {@code /issuecredential} request (requires Trusted Launch VM)
     * @return binding info containing the CNG private key and the IMDS-issued certificate
     * @throws MtlsMsiException on any error
     */
    static MtlsBindingInfo getOrCreate(boolean withAttestation) throws MtlsMsiException {
        // Fetch platform metadata to determine the cache key.
        ImdsV2Client.PlatformMetadata meta = ImdsV2Client.getPlatformMetadata();
        String cacheKey = meta.clientId + "|" + meta.tenantId;

        synchronized (CACHE_LOCK) {
            MtlsBindingInfo existing = CACHE.get(cacheKey);
            if (existing != null && !existing.isExpired()) {
                return existing;
            }
            CACHE.remove(cacheKey);
        }

        // Build new binding info outside the lock (slow: JNA + HTTP).
        MtlsBindingInfo info = buildBindingInfo(meta, withAttestation);

        synchronized (CACHE_LOCK) {
            CACHE.put(cacheKey, info);
        }
        return info;
    }

    private static MtlsBindingInfo buildBindingInfo(ImdsV2Client.PlatformMetadata meta,
                                                     boolean withAttestation)
            throws MtlsMsiException {

        String cuId = meta.cuIdString(); // vmId if present, else clientId

        // 1. Get or create the KeyGuard CNG key.
        CngRsaPrivateKey privateKey = CngKeyGuard.getOrCreateKey("MSALMtlsKey_" + cuId);

        // 2. Export the public key components for CSR construction.
        BigInteger[] pubKey;
        try {
            pubKey = CngKeyGuard.exportPublicKey(privateKey.getHandle());
        } catch (MtlsMsiException e) {
            privateKey.close();
            throw e;
        }

        // 3. Generate PKCS#10 CSR.
        String csrBase64;
        try {
            csrBase64 = Pkcs10Builder.generate(
                    privateKey.getHandle(),
                    pubKey[0],              // modulus
                    pubKey[1].intValue(),   // publicExponent
                    meta.clientId,
                    meta.tenantId,
                    meta.vmId,
                    meta.vmssId);
        } catch (MtlsMsiException e) {
            privateKey.close();
            throw e;
        }

        // 4. MAA attestation (if endpoint is known and attestation requested).
        String attestationToken = null;
        if (withAttestation && meta.attestationEndpoint != null && !meta.attestationEndpoint.isEmpty()) {
            try {
                attestationToken = CngKeyGuard.getAttestationToken(
                        privateKey.getHandle(), meta.attestationEndpoint, meta.clientId);
            } catch (MtlsMsiException e) {
                privateKey.close();
                throw e;
            }
        }

        // 5. Issue credential from IMDS.
        ImdsV2Client.CredentialResponse credResp;
        try {
            credResp = ImdsV2Client.issueCredential(csrBase64, attestationToken);
        } catch (MtlsMsiException e) {
            privateKey.close();
            throw e;
        }

        // 6. Parse the DER certificate.
        X509Certificate cert;
        try {
            byte[] certDer = Base64.getDecoder().decode(credResp.certificate);
            cert = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(certDer));
        } catch (CertificateException | IllegalArgumentException e) {
            privateKey.close();
            throw new MtlsMsiException("Failed to parse IMDS certificate: " + e.getMessage(), e);
        }

        String resolvedClientId = notEmpty(credResp.clientId, meta.clientId);
        String resolvedTenantId = notEmpty(credResp.tenantId, meta.tenantId);
        String endpoint         = notEmpty(credResp.mtlsAuthenticationEndpoint,
                                           "https://mtlsauth.microsoft.com");

        return new MtlsBindingInfo(privateKey, cert, endpoint, resolvedClientId, resolvedTenantId);
    }

    private static String notEmpty(String preferred, String fallback) {
        return (preferred != null && !preferred.isEmpty()) ? preferred : fallback;
    }
}
