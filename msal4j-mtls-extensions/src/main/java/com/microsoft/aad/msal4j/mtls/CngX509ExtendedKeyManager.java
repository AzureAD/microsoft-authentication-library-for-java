// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

final class CngX509ExtendedKeyManager extends X509ExtendedKeyManager {

    private static final AtomicLong ALIASES = new AtomicLong();
    private final Map<String, KeyMaterial> generations = new ConcurrentHashMap<>();
    private volatile KeyMaterial current;

    CngX509ExtendedKeyManager() {
    }

    CngX509ExtendedKeyManager(
            CngRsaPrivateKey privateKey,
            X509Certificate certificate) {
        current = new KeyMaterial(privateKey, certificate);
        generations.put(current.alias, current);
    }

    void activate(CngX509ExtendedKeyManager generation) {
        KeyMaterial material = generation.current;
        generations.put(material.alias, material);
        current = material;
    }

    void retire(CngX509ExtendedKeyManager generation) {
        KeyMaterial material = generation.current;
        generations.remove(material.alias, material);
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        KeyMaterial material = current;
        return material != null && supportsKeyType(keyType)
                ? new String[]{material.alias} : null;
    }

    @Override
    public String chooseClientAlias(
            String[] keyTypes,
            Principal[] issuers,
            Socket socket) {
        KeyMaterial material = current;
        return material != null && supportsAnyKeyType(keyTypes) ? material.alias : null;
    }

    @Override
    public String chooseEngineClientAlias(
            String[] keyTypes,
            Principal[] issuers,
            SSLEngine engine) {
        return chooseClientAlias(keyTypes, issuers, null);
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        KeyMaterial material = alias == null ? null : generations.get(alias);
        return material == null ? null : material.certificateChain.clone();
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        KeyMaterial material = alias == null ? null : generations.get(alias);
        return material == null ? null : material.privateKey;
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

    private static final class KeyMaterial {
        final String alias = "msal-keyguard-mtls-" + ALIASES.incrementAndGet();
        final CngRsaPrivateKey privateKey;
        final X509Certificate[] certificateChain;

        KeyMaterial(CngRsaPrivateKey privateKey, X509Certificate certificate) {
            this.privateKey = privateKey;
            this.certificateChain = new X509Certificate[]{certificate};
        }
    }
}
