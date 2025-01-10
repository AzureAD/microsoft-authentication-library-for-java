// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.util.List;

/**
 * Credential type containing X509 public certificate and RSA private key.
 * <p>
 * For more details, see https://aka.ms/msal4j-client-credentials
 */
public interface IClientCertificate extends IClientCredential {

    /**
     * Returns private key of the credential.
     *
     * @return private key.
     */
    PrivateKey privateKey();

    /**
     * Base64 encoded SHA-256 hash of the public certificate.
     *
     * @return base64 encoded string
     * @throws CertificateEncodingException if an encoding error occurs
     * @throws NoSuchAlgorithmException     if requested algorithm is not available in the environment
     */
    default String publicCertificateHash256() throws CertificateEncodingException, NoSuchAlgorithmException {
        //Default implementation that returns null, to add backwards compatibility for those implementing this public interface.
        //If left as null, the library will default to the older publicCertificateHash() method and SHA-1 hashing.
        return null;
    }

    /**
     * Base64 encoded SHA-1 hash of the public certificate.
     *
     * @return base64 encoded string
     * @throws CertificateEncodingException if an encoding error occurs
     * @throws NoSuchAlgorithmException     if requested algorithm is not available in the environment
     */
    String publicCertificateHash() throws CertificateEncodingException, NoSuchAlgorithmException;

    /**
     * Base64 encoded public certificate.
     *
     * @return base64 encoded string
     * @throws CertificateEncodingException if an encoding error occurs
     */
    List<String> getEncodedPublicKeyCertificateChain() throws CertificateEncodingException;
}
