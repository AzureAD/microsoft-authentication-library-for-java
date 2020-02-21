package com.microsoft.aad.msal4j;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerTelemetryTests {

    private static final String SCHEMA_VERSION = "2";
    private final static String CURRENT_REQUEST_HEADER_NAME = "x-client-current-telemetry";
    private final static String LAST_REQUEST_HEADER_NAME = "x-client-last-telemetry";

    private final static String PUBLIC_API_ID = String.valueOf(PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE.getApiId());
    private final static String ERROR = "invalid_grant";

    @Test
    public void serverTelemetryHeaders_correctSchema(){

        CurrentRequest currentRequest = new CurrentRequest(PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE);
        currentRequest.forceRefresh(false);

        ServerSideTelemetry serverSideTelemetry = new ServerSideTelemetry();
        serverSideTelemetry.setCurrentRequest(currentRequest);

        String correlationId = "936732c6-74b9-4783-aad9-fa205eae8763";
        serverSideTelemetry.addFailedRequestTelemetry(PUBLIC_API_ID, correlationId, ERROR);

        Map<String, String> headers = serverSideTelemetry.getServerTelemetryHeaderMap();

        //Current request tests
        List<String> currentRequestHeader = Arrays.asList(headers.get(CURRENT_REQUEST_HEADER_NAME).split("\\|"));

        // ["2", "831, false"]
        Assert.assertEquals(currentRequestHeader.size(), 2);
        Assert.assertEquals(currentRequestHeader.get(0), SCHEMA_VERSION);

        // ["831", "false"]
        List<String> secondSegment = Arrays.asList(currentRequestHeader.get(1).split(","));
        Assert.assertEquals(secondSegment.get(0), String.valueOf(PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE.getApiId()));
        Assert.assertEquals(secondSegment.get(1), "false");


        // Previous request test
        List<String> previousRequestHeader = Arrays.asList(headers.get(LAST_REQUEST_HEADER_NAME).split("\\|"));

        // ["2","0","831,936732c6-74b9-4783-aad9-fa205eae8763","invalid_grant"]
        Assert.assertEquals(previousRequestHeader.size(), 4);
        Assert.assertEquals(previousRequestHeader.get(0), SCHEMA_VERSION);
        Assert.assertEquals(previousRequestHeader.get(1), "0");
        Assert.assertEquals(previousRequestHeader.get(3), ERROR);

        List<String> thirdSegment = Arrays.asList(previousRequestHeader.get(2).split(","));

        Assert.assertEquals(thirdSegment.get(0), PUBLIC_API_ID);
        Assert.assertEquals(thirdSegment.get(1), correlationId);
    }

    @Test
    public void serverTelemetryHeaders_previewsRequestNull(){

        ServerSideTelemetry serverSideTelemetry = new ServerSideTelemetry();
        for(int i = 0; i < 3; i++){
            serverSideTelemetry.incrementSilentSuccessfulCount();
        }

        Map<String, String> headers = serverSideTelemetry.getServerTelemetryHeaderMap();

        Assert.assertEquals(headers.get(LAST_REQUEST_HEADER_NAME), "2|3|||");
    }

    @Test
    public void serverTelemetryHeader_testMaximumHeaderSize(){

        ServerSideTelemetry serverSideTelemetry = new ServerSideTelemetry();

        for(int i = 0; i <100; i++){
            String correlationId = UUID.randomUUID().toString();
            serverSideTelemetry.addFailedRequestTelemetry(PUBLIC_API_ID, correlationId, ERROR);
        }

       Map<String, String> headers = serverSideTelemetry.getServerTelemetryHeaderMap();

       String lastRequest = headers.get(LAST_REQUEST_HEADER_NAME);

       // http headers are encoded in ISO_8859_1
       byte[] lastRequestBytes = lastRequest.getBytes(StandardCharsets.ISO_8859_1);

       Assert.assertTrue(lastRequestBytes.length < 4000);
    }

    @Test
    public void serverTelemetryHeaders_multipleThreadsWrite(){

        ServerSideTelemetry serverSideTelemetry = new ServerSideTelemetry();
        ExecutorService executor = Executors.newFixedThreadPool(10);

        try{
            for (int i=0; i < 10; i++){
                executor.execute(new FailedRequestRunnable(serverSideTelemetry));
                executor.execute(new SilentSuccessfulRequestRunnable(serverSideTelemetry));
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
        executor.shutdown();

        try{
            Thread.sleep( 1000);
        } catch(InterruptedException ex){
            ex.printStackTrace();
        }

        Map<String, String> headers =  serverSideTelemetry.getServerTelemetryHeaderMap();

        List<String> previousRequestHeader = Arrays.asList(headers.get(LAST_REQUEST_HEADER_NAME).split("\\|"));

        Assert.assertEquals(previousRequestHeader.get(1), "10");

        List<String> thirdSegment = Arrays.asList(previousRequestHeader.get(2).split(","));
        Assert.assertEquals(thirdSegment.size(), 20);

        List<String> fourthSegment = Arrays.asList(previousRequestHeader.get(3).split(","));
        Assert.assertEquals(fourthSegment.size(), 10);
    }

    class FailedRequestRunnable implements Runnable {

        ServerSideTelemetry telemetry;

        FailedRequestRunnable(ServerSideTelemetry telemetry){
            this.telemetry = telemetry;
        }

        @Override
        public void run(){

            Random rand = new Random();
            int n = rand.nextInt(250);
            try {
                Thread.sleep(n);
            } catch (InterruptedException ex){
                ex.printStackTrace();
            }
            String correlationId = UUID.randomUUID().toString();
            telemetry.addFailedRequestTelemetry(PUBLIC_API_ID, correlationId, ERROR);
        }
    }

    class SilentSuccessfulRequestRunnable implements Runnable {

        ServerSideTelemetry telemetry;

        SilentSuccessfulRequestRunnable(ServerSideTelemetry telemetry){
            this.telemetry = telemetry;
        }

        @Override
        public void run(){
            telemetry.incrementSilentSuccessfulCount();
        }
    }
}
