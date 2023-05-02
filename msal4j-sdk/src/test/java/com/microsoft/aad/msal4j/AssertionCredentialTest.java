// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.testng.annotations.Test;

@Test(groups = {"checkin"})
public class AssertionCredentialTest {

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "assertion")
    public void testAssertionNull() {
        new ClientAssertion(null);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "assertion")
    public void testAssertionEmpty() {
        new ClientAssertion("");
    }
}
