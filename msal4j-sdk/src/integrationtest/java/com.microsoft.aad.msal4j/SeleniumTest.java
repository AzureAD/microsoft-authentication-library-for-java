// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.UserConfig;
import infrastructure.SeleniumExtensions;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class SeleniumTest {
    private static final Logger LOG = LoggerFactory.getLogger(SeleniumTest.class);

    WebDriver seleniumDriver;
    HttpListener httpListener;

    public void cleanUp() {
        if (seleniumDriver != null) {
            try {
                seleniumDriver.quit();
            } catch (Exception e) {
                LOG.error("Error closing WebDriver: {}", e.getMessage());
            }
        }
        if (httpListener != null) {
            try {
                httpListener.stopListener();
            } catch (Exception e) {
                LOG.error("Error stopping HttpListener: {}", e.getMessage());
            }
        }
    }

    public void startUpBrowser() {
        seleniumDriver = SeleniumExtensions.createDefaultWebDriver();
    }

    void runSeleniumAutomatedLogin(UserConfig user, AbstractClientApplicationBase app) {
        AuthorityType authorityType = app.authenticationAuthority.authorityType;

        try {
            switch (authorityType) {
                case B2C:
                    SeleniumExtensions.performLocalLogin(seleniumDriver, user);
                    break;
                case AAD:
                    SeleniumExtensions.performADOrCiamLogin(seleniumDriver, user);
                    break;
                case ADFS:
                    SeleniumExtensions.performADFSLogin(seleniumDriver, user);
                    break;
                case CIAM:
                case OIDC:
                    SeleniumExtensions.performADOrCiamLogin(seleniumDriver, user);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported authority type: " + authorityType);
            }
        } catch (Exception e) {
            LOG.error("Selenium automation failed for authority type {}: {}", authorityType, e.getMessage());
            throw new RuntimeException("Selenium automation failed", e);
        }
    }
}
