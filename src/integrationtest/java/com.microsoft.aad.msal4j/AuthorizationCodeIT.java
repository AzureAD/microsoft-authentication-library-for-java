// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import infrastructure.SeleniumExtensions;
import infrastructure.TcpListener;
import labapi.*;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.util.Strings;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthorizationCodeIT {
    private final static Logger LOG = LoggerFactory.getLogger(AuthorizationCodeIT.class);

    private LabUserProvider labUserProvider;
    private WebDriver seleniumDriver;
    private TcpListener tcpListener;
    private BlockingQueue<String> AuthorizationCodeQueue;

    @BeforeClass
    public void setUpLapUserProvider() {
        labUserProvider = LabUserProvider.getInstance();
    }

    @AfterMethod
    public void cleanUp(){
        seleniumDriver.quit();
        if(AuthorizationCodeQueue != null){
            AuthorizationCodeQueue.clear();
        }
        tcpListener.close();
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
        String authCode = acquireAuthorizationCodeAutomated(user, AuthorityType.ADFS);
        IAuthenticationResult result = acquireTokenInteractive(authCode,
                TestConstants.ADFS_AUTHORITY, Collections.singleton(TestConstants.ADFS_SCOPE),
                TestConstants.ADFS_APP_ID);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(user.getUpn(), result.account().username());
    }

    private void assertAcquireTokenAAD(User user){
        String authCode = acquireAuthorizationCodeAutomated(user, AuthorityType.AAD);
        IAuthenticationResult result = acquireTokenInteractiveAAD(user, authCode);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(user.getUpn(), result.account().username());
    }

    private void assertAcquireTokenB2C(User user){
        String authCode = acquireAuthorizationCodeAutomated(user, AuthorityType.B2C);
        IAuthenticationResult result = acquireTokenInteractiveB2C(user, authCode);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(user.getUpn(), result.account().username());
    }

    private IAuthenticationResult acquireTokenInteractive(
            String authCode,
            String authority,
            Set<String> scopes,
            String clientId){

        IAuthenticationResult result;
        try {
            PublicClientApplication pca = PublicClientApplication.builder(
                    clientId).
                    authority(authority).
                    build();

            result = pca.acquireToken(AuthorizationCodeParameters
                    .builder(authCode,
                            new URI(TestConstants.LOCALHOST + tcpListener.getPort()))
                    .scopes(scopes)
                    .build())
                    .get();

        } catch(Exception e){
            LOG.error("Error acquiring token with authCode: " + e.getMessage());
            throw new RuntimeException("Error acquiring token with authCode: " + e.getMessage());
        }
        return result;
    }

    private IAuthenticationResult acquireTokenInteractiveAAD(
            User user,
            String authCode){
        return acquireTokenInteractive(authCode,
                TestConstants.ORGANIZATIONS_AUTHORITY,
                Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                user.getAppId());
    }

    private IAuthenticationResult acquireTokenInteractiveB2C(User user,
                                                            String authCode) {
        IAuthenticationResult result;
        try{
            IClientCredential credential = ClientCredentialFactory.createFromSecret("");
            ConfidentialClientApplication cca = ConfidentialClientApplication.builder(
                    user.getAppId(),
                    credential)
                    .b2cAuthority(TestConstants.B2C_AUTHORITY_SIGN_IN)
                    .build();

            result = cca.acquireToken(AuthorizationCodeParameters.builder(
                    authCode,
                    new URI(TestConstants.LOCALHOST + tcpListener.getPort()))
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
            AuthorityType authorityType){
        BlockingQueue<Boolean> tcpStartUpNotificationQueue = new LinkedBlockingQueue<>();
        startTcpListener(tcpStartUpNotificationQueue);

        String authServerResponse;
        try {
            Boolean tcpListenerStarted = tcpStartUpNotificationQueue.poll(
                    30,
                    TimeUnit.SECONDS);
            if (tcpListenerStarted == null || !tcpListenerStarted){
                throw new RuntimeException("Could not start TCP listener");
            }
            runSeleniumAutomatedLogin(user, authorityType);
            String page  = seleniumDriver.getPageSource();
            authServerResponse = getResponseFromTcpListener();
        } catch(Exception e){
            if(!Strings.isNullOrEmpty(
                    System.getenv(TestConstants.LOCAL_FLAG_ENV_VAR))){
                SeleniumExtensions.takeScreenShot(seleniumDriver);
            }
            LOG.error("Error running automated selenium login: " + e.getMessage());
            throw new RuntimeException("Error running automated selenium login: " + e.getMessage());
        }
        return parseServerResponse(authServerResponse,authorityType);
    }

    private void runSeleniumAutomatedLogin(User user, AuthorityType authorityType)
            throws UnsupportedEncodingException{
        String url = buildAuthenticationCodeURL(user.getAppId(), authorityType);
        seleniumDriver.navigate().to(url);

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

    private void startTcpListener(BlockingQueue<Boolean> tcpStartUpNotifierQueue){
        AuthorizationCodeQueue = new LinkedBlockingQueue<>();
        tcpListener = new TcpListener(AuthorizationCodeQueue, tcpStartUpNotifierQueue);
        tcpListener.startServer();
    }

    private String getResponseFromTcpListener(){
        String response;
        try {
            response = AuthorizationCodeQueue.poll(30, TimeUnit.SECONDS);
            if (Strings.isNullOrEmpty(response)){
                LOG.error("Server response is null");
                throw new NullPointerException("Server response is null");
            }
        } catch(Exception e){
            LOG.error("Error reading from server response AuthorizationCodeQueue: " + e.getMessage());
            throw new RuntimeException("Error reading from server response AuthorizationCodeQueue: " +
                    e.getMessage());
        }
        return response;
    }

    private String parseServerResponse(String serverResponse, AuthorityType authorityType){
        // Response will be a GET request with query parameter ?code=authCode
        String regexp;
        if(authorityType == AuthorityType.B2C){
            regexp = "(?<=code=)(?:(?! HTTP).)*";
        } else {
            regexp = "(?<=code=)(?:(?!&).)*";
        }

        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(serverResponse);

        if(!matcher.find()){
            LOG.error("No authorization code in server response: " + serverResponse);
            throw new IllegalStateException("No authorization code in server response: " +
                    serverResponse);
        }
        return matcher.group(0);
    }

    private String buildAuthenticationCodeURL(String appId, AuthorityType authorityType)
            throws UnsupportedEncodingException{
        String redirectUrl;
        int portNumber = tcpListener.getPort();

        String authority;
        String scope;
        if(authorityType == AuthorityType.AAD){
            authority = TestConstants.ORGANIZATIONS_AUTHORITY;
            scope = TestConstants.GRAPH_DEFAULT_SCOPE;
        } else if (authorityType == AuthorityType.B2C) {
            authority = TestConstants.B2C_AUTHORITY_URL;
            scope = TestConstants.B2C_LAB_SCOPE;
        }
        else if (authorityType == AuthorityType.ADFS){
            authority = TestConstants.ADFS_AUTHORITY;
            scope = TestConstants.ADFS_SCOPE;
        }
        else{
            return null;
        }

        redirectUrl = authority + "oauth2/" +
                (authorityType != AuthorityType.ADFS ? "v2.0/" : "") +
                "authorize?" +
                "response_type=code" +
                "&response_mode=query" +
                "&client_id=" + (authorityType == AuthorityType.ADFS ? TestConstants.ADFS_APP_ID : appId) +
                "&redirect_uri=" + URLEncoder.encode(TestConstants.LOCALHOST + portNumber, "UTF-8") +
                "&scope=" + URLEncoder.encode("openid offline_access profile " + scope, "UTF-8");

        if(authorityType == AuthorityType.B2C){
            redirectUrl = redirectUrl + "&p=" + TestConstants.B2C_SIGN_IN_POLICY;
        }

        return redirectUrl;
    }
}
