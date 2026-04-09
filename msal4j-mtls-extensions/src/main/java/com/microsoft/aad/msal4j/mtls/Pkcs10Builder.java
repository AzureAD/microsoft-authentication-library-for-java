// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Pointer;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Builds a PKCS#10 Certification Request (CSR) that matches the format produced by
 * MSAL.NET and msal-go for the Azure IMDSv2 {@code /issuecredential} endpoint.
 *
 * <h2>CSR Structure</h2>
 * <pre>
 * CertificationRequest ::= SEQUENCE {
 *     certificationRequestInfo  CertificationRequestInfo,
 *     signatureAlgorithm        AlgorithmIdentifier,   -- RSASSA-PSS with SHA-256 params
 *     signature                 BIT STRING
 * }
 *
 * CertificationRequestInfo ::= SEQUENCE {
 *     version       INTEGER { v1(0) }
 *     subject       Name   -- CN={clientId}, DC={tenantId}
 *     subjectPKInfo SubjectPublicKeyInfo
 *     attributes    [0] IMPLICIT SET OF -- OID 1.3.6.1.4.1.311.90.2.10 = cuId JSON
 * }
 * </pre>
 *
 * <h2>Signing</h2>
 * Signature: RSASSA-PSS with SHA-256, salt length = 32 bytes (hash output length).
 * Signing is delegated to {@link CngKeyGuard#signPss} so the non-exportable KeyGuard key
 * never leaves CNG.
 *
 * <p>This is a pure-Java port of msal-go's {@code generateCSR()} in {@code imdsv2.go},
 * using manual DER encoding to avoid adding external ASN.1 library dependencies.</p>
 */
final class Pkcs10Builder {

    private Pkcs10Builder() {}

    // ─── OIDs (pre-encoded DER) ────────────────────────────────────────────────

    // rsaEncryption: 1.2.840.113549.1.1.1
    private static final byte[] OID_RSA_ENCRYPTION = hexToBytes("2a864886f70d010101");
    // sha256: 2.16.840.1.101.3.4.2.1
    private static final byte[] OID_SHA256 = hexToBytes("608648016503040201");
    // mgf1: 1.2.840.113549.1.1.8
    private static final byte[] OID_MGF1   = hexToBytes("2a864886f70d010108");
    // id-RSASSA-PSS: 1.2.840.113549.1.1.10
    private static final byte[] OID_RSASSA_PSS = hexToBytes("2a864886f70d01010a");
    // commonName: 2.5.4.3
    private static final byte[] OID_COMMON_NAME = hexToBytes("5504 03".replace(" ", ""));
    // domainComponent: 0.9.2342.19200300.100.1.25
    private static final byte[] OID_DOMAIN_COMPONENT = hexToBytes("0992268993f22c6401 19".replace(" ", ""));
    // cuId attribute: 1.3.6.1.4.1.311.90.2.10
    private static final byte[] OID_CU_ID = hexToBytes("2b060104018237 5a02 0a".replace(" ", ""));

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Generates a PKCS#10 CSR and returns it as standard Base64-encoded DER
     * (no PEM headers), ready to be placed in the {@code csr} field of the
     * IMDS {@code /issuecredential} JSON request.
     *
     * @param keyHandle CNG key handle (the private key — signs the CSR TBS bytes)
     * @param modulus   RSA public key modulus (from {@link CngKeyGuard#exportPublicKey})
     * @param publicExp RSA public exponent
     * @param clientId  managed identity client ID → CN in subject
     * @param tenantId  tenant GUID → DC in subject
     * @param vmId      VM ID for the cuId attribute ({@code cuId.vmId}); may be null
     * @param vmssId    VMSS ID for the cuId attribute; may be null
     * @return Base64-encoded DER of the PKCS#10 CSR
     */
    static String generate(Pointer keyHandle, BigInteger modulus, int publicExp,
                           String clientId, String tenantId, String vmId, String vmssId)
            throws MtlsMsiException {

        // --- SubjectPublicKeyInfo ------------------------------------------
        byte[] spki = buildSpki(modulus, publicExp);

        // --- Subject: CN={clientId}, DC={tenantId} -------------------------
        byte[] subject = buildSubject(clientId, tenantId);

        // --- cuId attribute ------------------------------------------------
        byte[] cuIdJson = buildCuIdJson(vmId, vmssId);
        byte[] attributes = buildCuIdAttribute(cuIdJson);

        // --- CertificationRequestInfo SEQUENCE -----------------------------
        byte[] version = derInteger(new byte[]{0x00}); // INTEGER v1(0)
        byte[] certReqInfo = derSequence(concat(version, subject, spki, attributes));

        // --- Sign with RSASSA-PSS SHA-256 (salt=32) -----------------------
        byte[] tbs;
        try {
            tbs = MessageDigest.getInstance("SHA-256").digest(certReqInfo);
        } catch (NoSuchAlgorithmException e) {
            throw new MtlsMsiException("SHA-256 not available: " + e.getMessage(), e);
        }
        byte[] sig = CngKeyGuard.signPss(keyHandle, tbs, "SHA256", 32);

        // --- AlgorithmIdentifier for RSASSA-PSS ----------------------------
        byte[] sigAlgId = buildPssAlgorithmIdentifier();

        // --- BIT STRING wrapping the signature -----------------------------
        byte[] sigBitString = derBitString(sig);

        // --- Final CertificationRequest SEQUENCE ---------------------------
        byte[] csr = derSequence(concat(certReqInfo, sigAlgId, sigBitString));

        return Base64.getEncoder().encodeToString(csr);
    }

    // ─── DER building blocks ──────────────────────────────────────────────────

    /** DER SEQUENCE */
    static byte[] derSequence(byte[] content) {
        return derTagLen(0x30, content);
    }

    /** DER SET */
    private static byte[] derSet(byte[] content) {
        return derTagLen(0x31, content);
    }

    /** DER INTEGER from raw bytes (big-endian, with sign byte if high bit set) */
    private static byte[] derInteger(byte[] value) {
        // Add leading 0x00 if high bit is set (unsigned → signed two's complement).
        byte[] content = (value[0] & 0x80) != 0
                ? concat(new byte[]{0x00}, value)
                : value;
        return derTagLen(0x02, content);
    }

    /** DER OBJECT IDENTIFIER from pre-encoded OID value bytes */
    private static byte[] derOid(byte[] oidBytes) {
        return derTagLen(0x06, oidBytes);
    }

    /** DER UTF8String */
    private static byte[] derUtf8String(String s) {
        byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return derTagLen(0x0C, bytes);
    }

    /** DER BIT STRING — prepend 0x00 (zero unused bits) */
    static byte[] derBitString(byte[] data) {
        byte[] content = new byte[data.length + 1];
        content[0] = 0x00;
        System.arraycopy(data, 0, content, 1, data.length);
        return derTagLen(0x03, content);
    }

    /** DER NULL */
    private static final byte[] DER_NULL = {0x05, 0x00};

    /** Context-specific explicit tag [N] wrapping content */
    private static byte[] contextExplicit(int n, byte[] content) {
        return derTagLen(0xA0 | n, content);
    }

    /** Context-specific implicit tag [N] wrapping content */
    private static byte[] contextImplicit(int n, byte[] content) {
        return derTagLen(0x80 | n, content);
    }

    /** Writes tag + DER length + content */
    private static byte[] derTagLen(int tag, byte[] content) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        int len = content.length;
        if (len < 0x80) {
            out.write(len);
        } else if (len < 0x100) {
            out.write(0x81);
            out.write(len);
        } else if (len < 0x10000) {
            out.write(0x82);
            out.write((len >> 8) & 0xFF);
            out.write(len & 0xFF);
        } else {
            out.write(0x83);
            out.write((len >> 16) & 0xFF);
            out.write((len >> 8) & 0xFF);
            out.write(len & 0xFF);
        }
        try { out.write(content); } catch (java.io.IOException ignored) {}
        return out.toByteArray();
    }

    // ─── Component builders ───────────────────────────────────────────────────

    /**
     * SubjectPublicKeyInfo ::= SEQUENCE { algorithm AlgorithmIdentifier, subjectPublicKey BIT STRING }
     * AlgorithmIdentifier for RSA: SEQUENCE { OID rsaEncryption, NULL }
     * Public key: BIT STRING containing RSAPublicKey SEQUENCE { modulus INTEGER, publicExp INTEGER }
     */
    private static byte[] buildSpki(BigInteger modulus, int publicExp) {
        // RSAPublicKey SEQUENCE { modulus INTEGER, publicExp INTEGER }
        byte[] modBytes = modulus.toByteArray();
        byte[] expBytes = BigInteger.valueOf(publicExp).toByteArray();
        byte[] rsaPublicKey = derSequence(concat(derInteger(modBytes), derInteger(expBytes)));

        // AlgorithmIdentifier for rsaEncryption
        byte[] algId = derSequence(concat(derOid(OID_RSA_ENCRYPTION), DER_NULL));

        // SubjectPublicKeyInfo
        return derSequence(concat(algId, derBitString(rsaPublicKey)));
    }

    /**
     * Name ::= SEQUENCE { RDN SEQUENCE { AttributeTypeAndValue SEQUENCE { OID, value } } }
     * Subject: CN={clientId}, DC={tenantId}
     * Matches msal-go: pkix.Name{CommonName: clientId, ExtraNames: []pkix.AttributeTypeAndValue{{Type: dcOID, Value: tenantId}}}
     */
    private static byte[] buildSubject(String clientId, String tenantId) {
        // AttributeTypeAndValue SEQUENCE { OID commonName, UTF8String clientId }
        byte[] cnAttr = derSequence(concat(derOid(OID_COMMON_NAME), derUtf8String(clientId)));
        byte[] cnRdn  = derSet(cnAttr);

        // AttributeTypeAndValue SEQUENCE { OID domainComponent, UTF8String tenantId }
        byte[] dcAttr = derSequence(concat(derOid(OID_DOMAIN_COMPONENT), derUtf8String(tenantId)));
        byte[] dcRdn  = derSet(dcAttr);

        // Name = SEQUENCE of RDNs
        return derSequence(concat(cnRdn, dcRdn));
    }

    /**
     * Builds the cuId JSON string. Matches msal-go's json.Marshal(cuID):
     * {@code {"vmId":"<vmId>","vmssId":"<vmssId>"}} with omitempty semantics.
     */
    private static byte[] buildCuIdJson(String vmId, String vmssId) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        if (vmId != null && !vmId.isEmpty()) {
            sb.append("\"vmId\":\"").append(vmId).append("\"");
            first = false;
        }
        if (vmssId != null && !vmssId.isEmpty()) {
            if (!first) sb.append(",");
            sb.append("\"vmssId\":\"").append(vmssId).append("\"");
        }
        sb.append("}");
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * CertificationRequestInfo attributes [0]:
     * [0] IMPLICIT SET OF {
     *   SEQUENCE { OID 1.3.6.1.4.1.311.90.2.10, SET { UTF8String(cuIdJson) } }
     * }
     * Matches msal-go's buildCuIDAttribute().
     */
    private static byte[] buildCuIdAttribute(byte[] cuIdJsonBytes) {
        byte[] utf8Str    = derTagLen(0x0C, cuIdJsonBytes);               // UTF8String
        byte[] valueSet   = derSet(utf8Str);                               // SET { UTF8String }
        byte[] attrSeq    = derSequence(concat(derOid(OID_CU_ID), valueSet)); // SEQUENCE { OID, SET }
        return contextImplicit(0, attrSeq);                                // [0] IMPLICIT
    }

    /**
     * AlgorithmIdentifier for RSASSA-PSS with SHA-256:
     * SEQUENCE {
     *   OID id-RSASSA-PSS,
     *   SEQUENCE {   -- RSASSA-PSS-params
     *     [0] SEQUENCE { OID sha-256, NULL },   -- hashAlgorithm
     *     [1] SEQUENCE { OID mgf1, SEQUENCE { OID sha-256, NULL } }, -- maskGenAlgorithm
     *     [2] INTEGER 32                          -- saltLength
     *   }
     * }
     * Matches msal-go's explicit PSS AlgorithmIdentifier.
     */
    private static byte[] buildPssAlgorithmIdentifier() {
        // sha256AlgID: SEQUENCE { OID sha256, NULL }
        byte[] sha256AlgId = derSequence(concat(derOid(OID_SHA256), DER_NULL));

        // hashAlgorithm [0]: sha256AlgID
        byte[] hashAlgorithm = contextExplicit(0, sha256AlgId);

        // mgf1AlgID: SEQUENCE { OID mgf1, sha256AlgID }
        byte[] mgf1AlgId = derSequence(concat(derOid(OID_MGF1), sha256AlgId));
        // maskGenAlgorithm [1]: mgf1AlgID
        byte[] maskGenAlgorithm = contextExplicit(1, mgf1AlgId);

        // saltLength [2]: INTEGER 32
        byte[] saltLength = contextExplicit(2, derInteger(new byte[]{32}));

        // RSASSA-PSS-params SEQUENCE
        byte[] pssParams = derSequence(concat(hashAlgorithm, maskGenAlgorithm, saltLength));

        // AlgorithmIdentifier SEQUENCE { OID id-RSASSA-PSS, pssParams }
        return derSequence(concat(derOid(OID_RSASSA_PSS), pssParams));
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    private static byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] a : arrays) total += a.length;
        byte[] result = new byte[total];
        int offset = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, result, offset, a.length);
            offset += a.length;
        }
        return result;
    }

    private static byte[] hexToBytes(String hex) {
        hex = hex.replace(" ", "");
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }
}
