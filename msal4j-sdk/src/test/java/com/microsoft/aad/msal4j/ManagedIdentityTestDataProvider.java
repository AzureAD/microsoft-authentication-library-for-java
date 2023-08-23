// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.AzureEnvironment;
import org.junit.jupiter.params.provider.Arguments;
import reactor.netty.transport.ClientTransport;

import java.util.stream.Stream;

public class ManagedIdentityTestDataProvider {
    private static final String CLIENT_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String RESOURCE_ID = "/subscriptions/ffa4aaa2-4444-4444-5555-e3ccedd3d046/resourcegroups/UAMI_group/providers/Microsoft.ManagedIdentityClient/userAssignedIdentities/UAMI";

    public static Stream<Arguments> createData() {
        return Stream.of(
                Arguments.of(ManagedIdentitySourceType.AppService, ManagedIdentityTests.appServiceEndpoint,
                        ManagedIdentityTests.resource),
                Arguments.of(ManagedIdentitySourceType.AppService, ManagedIdentityTests.appServiceEndpoint,
                        ManagedIdentityTests.resourceDefaultSuffix),
                Arguments.of(ManagedIdentitySourceType.Imds, ManagedIdentityTests.IMDS_ENDPOINT,
                        ManagedIdentityTests.resource),
                Arguments.of(ManagedIdentitySourceType.Imds, ManagedIdentityTests.IMDS_ENDPOINT,
                        ManagedIdentityTests.resourceDefaultSuffix),
                Arguments.of(ManagedIdentitySourceType.Imds, null,
                        ManagedIdentityTests.resource));
    }

    public static Stream<Arguments> createDataUserAssigned() {
        return Stream.of(
                Arguments.of(ManagedIdentitySourceType.AppService, ManagedIdentityTests.appServiceEndpoint,
                        ManagedIdentityId.UserAssignedClientId(CLIENT_ID)),
                Arguments.of(ManagedIdentitySourceType.AppService, ManagedIdentityTests.appServiceEndpoint,
                        ManagedIdentityId.UserAssignedResourceId(RESOURCE_ID)),
                Arguments.of(ManagedIdentitySourceType.Imds, null,
                        ManagedIdentityId.UserAssignedClientId(CLIENT_ID)),
                Arguments.of(ManagedIdentitySourceType.Imds, null,
                        ManagedIdentityId.UserAssignedResourceId(RESOURCE_ID)));
    }

    public static Stream<Arguments> createDataWrongScope() {
        return Stream.of(
                Arguments.of(ManagedIdentitySourceType.AppService, ManagedIdentityTests.appServiceEndpoint,
                        "user.read"),
                Arguments.of(ManagedIdentitySourceType.AppService, ManagedIdentityTests.appServiceEndpoint,
                        "https://management.core.windows.net//user_impersonation"),
                Arguments.of(ManagedIdentitySourceType.Imds, ManagedIdentityTests.IMDS_ENDPOINT,
                        "user.read"),
                Arguments.of(ManagedIdentitySourceType.Imds, ManagedIdentityTests.IMDS_ENDPOINT,
                        "https://management.core.windows.net//user_impersonation"));
    }

    public static Stream<Arguments> createDataError() {
        return Stream.of(
                Arguments.of(ManagedIdentitySourceType.AppService, ManagedIdentityTests.appServiceEndpoint),
                Arguments.of(ManagedIdentitySourceType.Imds, ManagedIdentityTests.IMDS_ENDPOINT));
    }
}
