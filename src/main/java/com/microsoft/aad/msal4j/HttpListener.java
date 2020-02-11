// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;

@Accessors(fluent = true)
class HttpListener {

    private HttpServer server;

    @Getter(AccessLevel.PACKAGE)
    private int port;

    void startListener(int port, HttpHandler httpHandler) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", httpHandler);
            this.port = server.getAddress().getPort();
            server.start();

        } catch (Exception e){
            throw new MsalClientException(e);
        }
    }

    void stopListener(){
        if(server != null){
            server.stop(0);
        }
    }
}
