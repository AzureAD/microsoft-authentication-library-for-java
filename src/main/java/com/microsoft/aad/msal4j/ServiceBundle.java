// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.net.ssl.SSLSocketFactory;
import java.net.Proxy;
import java.util.concurrent.ExecutorService;

class ServiceBundle {

    private ExecutorService executorService;
    private Proxy proxy;
    private SSLSocketFactory sslSocketFactory;
    private TelemetryManager telemetryManager;
    private ServerSideTelemetry serverSideTelemetry;

    ServiceBundle(ExecutorService executorService, Proxy proxy, SSLSocketFactory sslSocketFactory,
                  TelemetryManager telemetryManager){
        this.executorService = executorService;
        this.proxy = proxy;
        this.sslSocketFactory = sslSocketFactory;
        this.telemetryManager = telemetryManager;

        serverSideTelemetry = new ServerSideTelemetry();
    }

    ExecutorService getExecutorService() {
        return executorService;
    }

    Proxy getProxy() {
        return proxy;
    }

    SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    TelemetryManager getTelemetryManager(){
        return telemetryManager;
    }

    ServerSideTelemetry getServerSideTelemetry(){
        return serverSideTelemetry;
    }
}