// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi2.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthorizationCodeIT extends SeleniumTest {
    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationCodeIT.class);

    private Config cfg;


    @AfterEach
    public void stopBrowser() {
        cleanUp();
    }

    @BeforeEach
    public void startBrowser() {
        startUpBrowser();
    }

    @Test
    public void acquireTokenWithAuthorizationCode_ManagedUser() {
        cfg = new Config();

        LabResponse labResponse = LabUserHelper.getDefaultUser();
        LabUser user = labResponse.getUser();
        assertAcquireTokenAAD(user, labResponse.getApp().getAppId(), null);
    }

    // TODO: labapi2 doesn't have ADFS v4 specific user helper yet - will be pulled from MSAL.NET
//    @ParameterizedTest
//    @MethodSource("com.microsoft.aad.msal4j.EnvironmentsProvider#createData")
//    @DisabledIfSystemProperty(named = "adfs.disabled", matches = "true")
//    public void acquireTokenWithAuthorizationCode_ADFSv4_Federated(String environment) {
//        cfg = new Config(environment);
//
//        LabResponse labResponse = LabUserHelper.getFederatedAdfsUser(environment, LabServiceParameters.FederationProvider.ADFS_V4);
//        LabUser user = labResponse.getUser();
//
//        assertAcquireTokenAAD(user, null);
//    }

    @Test
    public void acquireTokenWithAuthorizationCode_B2C_Local() {
        cfg = new Config();

        LabResponse labResponse = LabUserHelper.getB2CLocalAccount();
        LabUser user = labResponse.getUser();
        assertAcquireTokenB2C(user);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_CiamCud() throws Exception {
        String authorityCud = "https://login.msidlabsciam.com/fe362aec-5d43-45d1-b730-9755e60dc3b9/v2.0/";

        LabResponse labResponse = LabUserHelper.getCiamCudUser();
        LabUser user = labResponse.getUser();
        LabApp app = labResponse.getApp();

        PublicClientApplication pca = PublicClientApplication.builder(
                        app.getAppId()).
                oidcAuthority(authorityCud).
                build();

        String authCode = acquireAuthorizationCodeAutomated(user, pca, null);

        IAuthenticationResult result = pca.acquireToken(AuthorizationCodeParameters
                        .builder(authCode,
                                new URI(TestConstants.LOCALHOST + httpListener.port()))
                        .scopes(Collections.singleton("user.read"))
                        .build())
                .get();

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
        assertEquals(user.getUpn(), result.account().username());

        IAuthenticationResult resultSilent = pca.acquireTokenSilently(SilentParameters
                .builder(Collections.singleton("user.read"), result.account())
                        .build())
                .get();

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
        assertEquals(resultSilent.accessToken(), result.accessToken());
        assertEquals(resultSilent.account().username(), result.account().username());
    }

    private void assertAcquireTokenADFS2019(LabUser user) {
        PublicClientApplication pca;
        try {
            pca = PublicClientApplication.builder(
                    TestConstants.ADFS_APP_ID).
                    authority(TestConstants.ADFS_AUTHORITY).
                    build();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex.getMessage());
        }

        String authCode = acquireAuthorizationCodeAutomated(user, pca, null);
        IAuthenticationResult result = acquireTokenAuthorizationCodeFlow(
                pca,
                authCode,
                Collections.singleton(TestConstants.ADFS_SCOPE));

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
        assertEquals(user.getUpn(), result.account().username());
    }

    private void assertAcquireTokenAAD(LabUser user, String appId, Map<String, Set<String>> parameters) {

        PublicClientApplication pca = IntegrationTestHelper.createPublicApp(appId, cfg.commonAuthority());

        String authCode = acquireAuthorizationCodeAutomated(user, pca, parameters);
        IAuthenticationResult result = acquireTokenAuthorizationCodeFlow(
                pca,
                authCode,
                Collections.singleton(cfg.graphDefaultScope()));

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
        assertEquals(user.getUpn(), result.account().username());
    }

    private void assertAcquireTokenB2C(LabUser user) {

        KeyVaultSecretsProvider keyVaultSecretsProvider = new KeyVaultSecretsProvider();
        String appId = keyVaultSecretsProvider.getSecretByName(TestConstants.B2C_CONFIDENTIAL_CLIENT_LAB_APP_ID).getValue();
        String appSecret = keyVaultSecretsProvider.getSecretByName(TestConstants.B2C_CONFIDENTIAL_CLIENT_APP_SECRETID).getValue();

        ConfidentialClientApplication cca;
        try {
            IClientCredential credential = ClientCredentialFactory.createFromSecret(appSecret);
            cca = ConfidentialClientApplication
                    .builder(appId, credential)
                    .b2cAuthority(TestConstants.B2C_AUTHORITY_SIGN_IN)
                    .build();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }

        String authCode = acquireAuthorizationCodeAutomated(user, cca, null);
        IAuthenticationResult result = acquireTokenInteractiveB2C(cca, authCode);

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
    }

    private IAuthenticationResult acquireTokenAuthorizationCodeFlow(
            PublicClientApplication pca,
            String authCode,
            Set<String> scopes) {

        IAuthenticationResult result;
        try {
            result = pca.acquireToken(AuthorizationCodeParameters
                    .builder(authCode,
                            new URI(TestConstants.LOCALHOST + httpListener.port()))
                    .scopes(scopes)
                    .build())
                    .get();

        } catch (Exception e) {
            LOG.error("Error acquiring token with authCode: {}", e.getMessage());
            throw new RuntimeException("Error acquiring token with authCode: " + e.getMessage());
        }
        return result;
    }

    private IAuthenticationResult acquireTokenInteractiveB2C(ConfidentialClientApplication cca,
                                                             String authCode) {
        IAuthenticationResult result;
        try {
            result = cca.acquireToken(AuthorizationCodeParameters
                    .builder(authCode, new URI(TestConstants.LOCALHOST + httpListener.port()))
                    .scopes(Collections.singleton(TestConstants.B2C_LAB_SCOPE))
                    .extraQueryParameters(new HashMap<>())
                    .build())
                    .get();
        } catch (Exception e) {
            LOG.error("Error acquiring token with authCode: {}", e.getMessage());
            throw new RuntimeException("Error acquiring token with authCode: " + e.getMessage());
        }
        return result;
    }

    private String acquireAuthorizationCodeAutomated(
            LabUser user,
            AbstractClientApplicationBase app,
            Map<String, Set<String>> parameters) {

        BlockingQueue<AuthorizationResult> authorizationCodeQueue = new LinkedBlockingQueue<>();

        AuthorizationResponseHandler authorizationResponseHandler = new AuthorizationResponseHandler(
                authorizationCodeQueue,
                SystemBrowserOptions.builder().build());

        httpListener = new HttpListener();
        httpListener.startListener(8080, authorizationResponseHandler);

        AuthorizationResult result = null;
        try {
            String url = buildAuthenticationCodeURL(app, parameters);
            seleniumDriver.navigate().to(url);
            runSeleniumAutomatedLogin(user, app);

            long expirationTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + 120;

            while (result == null &&
                    TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) < expirationTime) {

                result = authorizationCodeQueue.poll(100, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            throw new MsalClientException(e);
        } finally {
            if (httpListener != null) {
                httpListener.stopListener();
            }
        }

        if (result == null || StringHelper.isBlank(result.code())) {
            throw new MsalClientException("No Authorization code was returned from the server",
                    AuthenticationErrorCode.INVALID_AUTHORIZATION_RESULT);
        }
        return result.code();
    }

    private String buildAuthenticationCodeURL(AbstractClientApplicationBase app, Map<String, Set<String>> parameters) {
        String scope;

        String claims = null;
        if (parameters != null) {
            claims = String.valueOf(parameters.getOrDefault("claims", Collections.singleton("")).toArray()[0]);
        }

        AuthorityType authorityType = app.authenticationAuthority.authorityType;
        if (authorityType == AuthorityType.AAD) {
            scope = TestConstants.GRAPH_DEFAULT_SCOPE;
        } else if (authorityType == AuthorityType.B2C) {
            scope = TestConstants.B2C_LAB_SCOPE;
        } else if (authorityType == AuthorityType.ADFS) {
            scope = TestConstants.ADFS_SCOPE;
        } else if (authorityType == AuthorityType.OIDC) {
            scope = TestConstants.USER_READ_SCOPE;
        } else {
            throw new RuntimeException("Authority type not recognized");
        }

        AuthorizationRequestUrlParameters authParameters =
                AuthorizationRequestUrlParameters
                        .builder(TestConstants.LOCALHOST + httpListener.port(),
                                Collections.singleton(scope))
                        .claimsChallenge(claims)
                        .build();

        return app.getAuthorizationRequestUrl(authParameters).toString();
    }
}
