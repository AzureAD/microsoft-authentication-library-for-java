// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import com.microsoft.aad.msal4jextensions.persistence.mac.KeyChainAccessor;
import com.sun.jna.Platform;
import org.junit.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class CacheLockKeyChainStorageTest extends CacheLockTestBase {

    public static final String SERVICE = "testMsalService";
    public static final String ACCOUNT = "testMsalAccount";

    KeyChainAccessor keyChainAccessor =
            new KeyChainAccessor(testFilePath, SERVICE, ACCOUNT);

    class KeyChainWriterRunnableFactory implements IRunnableFactory {
        @Override
        public Runnable create(String id) {
            return new KeyChainWriterRunnable
                    (id, lockFilePath, testFilePath, lockHoldingIntervalsFilePath,
                            SERVICE,
                            ACCOUNT);
        }
    }

    //@Test
    public void multipleThreadsWriting_KeyChain() throws IOException, InterruptedException {
        int numOfThreads = 100;

        multipleThreadsWriting(keyChainAccessor, numOfThreads, new KeyChainWriterRunnableFactory());
    }


    //@Test
    public void multipleProcessesWriting_KeyChain() throws IOException, InterruptedException {
        int numOfProcesses = 10;

        String writerClass = com.microsoft.aad.msal4jextensions.KeyChainWriter.class.getName();

        String writerClassArgs = lockFilePath + " " +
                testFilePath + " " +
                lockHoldingIntervalsFilePath + " " +
                SERVICE + " " +
                ACCOUNT;

        multipleProcessesWriting(keyChainAccessor, numOfProcesses, writerClass, writerClassArgs);
    }
}
