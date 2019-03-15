package com.microsoft.aad.msal4j;

import javax.net.ssl.SSLSocketFactory;
import java.net.Proxy;
import java.util.concurrent.ExecutorService;

class ServiceBundle {

    private ExecutorService executorService;
    private Proxy proxy;
    private SSLSocketFactory sslSocketFactory;

    ServiceBundle(ExecutorService executorService, Proxy proxy, SSLSocketFactory sslSocketFactory){
        this.executorService = executorService;
        this.proxy = proxy;
        this.sslSocketFactory = sslSocketFactory;
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
}
