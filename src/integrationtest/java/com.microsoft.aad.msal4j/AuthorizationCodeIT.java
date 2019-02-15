package com.microsoft.aad.msal4j;

import Infrastructure.SeleniumExtensions;
import Infrastructure.TcpListener;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import jdk.nashorn.internal.ir.Block;
import lapapi.FederationProvider;
import lapapi.LabResponse;
import lapapi.LabUser;
import lapapi.LabUserProvider;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.util.Strings;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthorizationCodeIT {

    private final static Logger LOG = LoggerFactory.getLogger(AuthorizationCodeIT.class);

    private LabUserProvider labUserProvider;
    private static final String authority = "https://login.microsoftonline.com/organizations/";
    private static final String scopes = "https://graph.windows.net/.default";
    private WebDriver seleniumDriver;
    TcpListener tcpListener;
    BlockingQueue<String> queue;


    @BeforeClass
    public void setUp() {
        labUserProvider = new LabUserProvider();
        seleniumDriver = SeleniumExtensions.createDefaultWebDriver();
    }


    @Test
    public void acquireTokenWithAuthorizationCode_ManagedUser(){
        LabResponse labResponse = labUserProvider.getDefaultUser();
        labUserProvider.getUserPassword(labResponse.getUser());

        String authCode = acquireAuthorizationCodeAutomated(labResponse);
        AuthenticationResult result = acquireTokenInteractiveCommmon(labResponse, authCode);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNotNull(result.getRefreshToken());
        Assert.assertNotNull(result.getIdToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv4_Federated(){
        LabResponse labResponse = labUserProvider.getAdfsUser(FederationProvider.ADFSV4, true);
        labUserProvider.getUserPassword(labResponse.getUser());

        String authCode = acquireAuthorizationCodeAutomated(labResponse);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv4_NotFederated(){

    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv3_Federated(){

    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv3_NotFederated(){

    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv2_Federated(){

    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv2_NotFederated(){

    }


    private AuthenticationResult acquireTokenInteractiveCommmon(LabResponse labResponse,
                                                                String authCode){
        AuthenticationResult result = null;
        try {
            PublicClientApplication pca = new PublicClientApplication.Builder(
                    labResponse.getAppId()).
                    authority(authority).
                    build();
            result = pca.acquireTokenByAuthorizationCode(
                    scopes,
                    authCode,
                    new URI("http://localhost:" + tcpListener.getPort())).get();
        } catch(Exception e){
            LOG.error("Error acquiring token with authCode" + e.getMessage());
        }

        return result;
    }

    private String acquireAuthorizationCodeAutomated(LabResponse labUserData){
        startTcpListener();
        try {
            // Wait for TCP listener to be up and running
            TimeUnit.SECONDS.sleep(5);
        } catch(InterruptedException e){
            LOG.error(e.getMessage());
        }
        runSeleniumAutomatedLogin(labUserData);
        String authServerResponse = getResponseFromTcpListener();
        return parseServerResponse(authServerResponse);
    }

    private void runSeleniumAutomatedLogin(LabResponse labUserData){
        String url = buildAuthenticationCodeURL(labUserData.getAppId());
        seleniumDriver.navigate().to(url);
        SeleniumExtensions.performLogin(seleniumDriver, labUserData.getUser());
    }

    private void startTcpListener(){
        queue = new LinkedBlockingQueue<>();
        tcpListener = new TcpListener(queue);
        tcpListener.startServer();
    }

    private String getResponseFromTcpListener(){
        String response = null;
        try {
            response = queue.poll(15, TimeUnit.SECONDS);

            if (Strings.isNullOrEmpty(response)){
                LOG.error("Server response is null");
                throw new RuntimeException("Server response is null");
            }


        } catch(InterruptedException e){
            LOG.error("Error reading from server response queue: " + e.getMessage());
            throw new RuntimeException();
        }
        return response;
    }

    private String parseServerResponse(String serverResponse){
        // Response will be a GET request with query parameter ?code=authCode
        String regexp = "code=(.*)&";
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(serverResponse);

        if(!matcher.find()){
            LOG.error("No authorization code in server reponse: " + serverResponse);
            throw new RuntimeException("No authorization code in server response");
        }

        return matcher.group(1);
    }

    private String buildAuthenticationCodeURL(String appId) {
        String redirectUrl;
        int portNumber = tcpListener.getPort();
        try {
            redirectUrl = authority + "oauth2/v2.0/authorize?" +
                    "response_type=code&" +
                    "response_mode=query&" +
                    "&client_id=" + appId +
                    "&redirect_uri=" + URLEncoder.encode("http://localhost:" + portNumber, "UTF-8") +
                    "&scope=" + URLEncoder.encode("openid offline_access profile " + scopes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.debug(e.getMessage());
            throw new RuntimeException();
        }
        return redirectUrl;
    }


}
