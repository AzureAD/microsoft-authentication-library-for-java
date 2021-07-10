// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerTelemetryTests {

    private static final String SCHEMA_VERSION = "5";
    private final static String CURRENT_REQUEST_HEADER_NAME = "x-client-current-telemetry";
    private final static String LAST_REQUEST_HEADER_NAME = "x-client-last-telemetry";

    private final static String PUBLIC_API_ID = String.valueOf(PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE.getApiId());
    private final static String ERROR = "invalid_grant";

    @Test
    public void serverTelemetryHeaders_correctSchema() {

        CurrentRequest currentRequest = new CurrentRequest(PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE);

        ServerSideTelemetry serverSideTelemetry = new ServerSideTelemetry();
        serverSideTelemetry.setCurrentRequest(currentRequest);

        String correlationId = "936732c6-74b9-4783-aad9-fa205eae8763";
        serverSideTelemetry.addFailedRequestTelemetry(PUBLIC_API_ID, correlationId, ERROR);

        Map<String, String> headers = serverSideTelemetry.getServerTelemetryHeaderMap();

        //Current request tests
        List<String> currentRequestHeader = Arrays.asList(headers.get(CURRENT_REQUEST_HEADER_NAME).split("\\|"));

        // ["5", "831,"]
        Assert.assertEquals(currentRequestHeader.size(), 2);
        Assert.assertEquals(currentRequestHeader.get(0), SCHEMA_VERSION);

        // ["831", ""]
        List<String> secondSegment = Arrays.asList(currentRequestHeader.get(1).split(","));
        Assert.assertEquals(secondSegment.get(0), String.valueOf(PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE.getApiId()));
        Assert.assertEquals(secondSegment.get(1), "");


        // Previous request test
        List<String> previousRequestHeader = Arrays.asList(headers.get(LAST_REQUEST_HEADER_NAME).split("\\|"));

        // ["5","0","831,936732c6-74b9-4783-aad9-fa205eae8763","invalid_grant"]
        Assert.assertEquals(previousRequestHeader.size(), 4);
        Assert.assertEquals(previousRequestHeader.get(0), SCHEMA_VERSION);
        Assert.assertEquals(previousRequestHeader.get(1), "0");
        Assert.assertEquals(previousRequestHeader.get(3), ERROR);

        List<String> thirdSegment = Arrays.asList(previousRequestHeader.get(2).split(","));

        Assert.assertEquals(thirdSegment.get(0), PUBLIC_API_ID);
        Assert.assertEquals(thirdSegment.get(1), correlationId);
    }

    @Test
    public void serverTelemetryHeaders_previewsRequestNull() {

        ServerSideTelemetry serverSideTelemetry = new ServerSideTelemetry();
        for (int i = 0; i < 3; i++) {
            serverSideTelemetry.incrementSilentSuccessfulCount();
        }

        Map<String, String> headers = serverSideTelemetry.getServerTelemetryHeaderMap();

        Assert.assertEquals(headers.get(LAST_REQUEST_HEADER_NAME), "5|3|||");
    }

    @Test
    public void serverTelemetryHeader_testMaximumHeaderSize() {

        ServerSideTelemetry serverSideTelemetry = new ServerSideTelemetry();

        for (int i = 0; i < 20; i++) {
            String correlationId = UUID.randomUUID().toString();
            serverSideTelemetry.addFailedRequestTelemetry(PUBLIC_API_ID, correlationId, ERROR);
        }

        Map<String, String> headers = serverSideTelemetry.getServerTelemetryHeaderMap();

        String lastRequest = headers.get(LAST_REQUEST_HEADER_NAME);

        byte[] lastRequestBytes = lastRequest.getBytes(StandardCharsets.UTF_8);

        Assert.assertTrue(lastRequestBytes.length <= 350);
    }

    @Test
    public void serverTelemetryHeaders_multipleThreadsWrite() {

        ServerSideTelemetry serverSideTelemetry = new ServerSideTelemetry();
        ExecutorService executor = Executors.newFixedThreadPool(10);

        try {
            for (int i = 0; i < 10; i++) {
                executor.execute(new FailedRequestRunnable(serverSideTelemetry));
                executor.execute(new SilentSuccessfulRequestRunnable(serverSideTelemetry));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        executor.shutdown();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        Map<String, String> headers = serverSideTelemetry.getServerTelemetryHeaderMap();

        List<String> previousRequestHeader = Arrays.asList(headers.get(LAST_REQUEST_HEADER_NAME).split("\\|"));

        Assert.assertEquals(previousRequestHeader.get(1), "10");

        List<String> thirdSegment = Arrays.asList(previousRequestHeader.get(2).split(","));
        Assert.assertEquals(thirdSegment.size(), 12);

        List<String> fourthSegment = Arrays.asList(previousRequestHeader.get(3).split(","));
        Assert.assertEquals(fourthSegment.size(), 6);

        Assert.assertTrue(headers.get(LAST_REQUEST_HEADER_NAME).getBytes(StandardCharsets.UTF_8).length < 350);

        // Not all requests fit into first header, so they would get dispatched in the next request
        Map<String, String> secondRequest = serverSideTelemetry.getServerTelemetryHeaderMap();

        previousRequestHeader = Arrays.asList(secondRequest.get(LAST_REQUEST_HEADER_NAME).split("\\|"));

        Assert.assertEquals(previousRequestHeader.get(1), "0");

        thirdSegment = Arrays.asList(previousRequestHeader.get(2).split(","));
        Assert.assertEquals(thirdSegment.size(), 8);

        fourthSegment = Arrays.asList(previousRequestHeader.get(3).split(","));
        Assert.assertEquals(fourthSegment.size(), 4);

        Assert.assertTrue(secondRequest.get(LAST_REQUEST_HEADER_NAME).getBytes(StandardCharsets.UTF_8).length < 350);

    }

    @Test
    public void serverTelemetryHeaders_testRegionTelemetry() throws Exception {

        CurrentRequest currentRequest = new CurrentRequest(PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE);
        ServerSideTelemetry serverSideTelemetry = new ServerSideTelemetry();
        serverSideTelemetry.setCurrentRequest(currentRequest);

        Map<String, String> headers = serverSideTelemetry.getServerTelemetryHeaderMap();

        Assert.assertEquals(headers.get(CURRENT_REQUEST_HEADER_NAME), "5|831,,,0,0|");

        serverSideTelemetry.getCurrentRequest().regionUsed("westus");
        serverSideTelemetry.getCurrentRequest().regionSource(RegionTelemetry.REGION_SOURCE_IMDS.telemetryValue);
        serverSideTelemetry.getCurrentRequest().regionOutcome(RegionTelemetry.REGION_OUTCOME_AUTODETECT_SUCCESS.telemetryValue);

        headers = serverSideTelemetry.getServerTelemetryHeaderMap();

        Assert.assertEquals(headers.get(CURRENT_REQUEST_HEADER_NAME), "5|831,,westus,4,4|");

        serverSideTelemetry.getCurrentRequest().regionUsed("centralus");
        serverSideTelemetry.getCurrentRequest().regionSource(RegionTelemetry.REGION_SOURCE_ENV_VARIABLE.telemetryValue);
        serverSideTelemetry.getCurrentRequest().regionOutcome(RegionTelemetry.REGION_OUTCOME_DEVELOPER_AUTODETECT_MISMATCH.telemetryValue);
        headers = serverSideTelemetry.getServerTelemetryHeaderMap();

        Assert.assertEquals(headers.get(CURRENT_REQUEST_HEADER_NAME), "5|831,,centralus,3,3|");

        PublicClientApplication pca = PublicClientApplication.builder(
                "client").
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).azureRegion("westus").autoDetectRegion(true).
                build();

        try {
            //This token call doesn't need to succeed to reach the AadInstanceDiscoveryProvider class where the region telemetry is set
            pca.acquireToken(UserNamePasswordParameters.
                    builder(Collections.singleton("https://graph.windows.net/.default"),
                            "user",
                            "password".toCharArray())
                    .build())
                    .get();

            Assert.fail("Expected MsalException was not thrown");
        } catch (Exception ex) {
            headers = pca.getServiceBundle().getServerSideTelemetry().getServerTelemetryHeaderMap();

            Assert.assertEquals(headers.get(CURRENT_REQUEST_HEADER_NAME), "5|300,,,1,2|");
        }
    }

    class FailedRequestRunnable implements Runnable {

        ServerSideTelemetry telemetry;

        FailedRequestRunnable(ServerSideTelemetry telemetry) {
            this.telemetry = telemetry;
        }

        @Override
        public void run() {

            Random rand = new Random();
            int n = rand.nextInt(250);
            try {
                Thread.sleep(n);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            String correlationId = UUID.randomUUID().toString();
            telemetry.addFailedRequestTelemetry(PUBLIC_API_ID, correlationId, ERROR);
        }
    }

    class SilentSuccessfulRequestRunnable implements Runnable {

        ServerSideTelemetry telemetry;

        SilentSuccessfulRequestRunnable(ServerSideTelemetry telemetry) {
            this.telemetry = telemetry;
        }

        @Override
        public void run() {
            telemetry.incrementSilentSuccessfulCount();
        }
    }
}
