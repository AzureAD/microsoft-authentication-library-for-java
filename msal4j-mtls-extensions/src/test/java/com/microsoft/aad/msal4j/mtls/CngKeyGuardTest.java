// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.PointerByReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CngKeyGuardTest {

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
