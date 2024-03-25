// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;

@Accessors(fluent = true)
class HttpListener {

    private final static Logger LOG = LoggerFactory.getLogger(HttpListener.class);

    private HttpServer server;

    @Getter(AccessLevel.PACKAGE)
    private int port;

    void startListener(int port, HttpHandler httpHandler) {
        try {
            //Originally this created a listener on the wildcard address from InetSocketAddress(port), which could cause firewall issues:
            // https://github.com/AzureAD/microsoft-authentication-library-for-java/issues/796
            //Since only loopback addresses are allowed for the flow that uses this listener, we now just use the loopback address instead
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
            server.createContext("/", httpHandler);
            this.port = server.getAddress().getPort();
            server.start();
            LOG.debug("Http listener started. Listening on port: " + port);
        } catch (Exception e) {
            throw new MsalClientException(e.getMessage(),
                    AuthenticationErrorCode.UNABLE_TO_START_HTTP_LISTENER);
        }
    }

    void stopListener() {
        if (server != null) {
            server.stop(0);
            LOG.debug("Http listener stopped");

        }
    }
}
