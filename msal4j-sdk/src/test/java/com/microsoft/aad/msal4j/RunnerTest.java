// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.testcase.TestCase;
import com.microsoft.aad.msal4j.testcase.TestCaseHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Test runner for executing MSAL4J integration tests from external test case definitions.
 * Uses parameterized tests to run different test cases through a common execution path.
 */
class RunnerTest {
    private static final Logger LOG = LoggerFactory.getLogger(RunnerTest.class);
    private static Map<String, TestCase> allTestConfigs;

    @BeforeAll
    static void setup() throws IOException {
        LOG.info("Loading all test configurations");
        allTestConfigs = TestCaseHelper.getAllTestCaseConfigs("https://smile-test.azurewebsites.net/testcases.json");
        LOG.info("Loaded {} test configurations", allTestConfigs.size());
    }

    /**
     * Defines a set of test cases that cover managed identity scenarios.
     */
    static Stream<String> managedIdentityTestsProvider() {
        return Stream.of(
                "mi_capability",
                "token_sha256_to_refresh",
                "mi_vm_pod"
        );
    }

    /**
     * Defines a set of test cases that cover OIDC authority scenarios.
     */
    static Stream<String> confidentialClientOidcTestsProvider() {
        return Stream.of(
                "issuer_validation"
        );
    }

    @ParameterizedTest
    @MethodSource("managedIdentityTestsProvider")
    void runManagedIdentityTest(String testCaseName) throws Exception {
        LOG.info("========== Executing Managed Identity Test: {} ==========", testCaseName);

        TestCase testCase = getTestCase(testCaseName);
        Map<String, RunnerHelper.AppCreationResult> appResults =
                RunnerHelper.createMangedIdentityAppsFromConfig(testCase);

        LOG.info("Created {} application(s) for test execution", appResults.size());
        RunnerHelper.executeTestCase(testCase, appResults);
    }

    @ParameterizedTest
    @MethodSource("confidentialClientOidcTestsProvider")
    void runConfidentialClientOidcTests(String testCaseName) throws Exception {
        LOG.info("========== Executing Confidential Client OIDC Test: {} ==========", testCaseName);

        TestCase testCase = getTestCase(testCaseName);
        Map<String, RunnerHelper.AppCreationResult> appResults =
                RunnerHelper.createConfidentialClientAppsFromConfig(testCase);

        LOG.info("Created {} application(s) for test execution", appResults.size());
        RunnerHelper.executeTestCase(testCase, appResults);
    }

    /**
     * Gets a test case by name from the loaded configurations
     */
    private static TestCase getTestCase(String testCaseName) {
        TestCase testCase = allTestConfigs.get(testCaseName);
        if (testCase == null) {
            throw new IllegalArgumentException("Test case not found: " + testCaseName);
        }
        return testCase;
    }
}