// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import com.microsoft.aad.msal4jextensions.persistence.linux.KeyRingAccessor;
import org.junit.Test;

import java.io.IOException;

public class CacheLockKeyRingStorageTest extends CacheLockTestBase {

    String SCHEMA = "testMsalSchema";
    String LABEL = "testMsalLabel";
    String ATTRIBUTE1_KEY = "testMsalAttribute1Key";
    String ATTRIBUTE1_VALUE = "testMsalAttribute1Value";
    String ATTRIBUTE2_KEY = "testMsalAttribute2Key";
    String ATTRIBUTE2_VALUE = "testMsalAttribute2Value";

    KeyRingAccessor keyRingAccessor = new KeyRingAccessor(testFilePath,
            null,
            SCHEMA,
            LABEL,
            ATTRIBUTE1_KEY,
            ATTRIBUTE1_VALUE,
            ATTRIBUTE2_KEY,
            ATTRIBUTE2_VALUE);

    class KeyRingWriterRunnableFactory implements IRunnableFactory {
        @Override
        public Runnable create(String id) {
            return new KeyRingWriterRunnable
                    (id, lockFilePath, testFilePath, lockHoldingIntervalsFilePath,
                            SCHEMA,
                            LABEL,
                            ATTRIBUTE1_KEY,
                            ATTRIBUTE1_VALUE,
                            ATTRIBUTE2_KEY,
                            ATTRIBUTE2_VALUE);
        }
    }

    //@Test
    public void multipleThreadsWriting_KeyRing() throws IOException, InterruptedException {
        int numOfThreads = 100;

        multipleThreadsWriting(keyRingAccessor, numOfThreads, new KeyRingWriterRunnableFactory());
    }

    //@Test
    public void multipleProcessesWriting_KeyRing() throws IOException, InterruptedException {
        int numOfProcesses = 20;

        String writerClass = com.microsoft.aad.msal4jextensions.KeyRingWriter.class.getName();

        String writerClassArgs = lockFilePath + " " +
                testFilePath + " " +
                lockHoldingIntervalsFilePath+ " " +
                SCHEMA + " " +
                LABEL + " " +
                ATTRIBUTE1_KEY + " " +
                ATTRIBUTE1_VALUE + " " +
                ATTRIBUTE2_KEY + " " +
                ATTRIBUTE2_VALUE;

        multipleProcessesWriting(keyRingAccessor, numOfProcesses, writerClass, writerClassArgs);
    }
}
