// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi2.LabUser;
import infrastructure.SeleniumExtensions;
import org.openqa.selenium.WebDriver;

abstract class SeleniumTest {

    WebDriver seleniumDriver;
    HttpListener httpListener;

    public void cleanUp() {
        seleniumDriver.quit();
        if (httpListener != null) {
            httpListener.stopListener();
        }
    }

    public void startUpBrowser() {
        seleniumDriver = SeleniumExtensions.createDefaultWebDriver();
    }

    void runSeleniumAutomatedLogin(LabUser user, AbstractClientApplicationBase app) {
        AuthorityType authorityType = app.authenticationAuthority.authorityType;
        if (authorityType == AuthorityType.B2C) {
            switch (user.getB2cProvider().toLowerCase()) {
                case "local":
                    SeleniumExtensions.performLocalLogin(seleniumDriver, user);
                    break;
                case "google":
                    SeleniumExtensions.performGoogleLogin(seleniumDriver, user);
                    break;
                case "facebook":
                    SeleniumExtensions.performFacebookLogin(seleniumDriver, user);
                    break;
            }
        } else if (authorityType == AuthorityType.AAD) {
            SeleniumExtensions.performADOrCiamLogin(seleniumDriver, user);
        } else if (authorityType == AuthorityType.ADFS) {
            SeleniumExtensions.performADFS2019Login(seleniumDriver, user);
        } else if (authorityType == AuthorityType.CIAM || authorityType == AuthorityType.OIDC) {
            SeleniumExtensions.performADOrCiamLogin(seleniumDriver, user);
        }
    }
}
