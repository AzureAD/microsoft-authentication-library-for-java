// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedIdentityResponseTest {

    @Test
    void expiresInAcceptsStringAndNumberValues() throws Exception {
        long before = System.currentTimeMillis() / 1000;

        ManagedIdentityResponse stringResponse =
                parse("{\"expires_in\":\"120\"}");
        ManagedIdentityResponse numberResponse =
                parse("{\"expires_in\":120}");

        assertTrue(Long.parseLong(stringResponse.expiresOn) >= before + 120);
        assertTrue(Long.parseLong(numberResponse.expiresOn) >= before + 120);
    }

    @Test
    void expiresOnWinsRegardlessOfFieldOrder() throws Exception {
        assertEquals("12345",
                parse("{\"expires_on\":\"12345\",\"expires_in\":\"120\"}")
                        .expiresOn);
        assertEquals("12345",
                parse("{\"expires_in\":120,\"expires_on\":12345}")
                        .expiresOn);
    }

    private static ManagedIdentityResponse parse(String json) throws Exception {
        try (JsonReader reader = JsonProviders.createReader(json)) {
            return ManagedIdentityResponse.fromJson(reader);
        }
    }
}
