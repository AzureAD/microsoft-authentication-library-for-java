// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.KeyVaultSecretsProvider;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CertificateHelper {
    static KeyStore createKeyStore() throws KeyStoreException, NoSuchProviderException {
        String os = SystemUtils.OS_NAME;
        if (os.contains("Mac")) {
            return KeyStore.getInstance("KeychainStore");
        } else {
            return KeyStore.getInstance("Windows-MY", "SunMSCAPI");
        }
    }

    static IClientCertificate getClientCertificate() throws
            KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException, NoSuchProviderException {

        KeyStore keystore = createKeyStore();

        keystore.load(null, null);

        PrivateKey key = (PrivateKey) keystore.getKey(KeyVaultSecretsProvider.CERTIFICATE_ALIAS, null);
        X509Certificate publicCertificate = (X509Certificate) keystore.getCertificate(
                KeyVaultSecretsProvider.CERTIFICATE_ALIAS);

        return ClientCredentialFactory.createFromCertificate(key, publicCertificate);
    }
}
