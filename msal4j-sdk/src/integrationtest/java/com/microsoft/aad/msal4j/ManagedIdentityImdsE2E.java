// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ManagedIdentityImdsE2E {

    private static final String ARM_RESOURCE = "https://management.azure.com";
    private static final String USER_ASSIGNED_CLIENT_ID =
            "6325cd32-9911-41f3-819c-416cdf9104e7";
    private static final String USER_ASSIGNED_RESOURCE_ID =
            "/subscriptions/c1686c51-b717-4fe0-9af3-24a20a41fb0c/"
                    + "resourcegroups/MSIV2-Testing-MSALNET/providers/"
                    + "Microsoft.ManagedIdentity/userAssignedIdentities/msiv2uami";
    private static final String USER_ASSIGNED_OBJECT_ID =
            "ecb2ad92-3e30-4505-b79f-ac640d069f24";

    @BeforeAll
    static void requireLiveManagedIdentityEnvironment() {
        assumeTrue(
                Boolean.getBoolean("managedIdentity.e2e.enabled"),
                "Live Managed Identity E2E execution is not enabled.");
    }

    @Test
    void systemAssignedIdentityAcquiresAndCachesToken() throws Exception {
        assertTokenAcquisition(ManagedIdentityId.systemAssigned());
    }

    @Test
    void userAssignedClientIdAcquiresAndCachesToken() throws Exception {
        assertTokenAcquisition(
                ManagedIdentityId.userAssignedClientId(USER_ASSIGNED_CLIENT_ID));
    }

    @Test
    void userAssignedResourceIdAcquiresAndCachesToken() throws Exception {
        assertTokenAcquisition(
                ManagedIdentityId.userAssignedResourceId(USER_ASSIGNED_RESOURCE_ID));
    }

    @Test
    void userAssignedObjectIdAcquiresAndCachesToken() throws Exception {
        assertTokenAcquisition(
                ManagedIdentityId.userAssignedObjectId(USER_ASSIGNED_OBJECT_ID));
    }

    private static void assertTokenAcquisition(ManagedIdentityId identity)
            throws Exception {
        ManagedIdentityApplication application =
                ManagedIdentityApplication.builder(identity).build();
        ManagedIdentityParameters parameters =
                ManagedIdentityParameters.builder(ARM_RESOURCE).build();

        IAuthenticationResult first =
                application.acquireTokenForManagedIdentity(parameters).get();
        assertValidResult(first);
        assertEquals(TokenSource.IDENTITY_PROVIDER, first.metadata().tokenSource());

        IAuthenticationResult cached =
                application.acquireTokenForManagedIdentity(parameters).get();
        assertValidResult(cached);
        assertEquals(TokenSource.CACHE, cached.metadata().tokenSource());
        assertEquals(first.accessToken(), cached.accessToken());
    }

    private static void assertValidResult(IAuthenticationResult result) {
        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertFalse(result.accessToken().isEmpty());
    }
}
