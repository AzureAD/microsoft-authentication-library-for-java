// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.microsoft.aad.msal4j.ManagedIdentityParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManagedIdentityAttestationExtensionsTest {

    @Test
    void enablesAttestationOnMtlsPopBuilder() {
        ManagedIdentityParameters.ManagedIdentityParametersBuilder builder =
                ManagedIdentityParameters.builder("https://vault.azure.net")
                        .withMtlsProofOfPossession();

        assertSame(
                builder,
                ManagedIdentityAttestationExtensions
                        .withAttestationSupport(builder));
        assertNotNull(builder.build());
    }

    @Test
    void enablesAttestationOnBearerOverMtlsBuilder() {
        ManagedIdentityParameters.ManagedIdentityParametersBuilder builder =
                ManagedIdentityParameters.builder("https://vault.azure.net")
                        .withRequestOverMtls();

        assertSame(
                builder,
                ManagedIdentityAttestationExtensions
                        .withAttestationSupport(builder));
        assertNotNull(builder.build());
    }

    @Test
    void rejectsNullBuilder() {
        assertThrows(
                NullPointerException.class,
                () -> ManagedIdentityAttestationExtensions
                        .withAttestationSupport(null));
    }
}
