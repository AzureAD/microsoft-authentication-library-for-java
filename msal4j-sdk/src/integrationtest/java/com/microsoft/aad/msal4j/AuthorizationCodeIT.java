// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.*;
import static com.microsoft.aad.msal4j.labapi.KeyVaultSecrets.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthorizationCodeIT extends SeleniumTest {
    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationCodeIT.class);

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
        AppConfig app = LabResponseHelper.getAppConfig(APP_MULTI_TENANT);
        UserConfig user = LabResponseHelper.getUserConfig(USER_PUBLIC_CLOUD);

        assertAcquireTokenAAD(user, app.getAppId(), null);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_CiamCud() throws Exception {
        String authorityCud = "https://login.msidlabsciam.com/fe362aec-5d43-45d1-b730-9755e60dc3b9/v2.0/";

        AppConfig app = LabResponseHelper.getAppConfig(APP_CIAM);
        UserConfig user = LabResponseHelper.getUserConfig(USER_CIAM);

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

    @Test
    @DisabledIfSystemProperty(named = "adfs.disabled", matches = "true")
    void acquireTokenWithAuthorizationCode_ADFSv2022() {
        AppConfig app = LabResponseHelper.getAppConfig(APP_PCACLIENT);
        UserConfig user = LabResponseHelper.getUserConfig(USER_FED_DEFAULT);

        assertAcquireTokenADFS(user, app.getAppId(), app.getAuthority() + "organizations/");
    }

    private void assertAcquireTokenADFS(UserConfig user, String appId, String authority) {
        PublicClientApplication pca;
        try {
            pca = PublicClientApplication.builder(
                            appId).
                    authority(authority).
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

    private void assertAcquireTokenAAD(UserConfig user, String appId, Map<String, Set<String>> parameters) {

        PublicClientApplication pca = IntegrationTestHelper.createPublicApp(appId, TestConstants.COMMON_AUTHORITY);

        String authCode = acquireAuthorizationCodeAutomated(user, pca, parameters);
        IAuthenticationResult result = acquireTokenAuthorizationCodeFlow(
                pca,
                authCode,
                Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE));

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
        assertEquals(user.getUpn(), result.account().username());
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

    private String acquireAuthorizationCodeAutomated(
            UserConfig user,
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
