// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.UserConfig;
import infrastructure.SeleniumExtensions;
import infrastructure.SeleniumTestWatcher;
import infrastructure.WebDriverProvider;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(SeleniumTestWatcher.class)
abstract class SeleniumTest implements WebDriverProvider {
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

    @Override
    public WebDriver getWebDriver() {
        return seleniumDriver;
    }

    void runSeleniumAutomatedLogin(UserConfig user, AbstractClientApplicationBase app) {
        AuthorityType authorityType = app.authenticationAuthority.authorityType;

        try {
            switch (authorityType) {
                case AAD:
                    SeleniumExtensions.performADOrCiamLogin(seleniumDriver, user);
                    break;
                case ADFS:
                    SeleniumExtensions.performADFSLogin(seleniumDriver, user);
                    break;
                case CIAM:
                    SeleniumExtensions.performCiamLogin(seleniumDriver, user);
                    break;
                case OIDC:
                    // OIDC authorities may use CIAM or AAD login pages depending on the host.
                    // Check the authority host to determine which page object to use.
                    if (app.authenticationAuthority.host.contains("ciam") ||
                            app.authenticationAuthority.host.contains("msidlabsciam")) {
                        SeleniumExtensions.performCiamLogin(seleniumDriver, user);
                    } else {
                        SeleniumExtensions.performADOrCiamLogin(seleniumDriver, user);
                    }
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
