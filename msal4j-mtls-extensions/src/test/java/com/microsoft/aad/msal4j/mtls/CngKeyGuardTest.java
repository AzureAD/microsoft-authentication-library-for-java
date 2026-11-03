// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.PointerByReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CngKeyGuardTest {

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void nativePaddingStructuresProduceVerifiablePkcs1AndPssSignatures() throws Exception {
        NCryptLibrary nativeApi = NCryptLibrary.INSTANCE;
        PointerByReference provider = new PointerByReference();
        PointerByReference key = new PointerByReference();
        assertEquals(0, nativeApi.NCryptOpenStorageProvider(
                provider, new WString("Microsoft Software Key Storage Provider"), 0));
        try {
            assertEquals(0, nativeApi.NCryptCreatePersistedKey(
                    provider.getValue(), key, new WString("RSA"), null, 0, 0));
            assertEquals(0, nativeApi.NCryptFinalizeKey(key.getValue(), 0));
            BigInteger[] publicComponents = CngKeyGuard.exportPublicKey(key.getValue());
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(
                    new RSAPublicKeySpec(publicComponents[0], publicComponents[1]));
            byte[] message = "native padding lifetime".getBytes(StandardCharsets.UTF_8);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(message);

            Signature pkcs1 = Signature.getInstance("SHA256withRSA");
            pkcs1.initVerify(publicKey);
            pkcs1.update(message);
            assertTrue(pkcs1.verify(CngKeyGuard.signPkcs1(key.getValue(), digest, "SHA256")));

            Signature pss = Signature.getInstance("RSASSA-PSS");
            pss.setParameter(new PSSParameterSpec("SHA-256", "MGF1",
                    MGF1ParameterSpec.SHA256, 32, 1));
            pss.initVerify(publicKey);
            pss.update(message);
            assertTrue(pss.verify(CngKeyGuard.signPss(key.getValue(), digest, "SHA256", 32)));
        } finally {
            if (key.getValue() != null) {
                nativeApi.NCryptFreeObject(key.getValue());
            }
            nativeApi.NCryptFreeObject(provider.getValue());
        }
    }

    @Test
    void staleOpenedKeyIsDeletedSoCallerRecreatesIt() {
        NCryptLibrary nativeApi = mock(NCryptLibrary.class);
        Pointer provider = Pointer.createConstant(11);
        Pointer staleKey = Pointer.createConstant(42);
        when(nativeApi.NCryptOpenKey(
                any(),
                any(),
                any(),
                anyInt(),
                anyInt()))
                .thenAnswer(invocation -> {
                    PointerByReference reference = invocation.getArgument(1);
                    reference.setValue(staleKey);
                    return NCryptLibrary.ERROR_SUCCESS;
                });

        Pointer opened = CngKeyGuard.openExistingUsableKey(
                provider,
                new WString("stale-key"),
                NCryptLibrary.NCRYPT_SILENT_FLAG,
                nativeApi,
                key -> {
                    // Public export may have succeeded before this private-operation failure.
                    throw new MtlsMsiException("NCryptSignHash failed");
                });

        assertNull(opened);
        verify(nativeApi).NCryptDeleteKey(staleKey, 0);
    }
}
