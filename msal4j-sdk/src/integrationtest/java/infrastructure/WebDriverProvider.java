// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package infrastructure;

import org.openqa.selenium.WebDriver;

/**
 * Interface for test classes that manage a WebDriver instance.
 * Used by {@link SeleniumTestWatcher} to access the driver for diagnostics on test failure.
 */
public interface WebDriverProvider {

    /**
     * Returns the current WebDriver instance, or null if the driver has not been initialized
     * or has already been closed.
     *
     * @return the WebDriver instance, or null
     */
    WebDriver getWebDriver();
}
