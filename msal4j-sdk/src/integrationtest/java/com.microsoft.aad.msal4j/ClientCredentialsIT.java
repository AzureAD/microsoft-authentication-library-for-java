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
import java.util.concurrent.Callable;

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
        String clientId = "2afb0add-2f32-4946-ac90-81a02aa4550e";
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
        String clientId = "2afb0add-2f32-4946-ac90-81a02aa4550e";

        ClientAssertion clientAssertion = getClientAssertion(clientId);

        IClientCredential credential = ClientCredentialFactory.createFromClientAssertion(clientAssertion.assertion());

        assertAcquireTokenCommon(clientId, credential);
    }

    @Test
    public void acquireTokenClientCredentials_Callback() throws Exception {
        String clientId = "2afb0add-2f32-4946-ac90-81a02aa4550e";

        // Creates a valid client assertion using a callback, and uses it to build the client app and make a request
        Callable<String> callable = () -> {
            ClientAssertion clientAssertion = getClientAssertion(clientId);

            return clientAssertion.assertion();
        };

        IClientCredential credential = ClientCredentialFactory.createFromCallback(callable);

        assertAcquireTokenCommon(clientId, credential);

        // Creates an invalid client assertion to build the application, but overrides it with a valid client assertion
        //  in the request parameters in order to make a successful token request
        ClientAssertion invalidClientAssertion = getClientAssertion("abc");

        IClientCredential invalidCredentials = ClientCredentialFactory.createFromClientAssertion(invalidClientAssertion.assertion());

        assertAcquireTokenCommon_withParameters(clientId, invalidCredentials, credential);
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

    @Test
    public void acquireTokenClientCredentials_Regional() throws Exception {
        String clientId = "2afb0add-2f32-4946-ac90-81a02aa4550e";

        assertAcquireTokenCommon_withRegion(clientId, certificate);
    }

    private ClientAssertion getClientAssertion(String clientId) {
        return JwtHelper.buildJwt(
                clientId,
                (ClientCertificate) certificate,
                "https://login.microsoftonline.com/common/oauth2/v2.0/token",
                true);
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

    private void assertAcquireTokenCommon_withParameters(String clientId, IClientCredential credential, IClientCredential credentialParam) throws Exception {

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(
                clientId, credential).
                authority(TestConstants.MICROSOFT_AUTHORITY).
                build();

        IAuthenticationResult result = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE)).clientCredential(credentialParam)
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
    }

    private void assertAcquireTokenCommon_withRegion(String clientId, IClientCredential credential) throws Exception {
        ConfidentialClientApplication ccaNoRegion = ConfidentialClientApplication.builder(
                clientId, credential).
                authority(TestConstants.MICROSOFT_AUTHORITY).
                build();

        ConfidentialClientApplication ccaRegion = ConfidentialClientApplication.builder(
                clientId, credential).
                authority("https://login.microsoft.com/microsoft.onmicrosoft.com").azureRegion("westus").
                build();

        //Ensure behavior when region not specified
        IAuthenticationResult resultNoRegion = ccaNoRegion.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        Assert.assertNotNull(resultNoRegion);
        Assert.assertNotNull(resultNoRegion.accessToken());
        Assert.assertEquals(resultNoRegion.environment(), TestConstants.MICROSOFT_AUTHORITY_BASIC_HOST);

        //Ensure regional tokens are properly cached and retrievable
        IAuthenticationResult resultRegion = ccaRegion.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        Assert.assertNotNull(resultRegion);
        Assert.assertNotNull(resultRegion.accessToken());
        Assert.assertEquals(resultRegion.environment(), TestConstants.REGIONAL_MICROSOFT_AUTHORITY_BASIC_HOST_WESTUS);

        IAuthenticationResult resultRegionCached = ccaRegion.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        Assert.assertNotNull(resultRegionCached);
        Assert.assertNotNull(resultRegionCached.accessToken());
        Assert.assertEquals(resultRegionCached.accessToken(), resultRegion.accessToken());

        //Tokens retrieved from regional endpoints should be interchangeable with non-regional, and vice-versa
        //For example, if an application doesn't configure a region but gets regional tokens added to its cache, they should be retrievable
        ccaNoRegion.tokenCache = ccaRegion.tokenCache;
        resultNoRegion = ccaNoRegion.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        Assert.assertNotNull(resultNoRegion);
        Assert.assertNotNull(resultNoRegion.accessToken());
        Assert.assertEquals(resultNoRegion.accessToken(), resultRegion.accessToken());
    }
}
