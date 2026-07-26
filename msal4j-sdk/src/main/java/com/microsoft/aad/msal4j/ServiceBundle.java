// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.concurrent.ExecutorService;

class ServiceBundle {

    private ExecutorService executorService;
    private TelemetryManager telemetryManager;
    private ThrottlingRequestChain httpHelper;
    private ServerSideTelemetry serverSideTelemetry;

    ServiceBundle(ExecutorService executorService, TelemetryManager telemetryManager, ThrottlingRequestChain httpHelper) {
        this.executorService = executorService;
        this.telemetryManager = telemetryManager;
        this.httpHelper = httpHelper;

        serverSideTelemetry = new ServerSideTelemetry();
    }

    ExecutorService getExecutorService() {
        return executorService;
    }

    TelemetryManager getTelemetryManager() {
        return telemetryManager;
    }

    ThrottlingRequestChain getHttpHelper() {
        return httpHelper;
    }

    ServerSideTelemetry getServerSideTelemetry() {
        return serverSideTelemetry;
    }
}