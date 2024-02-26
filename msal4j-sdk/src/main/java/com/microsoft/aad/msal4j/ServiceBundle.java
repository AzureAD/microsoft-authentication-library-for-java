// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.concurrent.ExecutorService;

class ServiceBundle {

    private ExecutorService executorService;
    private TelemetryManager telemetryManager;
    private IHttpClient httpClient;
    private IHttpHelper httpHelper;
    private ServerSideTelemetry serverSideTelemetry;

    ServiceBundle(ExecutorService executorService, TelemetryManager telemetryManager,
                  IHttpClient httpClient, IHttpHelper httpHelper) {
        this.executorService = executorService;
        this.telemetryManager = telemetryManager;
        this.httpClient = httpClient;
        this.httpHelper = httpHelper;

        serverSideTelemetry = new ServerSideTelemetry();
    }

    ExecutorService getExecutorService() {
        return executorService;
    }

    TelemetryManager getTelemetryManager() {
        return telemetryManager;
    }

    IHttpClient getHttpClient() {
        return httpClient;
    }

    IHttpHelper getHttpHelper() {
        return httpHelper;
    }

    ServerSideTelemetry getServerSideTelemetry() {
        return serverSideTelemetry;
    }
}