// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import com.microsoft.aad.msal4jextensions.persistence.CacheFileAccessor;
import com.microsoft.aad.msal4jextensions.persistence.ICacheAccessor;
import com.microsoft.aad.msal4jextensions.persistence.linux.KeyRingAccessor;
import com.microsoft.aad.msal4jextensions.persistence.mac.KeyChainAccessor;
import com.sun.jna.Platform;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class CacheAccessorTest {

    String cacheFilePath;

    @Before
    public void init(){
        cacheFilePath = java.nio.file.Paths.get(System.getProperty("user.home"), "test.cache").toString();
    }

    @Test
    public void keyChainIOTest() throws IOException {
        if(!Platform.isMac()){
            return;
        }
        String keyChainAccount = "MSAL_Test_Account";
        String keyChainService = "MSAL_Test_Service";

        ICacheAccessor cacheAccessor = new KeyChainAccessor(cacheFilePath, keyChainService, keyChainAccount);

        readWriteTest(cacheAccessor);
    }

    @Test
    public void cacheFileIOTest() throws IOException {

        ICacheAccessor cacheAccessor = new CacheFileAccessor(cacheFilePath);

        readWriteTest(cacheAccessor);
    }

    @Test
    public void keyRingIOTest() throws IOException {
        if(!Platform.isLinux()){
            return;
        }
        // default collection
        String keyringCollection = null;
        String keyringSchemaName = "TestSchemaName";
        String keyringSecretLabel = "TestSecretLabel";
        String attributeKey1 = "TestAttributeKey1";
        String attributeValue1 = "TestAttributeValue1";
        String attributeKey2 = "TestAttributeKey2";
        String attributeValue2 = "TestAttributeValue2";

        ICacheAccessor cacheAccessor = new KeyRingAccessor(cacheFilePath,
                keyringCollection,
                keyringSchemaName,
                keyringSecretLabel,
                attributeKey1,
                attributeValue1,
                attributeKey2,
                attributeValue2);

        readWriteTest(cacheAccessor);
    }

    private void readWriteAssert(ICacheAccessor cacheAccessor, String data) throws IOException {
        cacheAccessor.write(data.getBytes());
        String receivedString = new String(cacheAccessor.read());

        Assert.assertEquals(receivedString, data);
    }


    private void readWriteTest(ICacheAccessor cacheAccessor) throws IOException {
        try {
            cacheAccessor.delete();

            readWriteAssert(cacheAccessor, "test data 1");
            readWriteAssert(cacheAccessor, "test data 2");
        } finally {
            cacheAccessor.delete();
        }
    }
}
