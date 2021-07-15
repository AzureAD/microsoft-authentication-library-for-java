// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.AppCredentialProvider;
import labapi.AzureEnvironment;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;

import static com.microsoft.aad.msal4j.TestConstants.KEYVAULT_DEFAULT_SCOPE;

@Test
public class ClientCredentialsIT {
    private IClientCertificate certificate;

    @BeforeClass
    void init() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException {
        certificate = CertificateHelper.getClientCertificate();
    }

    @Test
    public void acquireTokenClientCredentials_ClientCertificate() throws Exception {
        String clientId = "55e7e5af-ca53-482d-9aa3-5cb1cc8eecb5";
        assertAcquireTokenCommon(clientId, certificate);
    }

    @Test
    public void acquireTokenClientCredentials_ClientSecret() throws Exception {
        AppCredentialProvider appProvider = new AppCredentialProvider(AzureEnvironment.AZURE);
        final String clientId = appProvider.getLabVaultAppId();
        final String password = appProvider.getLabVaultPassword();
        IClientCredential credential = ClientCredentialFactory.createFromSecret(password);

        assertAcquireTokenCommon(clientId, credential);
    }

    @Test
    public void acquireTokenClientCredentials_ClientAssertion() throws Exception {
        String clientId = "55e7e5af-ca53-482d-9aa3-5cb1cc8eecb5";

        ClientAssertion clientAssertion = JwtHelper.buildJwt(
                clientId,
                (ClientCertificate) certificate,
                "https://login.microsoftonline.com/common/oauth2/v2.0/token",
                true);

        IClientCredential credential = ClientCredentialFactory.createFromClientAssertion(
                clientAssertion.assertion());

        assertAcquireTokenCommon(clientId, credential);
    }

    @Test
    public void acquireTokenClientCredentials_DefaultCacheLookup() throws Exception {
        AppCredentialProvider appProvider = new AppCredentialProvider(AzureEnvironment.AZURE);
        final String clientId = appProvider.getLabVaultAppId();
        final String password = appProvider.getLabVaultPassword();
        IClientCredential credential = ClientCredentialFactory.createFromSecret(password);

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(
                clientId, credential).
                authority(TestConstants.MICROSOFT_AUTHORITY).
                build();

        IAuthenticationResult result1 = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        Assert.assertNotNull(result1);
        Assert.assertNotNull(result1.accessToken());

        IAuthenticationResult result2 = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        Assert.assertEquals(result1.accessToken(), result2.accessToken());

        IAuthenticationResult result3 = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .skipCache(true)
                .build())
                .get();

        Assert.assertNotNull(result3);
        Assert.assertNotNull(result3.accessToken());
        Assert.assertNotEquals(result2.accessToken(), result3.accessToken());
    }


    private void assertAcquireTokenCommon(String clientId, IClientCredential credential) throws Exception {
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
    }
}
