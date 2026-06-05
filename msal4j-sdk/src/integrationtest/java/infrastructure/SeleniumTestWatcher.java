// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package infrastructure;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit 5 extension that captures Selenium diagnostics (screenshot, page source, browser logs)
 * when a test fails.
 * <p>
 * This extension uses {@link AfterTestExecutionCallback} which runs <em>after</em> the test method
 * but <em>before</em> {@code @AfterEach} teardown. This ordering is critical because it ensures
 * the WebDriver is still alive when diagnostics are captured — the driver is typically closed
 * in {@code @AfterEach}.
 * <p>
 * Test classes must implement {@link WebDriverProvider} to expose their WebDriver instance.
 * If the test class does not implement the interface, or the driver is null, diagnostics
 * are silently skipped.
 * <p>
 * Usage:
 * <pre>
 * {@literal @}ExtendWith(SeleniumTestWatcher.class)
 * class MySeleniumTest implements WebDriverProvider {
 *     WebDriver driver;
 *
 *     public WebDriver getWebDriver() { return driver; }
 * }
 * </pre>
 *
 * @see SeleniumDiagnostics
 * @see WebDriverProvider
 */
public class SeleniumTestWatcher implements AfterTestExecutionCallback {

    private static final Logger LOG = LoggerFactory.getLogger(SeleniumTestWatcher.class);

    @Override
    public void afterTestExecution(ExtensionContext context) {
        // Only capture diagnostics if the test failed
        if (!context.getExecutionException().isPresent()) {
            return;
        }

        Object testInstance = context.getTestInstance().orElse(null);
        if (!(testInstance instanceof WebDriverProvider)) {
            LOG.debug("Test class does not implement WebDriverProvider, skipping diagnostics");
            return;
        }

        WebDriver driver = ((WebDriverProvider) testInstance).getWebDriver();
        if (driver == null) {
            LOG.warn("WebDriver is null, cannot capture diagnostics for failed test: {}",
                    context.getDisplayName());
            return;
        }

        String testName = context.getTestClass()
                .map(Class::getSimpleName)
                .orElse("UnknownClass")
                + "." + context.getDisplayName();

        LOG.info("Test failed: {} — capturing diagnostics", testName);
        SeleniumDiagnostics.captureAll(driver, testName);
    }
}
