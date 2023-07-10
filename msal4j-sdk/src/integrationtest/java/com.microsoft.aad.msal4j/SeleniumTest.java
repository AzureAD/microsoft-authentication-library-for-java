// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import infrastructure.SeleniumExtensions;
import labapi.B2CProvider;
import labapi.LabUserProvider;
import labapi.User;
import org.openqa.selenium.WebDriver;

abstract class SeleniumTest {

    protected LabUserProvider labUserProvider;
    WebDriver seleniumDriver;
    HttpListener httpListener;

    public void setUpLapUserProvider() {
        labUserProvider = LabUserProvider.getInstance();
    }

    public void cleanUp() {
        seleniumDriver.quit();
        if (httpListener != null) {
            httpListener.stopListener();
        }
    }

    public void startUpBrowser() {
        seleniumDriver = SeleniumExtensions.createDefaultWebDriver();
    }

    void runSeleniumAutomatedLogin(User user, AbstractClientApplicationBase app) {
        AuthorityType authorityType = app.authenticationAuthority.authorityType;
        if (authorityType == AuthorityType.B2C) {
            switch (user.getB2cProvider().toLowerCase()) {
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
            SeleniumExtensions.performADOrCiamLogin(seleniumDriver, user);
        } else if (authorityType == AuthorityType.ADFS) {
            SeleniumExtensions.performADFS2019Login(seleniumDriver, user);
        } else if (authorityType == AuthorityType.CIAM) {
            SeleniumExtensions.performADOrCiamLogin(seleniumDriver, user);
        }
    }
}
