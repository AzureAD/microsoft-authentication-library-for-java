// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ManagedIdentityAzureArcE2E {

    private static final String ARM_RESOURCE = "https://management.azure.com";
    private static final String UNASSIGNED_CLIENT_ID =
            "00000000-0000-0000-0000-000000000001";

    @BeforeAll
    static void requireLiveManagedIdentityEnvironment() {
        assumeTrue(
                Boolean.getBoolean("managedIdentity.e2e.enabled"),
                "Live Managed Identity E2E execution is not enabled.");
    }

    @Test
    void systemAssignedIdentityAcquiresAndCachesToken() throws Exception {
        ManagedIdentityApplication application = ManagedIdentityApplication.builder(
                ManagedIdentityId.systemAssigned()).build();
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

    @Test
    void unassignedUserAssignedIdentityFailsClosed() {
        ManagedIdentityApplication application = ManagedIdentityApplication.builder(
                ManagedIdentityId.userAssignedClientId(UNASSIGNED_CLIENT_ID)).build();
        ManagedIdentityParameters parameters =
                ManagedIdentityParameters.builder(ARM_RESOURCE).build();

        ExecutionException executionException = assertThrows(
                ExecutionException.class,
                () -> application.acquireTokenForManagedIdentity(parameters).get());
        assertTrue(executionException.getCause() instanceof MsalServiceException);

        MsalServiceException serviceException =
                (MsalServiceException) executionException.getCause();
        assertEquals(
                MsalError.MANAGED_IDENTITY_REQUEST_FAILED,
                serviceException.errorCode());
        assertEquals(
                ManagedIdentitySourceType.AZURE_ARC.name(),
                serviceException.managedIdentitySource());
        assertTrue(
                serviceException.getMessage().contains("identity_not_found")
                        || serviceException.getMessage().contains("Identity not found"));
    }

    private static void assertValidResult(IAuthenticationResult result) {
        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertFalse(result.accessToken().isEmpty());
    }
}
