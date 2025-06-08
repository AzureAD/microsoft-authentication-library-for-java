package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.aad.msal4j.Shortcuts.TestConfig;
import com.microsoft.aad.msal4j.Shortcuts.TestStep;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Stream;

class RunnerTest {
    private static final Logger LOG = LoggerFactory.getLogger(RunnerTest.class);

    /**
     * Defines a set of test cases for a single unit test to run.
     */
    static Stream<String> managedIdentityTestsProvider() {
        return Stream.of(
                "mi_capability",
                "token_sha256_to_refresh",
                "mi_vm_pod"
        );
    }

    @ParameterizedTest
    @MethodSource("managedIdentityTestsProvider")
    void runManagedIdentityTest(String testCaseName) throws Exception {
        LOG.info("==========Executing Test Case==========");

        // Get all test configurations
        Map<String, JsonNode> configs = RunnerHelper.getAllTestCaseConfigs("https://smile-test.azurewebsites.net/testcases.json");

        LOG.info(String.format("---Found test case: %s", configs.get(testCaseName).toString()));

        TestConfig config = RunnerJsonHelper.parseTestConfig(configs.get(testCaseName).toString());

        // Create applications from the configuration
        Map<String, ManagedIdentityApplication> apps = RunnerHelper.createAppsFromConfig(config);

        // For each application, execute all steps
        for (ManagedIdentityApplication app : apps.values()) {
            app.tokenCache.accessTokens.clear(); // Clear the static token cache for each test run

            // Execute each step in the test configuration
            for (TestStep step : config.getSteps()) {
                LOG.info("----------Executing step----------");

                // Execute the action
                IAuthenticationResult result = RunnerHelper.executeAction(app, step.getAction());

                // Validate assertions
                RunnerHelper.validateAssertions(result, step.getAssertions());
            }
        }
    }
}