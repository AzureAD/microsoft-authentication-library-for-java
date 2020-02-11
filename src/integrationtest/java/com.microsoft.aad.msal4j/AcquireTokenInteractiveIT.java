// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.B2CProvider;
import labapi.FederationProvider;
import labapi.LabService;
import labapi.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;

public class AcquireTokenInteractiveIT extends SeleniumTest {

    private final static Logger LOG = LoggerFactory.getLogger(AuthorizationCodeIT.class);

    @Test
    public void acquireTokenInteractive_ManagedUser(){
        User user = labUserProvider.getDefaultUser();
        assertAcquireTokenAAD(user);
    }

    @Test
    public void acquireTokenInteractive_ADFSv2019_OnPrem(){
        User user = labUserProvider.getOnPremAdfsUser(FederationProvider.ADFS_2019);
        assertAcquireTokenADFS2019(user);
    }

    @Test
    public void acquireTokenInteractive_ADFSv2019_Federated(){
        User user = labUserProvider.getFederatedAdfsUser(FederationProvider.ADFS_2019);
        assertAcquireTokenAAD(user);
    }

    @Test
    public void acquireTokenInteractive_ADFSv4_Federated(){
        User user = labUserProvider.getFederatedAdfsUser(FederationProvider.ADFS_4);
        assertAcquireTokenAAD(user);
    }

    @Test
    public void acquireTokenInteractive_ADFSv3_Federated(){
        User user = labUserProvider.getFederatedAdfsUser(FederationProvider.ADFS_3);
        assertAcquireTokenAAD(user);
    }

    @Test
    public void acquireTokenInteractive_ADFSv2_Federated(){
        User user = labUserProvider.getFederatedAdfsUser(FederationProvider.ADFS_2);
        assertAcquireTokenAAD(user);
    }

    @Test
    public void acquireTokenWithAuthorizationCode_B2C_Local(){
        User user = labUserProvider.getB2cUser(B2CProvider.LOCAL);
        assertAcquireTokenB2C(user);
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

        IAuthenticationResult result = acquireTokenInteractive(
                user,
                pca,
                TestConstants.GRAPH_DEFAULT_SCOPE);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(user.getUpn(), result.account().username());
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

        IAuthenticationResult result = acquireTokenInteractive(user, pca, TestConstants.ADFS_SCOPE);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(user.getUpn(), result.account().username());
    }

    private void assertAcquireTokenB2C(User user){

        PublicClientApplication pca;
        try {
            pca = PublicClientApplication.builder(
                    user.getAppId()).
                    b2cAuthority(TestConstants.B2C_AUTHORITY_SIGN_IN).
                    build();
        } catch(MalformedURLException ex){
            throw new RuntimeException(ex.getMessage());
        }

        IAuthenticationResult result = acquireTokenInteractive(user, pca, user.getAppId());
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
    }

    private IAuthenticationResult acquireTokenInteractive(
            User user,
            PublicClientApplication pca,
            String scope){

        IAuthenticationResult result;
        try {
            URI url = new URI("http://localhost:8080");

            SystemBrowserOptions browserOptions = new SystemBrowserOptions();
            browserOptions.openBrowserAction(
                    new SeleniumOpenBrowserAction(user, pca));

            InteractiveRequestParameters parameters = InteractiveRequestParameters
                    .builder(Collections.singleton(scope), url)
                    .systemBrowserOptions(browserOptions)
                    .build();

            result = pca.acquireToken(parameters).get();

        } catch(Exception e){
            LOG.error("Error acquiring token with authCode: " + e.getMessage());
            throw new RuntimeException("Error acquiring token with authCode: " + e.getMessage());
        }
        return result;
    }

    class SeleniumOpenBrowserAction implements OpenBrowserAction {

        private User user;
        private PublicClientApplication pca;

        SeleniumOpenBrowserAction(User user, PublicClientApplication pca){
            this.user = user;
            this.pca = pca;
        }

        public void openBrowser(URL url){
            seleniumDriver.navigate().to(url);
            runSeleniumAutomatedLogin(user, pca);
        }
    }
}
