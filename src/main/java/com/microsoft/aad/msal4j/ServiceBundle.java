// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.net.Proxy;
import java.util.concurrent.ExecutorService;

class ServiceBundle {

    private ExecutorService executorService;
    private Proxy proxy;
    private SSLSocketFactory sslSocketFactory;
    private HostnameVerifier hostnameVerifier;
    private TelemetryManager telemetryManager;

    ServiceBundle(ExecutorService executorService, Proxy proxy, SSLSocketFactory sslSocketFactory,
                  HostnameVerifier hostnameVerifier, TelemetryManager telemetryManager) {
        this.executorService = executorService;
        this.proxy = proxy;
        this.sslSocketFactory = sslSocketFactory;
        this.hostnameVerifier = hostnameVerifier;
        this.telemetryManager = telemetryManager;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public TelemetryManager getTelemetryManager(){
        return telemetryManager;
    }
}