// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls.e2e;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManagedIdentityMtlsPopKeyVaultDevAppTest {

    @Test
    void bindingControlRequiresUnauthorizedRatherThanAnyFailedRequest() {
        String unauthorized = "{\"error\":{\"code\":\"Unauthorized\"}}";
        assertDoesNotThrow(() -> ManagedIdentityMtlsPopKeyVaultDevApp.verifyBindingRejected(
                new ManagedIdentityMtlsPopKeyVaultDevApp.KeyVaultResponse(401, unauthorized),
                "with a different binding certificate"));

        for (int status : new int[]{200, 302, 403, 429, 500, 503}) {
            assertThrows(IllegalStateException.class,
                    () -> ManagedIdentityMtlsPopKeyVaultDevApp.verifyBindingRejected(
                            new ManagedIdentityMtlsPopKeyVaultDevApp.KeyVaultResponse(
                                    status, unauthorized),
                            "with a different binding certificate"));
        }
        for (String body : new String[]{null, "", "{}", "{\"error\":{\"code\":\"AccessDenied\"}}"}) {
            assertThrows(IllegalStateException.class,
                    () -> ManagedIdentityMtlsPopKeyVaultDevApp.verifyBindingRejected(
                            new ManagedIdentityMtlsPopKeyVaultDevApp.KeyVaultResponse(401, body),
                            "without its binding certificate"));
        }
    }
}
