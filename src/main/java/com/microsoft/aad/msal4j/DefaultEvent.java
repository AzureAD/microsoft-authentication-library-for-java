// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;

class DefaultEvent extends Event {

    private final static String CLIENT_ID_KEY = TelemetryConstants.EVENT_NAME_PREFIX + "client_id";
    private final static String SDK_PLATFORM_KEY = TelemetryConstants.EVENT_NAME_PREFIX + "sdk_platform";
    private final static String SDK_VERSION_KEY = TelemetryConstants.EVENT_NAME_PREFIX + "sdk_version";
    private final static String HTTP_EVENT_COUNT_KEY = TelemetryConstants.EVENT_NAME_PREFIX + "http_event_count";
    private final static String CACHE_EVENT_COUNT_KEY = TelemetryConstants.EVENT_NAME_PREFIX + "cache_event_count";
    private Map<String, Integer> eventCount;

    public DefaultEvent(String clientId, Map<String, Integer> eventCount){
        super(TelemetryConstants.DEFAULT_EVENT_NAME_KEY);
        setClientId(clientId);
        setSdkPlatform();
        setSdkVersion();

        this.eventCount = eventCount;
        setHttpEventCount();
        setCacheEventCount();
    }

    private void setClientId(String clientId){
        this.put(CLIENT_ID_KEY, clientId);
    }

    private void setSdkPlatform(){
        this.put(SDK_PLATFORM_KEY, System.getProperty("os.name"));
    }

    private void setSdkVersion(){
        this.put(SDK_VERSION_KEY, this.getClass().getPackage().getImplementationVersion());
    }

    private void setHttpEventCount(){
        this.put(HTTP_EVENT_COUNT_KEY, getEventCount(TelemetryConstants.HTTP_EVENT_NAME_KEY));
    }

    private void setCacheEventCount(){
        this.put(CACHE_EVENT_COUNT_KEY, getEventCount(TelemetryConstants.CACHE_EVENT_NAME_KEY));
    }

    private String getEventCount(String eventName){
        return eventCount.getOrDefault(eventName, 0).toString();
    }


}
