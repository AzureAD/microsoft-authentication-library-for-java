// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.win32.W32APIOptions;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

final class AuthenticodeVerifier {

    private static final int ERROR_SUCCESS = 0;
    private static final int WTD_UI_NONE = 2;
    private static final int WTD_REVOKE_NONE = 0;
    private static final int WTD_CHOICE_FILE = 1;
    private static final int WTD_STATEACTION_IGNORE = 0;

    private AuthenticodeVerifier() {
    }

    static void verify(Path file) {
        verify(file, WinTrustHolder.INSTANCE);
    }

    static void verify(Path file, WinTrustLibrary winTrust) {
        WinTrustFileInfo fileInfo = new WinTrustFileInfo(file);
        fileInfo.write();
        WinTrustData trustData = new WinTrustData(fileInfo);
        trustData.write();

        int result = winTrust.WinVerifyTrust(
                Pointer.NULL,
                new Guid(
                        0x00aac56b,
                        (short) 0xcd44,
                        (short) 0x11d0,
                        new byte[]{
                                (byte) 0x8c, (byte) 0xc2, 0x00, (byte) 0xc0,
                                0x4f, (byte) 0xc2, (byte) 0x95, (byte) 0xee
                        }),
                trustData);
        if (result != ERROR_SUCCESS) {
            throw new MtlsMsiException(String.format(
                    "The bundled attestation library failed Windows Authenticode "
                            + "verification (WinVerifyTrust=0x%08x).",
                    result));
        }
    }

    interface WinTrustLibrary extends Library {
        int WinVerifyTrust(
                Pointer windowHandle,
                Guid actionId,
                WinTrustData trustData);
    }

    private static final class WinTrustHolder {
        private static final WinTrustLibrary INSTANCE =
                Native.load(
                        "wintrust",
                        WinTrustLibrary.class,
                        W32APIOptions.UNICODE_OPTIONS);
    }

    public static final class Guid extends Structure {
        public int data1;
        public short data2;
        public short data3;
        public byte[] data4 = new byte[8];

        public Guid() {
        }

        Guid(int data1, short data2, short data3, byte[] data4) {
            this.data1 = data1;
            this.data2 = data2;
            this.data3 = data3;
            this.data4 = data4.clone();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("data1", "data2", "data3", "data4");
        }
    }

    public static final class WinTrustFileInfo extends Structure {
        public int cbStruct;
        public WString filePath;
        public Pointer fileHandle;
        public Pointer knownSubject;

        public WinTrustFileInfo() {
        }

        WinTrustFileInfo(Path path) {
            cbStruct = size();
            filePath = new WString(path.toAbsolutePath().toString());
            fileHandle = Pointer.NULL;
            knownSubject = Pointer.NULL;
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(
                    "cbStruct",
                    "filePath",
                    "fileHandle",
                    "knownSubject");
        }
    }

    public static final class WinTrustData extends Structure {
        public int cbStruct;
        public Pointer policyCallbackData;
        public Pointer sipClientData;
        public int uiChoice;
        public int revocationChecks;
        public int unionChoice;
        public Pointer fileInfo;
        public int stateAction;
        public Pointer stateData;
        public WString urlReference;
        public int providerFlags;
        public int uiContext;

        public WinTrustData() {
        }

        WinTrustData(WinTrustFileInfo file) {
            cbStruct = size();
            policyCallbackData = Pointer.NULL;
            sipClientData = Pointer.NULL;
            uiChoice = WTD_UI_NONE;
            revocationChecks = WTD_REVOKE_NONE;
            unionChoice = WTD_CHOICE_FILE;
            fileInfo = file.getPointer();
            stateAction = WTD_STATEACTION_IGNORE;
            stateData = Pointer.NULL;
            urlReference = null;
            providerFlags = 0;
            uiContext = 0;
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(
                    "cbStruct",
                    "policyCallbackData",
                    "sipClientData",
                    "uiChoice",
                    "revocationChecks",
                    "unionChoice",
                    "fileInfo",
                    "stateAction",
                    "stateData",
                    "urlReference",
                    "providerFlags",
                    "uiContext");
        }
    }
}
