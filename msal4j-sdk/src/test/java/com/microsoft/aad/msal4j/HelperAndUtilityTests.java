// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelperAndUtilityTests {

    @Test
    void StringHelper_serializeQueryParameters_ValidUrlQueryStrings() {
        //Empty map
        Map<String, String> params = new LinkedHashMap<>();
        String result = StringHelper.serializeQueryParameters(params);
        assertEquals("", result);

        //Null map
        result = StringHelper.serializeQueryParameters(null);
        assertEquals("", result);

        //Basic parameters
        params = new LinkedHashMap<>();
        params.put("client_id", "abc123");
        params.put("scope", "openid profile");
        params.put("redirect_uri", "https://example.com/auth");

        result = StringHelper.serializeQueryParameters(params);
        assertEquals("client_id=abc123&scope=openid+profile&redirect_uri=https%3A%2F%2Fexample.com%2Fauth", result);

        //(Unrealistic) parameters with special characters
        params = new LinkedHashMap<>();
        params.put("client_id", "special@client");
        params.put("scope", "openid offline_access");
        params.put("redirect_uri", "https://example.com/query?key=value");

        result = StringHelper.serializeQueryParameters(params);
        assertEquals("client_id=special%40client&scope=openid+offline_access&redirect_uri=https%3A%2F%2Fexample.com%2Fquery%3Fkey%3Dvalue", result);

        //Null values in map
        params = new LinkedHashMap<>();
        params.put("client_id", "abc123");
        params.put("scope", null);
        params.put("redirect_uri", "https://example.com/auth");

        result = StringHelper.serializeQueryParameters(params);
        assertEquals("client_id=abc123&redirect_uri=https%3A%2F%2Fexample.com%2Fauth", result);
    }

    @Test
    void StringHelper_convertToMultiValueMap() {
        //Historically, much of the library once used Map<String, List<String>> to represent URL query params, though it now uses Map<String, String>.
        // AuthorizationRequestUrlParameters unfortunately has a public API returning Map<String, List<String>>,
        // so this test helps ensure we still return an equivalent map to what we're using internally.
        AuthorizationRequestUrlParameters params = new AuthorizationRequestUrlParameters.Builder()
                .redirectUri("https://myapp.com/callback")
                .scopes(Collections.singleton("openid profile email"))
                .state("state123")
                .nonce("nonce123")
                .loginHint("user@example.com")
                .correlationId("correlation-id-123").build();

        Map<String, String> internalRequestParams = params.requestParameters;
        Map<String, List<String>> convertedInternalMap = StringHelper.convertToMultiValueMap(internalRequestParams);

        // Get the map returned by the method
        Map<String, List<String>> methodReturnedMap = params.requestParameters();

        // Assert
        assertNotNull(convertedInternalMap, "Converted map should not be null");
        assertNotNull(methodReturnedMap, "Method returned map should not be null");

        assertEquals(convertedInternalMap.size(), methodReturnedMap.size(), "Maps should have the same size");

        for (String key : convertedInternalMap.keySet()) {
            assertTrue(methodReturnedMap.containsKey(key), "Method returned map should contain key: " + key);

            List<String> convertedValues = convertedInternalMap.get(key);
            List<String> methodValues = methodReturnedMap.get(key);

            assertEquals(convertedValues, methodValues,
                    "Values for key '" + key + "' should be equal");
        }
    }
}
