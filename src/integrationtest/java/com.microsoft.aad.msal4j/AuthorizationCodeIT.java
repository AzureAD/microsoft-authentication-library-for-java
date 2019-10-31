// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import infrastructure.SeleniumExtensions;
import infrastructure.TcpListener;
import labapi.B2CIdentityProvider;
import labapi.FederationProvider;
import labapi.LabResponse;
import labapi.LabUserProvider;
import labapi.NationalCloud;
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
        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        labUserProvider.getUserPassword(labResponse.getUser());

        assertAcquireTokenAAD(labResponse);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv2019_Federated(){
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSv2019,
                true,
                true);
        labUserProvider.getUserPassword(labResponse.getUser());

        assertAcquireTokenAAD(labResponse);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv2019_NotFederated(){
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSv2019,
                false,
                true);
        labUserProvider.getUserPassword(labResponse.getUser());

        assertAcquireTokenAAD(labResponse);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv4_Federated(){
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV4,
                true,
                false);
        labUserProvider.getUserPassword(labResponse.getUser());

        assertAcquireTokenAAD(labResponse);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv4_NotFederated(){
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV4,
                false,
                false);
        labUserProvider.getUserPassword(labResponse.getUser());

        assertAcquireTokenAAD(labResponse);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv3_Federated(){
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV3,
                true,
                false);
        labUserProvider.getUserPassword(labResponse.getUser());
        assertAcquireTokenAAD(labResponse);

    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv3_NotFederated(){
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV3,
                false,
                false);
        labUserProvider.getUserPassword(labResponse.getUser());

        assertAcquireTokenAAD(labResponse);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv2_Federated(){
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV2,
                true,
                false);
        labUserProvider.getUserPassword(labResponse.getUser());

        assertAcquireTokenAAD(labResponse);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv2_NotFederated(){
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV2,
                false,
                false);
        labUserProvider.getUserPassword(labResponse.getUser());

        assertAcquireTokenAAD(labResponse);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_B2C_Local(){
        LabResponse labResponse = labUserProvider.getB2cUser(
                B2CIdentityProvider.LOCAL,
                false);
        labUserProvider.getUserPassword(labResponse.getUser());

        String b2CAppId = "b876a048-55a5-4fc5-9403-f5d90cb1c852";
        labResponse.setAppId(b2CAppId);

        assertAcquireTokenB2C(labResponse);
    }

    // failing on azure devOps
    //@Test
    public void acquireTokenWithAuthorizationCode_B2C_Google(){
        LabResponse labResponse = labUserProvider.getB2cUser(
                B2CIdentityProvider.GOOGLE,
                false);
        labUserProvider.getUserPassword(labResponse.getUser());

        String b2CAppId = "b876a048-55a5-4fc5-9403-f5d90cb1c852";
        labResponse.setAppId(b2CAppId);

        assertAcquireTokenB2C(labResponse);
    }

    // TODO uncomment when lab fixes facebook test account
/*    @Test
    public void acquireTokenWithAuthorizationCode_B2C_Facebook(){
        LabResponse labResponse = labUserProvider.getB2cUser(
                B2CIdentityProvider.FACEBOOK,
                false);
        labUserProvider.getUserPassword(labResponse.getUser());

        String b2CAppId = "b876a048-55a5-4fc5-9403-f5d90cb1c852";
        labResponse.setAppId(b2CAppId);

        assertAcquireTokenB2C(labResponse);
    }*/

    private void assertAcquireTokenAAD(LabResponse labResponse){
        String authCode = acquireAuthorizationCodeAutomated(labResponse, AuthorityType.AAD);
        IAuthenticationResult result = acquireTokenInteractiveAAD(labResponse, authCode);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }

    private void assertAcquireTokenB2C(LabResponse labResponse){
        String authCode = acquireAuthorizationCodeAutomated(labResponse, AuthorityType.B2C);
        IAuthenticationResult result = acquireTokenInteractiveB2C(labResponse, authCode);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }

    private IAuthenticationResult acquireTokenInteractiveAAD(
            LabResponse labResponse,
            String authCode){

        IAuthenticationResult result;
        try {
            PublicClientApplication pca = PublicClientApplication.builder(
                    labResponse.getAppId()).
                    authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                    build();

            result = pca.acquireToken(AuthorizationCodeParameters
                    .builder(authCode,
                            new URI(TestConstants.LOCALHOST + tcpListener.getPort()))
                    .scopes(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE))
                    .build())
                    .get();

        } catch(Exception e){
            LOG.error("Error acquiring token with authCode: " + e.getMessage());
            throw new RuntimeException("Error acquiring token with authCode: " + e.getMessage());
        }
        return result;
    }

    private IAuthenticationResult acquireTokenInteractiveB2C(LabResponse labResponse,
                                                            String authCode) {
        IAuthenticationResult result;
        try{
            IClientCredential credential = ClientCredentialFactory.createFromSecret("");
            ConfidentialClientApplication cca = ConfidentialClientApplication.builder(
                    labResponse.getAppId(),
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
            LabResponse labUserData,
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
            runSeleniumAutomatedLogin(labUserData, authorityType);
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

    private void runSeleniumAutomatedLogin(LabResponse labUserData, AuthorityType authorityType)
            throws UnsupportedEncodingException{
        String url = buildAuthenticationCodeURL(labUserData.getAppId(), authorityType);
        seleniumDriver.navigate().to(url);
        if(authorityType == AuthorityType.B2C){
            switch(labUserData.getUser().getB2CIdentityProvider()){
                case LOCAL:
                    SeleniumExtensions.performLocalLogin(seleniumDriver, labUserData.getUser());
                    break;
                case GOOGLE:
                    SeleniumExtensions.performGoogleLogin(seleniumDriver, labUserData.getUser());
                    break;
                case FACEBOOK:
                    SeleniumExtensions.performFacebookLogin(seleniumDriver, labUserData.getUser());
                    break;
            }
        } else {
            SeleniumExtensions.performADLogin(seleniumDriver, labUserData.getUser());
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
        } else {
            authority = TestConstants.B2C_AUTHORITY_URL;
            scope = TestConstants.B2C_LAB_SCOPE;
        }

        redirectUrl = authority + "oauth2/v2.0/authorize?" +
                "response_type=code" +
                "&response_mode=query" +
                "&client_id=" + appId +
                "&redirect_uri=" + URLEncoder.encode(TestConstants.LOCALHOST + portNumber, "UTF-8") +
                "&scope=" + URLEncoder.encode("openid offline_access profile " + scope, "UTF-8");

        if(authorityType == AuthorityType.B2C){
            redirectUrl = redirectUrl + "&p=" + TestConstants.B2C_SIGN_IN_POLICY;
        }

        return redirectUrl;
    }
}
