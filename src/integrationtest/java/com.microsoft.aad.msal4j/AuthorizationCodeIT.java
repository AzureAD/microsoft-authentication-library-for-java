// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AuthorizationCodeIT extends SeleniumTest {
    private final static Logger LOG = LoggerFactory.getLogger(AuthorizationCodeIT.class);

    private Config cfg;

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenWithAuthorizationCode_ManagedUser(String environment){
        cfg = new Config(environment);

        User user = labUserProvider.getDefaultUser(cfg.azureEnvironment);
        assertAcquireTokenAAD(user, null);
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenWithAuthorizationCode_ManagedUserWithClaimsAndCapabilities(String environment){
        cfg = new Config(environment);

        User user = labUserProvider.getDefaultUser(cfg.azureEnvironment);

        Map<String, Set<String>> claimsAndCapabilities = new HashMap<>();

        claimsAndCapabilities.put("claims", Collections.singleton(TestConstants.CLAIMS));
        claimsAndCapabilities.put("clientCapabilities", TestConstants.CLIENT_CAPABILITIES_EMPTY);

        assertAcquireTokenAAD(user, claimsAndCapabilities);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv2019_OnPrem(){
        User user = labUserProvider.getOnPremAdfsUser(FederationProvider.ADFS_2019);
        assertAcquireTokenADFS2019(user);
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenWithAuthorizationCode_ADFSv2019_Federated(String environment){
        cfg = new Config(environment);

        User user = labUserProvider.getFederatedAdfsUser(cfg.azureEnvironment, FederationProvider.ADFS_2019);
        assertAcquireTokenAAD(user, null);
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenWithAuthorizationCode_ADFSv4_Federated(String environment){
        cfg = new Config(environment);

        User user = labUserProvider.getFederatedAdfsUser(cfg.azureEnvironment, FederationProvider.ADFS_4);

        assertAcquireTokenAAD(user, null);
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenWithAuthorizationCode_ADFSv3_Federated(String environment){
        cfg = new Config(environment);

        User user = labUserProvider.getFederatedAdfsUser(cfg.azureEnvironment, FederationProvider.ADFS_3);
        assertAcquireTokenAAD(user, null);
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenWithAuthorizationCode_ADFSv2_Federated(String environment){
        cfg = new Config(environment);

        User user = labUserProvider.getFederatedAdfsUser(cfg.azureEnvironment, FederationProvider.ADFS_2);
        assertAcquireTokenAAD(user, null);
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenWithAuthorizationCode_B2C_Local(String environment){
        cfg = new Config(environment);

        User user = labUserProvider.getB2cUser(environment, B2CProvider.LOCAL);
        assertAcquireTokenB2C(user);
    }

    // failing on azure devOps
    //@Test
    // TODO Redirect URI localhost in not registered
    public void acquireTokenWithAuthorizationCode_B2C_Google(){
/*        LabResponse labResponse = labUserProvider.getB2cUser(
                B2CIdentityProvider.GOOGLE,
                false);
        labUserProvider.getUserPassword(labResponse.getUser());

        String b2CAppId = "b876a048-55a5-4fc5-9403-f5d90cb1c852";
        labResponse.setAppId(b2CAppId);*/
        User user = labUserProvider.getB2cUser(B2CProvider.GOOGLE);
        assertAcquireTokenB2C(user);
    }

    // TODO uncomment when lab fixes facebook test account
    //@Test
    // TODO Redirect URI localhost in not registered
    public void acquireTokenWithAuthorizationCode_B2C_Facebook(){
/*        LabResponse labResponse = labUserProvider.getB2cUser(
                B2CIdentityProvider.FACEBOOK,
                false);


        String b2CAppId = "b876a048-55a5-4fc5-9403-f5d90cb1c852";
        labResponse.setAppId(b2CAppId);*/
        User user = labUserProvider.getB2cUser(B2CProvider.FACEBOOK);

        assertAcquireTokenB2C(user);
    }

    private void assertAcquireTokenADFS2019(User user){
        PublicClientApplication pca;
        try {
             pca = PublicClientApplication.builder(
                    TestConstants.ADFS_APP_ID).
                    authority(TestConstants.ADFS_AUTHORITY).
                    build();
        } catch(MalformedURLException ex){
            throw new RuntimeException(ex.getMessage());
        }

        String authCode = acquireAuthorizationCodeAutomated(user, pca, null);
        IAuthenticationResult result = acquireTokenAuthorizationCodeFlow(
                pca,
                authCode,
                Collections.singleton(TestConstants.ADFS_SCOPE));

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(user.getUpn(), result.account().username());
    }

    private void assertAcquireTokenAAD(User user, Map<String, Set<String>> parameters){

        PublicClientApplication pca;
        try {
            if (parameters != null) {
                pca = PublicClientApplication.builder(
                        user.getAppId()).
                        authority(cfg.organizationsAuthority()).
                        clientCapabilities(parameters.getOrDefault("clientCapabilities", null)).
                        build();
            } else {
                pca = PublicClientApplication.builder(
                        user.getAppId()).
                        authority(cfg.organizationsAuthority()).
                        build();
            }
        } catch(MalformedURLException ex){
            throw new RuntimeException(ex.getMessage());
        }

        String authCode = acquireAuthorizationCodeAutomated(user, pca, parameters);
        IAuthenticationResult result = acquireTokenAuthorizationCodeFlow(
                pca,
                authCode,
                Collections.singleton(cfg.graphDefaultScope()));

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(user.getUpn(), result.account().username());
    }

    private void assertAcquireTokenB2C(User user){

        String appId = LabService.getSecret(TestConstants.B2C_CONFIDENTIAL_CLIENT_LAB_APP_ID);
        String appSecret = LabService.getSecret(TestConstants.B2C_CONFIDENTIAL_CLIENT_APP_SECRET);

        ConfidentialClientApplication cca;
        try {
            IClientCredential credential = ClientCredentialFactory.createFromSecret(appSecret);
            cca = ConfidentialClientApplication
                    .builder(appId, credential)
                    .b2cAuthority(TestConstants.B2C_AUTHORITY_SIGN_IN)
                    .build();
        } catch(Exception ex){
            throw new RuntimeException(ex.getMessage());
        }

        String authCode = acquireAuthorizationCodeAutomated(user, cca, null);
        IAuthenticationResult result = acquireTokenInteractiveB2C(cca, authCode);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
    }

    private IAuthenticationResult acquireTokenAuthorizationCodeFlow(
            PublicClientApplication pca,
            String authCode,
            Set<String> scopes){

        IAuthenticationResult result;
        try {
            result = pca.acquireToken(AuthorizationCodeParameters
                    .builder(authCode,
                            new URI(TestConstants.LOCALHOST + httpListener.port()))
                    .scopes(scopes)
                    .build())
                    .get();

        } catch(Exception e){
            LOG.error("Error acquiring token with authCode: " + e.getMessage());
            throw new RuntimeException("Error acquiring token with authCode: " + e.getMessage());
        }
        return result;
    }

    private IAuthenticationResult acquireTokenInteractiveB2C(ConfidentialClientApplication cca,
                                                            String authCode) {
        IAuthenticationResult result;
        try{
            result = cca.acquireToken(AuthorizationCodeParameters
                    .builder(authCode, new URI(TestConstants.LOCALHOST + httpListener.port()))
                    .scopes(Collections.singleton(TestConstants.B2C_LAB_SCOPE))
                    .build())
                    .get();
        } catch (Exception e){
            LOG.error("Error acquiring token with authCode: " + e.getMessage());
            throw new RuntimeException("Error acquiring token with authCode: " + e.getMessage());
        }
        return result;
    }

    private String acquireAuthorizationCodeAutomated(
            User user,
            ClientApplicationBase app,
            Map<String, Set<String>> parameters){

        BlockingQueue<AuthorizationResult> authorizationCodeQueue = new LinkedBlockingQueue<>();

        AuthorizationResponseHandler authorizationResponseHandler = new AuthorizationResponseHandler(
                authorizationCodeQueue,
                SystemBrowserOptions.builder().build());

        httpListener = new HttpListener();
        httpListener.startListener(8080, authorizationResponseHandler);

        AuthorizationResult result = null;
        try {
            String url;
            if (parameters == null) {
                url = buildAuthenticationCodeURL(app);
            } else {
                url = buildAuthenticationCodeURLWithParameters(app, parameters);
            }
            seleniumDriver.navigate().to(url);
            runSeleniumAutomatedLogin(user, app);

            long expirationTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + 120;

            while(result == null &&
                    TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) < expirationTime) {

                result = authorizationCodeQueue.poll(100, TimeUnit.MILLISECONDS);
            }
        } catch(Exception e){
            throw new MsalClientException(e);
        } finally {
            if(httpListener != null){
                httpListener.stopListener();
            }
        }

        if (result == null || StringHelper.isBlank(result.code())) {
            throw new MsalClientException("No Authorization code was returned from the server",
                    AuthenticationErrorCode.INVALID_AUTHORIZATION_RESULT);
        }
        return result.code();
    }

    private String buildAuthenticationCodeURL(ClientApplicationBase app) {
        String scope;

        AuthorityType authorityType= app.authenticationAuthority.authorityType;
        if(authorityType == AuthorityType.AAD){
            scope = TestConstants.GRAPH_DEFAULT_SCOPE;
        } else if (authorityType == AuthorityType.B2C) {
            scope = TestConstants.B2C_LAB_SCOPE;
        }
        else if (authorityType == AuthorityType.ADFS){
            scope = TestConstants.ADFS_SCOPE;
        }
        else{
            throw new RuntimeException("Authority type not recognized");
        }

        AuthorizationRequestUrlParameters parameters =
                AuthorizationRequestUrlParameters
                        .builder(TestConstants.LOCALHOST + httpListener.port(),
                                Collections.singleton(scope))
                        .build();

        return app.getAuthorizationRequestUrl(parameters).toString();
    }

    private String buildAuthenticationCodeURLWithParameters(ClientApplicationBase app, Map<String, Set<String>> parameters) {
        String scope;

        AuthorityType authorityType= app.authenticationAuthority.authorityType;
        if(authorityType == AuthorityType.AAD){
            scope = TestConstants.GRAPH_DEFAULT_SCOPE;
        } else if (authorityType == AuthorityType.B2C) {
            scope = TestConstants.B2C_LAB_SCOPE;
        }
        else if (authorityType == AuthorityType.ADFS){
            scope = TestConstants.ADFS_SCOPE;
        }
        else{
            throw new RuntimeException("Authority type not recognized");
        }

        AuthorizationRequestUrlParameters requestUrlParameters =
                AuthorizationRequestUrlParameters
                        .builder(TestConstants.LOCALHOST + httpListener.port(),
                                Collections.singleton(scope))
                        .claims(String.valueOf(parameters.getOrDefault("claims", Collections.singleton("")).toArray()[0]))
                        .build();

        return app.getAuthorizationRequestUrl(requestUrlParameters).toString();
    }
}
