// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.AzureEnvironment;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public class ManagedIdentityTestDataProvider {

    public static Stream<Arguments> createData() {
        return Stream.of(Arguments.of(ManagedIdentitySourceType.AppService, ManagedIdentityTests.appServiceEndpoint, ManagedIdentityTests.resource),
                Arguments.of(ManagedIdentitySourceType.Imds, ManagedIdentityTests.IMDS_ENDPOINT, ManagedIdentityTests.resource));
    }
}
