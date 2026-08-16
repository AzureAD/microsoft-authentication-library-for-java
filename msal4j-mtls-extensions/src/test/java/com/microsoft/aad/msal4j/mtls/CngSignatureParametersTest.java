// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

import static org.junit.jupiter.api.Assertions.*;

class CngSignatureParametersTest {

    @Test
    void pssAcceptsSupportedTlsParameters() throws Exception {
        CngSignatureSpi spi = new CngSignatureSpi.RsaSsaPss();

        spi.engineSetParameter(new PSSParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
        spi.engineSetParameter(new PSSParameterSpec(
                "SHA-384", "MGF1", MGF1ParameterSpec.SHA384, 48, 1));
        spi.engineSetParameter(new PSSParameterSpec(
                "SHA-512", "MGF1", MGF1ParameterSpec.SHA512, 64, 1));
    }

    @Test
    void pssRejectsMismatchedMgfDigest() {
        CngSignatureSpi spi = new CngSignatureSpi.RsaSsaPss();

        assertThrows(InvalidAlgorithmParameterException.class,
                () -> spi.engineSetParameter(new PSSParameterSpec(
                        "SHA-256", "MGF1", MGF1ParameterSpec.SHA384, 32, 1)));
    }

    @Test
    void pssRejectsUnexpectedSaltLength() {
        CngSignatureSpi spi = new CngSignatureSpi.RsaSsaPss();

        assertThrows(InvalidAlgorithmParameterException.class,
                () -> spi.engineSetParameter(new PSSParameterSpec(
                        "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 20, 1)));
    }

    @Test
    void pssRejectsUnsupportedDigestInsteadOfFallingBack() {
        CngSignatureSpi spi = new CngSignatureSpi.RsaSsaPss();

        assertThrows(InvalidAlgorithmParameterException.class,
                () -> spi.engineSetParameter(new PSSParameterSpec(
                        "SHA-1", "MGF1", MGF1ParameterSpec.SHA1, 20, 1)));
    }
}
