package com.microsoft.aad.msal4j;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

@Test(groups = { "checkin" })
public class TelemetryTests {

    public List<HashMap<String,String>> eventsReceived = new ArrayList<>();

    private class MyTelemetryConsumer {

        Consumer<List<HashMap<String, String>>> telemetryConsumer =
                (List<HashMap<String, String>> telemetryEvents) -> {
                    eventsReceived.addAll(telemetryEvents);
                    System.out.println("Received " + telemetryEvents.size() + " events");
                    telemetryEvents.forEach(event -> {
                            System.out.print("Event Name: " + event.get("event_name"));
                            event.entrySet().forEach(entry -> System.out.println("   " + entry));
                    });
                };
    }

    @AfterMethod
    private void cleanUp(){
        eventsReceived.clear();
    }

    @Test
    public void telemetryConsumerRegistration_ConsumerNotNullTest(){
        PublicClientApplication app = new PublicClientApplication.Builder("a1b2c3")
                .telemetryConsumer(new MyTelemetryConsumer().telemetryConsumer)
                .build();

        Assert.assertNotNull(app.getTelemetryConsumer());
    }

    @Test
    public void telemetryManagerFlush_EventCountTest(){
        Consumer<List<HashMap<String, String>>> telemetryConsumer =
                new MyTelemetryConsumer().telemetryConsumer;
        TelemetryManager telemetryManager = new TelemetryManager(telemetryConsumer, false);
        String reqId = telemetryManager.generateRequestId();

        ApiEvent apiEvent1 = createApiEvent();
        telemetryManager.startEvent(reqId, apiEvent1);
        apiEvent1.setWasSuccessful(true);
        telemetryManager.stopEvent(reqId, apiEvent1);

        HttpEvent httpEvent1 = createHttpEvent();
        telemetryManager.startEvent(reqId, httpEvent1);
        httpEvent1.setHttpResponseStatus(200);
        telemetryManager.stopEvent(reqId, httpEvent1);

        telemetryManager.flush(reqId,"a1b3c3d4" );

        // 1 Default event, 1 API event, 1 Http event
        Assert.assertEquals(eventsReceived.size(), 3);
    }

    @Test
    public void onSendFailureTrue_SkipEventsIfSuccessfulTest(){
        Consumer<List<HashMap<String, String>>> telemetryConsumer =
                new MyTelemetryConsumer().telemetryConsumer;
        // Only send on failure
        TelemetryManager telemetryManager = new TelemetryManager(telemetryConsumer, true);
        String reqId = telemetryManager.generateRequestId();


        ApiEvent apiEvent1 = createApiEvent();
        telemetryManager.startEvent(reqId, apiEvent1);
        apiEvent1.setWasSuccessful(true);
        telemetryManager.stopEvent(reqId, apiEvent1);

        HttpEvent httpEvent1 = createHttpEvent();
        telemetryManager.startEvent(reqId, httpEvent1);
        httpEvent1.setHttpResponseStatus(200);
        telemetryManager.stopEvent(reqId, httpEvent1);

        telemetryManager.flush(reqId,"a1b3c3d4" );

        // API event was successful, so count should be 0
        Assert.assertEquals(eventsReceived.size(), 0);
        eventsReceived.clear();

        String reqId2 = telemetryManager.generateRequestId();
        ApiEvent apiEvent2 = createApiEvent();
        telemetryManager.startEvent(reqId2, apiEvent2);
        apiEvent2.setWasSuccessful(false);
        telemetryManager.stopEvent(reqId2, apiEvent2);

        HttpEvent httpEvent2 = createHttpEvent();
        telemetryManager.startEvent(reqId2, httpEvent2);
        httpEvent2.setHttpResponseStatus(200);
        telemetryManager.stopEvent(reqId2, httpEvent2);

        telemetryManager.flush(reqId2,"a1b3c3d4" );

        // API event failed, so count should be 3 (1 default, 1 Api, 1 http)
        Assert.assertEquals(eventsReceived.size(), 3);
    }

    @Test
    public void telemetryInternalApi_ScrubTenantFromUriTest() throws Exception{
        Assert.assertEquals(Event.scrubTenant(new URI("https://login.microsoftonline.com/common/oauth2/v2.0/token")) ,
                "https://login.microsoftonline.com/<tenant>/oauth2/v2.0/token");

        Assert.assertEquals( Event.scrubTenant(new URI("https://login.microsoftonline.com/common")),
                "https://login.microsoftonline.com/<tenant>");

        Assert.assertNull(Event.scrubTenant(new URI("https://login.contoso.com/adfs")));
    }

