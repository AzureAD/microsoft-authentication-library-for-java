package com.microsoft.aad.msal4j;

import lapapi.AppIdentityProvider;
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
import java.util.Collection;
import java.util.Collections;

import static com.microsoft.aad.msal4j.TestConstants.KEYVAULT_DEFAULT_SCOPE;

@Test
public class ClientCredentialsIT {

    private final static String CLIENT_ID = "55e7e5af-ca53-482d-9aa3-5cb1cc8eecb5";
    private final static String CERTIFICATE_ALIAS = "JavaAutomationRunner";
    private final static String KEYSTORE_TYPE = "Windows-MY";
    private final static String KEYSTORE_PROVIDER = "SunMSCAPI";

    // Don't know if this test is necessary - This API is being tested in setup for every other test.
    @Test
    public void acquireTokenClientCredentials_AsymmetricKeyCredential() throws Exception{
        IClientCredential credential = getCertificateFromKeyStore();
        ConfidentialClientApplication cca = new ConfidentialClientApplication.Builder(
                CLIENT_ID, credential).
                authority(TestConstants.AUTHORITY_MICROSOFT).
                build();

        AuthenticationResult result = cca.acquireTokenForClient(
                Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNull(result.getRefreshToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }

    @Test
    public void acquireTokenClientCredentials_ClientSecret() throws Exception{
        AppIdentityProvider appProvider = new AppIdentityProvider();
        final String appId = appProvider.getDefaultLabId();
        final String password = appProvider.getDefaultLabPassword();
        IClientCredential credential = ClientCredentialFactory.create(password);

        ConfidentialClientApplication cca = new ConfidentialClientApplication.Builder(
                appId, credential).
                authority(TestConstants.AUTHORITY_MICROSOFT).
                build();

        AuthenticationResult result = cca.acquireTokenForClient(
                Collections.singleton(KEYVAULT_DEFAULT_SCOPE)).
                get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNull(result.getRefreshToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }

    private IClientCredential getCertificateFromKeyStore() throws
            NoSuchProviderException, KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException {
        KeyStore keystore = KeyStore.getInstance(KEYSTORE_TYPE, KEYSTORE_PROVIDER);
        keystore.load(null, null);

        PrivateKey key = (PrivateKey)keystore.getKey(CERTIFICATE_ALIAS, null);
        X509Certificate publicCertificate = (X509Certificate)keystore.getCertificate(
                CERTIFICATE_ALIAS);
        return ClientCredentialFactory.create(key, publicCertificate);
    }
}
