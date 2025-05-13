// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TokenRevocationTest {

    private static final String TEST_TOKEN = "test_token";
    private static final String EXPECTED_TOKEN_HASH = "cc0af97287543b65da2c7e1476426021826cab166f1e063ed012b855ff819656";
    private static final String TEST_RESOURCE = "https://management.azure.com";
      @Test
    public void testConvertTokenToSHA256Hash() {
        String hash = StringHelper.createSha256HashHexString(TEST_TOKEN);
        assertEquals(EXPECTED_TOKEN_HASH, hash);
    }
    
    @Test
    public void testTokenToRevokeValidation() {
        // Should throw exception when null
        assertThrows(IllegalArgumentException.class, () -> {
            StringHelper.createSha256HashHexString(null);
        });
        
        // Should throw exception when empty
        assertThrows(IllegalArgumentException.class, () -> {
            StringHelper.createSha256HashHexString("");
        });
    }
    
    @Test
    public void testManagedIdentityParametersBuilder() {
        ManagedIdentityParameters params = ManagedIdentityParameters.builder(TEST_RESOURCE)
                .build();

        params.revokedTokenHash = StringHelper.createSha256HashHexString(TEST_TOKEN);

        assertEquals(TEST_RESOURCE, params.resource());
        assertEquals(EXPECTED_TOKEN_HASH, params.revokedTokenHash());
    }
}
