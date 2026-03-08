// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Generates PKCS#10 Certificate Signing Requests (CSRs) for the MSI v2 mTLS PoP flow.
 * <p>
 * The CSR is signed with the KeyGuard RSA key (via JNI) and includes a Microsoft-specific
 * OID attribute ({@code 1.3.6.1.4.1.311.90.2.10}) containing the compute unit ID (cuId)
 * as a UTF8String JSON value.
 * <p>
 * The generated CSR uses RSA-PSS with SHA-256 as the signature algorithm.
 */
class CsrGenerator {

    // Pre-encoded OID byte arrays (DER OID encoding: tag 0x06 + length + VLQ-encoded OID arcs)
    // rsaEncryption: 1.2.840.113549.1.1.1
    private static final byte[] OID_RSA_ENCRYPTION =
            {0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01};
    // id-RSASSA-PSS: 1.2.840.113549.1.1.10
    private static final byte[] OID_RSASSA_PSS =
            {0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x0A};
    // id-mgf1: 1.2.840.113549.1.1.8
    private static final byte[] OID_MGF1 =
            {0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x08};
    // sha-256: 2.16.840.1.101.3.4.2.1
    private static final byte[] OID_SHA256 =
            {0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01};
    // commonName: 2.5.4.3
    private static final byte[] OID_COMMON_NAME = {0x55, 0x04, 0x03};
    // Microsoft MSI v2 cuId OID: 1.3.6.1.4.1.311.90.2.10
    private static final byte[] OID_MSI_V2_CU_ID =
            {0x2B, 0x06, 0x01, 0x04, 0x01, (byte) 0x82, 0x37, 0x5A, 0x02, 0x0A};

    // RSA-PSS AlgorithmIdentifier DER bytes (pre-computed for SHA-256, MGF1-SHA256, saltLen=32)
    // SEQUENCE { OID rsaPSS, SEQUENCE { [0] SHA256-AlgId, [1] MGF1-SHA256-AlgId, [2] saltLen=32 } }
    private static final byte[] ALG_ID_RSASSA_PSS = buildRsaPssAlgorithmIdentifier();

    /**
     * Generates a PKCS#10 CSR and returns it in PEM format.
     *
     * @param publicKeyDer  DER-encoded RSA SubjectPublicKeyInfo bytes (from the KeyGuard key)
     * @param cuId          the compute unit ID (vmId or vmssId) from IMDS platform metadata
     * @param keyHandle     the native KeyGuard key handle used for signing via JNI
     * @return PEM-encoded PKCS#10 CSR string
     * @throws MsiV2Exception if CSR construction or signing fails
     */
    static String generate(byte[] publicKeyDer, String cuId, byte[] keyHandle) {
        try {
            // Build the CertificationRequestInfo (TBS) structure
            byte[] certRequestInfo = buildCertificationRequestInfo(publicKeyDer, cuId);

            // Sign the TBS bytes using the KeyGuard key via JNI
            byte[] signature = WindowsKeyGuardJNI.signWithKeyGuardNative(keyHandle, certRequestInfo);

            // Assemble the final PKCS#10 structure
            byte[] pkcs10Der = buildPkcs10(certRequestInfo, signature);

            // Encode to PEM
            return toPem(pkcs10Der);
        } catch (IOException e) {
            throw new MsiV2Exception("[MSI v2] Failed to generate CSR: " + e.getMessage(),
                    MsalError.MSI_V2_ERROR, e);
        }
    }

    /**
     * Builds the DER-encoded CertificationRequestInfo structure (the TBS part of PKCS#10).
     */
    private static byte[] buildCertificationRequestInfo(byte[] publicKeyDer, String cuId)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // version INTEGER 0
        out.write(encodeInteger(0));

        // subject: SET { SEQUENCE { OID commonName, UTF8String "managed-identity-csr" } }
        out.write(encodeSubject("managed-identity-csr"));

