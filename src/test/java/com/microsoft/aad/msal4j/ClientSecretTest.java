// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.testng.annotations.Test;

/**
 *
 */
public class ClientSecretTest {

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "clientSecret is null or empty")
    public void testConstructorNullClientId() {
        new ClientSecret(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "clientSecret is null or empty")
    public void testConstructorEmptyClientId() {
        new ClientSecret("");
    }
}
