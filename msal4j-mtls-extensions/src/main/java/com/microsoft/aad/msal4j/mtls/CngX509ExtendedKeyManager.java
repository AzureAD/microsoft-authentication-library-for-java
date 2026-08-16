// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

final class CngX509ExtendedKeyManager extends X509ExtendedKeyManager {

    private static final String ALIAS = "msal-keyguard-mtls";
    private final CngRsaPrivateKey privateKey;
    private final X509Certificate[] certificateChain;

    CngX509ExtendedKeyManager(
            CngRsaPrivateKey privateKey,
            X509Certificate certificate) {
        this.privateKey = privateKey;
        this.certificateChain = new X509Certificate[]{certificate};
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return supportsKeyType(keyType) ? new String[]{ALIAS} : null;
    }

    @Override
    public String chooseClientAlias(
            String[] keyTypes,
            Principal[] issuers,
            Socket socket) {
        return supportsAnyKeyType(keyTypes) ? ALIAS : null;
    }

    @Override
    public String chooseEngineClientAlias(
            String[] keyTypes,
            Principal[] issuers,
            SSLEngine engine) {
        return supportsAnyKeyType(keyTypes) ? ALIAS : null;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return ALIAS.equals(alias) ? certificateChain.clone() : null;
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return ALIAS.equals(alias) ? privateKey : null;
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return null;
    }

    @Override
    public String chooseServerAlias(
            String keyType,
            Principal[] issuers,
            Socket socket) {
        return null;
    }

    @Override
    public String chooseEngineServerAlias(
            String keyType,
            Principal[] issuers,
            SSLEngine engine) {
        return null;
    }

    private static boolean supportsAnyKeyType(String[] keyTypes) {
        if (keyTypes == null) {
            return false;
        }
        for (String keyType : keyTypes) {
            if (supportsKeyType(keyType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean supportsKeyType(String keyType) {
        return keyType != null
                && ("RSA".equalsIgnoreCase(keyType)
                || "RSASSA-PSS".equalsIgnoreCase(keyType));
    }
}
