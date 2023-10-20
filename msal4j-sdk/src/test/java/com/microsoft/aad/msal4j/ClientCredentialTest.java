// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientCredentialTest {

    @Test
    void testAssertionNullAndEmpty() {
        assertThrows(NullPointerException.class, () ->
                new ClientAssertion(""));

        assertThrows(NullPointerException.class, () ->
                new ClientAssertion(null));
    }

    @Test
    void testSecretNullAndEmpty() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new ClientSecret(""));

        assertTrue(ex.getMessage().contains("clientSecret is null or empty"));

        assertThrows(IllegalArgumentException.class, () ->
                new ClientSecret(null));

        assertTrue(ex.getMessage().contains("clientSecret is null or empty"));
    }
}
