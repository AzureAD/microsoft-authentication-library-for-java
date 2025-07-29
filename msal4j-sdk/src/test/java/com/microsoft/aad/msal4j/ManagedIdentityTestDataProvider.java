// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

class ManagedIdentityTestDataProvider {

    static Stream<Arguments> createData() {
        return Stream.of(
                Arguments.of(ManagedIdentitySourceType.APP_SERVICE, ManagedIdentityTestConstants.APP_SERVICE_ENDPOINT,
                        ManagedIdentityTestConstants.RESOURCE),
                Arguments.of(ManagedIdentitySourceType.APP_SERVICE, ManagedIdentityTestConstants.APP_SERVICE_ENDPOINT,
                        ManagedIdentityTestConstants.RESOURCE_DEFAULT_SUFFIX),
                Arguments.of(ManagedIdentitySourceType.CLOUD_SHELL, ManagedIdentityTestConstants.CLOUDSHELL_ENDPOINT,
                        ManagedIdentityTestConstants.RESOURCE),
                Arguments.of(ManagedIdentitySourceType.CLOUD_SHELL, ManagedIdentityTestConstants.CLOUDSHELL_ENDPOINT,
                        ManagedIdentityTestConstants.RESOURCE_DEFAULT_SUFFIX),
                Arguments.of(ManagedIdentitySourceType.AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT,
                        ManagedIdentityTestConstants.RESOURCE),
                Arguments.of(ManagedIdentitySourceType.AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT,
                        ManagedIdentityTestConstants.RESOURCE_DEFAULT_SUFFIX),
                Arguments.of(ManagedIdentitySourceType.IMDS, ManagedIdentityTestConstants.IMDS_ENDPOINT,
                        ManagedIdentityTestConstants.RESOURCE),
                Arguments.of(ManagedIdentitySourceType.IMDS, ManagedIdentityTestConstants.IMDS_ENDPOINT,
                        ManagedIdentityTestConstants.RESOURCE_DEFAULT_SUFFIX),
                Arguments.of(ManagedIdentitySourceType.IMDS, null,
                        ManagedIdentityTestConstants.RESOURCE),
                Arguments.of(ManagedIdentitySourceType.SERVICE_FABRIC, ManagedIdentityTestConstants.SERVICE_FABRIC_ENDPOINT,
                        ManagedIdentityTestConstants.RESOURCE),
                Arguments.of(ManagedIdentitySourceType.SERVICE_FABRIC, ManagedIdentityTestConstants.SERVICE_FABRIC_ENDPOINT,
                        ManagedIdentityTestConstants.RESOURCE_DEFAULT_SUFFIX));
    }

    static Stream<Arguments> createDataUserAssigned() {
        return Stream.of(
                Arguments.of(ManagedIdentitySourceType.APP_SERVICE, ManagedIdentityTestConstants.APP_SERVICE_ENDPOINT,
                        ManagedIdentityId.userAssignedClientId(ManagedIdentityTestConstants.CLIENT_ID)),
                Arguments.of(ManagedIdentitySourceType.APP_SERVICE, ManagedIdentityTestConstants.APP_SERVICE_ENDPOINT,
                        ManagedIdentityId.userAssignedResourceId(ManagedIdentityTestConstants.RESOURCE_ID)),
                Arguments.of(ManagedIdentitySourceType.APP_SERVICE, ManagedIdentityTestConstants.APP_SERVICE_ENDPOINT,
                        ManagedIdentityId.userAssignedObjectId(ManagedIdentityTestConstants.OBJECT_ID)),
                Arguments.of(ManagedIdentitySourceType.IMDS, null,
                        ManagedIdentityId.userAssignedClientId(ManagedIdentityTestConstants.CLIENT_ID)),
                Arguments.of(ManagedIdentitySourceType.IMDS, null,
                        ManagedIdentityId.userAssignedResourceId(ManagedIdentityTestConstants.RESOURCE_ID)),
                Arguments.of(ManagedIdentitySourceType.IMDS, null,
                        ManagedIdentityId.userAssignedObjectId(ManagedIdentityTestConstants.OBJECT_ID)),
                Arguments.of(ManagedIdentitySourceType.SERVICE_FABRIC, ManagedIdentityTestConstants.SERVICE_FABRIC_ENDPOINT,
                        ManagedIdentityId.userAssignedResourceId(ManagedIdentityTestConstants.CLIENT_ID)),
                Arguments.of(ManagedIdentitySourceType.SERVICE_FABRIC, ManagedIdentityTestConstants.SERVICE_FABRIC_ENDPOINT,
                        ManagedIdentityId.userAssignedResourceId(ManagedIdentityTestConstants.RESOURCE_ID)),
                Arguments.of(ManagedIdentitySourceType.SERVICE_FABRIC, ManagedIdentityTestConstants.SERVICE_FABRIC_ENDPOINT,
                        ManagedIdentityId.userAssignedObjectId(ManagedIdentityTestConstants.OBJECT_ID)));
    }

