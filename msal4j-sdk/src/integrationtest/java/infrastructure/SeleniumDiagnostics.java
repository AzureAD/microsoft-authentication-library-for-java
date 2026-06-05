// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package infrastructure;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Utility for capturing diagnostic information from Selenium WebDriver on test failure.
 * <p>
 * Captures screenshots, page source (HTML), and browser console logs to help diagnose
 * failures in browser-based integration tests. All capture methods are fail-safe and will
 * not throw exceptions, so they can be called safely during test teardown without masking
 * the original test failure.
 * <p>
 * Output is written to {@code target/selenium-diagnostics/} with filenames that include
 * the test name and a timestamp for uniqueness.
 */
public final class SeleniumDiagnostics {

    private static final Logger LOG = LoggerFactory.getLogger(SeleniumDiagnostics.class);
    private static final String OUTPUT_DIR = "target/selenium-diagnostics";

    private SeleniumDiagnostics() {
    }

    /**
     * Capture all available diagnostics (screenshot, page source, browser logs) for a failed test.
     *
     * @param driver   the WebDriver instance (may be null)
     * @param testName the name of the test method that failed
     */
    public static void captureAll(WebDriver driver, String testName) {
        if (driver == null) {
            LOG.warn("Cannot capture diagnostics: WebDriver is null");
            return;
        }

        String sanitizedName = sanitizeFileName(testName);
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String filePrefix = sanitizedName + "-" + timestamp;

        captureScreenshot(driver, filePrefix);
        capturePageSource(driver, filePrefix);
        captureBrowserLogs(driver, filePrefix);
    }

    /**
     * Capture a screenshot of the current browser state.
     *
     * @param driver     the WebDriver instance
     * @param filePrefix the file name prefix (test name + timestamp)
     */
    static void captureScreenshot(WebDriver driver, String filePrefix) {
        try {
            if (!(driver instanceof TakesScreenshot)) {
                LOG.warn("WebDriver does not support screenshots");
                return;
            }

            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Path destination = getOutputPath(filePrefix + ".png");
            Files.copy(screenshot.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Screenshot saved: {}", destination);
        } catch (Exception e) {
            LOG.warn("Failed to capture screenshot: {}", e.getMessage());
        }
    }

    /**
     * Capture the current page source (HTML) for element inspection.
     * <p>
     * Sensitive parameters (auth codes, tokens, etc.) in URLs and form actions are redacted.
     *
     * @param driver     the WebDriver instance
     * @param filePrefix the file name prefix (test name + timestamp)
     */
    static void capturePageSource(WebDriver driver, String filePrefix) {
        try {
            String pageSource = driver.getPageSource();
            if (pageSource == null || pageSource.isEmpty()) {
                LOG.warn("Page source is empty");
                return;
            }

            String currentUrl = driver.getCurrentUrl();
            String redactedUrl = redactSensitiveParams(currentUrl);

            StringBuilder content = new StringBuilder();
            content.append("<!-- URL: ").append(redactedUrl).append(" -->\n");
            content.append("<!-- Title: ").append(driver.getTitle()).append(" -->\n");
            content.append(pageSource);

            Path destination = getOutputPath(filePrefix + ".html");
            Files.write(destination, content.toString().getBytes("UTF-8"));
            LOG.info("Page source saved: {}", destination);
        } catch (Exception e) {
            LOG.warn("Failed to capture page source: {}", e.getMessage());
        }
    }

    /**
     * Capture browser console logs (JavaScript errors, network issues, etc.).
     *
     * @param driver     the WebDriver instance
     * @param filePrefix the file name prefix (test name + timestamp)
     */
    static void captureBrowserLogs(WebDriver driver, String filePrefix) {
        try {
            List<LogEntry> logs = driver.manage().logs().get(LogType.BROWSER).getAll();

            if (logs.isEmpty()) {
                LOG.debug("No browser console logs to capture");
                return;
            }

            StringBuilder content = new StringBuilder();
            content.append("Browser Console Logs\n");
            content.append("====================\n\n");

            for (LogEntry entry : logs) {
                content.append("[").append(entry.getLevel()).append("] ");
                content.append(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(entry.getTimestamp())));
                content.append(" - ").append(redactSensitiveParams(entry.getMessage()));
                content.append("\n");
            }

            Path destination = getOutputPath(filePrefix + "-console.log");
            Files.write(destination, content.toString().getBytes("UTF-8"));
            LOG.info("Browser logs saved: {}", destination);
        } catch (Exception e) {
            LOG.warn("Failed to capture browser logs: {} (this may be expected if logging prefs are not supported)", e.getMessage());
        }
    }

    /**
     * Get the output path for a diagnostic file, creating the directory if needed.
     */
    private static Path getOutputPath(String fileName) throws IOException {
        Path dir = Paths.get(OUTPUT_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir.resolve(fileName);
    }

    /**
     * Sanitize a test name to be safe for use as a file name on all platforms.
     * Removes/replaces characters that are invalid in Windows file paths.
     */
    private static String sanitizeFileName(String name) {
        if (name == null) {
            return "unknown";
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Redact sensitive OAuth parameters from URLs and log messages.
     * Replaces values of known sensitive query parameters with "[REDACTED]".
     */
    public static String redactSensitiveParams(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll(
                "(?i)(code|access_token|id_token|refresh_token|client_secret|password|assertion)=([^&\\s\"']*)",
                "$1=[REDACTED]");
    }
}
