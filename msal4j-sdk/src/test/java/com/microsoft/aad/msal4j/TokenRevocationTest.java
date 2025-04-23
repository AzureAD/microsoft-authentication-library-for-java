// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TokenRevocationTest {

    private static final String TEST_TOKEN = "test_token";
    private static final String EXPECTED_TOKEN_HASH = "cc0af97287543b65da2c7e1476426021826cab166f1e063ed012b855ff819656";
    private static final String TEST_RESOURCE = "https://management.azure.com";
    
    @Test
    public void testConvertTokenToSHA256Hash() {
        String hash = TokenRevocationUtil.convertTokenToSHA256HashString(TEST_TOKEN);
        assertEquals(EXPECTED_TOKEN_HASH, hash);
    }
    
    @Test
    public void testTokenToRevokeValidation() {
        // Should throw exception when null
        assertThrows(IllegalArgumentException.class, () -> {
            TokenRevocationUtil.convertTokenToSHA256HashString(null);
        });
        
        // Should throw exception when empty
        assertThrows(IllegalArgumentException.class, () -> {
            TokenRevocationUtil.convertTokenToSHA256HashString("");
        });
    }
    
    @Test
    public void testManagedIdentityParametersBuilder() {
        ManagedIdentityParameters params = ManagedIdentityParameters.builder(TEST_RESOURCE)
                .tokenToRevoke(TEST_TOKEN)
                .build();
        
        assertEquals(TEST_RESOURCE, params.resource());
        assertEquals(TEST_TOKEN, params.getTokenToRevoke());
    }
    
    @Test
    public void testManagedIdentityParametersBuilderValidation() {
        // Should throw exception when tokenToRevoke is null or empty
        assertThrows(IllegalArgumentException.class, () -> {
            ManagedIdentityParameters.builder(TEST_RESOURCE)
                .tokenToRevoke(null)
                .build();
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            ManagedIdentityParameters.builder(TEST_RESOURCE)
                .tokenToRevoke("")
                .build();
        });
    }
}
