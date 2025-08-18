package com.microsoft.aad.msal4j;

import com.nimbusds.jwt.JWTParser;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

//These tests were added to ensure the new usages of com.azure.json are functionally the same as the old usages of com.nimbusds packages.
//Once we are confident in the new behavior these should no longer be necessary.
class JsonCompatibilityTests {

    //New style, using helper methods in JsonHelper that use com.azure.json
    private final Map<String, Object> newStyleParsedClaims = JsonHelper.parseJsonToMap(JsonHelper.getTokenPayloadClaims(TestHelper.ENCODED_JWT));
    //Old style, using com.nimbusds methods
    private final Map<String, Object> oldStyleParsedClaims = JWTParser.parse(TestHelper.ENCODED_JWT).getJWTClaimsSet().getClaims();

    JsonCompatibilityTests() throws ParseException {
    }

    @Test
    void testBasicClaimsMatching() {
        // Test basic string claims
        assertEquals(oldStyleParsedClaims.get("aud"), newStyleParsedClaims.get("aud"));
        assertEquals(oldStyleParsedClaims.get("iss"), newStyleParsedClaims.get("iss"));
        assertEquals(oldStyleParsedClaims.get("name"), newStyleParsedClaims.get("name"));
        assertEquals(oldStyleParsedClaims.get("oid"), newStyleParsedClaims.get("oid"));
        assertEquals(oldStyleParsedClaims.get("preferred_username"), newStyleParsedClaims.get("preferred_username"));
        assertEquals(oldStyleParsedClaims.get("sub"), newStyleParsedClaims.get("sub"));
        assertEquals(oldStyleParsedClaims.get("tid"), newStyleParsedClaims.get("tid"));
        assertEquals(oldStyleParsedClaims.get("ver"), newStyleParsedClaims.get("ver"));
    }

    @Test
    void testNullValues() {
        // Check null values are handled the same
        assertEquals(oldStyleParsedClaims.get("email"), newStyleParsedClaims.get("email"));
    }

    @Test
    void testNumericValues() {
        // Test numeric claims
        assertEquals(oldStyleParsedClaims.get("exp"), newStyleParsedClaims.get("exp"));
        assertEquals(oldStyleParsedClaims.get("iat"), newStyleParsedClaims.get("iat"));
        assertEquals(oldStyleParsedClaims.get("nbf"), newStyleParsedClaims.get("nbf"));
        assertEquals(oldStyleParsedClaims.get("auth_time"), newStyleParsedClaims.get("auth_time"));
    }

    @Test
    void testListValues() {
        // Test list claims
        List<?> oldGroups = (List<?>) oldStyleParsedClaims.get("groups");
        List<?> newGroups = (List<?>) newStyleParsedClaims.get("groups");
        assertEquals(oldGroups, newGroups);

        List<?> oldAmr = (List<?>) oldStyleParsedClaims.get("amr");
        List<?> newAmr = (List<?>) newStyleParsedClaims.get("amr");
        assertEquals(oldAmr, newAmr);

        List<?> oldRoles = (List<?>) oldStyleParsedClaims.get("roles");
        List<?> newRoles = (List<?>) newStyleParsedClaims.get("roles");
        assertEquals(oldRoles, newRoles);
    }

    @Test
    void testNestedObjects() {
        // Test nested objects
        Map<?, ?> oldExtensionData = (Map<?, ?>) oldStyleParsedClaims.get("extension_data");
        Map<?, ?> newExtensionData = (Map<?, ?>) newStyleParsedClaims.get("extension_data");

        assertEquals(oldExtensionData.get("department"), newExtensionData.get("department"));
        assertEquals(oldExtensionData.get("manager"), newExtensionData.get("manager"));
        assertEquals(oldExtensionData.get("employeeId"), newExtensionData.get("employeeId"));
    }

    @Test
    void testCompleteTokenParsing() {
        assertEquals(oldStyleParsedClaims.size(), newStyleParsedClaims.size());

        // Check that all keys and values match
        for (String key : oldStyleParsedClaims.keySet()) {
            assertTrue(newStyleParsedClaims.containsKey(key), "New claims should contain key: " + key);

            Object oldValue = oldStyleParsedClaims.get(key);
            Object newValue = newStyleParsedClaims.get(key);

            if (oldValue instanceof List) {
                assertListsEqual((List<?>) oldValue, (List<?>) newValue);
            } else if (oldValue instanceof Map) {
                assertMapsEqual((Map<?, ?>) oldValue, (Map<?, ?>) newValue);
            } else {
                assertEquals(oldValue, newValue, "Value mismatch for key: " + key);
            }
        }
    }

    @Test
    void testInvalidJSONHandling() {
        // Test that both parsers throw exceptions for invalid JSON
        String invalidJson = "{ this is not valid json }";

        assertThrows(Exception.class, () -> JsonHelper.parseJsonToMap(invalidJson));
        assertThrows(Exception.class, () -> JWTParser.parse("header." +
                Base64.getUrlEncoder().encodeToString(invalidJson.getBytes()) + ".signature"));
    }

    /**
     * Utility method to compare lists deeply
     */
    private void assertListsEqual(List<?> list1, List<?> list2) {
        assertEquals(list1.size(), list2.size());
        for (int i = 0; i < list1.size(); i++) {
            Object item1 = list1.get(i);
            Object item2 = list2.get(i);

            if (item1 == null) {
                assertNull(item2);
            } else if (item1 instanceof List) {
                assertListsEqual((List<?>) item1, (List<?>) item2);
            } else if (item1 instanceof Map) {
                assertMapsEqual((Map<?, ?>) item1, (Map<?, ?>) item2);
            } else {
                assertEquals(item1, item2);
            }
        }
    }

    /**
     * Utility method to compare maps deeply
     */
    private void assertMapsEqual(Map<?, ?> map1, Map<?, ?> map2) {
        assertEquals(map1.size(), map2.size());
        for (Object key : map1.keySet()) {
            assertTrue(map2.containsKey(key));

            Object value1 = map1.get(key);
            Object value2 = map2.get(key);

            if (value1 == null) {
                assertNull(value2);
            } else if (value1 instanceof List) {
                assertListsEqual((List<?>) value1, (List<?>) value2);
            } else if (value1 instanceof Map) {
                assertMapsEqual((Map<?, ?>) value1, (Map<?, ?>) value2);
            } else {
                assertEquals(value1, value2);
            }
        }
    }
}