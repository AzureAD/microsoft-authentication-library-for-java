// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

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

    private List<HashMap<String,String>> eventsReceived = new ArrayList<>();
    private String tenantId = "tenantId123";
    private String clientId = "a1b3c3d4";

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

        Assert.assertNotNull(app.telemetryConsumer());
    }

    @Test
    public void telemetryManagerFlush_EventCountTest(){
        Consumer<List<HashMap<String, String>>> telemetryConsumer =
                new MyTelemetryConsumer().telemetryConsumer;
        TelemetryManager telemetryManager = new TelemetryManager(telemetryConsumer, false);
        String reqId = telemetryManager.generateRequestId();

        ApiEvent apiEvent1 = createApiEvent(false);
        telemetryManager.startEvent(reqId, apiEvent1);
        apiEvent1.setWasSuccessful(true);
        telemetryManager.stopEvent(reqId, apiEvent1);

        HttpEvent httpEvent1 = createHttpEvent();
        telemetryManager.startEvent(reqId, httpEvent1);
        httpEvent1.setHttpResponseStatus(200);
        telemetryManager.stopEvent(reqId, httpEvent1);

        telemetryManager.flush(reqId,clientId );

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


        ApiEvent apiEvent1 = createApiEvent(false);
        telemetryManager.startEvent(reqId, apiEvent1);
        apiEvent1.setWasSuccessful(true);
        telemetryManager.stopEvent(reqId, apiEvent1);

        HttpEvent httpEvent1 = createHttpEvent();
        telemetryManager.startEvent(reqId, httpEvent1);
        httpEvent1.setHttpResponseStatus(200);
        telemetryManager.stopEvent(reqId, httpEvent1);

        telemetryManager.flush(reqId,clientId );

        // API event was successful, so count should be 0
        Assert.assertEquals(eventsReceived.size(), 0);
        eventsReceived.clear();

        String reqId2 = telemetryManager.generateRequestId();
        ApiEvent apiEvent2 = createApiEvent(false);
        telemetryManager.startEvent(reqId2, apiEvent2);
        apiEvent2.setWasSuccessful(false);
        telemetryManager.stopEvent(reqId2, apiEvent2);

        HttpEvent httpEvent2 = createHttpEvent();
        telemetryManager.startEvent(reqId2, httpEvent2);
        httpEvent2.setHttpResponseStatus(200);
        telemetryManager.stopEvent(reqId2, httpEvent2);

        telemetryManager.flush(reqId2,clientId );

        // API event failed, so count should be 3 (1 default, 1 Api, 1 http)
        Assert.assertEquals(eventsReceived.size(), 3);
    }

    @Test
    public void telemetryInternalApi_ScrubTenantFromUriTest() throws Exception{
        Assert.assertEquals(Event.scrubTenant(new URI("https://login.microsoftonline.com/common/oauth2/v2.0/token")) ,
                "https://login.microsoftonline.com/<tenant>/oauth2/v2.0/token");

        Assert.assertEquals( Event.scrubTenant(new URI("https://login.microsoftonline.com/common")),
                "https://login.microsoftonline.com/<tenant>");

        Assert.assertEquals(Event.scrubTenant(new URI("https://login.microsoftonline.com/tfp/msidlabb2c.onmicrosoft.com/B2C_1_ROPC_Auth")),
                "https://login.microsoftonline.com/tfp/<tenant>/B2C_1_ROPC_Auth");

        Assert.assertNull(Event.scrubTenant(new URI("https://msidlabb2c.b2clogin.com/tfp/msidlabb2c.onmicrosoft.com/B2C_1_ROPC_Auth")));

        Assert.assertNull(Event.scrubTenant(new URI("https://login.contoso.com/adfs")));
    }

    @Test
    public void telemetryContainsDefaultEventTest(){
        Consumer<List<HashMap<String, String>>> telemetryConsumer =
                new MyTelemetryConsumer().telemetryConsumer;
        // Only send on failure
        TelemetryManager telemetryManager = new TelemetryManager(telemetryConsumer, false);
        String reqId = telemetryManager.generateRequestId();

        ApiEvent apiEvent1 = createApiEvent(false);
        telemetryManager.startEvent(reqId, apiEvent1);
        apiEvent1.setWasSuccessful(true);
        telemetryManager.stopEvent(reqId, apiEvent1);

        HttpEvent httpEvent1 = createHttpEvent();
        telemetryManager.startEvent(reqId, httpEvent1);
        httpEvent1.setHttpResponseStatus(200);
        telemetryManager.stopEvent(reqId, httpEvent1);

        telemetryManager.flush(reqId,clientId );

        Assert.assertEquals(eventsReceived.get(0).get("event_name"), "msal.default_event");
    }

    @Test
    public void telemetryFlushEventWithoutStopping_OrphanedEventIncludedTest(){
        Consumer<List<HashMap<String, String>>> telemetryConsumer =
                new MyTelemetryConsumer().telemetryConsumer;
        TelemetryManager telemetryManager = new TelemetryManager(telemetryConsumer, false);
        String reqId = telemetryManager.generateRequestId();

        ApiEvent apiEvent1 = createApiEvent(false);
        telemetryManager.startEvent(reqId, apiEvent1);
        apiEvent1.setWasSuccessful(true);

        HttpEvent httpEvent1 = createHttpEvent();
        telemetryManager.startEvent(reqId, httpEvent1);
        httpEvent1.setHttpResponseStatus(200);

        // didn't stop http event, should still be sent
        telemetryManager.stopEvent(reqId, apiEvent1);
        telemetryManager.flush(reqId,clientId );

        Assert.assertEquals(eventsReceived.size(), 3);
        Assert.assertTrue(eventsReceived.stream().anyMatch(event -> event.get("event_name").equals("msal.http_event")));
    }

    @Test
    public void telemetryStopEventWithoutStarting_NoExceptionThrownTest(){
        Consumer<List<HashMap<String, String>>> telemetryConsumer =
                new MyTelemetryConsumer().telemetryConsumer;
        TelemetryManager telemetryManager = new TelemetryManager(telemetryConsumer, false);
        String reqId = telemetryManager.generateRequestId();

        ApiEvent apiEvent1 = createApiEvent(false);
        telemetryManager.startEvent(reqId, apiEvent1);
        apiEvent1.setWasSuccessful(true);

        // http event never started
        HttpEvent httpEvent1 = createHttpEvent();
        httpEvent1.setHttpResponseStatus(200);


        telemetryManager.stopEvent(reqId, apiEvent1);
        telemetryManager.stopEvent(reqId, httpEvent1);

        telemetryManager.flush(reqId, clientId);

        Assert.assertEquals(eventsReceived.size(), 2);
        Assert.assertFalse(eventsReceived.stream().anyMatch(event -> event.get("event_name").equals("msal.http_event")));
    }

    @Test
    public void piiLoggingEnabled_ApiEventHashTest(){
        Consumer<List<HashMap<String, String>>> telemetryConsumer =
                new MyTelemetryConsumer().telemetryConsumer;
        TelemetryManager telemetryManager = new TelemetryManager(telemetryConsumer, false);
        String reqId = telemetryManager.generateRequestId();

        // TODO account_id should also be hashed when cache is added
        // set log pii to true
        ApiEvent apiEvent = createApiEvent(true);

        telemetryManager.startEvent(reqId, apiEvent);
        apiEvent.setWasSuccessful(true);
        telemetryManager.stopEvent(reqId, apiEvent);

        Assert.assertNotNull(apiEvent.get("msal.tenant_id"));
        Assert.assertNotEquals(apiEvent.get("msal.tenant_id"), tenantId);
    }

    @Test
    public void piiLoggingEnabledFalse_TenantIdUserIdSetToNullTest(){
        Consumer<List<HashMap<String, String>>> telemetryConsumer =
                new MyTelemetryConsumer().telemetryConsumer;
        TelemetryManager telemetryManager = new TelemetryManager(telemetryConsumer, false);
        String reqId = telemetryManager.generateRequestId();

        // TODO account_id should also be null when piiLogging = false
        // set log pii to true
        ApiEvent apiEvent = createApiEvent(false);

        telemetryManager.startEvent(reqId, apiEvent);
        apiEvent.setWasSuccessful(true);
        telemetryManager.stopEvent(reqId, apiEvent);

        Assert.assertNull(apiEvent.get("msal.tenant_id"));
    }

    @Test
    public void authorityNotInTrustedHostList_AuthorityIsNullTest() throws URISyntaxException{
        Consumer<List<HashMap<String, String>>> telemetryConsumer =
                new MyTelemetryConsumer().telemetryConsumer;
        TelemetryManager telemetryManager = new TelemetryManager(telemetryConsumer, false);
        String reqId = telemetryManager.generateRequestId();

        ApiEvent apiEvent = new ApiEvent(false);
        apiEvent.setAuthority(new URI("https://login.microsoftonline.com"));
        telemetryManager.startEvent(reqId, apiEvent);
        apiEvent.setWasSuccessful(true);
        telemetryManager.stopEvent(reqId, apiEvent);

        Assert.assertEquals(apiEvent.get("msal.authority"), "https://login.microsoftonline.com");


        ApiEvent apiEvent2 = new ApiEvent(false);
        apiEvent2.setAuthority(new URI("https://login.contoso.com"));
        telemetryManager.startEvent(reqId, apiEvent2);
        apiEvent2.setWasSuccessful(true);
        telemetryManager.stopEvent(reqId, apiEvent2);

        Assert.assertNull(apiEvent2.get("msal.authority"));
    }

    @Test
    public void xmsCliTelemetryTest_CorrectFormatTest(){
        String responseHeader = "1,0,0,," ;
        XmsClientTelemetryInfo info = XmsClientTelemetryInfo.parseXmsTelemetryInfo(responseHeader);

        Assert.assertEquals(info.getServerErrorCode(), "0");
        Assert.assertEquals(info.getServerSubErrorCode(), "0");
        Assert.assertEquals(info.getTokenAge(), "");
        Assert.assertEquals(info.getSpeInfo(), "");
    }

    @Test
    public void xmsCliTelemetryTest_IncorrectFormatTest(){
        String responseHeader =  "1,2,3,4,5,6";
        XmsClientTelemetryInfo info = XmsClientTelemetryInfo.parseXmsTelemetryInfo(responseHeader);

        Assert.assertNull(info.getServerErrorCode());
        Assert.assertNull(info.getServerSubErrorCode());
        Assert.assertNull(info.getTokenAge());
        Assert.assertNull(info.getSpeInfo());
    }

    @Test
    public void xmsCliTelemetryTest_IncorrectHeaderTest(){
        String responseHeader = "3,0,0,,";
        XmsClientTelemetryInfo info = XmsClientTelemetryInfo.parseXmsTelemetryInfo(responseHeader);

        Assert.assertNull(info);
    }

    private ApiEvent createApiEvent(Boolean logPii){
        ApiEvent apiEvent1;
        try {
            apiEvent1 = new ApiEvent(logPii);
            apiEvent1.setAuthority(new URI("https://login.microsoft.com"));
            apiEvent1.setTenantId(tenantId);

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

