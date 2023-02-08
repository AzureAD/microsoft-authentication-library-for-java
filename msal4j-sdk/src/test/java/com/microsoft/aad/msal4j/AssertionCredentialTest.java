// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

//// @Test(groups = {"checkin"})
class AssertionCredentialTest {

    @Test
    void testAssertionNull() {
        NullPointerException exception = Assertions.assertThrows(NullPointerException.class, () -> {
            new ClientAssertion(null);
        });

        assertEquals("assertion", exception.getMessage());


    }

    @Test
    void testAssertionEmpty() {
        NullPointerException exception = Assertions.assertThrows(NullPointerException.class, () -> {
            new ClientAssertion("");
        });

        assertEquals("assertion", exception.getMessage());
    }
}
