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
            this.port = port;
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", httpHandler);
            server.start();
        } catch (Exception e){
            //TODO handle exception
            System.out.println(e.getMessage());
        }
    }

    void stopListener(){
        if(server != null){
            server.stop(0);
        }
    }
}
