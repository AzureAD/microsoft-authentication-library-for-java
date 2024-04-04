// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import com.microsoft.aad.msal4jextensions.persistence.ICacheAccessor;
import com.sun.jna.Platform;
import org.junit.Assert;
import org.junit.BeforeClass;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CacheLockTestBase {
    static String folder;
    static String testFilePath;
    static String lockFilePath;

    static String testClassesPath;

    static String lockHoldingIntervalsFilePath;

    @BeforeClass
    public static void setup() {
        // get proper file paths
        String currDir = System.getProperty("user.dir");
        String home = System.getProperty("user.home");

        java.nio.file.Path classes = java.nio.file.Paths.get(currDir, "target", "classes");
        testClassesPath = java.nio.file.Paths.get(currDir, "target", "test-classes").toString();

        testFilePath = java.nio.file.Paths.get(home, "test.txt").toString();
        lockFilePath = java.nio.file.Paths.get(home, "testLock.lockfile").toString();

        lockHoldingIntervalsFilePath = java.nio.file.Paths.get(home, "lockHoldingIntervals.txt").toString();

        String delimiter = ":";
        if (Platform.isWindows()) {
            delimiter = ";";
        }
        folder = classes.toString() + delimiter + testClassesPath;
    }

    void waitForProcess(Process process) throws InterruptedException {
        if (process.waitFor() != 0) {
            throw new RuntimeException(new BufferedReader(new InputStreamReader(process.getErrorStream()))
                    .lines().collect(Collectors.joining("\n")));
        }
    }

    void validateResult(String data, int expectedNum) {
        System.out.println("DATA TO VALIDATE: ");
        System.out.println(data);

        String prevTag = null;
        String prevProcId = null;
        int count = 0;

        for (String line : data.split("\\r?\\n")) {

            String[] tokens = line.split(" ");
            String tag = tokens[0];
            String procId = tokens[1];
            switch (tag) {
                case ("<"):
                    if ("<".equals(prevTag)) {
                        Assert.fail("Unexpected Token");
                    }
                    break;
                case (">"):
                    count++;
                    if (!"<".equals(prevTag) || !prevProcId.equals(procId)) {
                        Assert.fail("Unexpected Token");
                    }
                    break;
                default:
                    Assert.fail("Unexpected Token");
            }
            prevTag = tag;
            prevProcId = procId;
        }
        if (!">".equals(prevTag)) {
            Assert.fail("Unexpected Token");
        }
        Assert.assertEquals(expectedNum, count);
    }

    void validateLockUsageIntervals(int expected_size) throws IOException {
        List<Long[]> list = new ArrayList<>();
        String data = readFile(lockHoldingIntervalsFilePath);

        for (String line : data.split("\\r?\\n")) {
            String[] split = line.split("-");
            list.add(new Long[]{Long.parseLong(split[0]), Long.parseLong(split[1])});
        }

        //Assert.assertEquals(expected_size, list.size());
        if (expected_size != list.size()) {
            System.out.println("lock intervals NUM = " + list.size());
        }

        list.sort(Comparator.comparingLong(a -> a[0]));

        int sum = 0;
        Long[] prev = null;
        for (Long[] interval : list) {
            Assert.assertTrue(interval[0] <= interval[1]);
            sum += interval[1] - interval[0];
            if (prev != null) {
                if (interval[0] < prev[1]) {
                    System.out.println("lock acquisition intersection detected");
                    //Assert.fail();
                }
            }
            prev = interval;
        }
        System.out.println("average lock holding time in ms - " + sum/list.size());
    }

    private String readFile(String filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    void validateResultInCache(ICacheAccessor keyChainAccessor, int expectedNum) throws IOException {
        validateResult(new String(keyChainAccessor.read(), StandardCharsets.UTF_8), expectedNum);
    }

    void multipleThreadsWriting(ICacheAccessor cacheAccessor, int num,
                                IRunnableFactory runnableFactory) throws IOException, InterruptedException {

        clearFile(lockHoldingIntervalsFilePath);
        cacheAccessor.delete();

        List<Thread> writersThreads = new ArrayList<>();

        for (int i = 0; i < num; i++) {

            Thread t = new Thread(runnableFactory.create("thread_" + i));
            t.start();
            writersThreads.add(t);
        }

        for (Thread t : writersThreads) {
            t.join();
        }
        validateLockUsageIntervals(num);
        validateResultInCache(cacheAccessor, num);
    }

    private void clearFile(String filePath) throws IOException {
        new FileOutputStream(filePath).close();
    }

    void multipleProcessesWriting(ICacheAccessor cacheAccessor, int num,
                                  String writerClass,
                                  String writerClassArgs)
            throws IOException, InterruptedException {

        clearFile(lockHoldingIntervalsFilePath);
        cacheAccessor.delete();

        List<Process> processes = new ArrayList<>();
        for (int i = 0; i < num; i++) {

            String mvnArgs = ("Process_" + i) + " " + writerClassArgs;

            String mvn = Platform.isWindows() ? "mvn.bat" : "mvn";

            String[] mvnCommand =
                    new String[]{mvn, "exec:java",
                            "-Dexec.mainClass=" + writerClass,
                            "-Dexec.classpathScope=test",
                            "-Dexec.args=" + mvnArgs};

            Process process = new ProcessBuilder(mvnCommand).inheritIO().start();
            processes.add(process);
        }

        for (Process process : processes) {
            waitForProcess(process);
        }

        validateLockUsageIntervals(num);
        validateResultInCache(cacheAccessor, num);
    }
}
