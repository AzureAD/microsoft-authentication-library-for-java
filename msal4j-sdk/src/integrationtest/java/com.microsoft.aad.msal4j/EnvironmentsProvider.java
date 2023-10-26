// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.AzureEnvironment;

public class EnvironmentsProvider {
    public static Object[][] createData() {
        return new Object[][]{
                {AzureEnvironment.AZURE},
                {AzureEnvironment.AZURE_US_GOVERNMENT}};
    }
}