    static Stream<Arguments> createDataUserAssignedNotSupported() {
        return Stream.of(
                Arguments.of(ManagedIdentitySourceType.CLOUD_SHELL, ManagedIdentityTestConstants.CLOUDSHELL_ENDPOINT,
                        ManagedIdentityId.userAssignedClientId(ManagedIdentityTestConstants.CLIENT_ID)),
                Arguments.of(ManagedIdentitySourceType.CLOUD_SHELL, ManagedIdentityTestConstants.CLOUDSHELL_ENDPOINT,
                        ManagedIdentityId.userAssignedResourceId(ManagedIdentityTestConstants.RESOURCE_ID)),
                Arguments.of(ManagedIdentitySourceType.AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT,
                        ManagedIdentityId.userAssignedClientId(ManagedIdentityTestConstants.CLIENT_ID)),
                Arguments.of(ManagedIdentitySourceType.AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT,
                        ManagedIdentityId.userAssignedResourceId(ManagedIdentityTestConstants.RESOURCE_ID)));
    }

    static Stream<Arguments> createDataWrongScope() {
        return Stream.of(
                Arguments.of(ManagedIdentitySourceType.APP_SERVICE, ManagedIdentityTestConstants.APP_SERVICE_ENDPOINT,
                        "user.read"),
                Arguments.of(ManagedIdentitySourceType.APP_SERVICE, ManagedIdentityTestConstants.APP_SERVICE_ENDPOINT,
                        "https://management.core.windows.net//user_impersonation"),
                Arguments.of(ManagedIdentitySourceType.CLOUD_SHELL, ManagedIdentityTestConstants.CLOUDSHELL_ENDPOINT,
                        "user.read"),
                Arguments.of(ManagedIdentitySourceType.CLOUD_SHELL, ManagedIdentityTestConstants.CLOUDSHELL_ENDPOINT,
                        "https://management.core.windows.net//user_impersonation"),
                Arguments.of(ManagedIdentitySourceType.AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT,
                        "user.read"),
                Arguments.of(ManagedIdentitySourceType.AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT,
                        "https://management.core.windows.net//user_impersonation"),
                Arguments.of(ManagedIdentitySourceType.IMDS, ManagedIdentityTestConstants.IMDS_ENDPOINT,
                        "user.read"),
                Arguments.of(ManagedIdentitySourceType.IMDS, ManagedIdentityTestConstants.IMDS_ENDPOINT,
                        "https://management.core.windows.net//user_impersonation"),
                Arguments.of(ManagedIdentitySourceType.SERVICE_FABRIC, ManagedIdentityTestConstants.SERVICE_FABRIC_ENDPOINT,
                        "user.read"),
                Arguments.of(ManagedIdentitySourceType.SERVICE_FABRIC, ManagedIdentityTestConstants.SERVICE_FABRIC_ENDPOINT,
                        "https://management.core.windows.net//user_impersonation"));
    }

    static Stream<Arguments> createDataError() {
        return Stream.of(
                Arguments.of(ManagedIdentitySourceType.AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT),
                Arguments.of(ManagedIdentitySourceType.APP_SERVICE, ManagedIdentityTestConstants.APP_SERVICE_ENDPOINT),
                Arguments.of(ManagedIdentitySourceType.CLOUD_SHELL, ManagedIdentityTestConstants.CLOUDSHELL_ENDPOINT),
                Arguments.of(ManagedIdentitySourceType.IMDS, ManagedIdentityTestConstants.IMDS_ENDPOINT),
                Arguments.of(ManagedIdentitySourceType.SERVICE_FABRIC, ManagedIdentityTestConstants.SERVICE_FABRIC_ENDPOINT));
    }

    static Stream<Arguments> createDataGetSource() {
        return Stream.of(
                Arguments.of(ManagedIdentitySourceType.AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT, ManagedIdentitySourceType.AZURE_ARC),
                Arguments.of(ManagedIdentitySourceType.APP_SERVICE, ManagedIdentityTestConstants.APP_SERVICE_ENDPOINT, ManagedIdentitySourceType.APP_SERVICE),
                Arguments.of(ManagedIdentitySourceType.CLOUD_SHELL, ManagedIdentityTestConstants.CLOUDSHELL_ENDPOINT, ManagedIdentitySourceType.CLOUD_SHELL),
                Arguments.of(ManagedIdentitySourceType.IMDS, ManagedIdentityTestConstants.IMDS_ENDPOINT, ManagedIdentitySourceType.DEFAULT_TO_IMDS),
                Arguments.of(ManagedIdentitySourceType.IMDS, "", ManagedIdentitySourceType.DEFAULT_TO_IMDS),
                Arguments.of(ManagedIdentitySourceType.SERVICE_FABRIC, ManagedIdentityTestConstants.SERVICE_FABRIC_ENDPOINT, ManagedIdentitySourceType.SERVICE_FABRIC));
    }

    static Stream<Arguments> createInvalidClaimsData() {
        return Stream.of(
                Arguments.of("invalid json format"),
                Arguments.of("{\"access_token\": }")
        );
    }
}
