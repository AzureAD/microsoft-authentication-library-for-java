// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import infrastructure.SeleniumExtensions;
import labapi.*;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AuthorizationCodeIT {
    private final static Logger LOG = LoggerFactory.getLogger(AuthorizationCodeIT.class);

    private LabUserProvider labUserProvider;
    private WebDriver seleniumDriver;
    private HttpListener httpListener;

    @BeforeClass
    public void setUpLapUserProvider() {
        labUserProvider = LabUserProvider.getInstance();
    }

    @AfterMethod
    public void cleanUp(){
        seleniumDriver.quit();
        httpListener.stopListener();
    }

    @BeforeMethod
    public void startUpBrowser(){
        seleniumDriver = SeleniumExtensions.createDefaultWebDriver();
    }

    @Test
    public void acquireTokenWithAuthorizationCode_ManagedUser(){
        User user = labUserProvider.getDefaultUser();
        assertAcquireTokenAAD(user);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv2019_Federated(){
        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.FEDERATION_PROVIDER,  FederationProvider.ADFS_2019);
        query.parameters.put(UserQueryParameters.USER_TYPE,  UserType.FEDERATED);

        User user = labUserProvider.getLabUser(query);
        assertAcquireTokenAAD(user);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv2019_OnPrem(){
        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.FEDERATION_PROVIDER,  FederationProvider.ADFS_2019);
        query.parameters.put(UserQueryParameters.USER_TYPE,  UserType.ON_PREM);

        User user = labUserProvider.getLabUser(query);
        assertAcquireTokenADFS2019(user);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv4_Federated(){
        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.FEDERATION_PROVIDER,  FederationProvider.ADFS_4);
        query.parameters.put(UserQueryParameters.USER_TYPE,  UserType.FEDERATED);

        User user = labUserProvider.getLabUser(query);
        assertAcquireTokenAAD(user);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv3_Federated(){
        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.FEDERATION_PROVIDER,  FederationProvider.ADFS_3);
        query.parameters.put(UserQueryParameters.USER_TYPE,  UserType.FEDERATED);

        User user = labUserProvider.getLabUser(query);
        assertAcquireTokenAAD(user);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv2_Federated(){
        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.FEDERATION_PROVIDER,  FederationProvider.ADFS_2);
        query.parameters.put(UserQueryParameters.USER_TYPE,  UserType.FEDERATED);

        User user = labUserProvider.getLabUser(query);
        assertAcquireTokenAAD(user);
    }

    //@Test
    // TODO Redirect URI localhost in not registered
    public void acquireTokenWithAuthorizationCode_B2C_Local(){
/*        User labResponse = labUserProvider.getLabUser(
                B2CIdentityProvider.LOCAL,
                false);
        labUserProvider.getUserPassword(labResponse.getUser());

        String b2CAppId = "b876a048-55a5-4fc5-9403-f5d90cb1c852";
        labResponse.setAppId(b2CAppId);*/

        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.USER_TYPE, UserType.B2C);
        query.parameters.put(UserQueryParameters.B2C_PROVIDER, B2CProvider.LOCAL);
        User user = labUserProvider.getLabUser(query);

        /*String b2CAppId = "b876a048-55a5-4fc5-9403-f5d90cb1c852";
        labResponse.setAppId(b2CAppId);
*/
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

        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.USER_TYPE, UserType.B2C);
        query.parameters.put(UserQueryParameters.B2C_PROVIDER, B2CProvider.GOOGLE);

        User user = labUserProvider.getLabUser(query);

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
        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.USER_TYPE, UserType.B2C);
        query.parameters.put(UserQueryParameters.B2C_PROVIDER, B2CProvider.FACEBOOK);

        User user = labUserProvider.getLabUser(query);

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

        String authCode = acquireAuthorizationCodeAutomated(user, pca);
        IAuthenticationResult result = acquireTokenAuthorizationCodeFlow(
                pca,
                authCode,
                Collections.singleton(TestConstants.ADFS_SCOPE));

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(user.getUpn(), result.account().username());
    }

    private void assertAcquireTokenAAD(User user){

        PublicClientApplication pca;
        try {
            pca = PublicClientApplication.builder(
                    user.getAppId()).
                    authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                    build();
        } catch(MalformedURLException ex){
            throw new RuntimeException(ex.getMessage());
        }

        String authCode = acquireAuthorizationCodeAutomated(user, pca);
        IAuthenticationResult result = acquireTokenAuthorizationCodeFlow(
                pca,
                authCode,
                Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE));

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(user.getUpn(), result.account().username());
    }

    private void assertAcquireTokenB2C(User user){

        ConfidentialClientApplication cca;
        try {
            IClientCredential credential = ClientCredentialFactory.createFromSecret("");
            cca = ConfidentialClientApplication.builder(
                    user.getAppId(),
                    credential)
                    .b2cAuthority(TestConstants.B2C_AUTHORITY_SIGN_IN)
                    .build();
        } catch(Exception ex){
            throw new RuntimeException(ex.getMessage());
        }

        String authCode = acquireAuthorizationCodeAutomated(user, cca);
        IAuthenticationResult result = acquireTokenInteractiveB2C(cca, authCode);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(user.getUpn(), result.account().username());
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
            result = cca.acquireToken(AuthorizationCodeParameters.builder(
                    authCode,
                    new URI(TestConstants.LOCALHOST + httpListener.port()))
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
            ClientApplicationBase app){

        BlockingQueue<AuthorizationResult> authorizationCodeQueue = new LinkedBlockingQueue<>();

        AuthorizationResponseHandler authorizationResponseHandler = new AuthorizationResponseHandler(
                authorizationCodeQueue,
                new SystemBrowserOptions());

        int[] ports = {3843, 4584, 4843, 60000};
        httpListener = new HttpListener();
        httpListener.startListener(ports[0], authorizationResponseHandler);

        AuthorizationResult result = null;
        try {
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
                    AuthenticationErrorCode.AUTHORIZATION_RESULT_BLANK);
        }
        return result.code();
    }

    private void runSeleniumAutomatedLogin(User user, ClientApplicationBase app) {
        String url = buildAuthenticationCodeURL(app);
        seleniumDriver.navigate().to(url);

        AuthorityType authorityType = app.authenticationAuthority.authorityType;
        if(authorityType == AuthorityType.B2C){
            switch(user.getB2cProvider().toLowerCase()){
                case B2CProvider.LOCAL:
                    SeleniumExtensions.performLocalLogin(seleniumDriver, user);
                    break;
                case B2CProvider.GOOGLE:
                    SeleniumExtensions.performGoogleLogin(seleniumDriver, user);
                    break;
                case B2CProvider.FACEBOOK:
                    SeleniumExtensions.performFacebookLogin(seleniumDriver, user);
                    break;
            }
        } else if (authorityType == AuthorityType.AAD) {
            SeleniumExtensions.performADLogin(seleniumDriver, user);
        }
        else if (authorityType == AuthorityType.ADFS) {
            SeleniumExtensions.performADFS2019Login(seleniumDriver, user);
        }
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
}
