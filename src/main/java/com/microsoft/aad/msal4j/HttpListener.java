package com.microsoft.aad.msal4j;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

class HttpListener {

    private HttpServer server;

    void startListener(int port, HttpHandler httpHandler) {
        try {
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
