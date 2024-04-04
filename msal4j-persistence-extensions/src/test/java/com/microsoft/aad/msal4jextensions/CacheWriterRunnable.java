// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import com.microsoft.aad.msal4jextensions.persistence.ICacheAccessor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CacheWriterRunnable implements Runnable {
    ICacheAccessor cacheAccessor;

    CrossProcessCacheFileLock lock;

    String lockHoldingIntervalsFilePath;

    @Override
    public void run() {
        long start = 0;
        long end = 0;

        try {
            lock.lock();
            start = System.currentTimeMillis();

            String jvmName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            String id = jvmName + ":" + Thread.currentThread().getId();

            byte[] data = cacheAccessor.read();

            String strData = (data != null) ? new String(data, StandardCharsets.UTF_8) : "";
            strData += "< " + id + "\n";
            strData += "> " + id + "\n";

            // in average deserialize/serialize of token cache with 100 tokens takes 130 ms
            Thread.sleep(150);

            cacheAccessor.write(strData.getBytes(StandardCharsets.UTF_8));
            end = System.currentTimeMillis();
        } catch (Exception ex) {
            System.out.println("File write failure " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                lock.unlock();
                if(start > 0 && end > 0) {
                    try (FileOutputStream os = new FileOutputStream(lockHoldingIntervalsFilePath, true)) {
                        os.write((start + "-" + end + "\n").getBytes());
                    }
                }
            } catch (IOException e) {
                System.out.println("Failed to unlock");
                e.printStackTrace();
            }
        }
    }
}
