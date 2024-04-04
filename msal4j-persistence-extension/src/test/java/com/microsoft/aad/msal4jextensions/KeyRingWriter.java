// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

public class KeyRingWriter {

    public static void main(String[] args) throws Exception {

        String executionId = args[0];
        String lockFilePath = args[1];
        String filePath = args[2];
        String lockHoldingIntervalsFilePath = args[3];

        String schema = args[4];
        String label = args[5];
        String attribute1Key = args[6];
        String attribute1Value = args[7];
        String attribute2Key = args[8];
        String attribute2Value = args[9];

        try {
            KeyRingWriterRunnable keyRingWriterRunnable =
                    new KeyRingWriterRunnable(executionId,
                            lockFilePath, filePath, lockHoldingIntervalsFilePath,
                            schema,
                            label,
                            attribute1Key,
                            attribute1Value,
                            attribute2Key,
                            attribute2Value);

            keyRingWriterRunnable.run();
        }
        catch (Throwable e){
            System.out.println("executionId - " + executionId + ">>>>>>>>>>>>>>>>> KeyRingWriter FAILURE <<<<<<<<<<<<<<<<<<<<<<<<<");
            System.out.println(e.getMessage());
        }
    }
}
