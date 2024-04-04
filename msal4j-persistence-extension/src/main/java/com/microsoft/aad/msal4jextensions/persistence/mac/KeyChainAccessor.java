// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence.mac;

import com.microsoft.aad.msal4jextensions.persistence.CacheFileAccessor;
import com.microsoft.aad.msal4jextensions.persistence.ICacheAccessor;
import com.sun.jna.Pointer;

import java.nio.charset.StandardCharsets;

/**
 * Implementation of CacheAccessor based on KeyChain for Mac
 */
public class KeyChainAccessor implements ICacheAccessor {
    private String cacheFilePath;
    private byte[] serviceNameBytes;
    private byte[] accountNameBytes;

    public KeyChainAccessor(String cacheFilePath, String serviceName, String accountName) {
        this.cacheFilePath = cacheFilePath;
        serviceNameBytes = serviceName.getBytes(StandardCharsets.UTF_8);
        accountNameBytes = accountName.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] read() {
        int[] dataLength = new int[1];
        Pointer[] data = new Pointer[1];

        try {
            int status = ISecurityLibrary.library.SecKeychainFindGenericPassword
                    (null,
                            serviceNameBytes.length, serviceNameBytes,
                            accountNameBytes.length, accountNameBytes,
                            dataLength, data,
                            null);

            if (status == ISecurityLibrary.ERR_SEC_ITEM_NOT_FOUND) {
                return null;
            }

            if (status != ISecurityLibrary.ERR_SEC_SUCCESS) {
                throw new KeyChainAccessException(convertErrorCodeToMessage(status));
            }

            return data[0].getByteArray(0, dataLength[0]);
        } finally {
            ISecurityLibrary.library.SecKeychainItemFreeContent(null, data[0]);
        }
    }

    private int writeNoRetry(byte[] data) {
        Pointer[] itemRef = new Pointer[1];
        int status;

        try {
            status = ISecurityLibrary.library.SecKeychainFindGenericPassword(
                    null,
                    serviceNameBytes.length, serviceNameBytes,
                    accountNameBytes.length, accountNameBytes,
                    null, null, itemRef);

            if (status == ISecurityLibrary.ERR_SEC_SUCCESS && itemRef[0] != null) {

                status = ISecurityLibrary.library.SecKeychainItemModifyContent(
                        itemRef[0], null, data.length, data);

            } else if (status == ISecurityLibrary.ERR_SEC_ITEM_NOT_FOUND) {
                status = ISecurityLibrary.library.SecKeychainAddGenericPassword(
                        null,
                        serviceNameBytes.length, serviceNameBytes,
                        accountNameBytes.length, accountNameBytes,
                        data.length, data, null);
            } else {
                throw new KeyChainAccessException(convertErrorCodeToMessage(status));
            }

        } finally {
            if (itemRef[0] != null) {
                ISecurityLibrary.library.CFRelease(itemRef[0]);
            }
        }
        return status;
    }

    @Override
    public void write(byte[] data) {
        int NUM_OF_RETRIES = 3;
        int RETRY_DELAY_IN_MS = 10;
        int status = 0;

        for (int i = 0; i < NUM_OF_RETRIES; i++) {
            status = writeNoRetry(data);

            if (status == ISecurityLibrary.ERR_SEC_SUCCESS) {
                new CacheFileAccessor(cacheFilePath).updateCacheFileLastModifiedTime();
                return;
            }
            try {
                Thread.sleep(RETRY_DELAY_IN_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        throw new KeyChainAccessException(convertErrorCodeToMessage(status));
    }

    @Override
    public void delete() {
        Pointer[] itemRef = new Pointer[1];
        try {
            int status = ISecurityLibrary.library.SecKeychainFindGenericPassword(
                    null,
                    serviceNameBytes.length, serviceNameBytes,
                    accountNameBytes.length, accountNameBytes,
                    null, null,
                    itemRef);

            if (status == ISecurityLibrary.ERR_SEC_ITEM_NOT_FOUND) {
                return;
            }

            if (status != ISecurityLibrary.ERR_SEC_SUCCESS) {
                throw new KeyChainAccessException(convertErrorCodeToMessage(status));
            }

            if (itemRef[0] != null) {
                status = ISecurityLibrary.library.SecKeychainItemDelete(itemRef[0]);

                if (status != ISecurityLibrary.ERR_SEC_SUCCESS) {
                    throw new KeyChainAccessException(convertErrorCodeToMessage(status));
                }
            }
            new CacheFileAccessor(cacheFilePath).updateCacheFileLastModifiedTime();
        } finally {
            if (itemRef[0] != null) {
                ISecurityLibrary.library.CFRelease(itemRef[0]);
            }
        }
    }

    private String convertErrorCodeToMessage(int errorCode) {
        Pointer msgPtr = null;
        try {
            msgPtr = ISecurityLibrary.library.SecCopyErrorMessageString(errorCode, null);
            if (msgPtr == null) {
                return null;
            }

            int bufSize = ISecurityLibrary.library.CFStringGetLength(msgPtr);
            char[] buf = new char[bufSize];

            for (int i = 0; i < buf.length; i++) {
                buf[i] = ISecurityLibrary.library.CFStringGetCharacterAtIndex(msgPtr, i);
            }
            return new String(buf);
        } finally {
            if (msgPtr != null) {
                ISecurityLibrary.library.CFRelease(msgPtr);
            }
        }
    }
}