    @Test
    public void telemetryContainsDefaultEventTest(){
        Consumer<List<HashMap<String, String>>> telemetryConsumer =
                new MyTelemetryConsumer().telemetryConsumer;
        // Only send on failure
        TelemetryManager telemetryManager = new TelemetryManager(telemetryConsumer, false);
        String reqId = telemetryManager.generateRequestId();

        ApiEvent apiEvent1 = createApiEvent();
        telemetryManager.startEvent(reqId, apiEvent1);
        apiEvent1.setWasSuccessful(true);
        telemetryManager.stopEvent(reqId, apiEvent1);

        HttpEvent httpEvent1 = createHttpEvent();
        telemetryManager.startEvent(reqId, httpEvent1);
        httpEvent1.setHttpResponseStatus(200);
        telemetryManager.stopEvent(reqId, httpEvent1);

        telemetryManager.flush(reqId,"a1b3c3d4" );

        Assert.assertEquals(eventsReceived.get(0).get("event_name"), "msal.default_event");
    }

    @Test
    public void telemetryFlushEventWithoutStopping_OrphanedEventIncludedTest(){
        Consumer<List<HashMap<String, String>>> telemetryConsumer =
                new MyTelemetryConsumer().telemetryConsumer;
        TelemetryManager telemetryManager = new TelemetryManager(telemetryConsumer, false);
        String reqId = telemetryManager.generateRequestId();

        ApiEvent apiEvent1 = createApiEvent();
        telemetryManager.startEvent(reqId, apiEvent1);
        apiEvent1.setWasSuccessful(true);

        HttpEvent httpEvent1 = createHttpEvent();
        telemetryManager.startEvent(reqId, httpEvent1);
        httpEvent1.setHttpResponseStatus(200);

        // didn't stop http event, should still be sent
        telemetryManager.stopEvent(reqId, apiEvent1);
        telemetryManager.flush(reqId,"a1b3c3d4" );

        Assert.assertEquals(eventsReceived.size(), 3);
        Assert.assertTrue(eventsReceived.stream().anyMatch(event -> event.get("event_name").equals("msal.http_event")));
    }

    @Test
    public void telemetryStopEventWithoutStarting_NoExceptionThrownTest(){
        Consumer<List<HashMap<String, String>>> telemetryConsumer =
                new MyTelemetryConsumer().telemetryConsumer;
        TelemetryManager telemetryManager = new TelemetryManager(telemetryConsumer, false);
        String reqId = telemetryManager.generateRequestId();

        ApiEvent apiEvent1 = createApiEvent();
        telemetryManager.startEvent(reqId, apiEvent1);
        apiEvent1.setWasSuccessful(true);

        // http event never started
        HttpEvent httpEvent1 = createHttpEvent();
        httpEvent1.setHttpResponseStatus(200);


        telemetryManager.stopEvent(reqId, apiEvent1);
        telemetryManager.stopEvent(reqId, httpEvent1);

        telemetryManager.flush(reqId, "a1b3c3d4");

        Assert.assertEquals(eventsReceived.size(), 2);
        Assert.assertFalse(eventsReceived.stream().anyMatch(event -> event.get("event_name").equals("msal.http_event")));
    }

    @Test
    public void piiLoggingEnabled_ApiEventHashTest(){

    }

    @Test
    public void piiLoggingEnabledFalse_TenantIdUserIdSetToNullTest(){

    }



    private ApiEvent createApiEvent(){
        ApiEvent apiEvent1;
        try {
            apiEvent1 = new ApiEvent(false);
            apiEvent1.setAuthority(new URI("https://login.microsoft.com"));
            apiEvent1.setTenantId("tenantId123");

        } catch(URISyntaxException e){
            throw new RuntimeException(e.getMessage());
        }
        return apiEvent1;
    }

    private HttpEvent createHttpEvent(){
        HttpEvent httpEvent1;
        try {
            httpEvent1 = new HttpEvent();
            httpEvent1.setHttpPath(new URI("https://contoso.com"));
            httpEvent1.setHttpMethod("GET");
            httpEvent1.setQueryParameters("?a=1&b=2");
        } catch(URISyntaxException e){
            throw new RuntimeException(e.getMessage());
        }
        return httpEvent1;
    }


}

