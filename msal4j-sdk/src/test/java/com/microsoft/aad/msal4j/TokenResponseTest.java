// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TokenResponseTest {

    @Test
    void testConstructor_PublicResponse() {
        final Map<String, String> jsonMap = JsonHelper.convertJsonToMap(TestConfiguration.TOKEN_ENDPOINT_OK_RESPONSE_ID_AND_ACCESS);
        final TokenResponse response = new TokenResponse(JsonHelper.convertJsonToMap(TestConfiguration.TOKEN_ENDPOINT_OK_RESPONSE_ID_AND_ACCESS));
        assertNotNull(response);
        assertEquals(jsonMap.get("access_token"), response.accessToken());
        assertEquals(jsonMap.get("id_token"), response.idToken());
        assertEquals(jsonMap.get("client_info"), response.getClientInfo());
        assertEquals(jsonMap.get("scope"), response.getScope());
        assertEquals(jsonMap.get("expires_in"), Long.toString(response.getExpiresIn()));
    }

    @Test
    void testConstructor_CLientCredentialResponse() {
        final Map<String, String> jsonMap = JsonHelper.convertJsonToMap(TestConfiguration.TOKEN_ENDPOINT_OK_RESPONSE_ACCESS_ONLY);
        final TokenResponse response = new TokenResponse(JsonHelper.convertJsonToMap(TestConfiguration.TOKEN_ENDPOINT_OK_RESPONSE_ACCESS_ONLY));
        assertNotNull(response);
        assertEquals(jsonMap.get("access_token"), response.accessToken());
        assertNull(response.idToken());
        assertNull(response.getClientInfo());
        assertEquals(jsonMap.get("expires_in"), Long.toString(response.getExpiresIn()));
    }
}
