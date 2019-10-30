// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.AppIdentityProvider;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;

import static com.microsoft.aad.msal4j.TestConstants.GRAPH_DEFAULT_SCOPE;
import static com.microsoft.aad.msal4j.TestConstants.KEYVAULT_DEFAULT_SCOPE;

@Test
public class ClientCredentialsIT {
    @Test
    public void acquireTokenClientCredentials_AsymmetricKeyCredential() throws Exception{
        String clientId = "55e7e5af-ca53-482d-9aa3-5cb1cc8eecb5";
        IClientCredential credential = getCertificateFromKeyStore();
        assertAcquireTokenCommon(clientId, credential);
    }

    @Test
    public void acquireTokenClientCredentials_ClientSecret() throws Exception{
        AppIdentityProvider appProvider = new AppIdentityProvider();
        final String clientId = appProvider.getDefaultLabId();
        final String password = appProvider.getDefaultLabPassword();
        IClientCredential credential = ClientCredentialFactory.create(password);

        assertAcquireTokenCommon(clientId, credential);
    }

    private void assertAcquireTokenCommon(String clientId, IClientCredential credential) throws Exception{
        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(
                clientId, credential).
                authority(TestConstants.MICROSOFT_AUTHORITY).
                build();

        IAuthenticationResult result = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());

        String cachedAt = result.accessToken();

        result = cca.acquireTokenSilently(SilentParameters
                .builder(Collections.singleton(GRAPH_DEFAULT_SCOPE))
                .build())
                .get();

        Assert.assertNull(result);

        result = cca.acquireTokenSilently(SilentParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertEquals(result.accessToken(), cachedAt);
    }


    private IClientCredential getCertificateFromKeyStore() throws
            NoSuchProviderException, KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException {
        KeyStore keystore = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
        keystore.load(null, null);

        String certificateAlias = "JavaAutomationRunner";
        PrivateKey key = (PrivateKey)keystore.getKey(certificateAlias, null);
        X509Certificate publicCertificate = (X509Certificate)keystore.getCertificate(
                certificateAlias);

        return ClientCredentialFactory.create(key, publicCertificate);
    }
}
