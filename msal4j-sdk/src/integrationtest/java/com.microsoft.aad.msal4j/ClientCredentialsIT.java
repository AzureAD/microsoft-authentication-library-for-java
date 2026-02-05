// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.concurrent.Callable;

import static com.microsoft.aad.msal4j.TestConstants.KEYVAULT_DEFAULT_SCOPE;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientCredentialsIT {
    private IClientCertificate certificate;

    @BeforeAll
    void init() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException {
        certificate = CertificateHelper.getClientCertificate();
    }

    @Test
    void acquireTokenClientCredentials_ClientCertificate() throws Exception {
        AppConfig app = LabResponseHelper.getAppConfig(KeyVaultSecrets.APP_S2S);

        assertAcquireTokenCommon(app.getAppId(), certificate, app.getAuthority());
    }

    @Test
    void acquireTokenClientCredentials_ClientSecret() throws Exception {
        AppConfig app = LabResponseHelper.getAppConfig(KeyVaultSecrets.APP_S2S);
        String password = KeyVaultRegistry.getMsalTeamProvider().getSecretByName(app.getSecretName()).getValue();

        IClientCredential credential = ClientCredentialFactory.createFromSecret(password);

        assertAcquireTokenCommon(app.getAppId(), credential, app.getAuthority());
    }

    @Test
    void acquireTokenClientCredentials_ClientAssertion() throws Exception {
        AppConfig app = LabResponseHelper.getAppConfig(KeyVaultSecrets.APP_S2S);

        ClientAssertion clientAssertion = getClientAssertion(app.getAppId());

        IClientCredential credential = ClientCredentialFactory.createFromClientAssertion(clientAssertion.assertion());

        assertAcquireTokenCommon(app.getAppId(), credential, app.getAuthority());
    }

    @Test
    void acquireTokenClientCredentials_Certificate_CiamCud() throws Exception {
        String authorityCud = "https://login.msidlabsciam.com/fe362aec-5d43-45d1-b730-9755e60dc3b9/v2.0/";

        AppConfig app = LabResponseHelper.getAppConfig(KeyVaultSecrets.APP_CIAM);

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(
                        app.getAppId(), CertificateHelper.getClientCertificate())
                .oidcAuthority(authorityCud)
                .build();

        IAuthenticationResult result = cca.acquireToken(ClientCredentialParameters
                        .builder(Collections.singleton(TestConstants.DEFAULT_SCOPE))
                        .build())
                .get();

        assertNotNull(result);
        assertNotNull(result.accessToken());
    }

    @Test
    void acquireTokenClientCredentials_Callback() throws Exception {
        AppConfig app = LabResponseHelper.getAppConfig(KeyVaultSecrets.APP_S2S);

        // Creates a valid client assertion using a callback, and uses it to build the client app and make a request
        Callable<String> callable = () -> {
            ClientAssertion clientAssertion = getClientAssertion(app.getAppId());

            return clientAssertion.assertion();
        };

        IClientCredential credential = ClientCredentialFactory.createFromCallback(callable);

        assertAcquireTokenCommon(app.getAppId(), credential, app.getAuthority());

        // Creates an invalid client assertion to build the application, but overrides it with a valid client assertion
        //  in the request parameters in order to make a successful token request
        ClientAssertion invalidClientAssertion = getClientAssertion("abc");

        IClientCredential invalidCredentials = ClientCredentialFactory.createFromClientAssertion(invalidClientAssertion.assertion());

        assertAcquireTokenCommon_withParameters(app, invalidCredentials, credential);
    }

    @Test
    void acquireTokenClientCredentials_DefaultCacheLookup() throws Exception {
        final String clientId = KeyVaultRegistry.getMsidLabProvider().getSecretByName("LabVaultAppID").getValue();

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(
                clientId, CertificateHelper.getClientCertificate()).
                authority(TestConstants.MICROSOFT_AUTHORITY).
                build();

        IAuthenticationResult result1 = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        assertNotNull(result1);
        assertNotNull(result1.accessToken());

        IAuthenticationResult result2 = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        assertEquals(result1.accessToken(), result2.accessToken());

        IAuthenticationResult result3 = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .skipCache(true)
                .build())
                .get();

        assertNotNull(result3);
        assertNotNull(result3.accessToken());
        assertNotEquals(result2.accessToken(), result3.accessToken());
    }

    @Test
    void acquireTokenClientCredentials_Regional() throws Exception {
        AppConfig app = LabResponseHelper.getAppConfig(KeyVaultSecrets.APP_S2S);

        assertAcquireTokenCommon_withRegion(app, certificate, "westus", TestConstants.REGIONAL_MICROSOFT_AUTHORITY_BASIC_HOST_WESTUS);
    }

    private ClientAssertion getClientAssertion(String clientId) {
        return JwtHelper.buildJwt(
                clientId,
                (ClientCertificate) certificate,
                "https://login.microsoftonline.com/common/oauth2/v2.0/token",
                true, false);
    }

    private void assertAcquireTokenCommon(String clientId, IClientCredential credential, String authority) throws Exception {
        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(
                clientId, credential).
                authority(authority).
                build();

        IAuthenticationResult result = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        assertNotNull(result);
        assertNotNull(result.accessToken());
    }

    private void assertAcquireTokenCommon_withParameters(AppConfig app, IClientCredential credential, IClientCredential credentialParam) throws Exception {

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(
                app.getAppId(), credential).
                authority(app.getAuthority()).
                build();

        IAuthenticationResult result = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE)).clientCredential(credentialParam)
                .build())
                .get();

        assertNotNull(result);
        assertNotNull(result.accessToken());
    }

    private void assertAcquireTokenCommon_withRegion(AppConfig app, IClientCredential credential, String region, String regionalAuthority) throws Exception {
        ConfidentialClientApplication ccaNoRegion = ConfidentialClientApplication.builder(
                app.getAppId(), credential).
                authority(app.getAuthority()).
                build();

        ConfidentialClientApplication ccaRegion = ConfidentialClientApplication.builder(
                app.getAppId(), credential).
                authority(app.getAuthority()).
                azureRegion(region).
                build();

        //Ensure behavior when region not specified
        IAuthenticationResult resultNoRegion = ccaNoRegion.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        assertNotNull(resultNoRegion);
        assertNotNull(resultNoRegion.accessToken());
        assertEquals(TestConstants.MICROSOFT_AUTHORITY_BASIC_HOST, resultNoRegion.environment());

        //Ensure regional tokens are properly cached and retrievable
        IAuthenticationResult resultRegion = ccaRegion.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        assertNotNull(resultRegion);
        assertNotNull(resultRegion.accessToken());
        assertEquals(resultRegion.environment(), regionalAuthority);

        IAuthenticationResult resultRegionCached = ccaRegion.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        assertNotNull(resultRegionCached);
        assertNotNull(resultRegionCached.accessToken());
        assertEquals(resultRegionCached.accessToken(), resultRegion.accessToken());

        //Tokens retrieved from regional endpoints should be interchangeable with non-regional, and vice-versa
        //For example, if an application doesn't configure a region but gets regional tokens added to its cache, they should be retrievable
        ccaNoRegion.tokenCache = ccaRegion.tokenCache;
        resultNoRegion = ccaNoRegion.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        assertNotNull(resultNoRegion);
        assertNotNull(resultNoRegion.accessToken());
        assertEquals(resultNoRegion.accessToken(), resultRegion.accessToken());
    }
}
