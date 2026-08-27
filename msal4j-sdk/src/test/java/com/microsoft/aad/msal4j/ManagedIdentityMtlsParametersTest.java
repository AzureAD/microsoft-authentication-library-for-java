// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ManagedIdentityMtlsParametersTest {

    @Test
    void attestationRequiresMtlsButMtlsCanBeRequestedAlone() {
        assertDoesNotThrow(
                () -> ManagedIdentityParameters.builder("https://vault.azure.net")
                        .withMtlsProofOfPossession()
                        .build());
        assertThrows(IllegalArgumentException.class,
                () -> ManagedIdentityParameters.builder("https://vault.azure.net")
                        .withAttestationSupport()
                        .build());
    }

    @Test
    void mtlsOptionsCarryMinimumBindingStrength() {
        ManagedIdentityParameters parameters = ManagedIdentityParameters
                .builder("https://vault.azure.net")
                .withMtlsProofOfPossession(
                        MtlsPopOptions.builder()
                                .minimumBindingStrength(
                                        MtlsBindingStrength.KEY_GUARD)
                                .build())
                .build();

        assertEquals(MtlsBindingStrength.KEY_GUARD,
                parameters.minimumBindingStrength());
    }

    @Test
    void bearerAndMtlsCachePartitionsCannotCollide() {
        ManagedIdentityParameters bearer =
                ManagedIdentityParameters.builder("https://vault.azure.net").build();
        ManagedIdentityParameters mtls = ManagedIdentityParameters
                .builder("https://vault.azure.net")
                .withMtlsProofOfPossession()
                .withAttestationSupport()
                .build();

        assertEquals("", bearer.computeExtCacheKeyHash());
        assertFalse(mtls.computeMtlsExtCacheKeyHash("certificate-a").isEmpty());
        assertNotEquals(bearer.computeExtCacheKeyHash(),
                mtls.computeMtlsExtCacheKeyHash("certificate-a"));
    }

    @Test
    void renewedCertificateCreatesNewCachePartition() {
        ManagedIdentityParameters parameters = ManagedIdentityParameters
                .builder("https://vault.azure.net")
                .withMtlsProofOfPossession()
                .withAttestationSupport()
                .build();
        String first = parameters.computeMtlsExtCacheKeyHash("certificate-a");

        assertNotEquals(first,
                parameters.computeMtlsExtCacheKeyHash("certificate-b"));
    }

    @Test
    void attestationModeCreatesDistinctCachePartition() {
        ManagedIdentityParameters unattested = ManagedIdentityParameters
                .builder("https://vault.azure.net")
                .withMtlsProofOfPossession()
                .build();
        ManagedIdentityParameters attested = ManagedIdentityParameters
                .builder("https://vault.azure.net")
                .withMtlsProofOfPossession()
                .withAttestationSupport()
                .build();

        assertNotEquals(
                unattested.computeMtlsExtCacheKeyHash("certificate-a"),
                attested.computeMtlsExtCacheKeyHash("certificate-a"));
    }

    @Test
    void minimumStrengthCreatesDistinctCachePartition() {
        ManagedIdentityParameters noFloor = ManagedIdentityParameters
                .builder("https://vault.azure.net")
                .withMtlsProofOfPossession()
                .build();
        ManagedIdentityParameters keyGuardFloor = ManagedIdentityParameters
                .builder("https://vault.azure.net")
                .withMtlsProofOfPossession(
                        MtlsPopOptions.builder()
                                .minimumBindingStrength(
                                        MtlsBindingStrength.KEY_GUARD)
                                .build())
                .build();

        assertNotEquals(
                noFloor.computeMtlsExtCacheKeyHash("certificate-a"),
                keyGuardFloor.computeMtlsExtCacheKeyHash("certificate-a"));
    }

    @Test
    void buildersDoNotExposeClaimsOrTokenMaterial() {
        ManagedIdentityParameters.ManagedIdentityParametersBuilder builder =
                ManagedIdentityParameters.builder("https://vault.azure.net")
                .claims("{\"access_token\":\"secret\"}")
                .withMtlsProofOfPossession()
                .withAttestationSupport();

        assertFalse(builder.toString().contains("secret"));
    }

    @Test
    void tokenEndpointMustExplicitlyReturnMtlsPop() {
        AuthenticationResult bearer = AuthenticationResult.builder()
                .accessToken("token")
                .tokenType("Bearer")
                .build();
        assertThrows(MsalServiceException.class,
                () -> AcquireTokenByManagedIdentitySupplier
                        .validateMtlsTokenResponse(bearer));

        AuthenticationResult missingTokenType = AuthenticationResult.builder()
                .accessToken("token")
                .build();
        assertThrows(MsalServiceException.class,
                () -> AcquireTokenByManagedIdentitySupplier
                        .validateMtlsTokenResponse(missingTokenType));

        AuthenticationResult mtlsPop = AuthenticationResult.builder()
                .accessToken("token")
                .tokenType("mtls_pop")
                .build();
        assertDoesNotThrow(() -> AcquireTokenByManagedIdentitySupplier
                .validateMtlsTokenResponse(mtlsPop));
    }
}