        // subjectPublicKeyInfo: already DER-encoded RSA public key
        out.write(publicKeyDer);

        // attributes [0]: Microsoft OID with cuId as UTF8String JSON
        out.write(encodeAttributes(cuId));

        // Wrap in SEQUENCE to get CertificationRequestInfo
        return encodeSequence(out.toByteArray());
    }

    /**
     * Builds the full PKCS#10 DER structure from CertificationRequestInfo and signature.
     */
    private static byte[] buildPkcs10(byte[] certRequestInfo, byte[] signature)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // CertificationRequestInfo
        out.write(certRequestInfo);

        // signatureAlgorithm: id-RSASSA-PSS with SHA-256/MGF1/saltLen=32
        out.write(ALG_ID_RSASSA_PSS);

        // signature: BIT STRING with 0 unused bits
        out.write(encodeBitString(signature));

        return encodeSequence(out.toByteArray());
    }

    /**
     * Encodes the subject as a Distinguished Name: RDN { AttributeTypeAndValue { CN, value } }.
     */
    private static byte[] encodeSubject(String commonName) throws IOException {
        // UTF8String for the CN value
        byte[] cnValue = encodeUtf8String(commonName);

        // AttributeTypeAndValue: SEQUENCE { OID commonName, UTF8String value }
        ByteArrayOutputStream atv = new ByteArrayOutputStream();
        atv.write(encodeOid(OID_COMMON_NAME));
        atv.write(cnValue);
        byte[] atvSeq = encodeSequence(atv.toByteArray());

        // RelativeDistinguishedName: SET { AttributeTypeAndValue }
        byte[] rdn = encodeSet(atvSeq);

        // Name: SEQUENCE { RDN }
        return encodeSequence(rdn);
    }

    /**
     * Encodes the PKCS#10 attributes containing the MSI v2 cuId OID attribute.
     * Structure: [0] IMPLICIT SET { SEQUENCE { OID, SET { UTF8String cuIdJson } } }
     */
    private static byte[] encodeAttributes(String cuId) throws IOException {
        // The cuId JSON representation (per the MSI v2 spec).
        // Escape any special JSON characters in the cuId value to produce valid JSON.
        String cuIdJson = "\"" + cuId.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";

        // Attribute value: SET { UTF8String cuIdJson }
        byte[] attrValue = encodeSet(encodeUtf8String(cuIdJson));

        // Attribute: SEQUENCE { OID msiV2CuId, attrValue }
        ByteArrayOutputStream attr = new ByteArrayOutputStream();
        attr.write(encodeOid(OID_MSI_V2_CU_ID));
        attr.write(attrValue);
        byte[] attrSeq = encodeSequence(attr.toByteArray());

        // attributes [0] IMPLICIT (context tag 0, constructed)
        return encodeContextTag(0, attrSeq);
    }

    // -------------------------------------------------------------------------
    // DER encoding helpers
    // -------------------------------------------------------------------------

    private static byte[] encodeSequence(byte[] content) throws IOException {
        return encodeTlv(0x30, content);
    }

    private static byte[] encodeSet(byte[] content) throws IOException {
        return encodeTlv(0x31, content);
    }

    private static byte[] encodeOid(byte[] oidBytes) throws IOException {
        return encodeTlv(0x06, oidBytes);
    }

    private static byte[] encodeInteger(int value) throws IOException {
        byte[] valueBytes;
        if (value == 0) {
            valueBytes = new byte[]{0x00};
        } else {
            // Minimal positive integer encoding
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            while (value > 0) {
                b.write(value & 0xFF);
                value >>= 8;
            }
            byte[] raw = b.toByteArray();
            // Reverse (we wrote LSB first)
            for (int i = 0, j = raw.length - 1; i < j; i++, j--) {
                byte tmp = raw[i];
                raw[i] = raw[j];
                raw[j] = tmp;
            }
            // Prepend 0x00 if high bit is set (to keep it positive)
            if ((raw[0] & 0x80) != 0) {
                byte[] padded = new byte[raw.length + 1];
                System.arraycopy(raw, 0, padded, 1, raw.length);
                valueBytes = padded;
            } else {
                valueBytes = raw;
            }
        }
        return encodeTlv(0x02, valueBytes);
    }

    private static byte[] encodeUtf8String(String value) throws IOException {
        return encodeTlv(0x0C, value.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] encodeBitString(byte[] content) throws IOException {
        // BIT STRING: prepend 0x00 (unused bits count)
        byte[] withUnused = new byte[content.length + 1];
        withUnused[0] = 0x00; // no unused bits
        System.arraycopy(content, 0, withUnused, 1, content.length);
        return encodeTlv(0x03, withUnused);
    }

    private static byte[] encodeContextTag(int tagNumber, byte[] content) throws IOException {
        // Constructed context tag: 0xA0 | tagNumber
        return encodeTlv(0xA0 | tagNumber, content);
    }

    private static byte[] encodeTlv(int tag, byte[] value) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        writeLength(out, value.length);
        out.write(value);
        return out.toByteArray();
    }

    private static void writeLength(ByteArrayOutputStream out, int length) {
        if (length < 128) {
            out.write(length);
        } else if (length < 256) {
            out.write(0x81);
            out.write(length);
        } else {
            out.write(0x82);
            out.write((length >> 8) & 0xFF);
            out.write(length & 0xFF);
        }
    }

    /**
     * Builds the pre-encoded DER bytes for the RSA-PSS AlgorithmIdentifier with SHA-256,
     * MGF1-SHA256, and saltLength=32.
     */
    private static byte[] buildRsaPssAlgorithmIdentifier() {
        try {
            // sha-256 AlgorithmIdentifier: SEQUENCE { OID sha256 } (no NULL per RFC 4055)
            byte[] sha256AlgId = encodeSequence(encodeOid(OID_SHA256));

            // [0] hashAlgorithm
            byte[] hashAlgField = encodeContextTag(0, sha256AlgId);

            // mgf1 AlgorithmIdentifier: SEQUENCE { OID mgf1, sha256AlgId }
            ByteArrayOutputStream mgf1Inner = new ByteArrayOutputStream();
            mgf1Inner.write(encodeOid(OID_MGF1));
            mgf1Inner.write(sha256AlgId);
            byte[] mgf1AlgId = encodeSequence(mgf1Inner.toByteArray());

            // [1] maskGenAlgorithm
            byte[] maskGenField = encodeContextTag(1, mgf1AlgId);

            // [2] saltLength: INTEGER 32
            byte[] saltLenField = encodeContextTag(2, encodeInteger(32));

            // RSASSA-PSS-params SEQUENCE
            ByteArrayOutputStream pssParams = new ByteArrayOutputStream();
            pssParams.write(hashAlgField);
            pssParams.write(maskGenField);
            pssParams.write(saltLenField);
            byte[] pssParamsSeq = encodeSequence(pssParams.toByteArray());

            // AlgorithmIdentifier: SEQUENCE { OID rsaPSS, pssParamsSeq }
            ByteArrayOutputStream algId = new ByteArrayOutputStream();
            algId.write(encodeOid(OID_RSASSA_PSS));
            algId.write(pssParamsSeq);
            return encodeSequence(algId.toByteArray());
        } catch (IOException e) {
            // Should never happen with ByteArrayOutputStream
            throw new RuntimeException("Failed to build RSA-PSS AlgorithmIdentifier", e);
        }
    }

    /**
     * Converts DER-encoded PKCS#10 bytes to PEM format.
     */
    private static String toPem(byte[] der) {
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
        return "-----BEGIN CERTIFICATE REQUEST-----\n"
                + base64
                + "\n-----END CERTIFICATE REQUEST-----\n";
    }

    private CsrGenerator() {
        // Utility class, not instantiable
    }
}
