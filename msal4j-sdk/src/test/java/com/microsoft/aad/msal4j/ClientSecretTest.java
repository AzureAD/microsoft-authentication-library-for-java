// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
public class ClientSecretTest {

    @Test
    public void testConstructorNullClientId() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new ClientSecret(null);
        });

        assertEquals("clientSecret is null or empty", exception.getMessage());
    }

    @Test
    public void testConstructorEmptyClientId() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new ClientSecret("");
        });

        assertEquals("clientSecret is null or empty", exception.getMessage());
    }
}
