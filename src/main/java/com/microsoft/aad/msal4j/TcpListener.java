// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class TcpListener implements AutoCloseable{

    private final static Logger LOG = LoggerFactory.getLogger(TcpListener.class);

    private BlockingQueue<String> authorizationCodeQueue;
    private BlockingQueue<Boolean> tcpStartUpNotificationQueue;
    private int port;
    private Thread serverThread;

    public TcpListener(BlockingQueue<String> authorizationCodeQueue,
                       BlockingQueue<Boolean> tcpStartUpNotificationQueue){

        this.authorizationCodeQueue = authorizationCodeQueue;
        this.tcpStartUpNotificationQueue = tcpStartUpNotificationQueue;
    }

    public void startServer(int[] ports){
        Runnable serverTask = () -> {
            try(ServerSocket serverSocket = createSocket(ports)) {
                port = serverSocket.getLocalPort();
                tcpStartUpNotificationQueue.put(Boolean.TRUE);
                Socket clientSocket = serverSocket.accept();
                new ClientTask(clientSocket).run();
            } catch (Exception e) {
                LOG.error("Unable to process client request: " + e.getMessage());
                throw new RuntimeException("Unable to process client request: " + e.getMessage());
            }
        };

        serverThread = new Thread(serverTask);
        serverThread.start();
    }

    private class ClientTask implements Runnable {
        private final Socket clientSocket;

        private ClientTask(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run(){
            StringBuilder builder = new StringBuilder();
            try(BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()))) {
                String line = in.readLine();
                while(!line.equals("")){
                    builder.append(line);
                    line = in.readLine();
                }
                authorizationCodeQueue.put(builder.toString());
            } catch (Exception e) {
                LOG.error("Error reading response from socket: " + e.getMessage());
                throw new RuntimeException("Error reading response from socket: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    LOG.error("Error closing socket: " + e.getMessage());
                }
            }
        }
    }

    public ServerSocket createSocket(int[] ports) {

        for (int port : ports) {
            try {
                return new ServerSocket(port);
            } catch (IOException ex) {
                LOG.warn("Port: " + port + "is blocked");
            }
        }
        throw new MsalClientException(String.format(
                "Unable to open port specified in redirect URI. Make sure port %s is not being used" +
                        "by another process", ports.toString()), AuthenticationErrorCode.PORT_BLOCKED);
    }

    public int getPort() {
        return port;
    }

    public void close(){
        serverThread.interrupt();
    }
}
