// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TokenResponseTest {

    @Test
    void testConstructor() {
        final Map<String, String> jsonMap = TestHelper.convertJsonToMap(TestConfiguration.TOKEN_ENDPOINT_OK_RESPONSE);
        final TokenResponse response = new TokenResponse(TestHelper.convertJsonToMap(TestConfiguration.TOKEN_ENDPOINT_OK_RESPONSE));
        assertNotNull(response);
        assertEquals(jsonMap.get("access_token"), response.accessToken());
        assertEquals(jsonMap.get("id_token"), response.idToken());
        assertEquals(jsonMap.get("client_info"), response.getClientInfo());
        assertEquals(jsonMap.get("scope"), response.getScope());
        assertEquals(jsonMap.get("expires_in"), Long.toString(response.getExpiresIn()));
    }
}
