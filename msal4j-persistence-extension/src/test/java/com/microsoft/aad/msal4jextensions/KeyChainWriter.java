// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

public class KeyChainWriter {

    public static void main(String[] args) throws Exception {
        String executionId = args[0];
        String lockFilePath = args[1];
        String filePath = args[2];
        String lockHoldingIntervalsFilePath = args[3];

        String serviceName = args[4];
        String accountName = args[5];

        try {
            KeyChainWriterRunnable keyChainWriterRunnable =
                    new KeyChainWriterRunnable(executionId, lockFilePath, filePath, lockHoldingIntervalsFilePath,
                            serviceName, accountName);

            keyChainWriterRunnable.run();
            System.out.println("executionId - " + executionId + " SUCCESS");
        }
        catch (Throwable e){
            System.out.println("executionId - " + executionId + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> FAILURE <<<<<<<<<<<<<<<<<<<<<<<<<");
            System.out.println(e.getMessage());
        }
    }
}
