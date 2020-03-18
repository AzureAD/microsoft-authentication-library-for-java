// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.AzureEnvironment;
import org.testng.annotations.DataProvider;

public class EnvironmentsProvider {
    @DataProvider(name = "environments")
    public static Object[][] createData() {
        return new Object[][] {
                { AzureEnvironment.AZURE },
                { AzureEnvironment.AZURE_US_GOVERNMENT }};
    }
}
