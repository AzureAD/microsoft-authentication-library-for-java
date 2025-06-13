// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.testcase;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.json.JsonWriter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParsingTest {
    private static final Logger LOG = LoggerFactory.getLogger(ParsingTest.class);
    private static final String MI_TEST_CASE_EXAMPLE = "{\"arrange\":{\"app1\":{\"ManagedIdentityClient\":{\"client_capabilities\":[\"cp1\",\"cp2\"],\"managed_identity\":{\"Id\":null,\"ManagedIdentityIdType\":\"SystemAssigned\"}}}},\"env\":{\"IDENTITY_ENDPOINT\":\"https://smile-test.azurewebsites.net/test/token_sha256_to_refresh/cp2,cp1\",\"IDENTITY_HEADER\":\"foo\",\"IDENTITY_SERVER_THUMBPRINT\":\"bar\"},\"steps\":[{\"act\":{\"app1.AcquireTokenForManagedIdentity\":{\"resource\":\"R\"}},\"assert\":{\"token_source\":\"identity_provider\",\"token_type\":\"Bearer\"}},{\"act\":{\"app1.AcquireTokenForManagedIdentity\":{\"resource\":\"R\"}},\"assert\":{\"token_source\":\"cache\",\"token_type\":\"Bearer\"}},{\"act\":{\"app1.AcquireTokenForManagedIdentity\":{\"claims_challenge\":\"{\\\"capability test case likely needs\\\": \\\"a valid json object\\\"}\",\"resource\":\"R\"}},\"assert\":{\"token_source\":\"identity_provider\",\"token_type\":\"Bearer\"}}],\"type\":\"MSAL Test\",\"ver\":1}";
    private static final String CONFIDENTIAL_TEST_CASE_EXAMPLE = "{\"arrange\":{\"cca1\":{\"ConfidentialClientApplication\":{\"client_credential\":\"fake-credential\",\"client_id\":\"foo\",\"oidc_authority\":\"https://smile-test.azurewebsites.net/wrong_issuer\"}},\"cca2\":{\"ConfidentialClientApplication\":{\"client_credential\":\"fake-credential\",\"client_id\":\"foo\",\"oidc_authority\":\"https://smile-test.azurewebsites.net/right_issuer\"}}},\"env\":{},\"steps\":[{\"act\":{\"cca1.AcquireTokenForClient\":{\"scopes\":[\"scope1\"]}},\"assert\":{\"error\":\"invalid_issuer\"}},{\"act\":{\"cca2.AcquireTokenForClient\":{\"scopes\":[\"scope2\"]}},\"assert\":{\"token_source\":\"identity_provider\",\"token_type\":\"Bearer\"}}],\"type\":\"MSAL Test\",\"ver\":1}";

    @Test
    void testFullJsonSerialization() throws Exception {
        // Parse the test case
        TestCase testCase = parseTestCase(MI_TEST_CASE_EXAMPLE);

        // Verify basic structure
        assertNotNull(testCase.getType(), "Test case type should not be null");
        LOG.info("Deserialized TestCase: type={}, version={}", testCase.getType(), testCase.getVersion());

        // Verify sections
        verifyArrangeSection(testCase);
        verifyEnvSection(testCase);
        verifyStepsSection(testCase);

        // Test round-trip serialization
        String json = serializeTestCase(testCase);
        TestCase reparsedTestCase = parseTestCase(json);

        // Verify key properties match after round-trip
        assertEquals(testCase.getType(), reparsedTestCase.getType());
        assertEquals(testCase.getVersion(), reparsedTestCase.getVersion());

        // Verify sections match after round-trip
        verifySectionsMatchAfterRoundTrip(testCase, reparsedTestCase);
    }

    @Test
    void testRealServerJsonConsistency() throws Exception {
        // First round: Parse and serialize
        TestCase testCase = parseTestCase(MI_TEST_CASE_EXAMPLE);
        String firstSerialization = serializeTestCase(testCase);

        // Second round: Parse and serialize again
        TestCase secondTestCase = parseTestCase(firstSerialization);
        String secondSerialization = serializeTestCase(secondTestCase);

        // Third round: One more cycle
        TestCase thirdTestCase = parseTestCase(secondSerialization);
        String thirdSerialization = serializeTestCase(thirdTestCase);

        // Verify consistency between serializations
        assertEquals(firstSerialization, secondSerialization, "First and second serializations should match");
        assertEquals(secondSerialization, thirdSerialization, "Second and third serializations should match");
        LOG.info("All three serialization rounds produced identical JSON");

        // Verify object equality in depth
        verifySectionsMatchAfterRoundTrip(testCase, secondTestCase);
    }

    private TestCase parseTestCase(String json) throws Exception {
        JsonReader reader = JsonProviders.createReader(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        return TestCase.fromJson(reader);
    }

    private String serializeTestCase(TestCase testCase) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonWriter writer = JsonProviders.createWriter(out);
        testCase.toJson(writer);
        writer.flush();
        return out.toString();
    }

    private void verifyArrangeSection(TestCase testCase) {
        if (testCase.getArrange() != null) {
            LOG.info("Arrange section contains {} object(s)", testCase.getArrange().getObjects().size());
            assertFalse(testCase.getArrange().getObjects().isEmpty(), "Arrange objects should not be empty");

            for (Map.Entry<String, Arrange.ObjectProperties> entry : testCase.getArrange().getObjects().entrySet()) {
                LOG.info("  Object: {}, Type: {}", entry.getKey(), entry.getValue().getType());
            }
        }
    }

    private void verifyEnvSection(TestCase testCase) {
        if (testCase.getEnv() != null) {
            LOG.info("Environment section contains {} variable(s)", testCase.getEnv().getVariables().size());
            assertFalse(testCase.getEnv().getVariables().isEmpty(), "Environment variables should not be empty");
        }
    }

    private void verifyStepsSection(TestCase testCase) {
        if (testCase.getSteps() != null) {
            LOG.info("Steps section contains {} step(s)", testCase.getSteps().getSteps().size());
            assertFalse(testCase.getSteps().getSteps().isEmpty(), "Steps should not be empty");
        }
    }

    private void verifySectionsMatchAfterRoundTrip(TestCase original, TestCase reparsed) {
        // Check arrange section
        if (original.getArrange() != null && reparsed.getArrange() != null) {
            assertEquals(original.getArrange().getObjects().size(), reparsed.getArrange().getObjects().size(),
                    "Arrange objects count should match");
        } else {
            assertTrue((original.getArrange() == null && reparsed.getArrange() == null),
                    "Both arrange sections should be null or non-null");
        }

        // Check env section
        if (original.getEnv() != null && reparsed.getEnv() != null) {
            assertEquals(original.getEnv().getVariables().size(), reparsed.getEnv().getVariables().size(),
                    "Environment variables count should match");
        } else {
            assertTrue((original.getEnv() == null && reparsed.getEnv() == null),
                    "Both env sections should be null or non-null");
        }

        // Check steps section
        if (original.getSteps() != null && reparsed.getSteps() != null) {
            assertEquals(original.getSteps().getSteps().size(), reparsed.getSteps().getSteps().size(),
                    "Steps count should match");
        } else {
            assertTrue((original.getSteps() == null && reparsed.getSteps() == null),
                    "Both steps sections should be null or non-null");
        }
    }
}