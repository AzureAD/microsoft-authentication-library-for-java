// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cross process lock based on lock file creation/deletion (optional) + OS level file lock.
 */
class CrossProcessCacheFileLock {
    private final static Logger LOG = LoggerFactory.getLogger(CrossProcessCacheFileLock.class);

    private int retryDelayMilliseconds;
    private int retryNumber;

    private File lockFile;

    private FileLock lock;

    private FileChannel fileChannel;

    private boolean locked;

    /**
     * Constructor
     *
     * @param lockfileName           Path of the lock file
     * @param retryDelayMilliseconds Delay between lock acquisition attempts in ms
     * @param retryNumber            Number of attempts to acquire lock
     */
    CrossProcessCacheFileLock(String lockfileName, int retryDelayMilliseconds, int retryNumber) {
        lockFile = new File(lockfileName);

        this.retryDelayMilliseconds = retryDelayMilliseconds;
        this.retryNumber = retryNumber;
    }

    private String getProcessId() {
        String vmName = ManagementFactory.getRuntimeMXBean().getName();

        return vmName.substring(0, vmName.indexOf("@"));
    }

    private String getLockProcessThreadId() {
        return "pid:" + getProcessId() + " thread:" + Thread.currentThread().getId();
    }

    private boolean tryToCreateLockFile() {
        for (int tryCount = 0; tryCount < retryNumber; tryCount++) {
            boolean fileCreated = false;
            try {
                fileCreated = lockFile.createNewFile();
            } catch (IOException ex) {
                LOG.error(ex.getMessage());
            }
            if (fileCreated) {
                return true;
            } else {
                waitBeforeRetry();
            }
        }
        return false;
    }

    private void waitBeforeRetry(){
        try {
            Thread.sleep(retryDelayMilliseconds);
        } catch (InterruptedException e) {
            LOG.error(e.getMessage());
        }
    }

    /**
     * Tries to acquire lock by creating lockFile (optional),
     * and acquiring OS lock for lockFile (mandatory)
     * Retries {@link #retryNumber} times with {@link #retryDelayMilliseconds} delay
     *
     * @throws CacheFileLockAcquisitionException if the lock was not obtained.
     */
    void lock() throws CacheFileLockAcquisitionException {
        if (!tryToCreateLockFile()) {
            LOG.debug(getLockProcessThreadId() + " Failed to create lock file!");
        }

        for (int tryCount = 0; tryCount < retryNumber; tryCount++) {
            try {
                lockFile.createNewFile();

                LOG.debug(getLockProcessThreadId() + " acquiring file lock");
                fileChannel = FileChannel.open(lockFile.toPath(),
                        StandardOpenOption.READ,
                        StandardOpenOption.SYNC,
                        StandardOpenOption.WRITE);

                // try to get file lock
                lock = fileChannel.tryLock();
                if (lock == null) {
                    throw new IllegalStateException("Lock is not available");
                }

                // for debugging purpose write jvm name to lock file
                writeJvmName(fileChannel);

                locked = true;
                LOG.debug(getLockProcessThreadId() + " acquired OK file lock");
                return;
            } catch (Exception ex) {
                LOG.debug(getLockProcessThreadId() + " failed to acquire lock," +
                        " exception msg - " + ex.getMessage());
                releaseResources();
                waitBeforeRetry();
            }
        }
        LOG.error(getLockProcessThreadId() + " failed to acquire lock");

        throw new CacheFileLockAcquisitionException(
                getLockProcessThreadId() + " failed to acquire lock");
    }

    private void writeJvmName(FileChannel fileChannel) throws IOException {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();

        ByteBuffer buff = ByteBuffer.wrap(jvmName.replace("@", " ").
                getBytes(StandardCharsets.UTF_8));
        fileChannel.write(buff);
    }

    /**
     * Release OS lock for lockFile,
     * delete lockFile if it was created by lock() method
     *
     * @throws IOException
     */
    void unlock() throws IOException {
        LOG.debug(getLockProcessThreadId() + " releasing lock");

        releaseResources();

        if (locked) {
            deleteLockFile();
            locked = false;
        }
    }

    private void deleteLockFile() throws IOException {
        if (!Files.deleteIfExists(lockFile.toPath())) {
            LOG.debug(getLockProcessThreadId() + " FAILED to delete lock file");
        }
    }

    private void releaseResources() {
        try {
            if (lock != null) {
                lock.release();
            }
            if (fileChannel != null) {
                fileChannel.close();
            }
        }
        catch (IOException ex){
            LOG.error(ex.getMessage());
        }
    }
}