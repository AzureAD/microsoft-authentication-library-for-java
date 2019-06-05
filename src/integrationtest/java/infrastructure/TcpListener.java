// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class TcpListener implements AutoCloseable{

    private final static Logger LOG = LoggerFactory.getLogger(SeleniumExtensions.class);

    private BlockingQueue<String> authorizationCodeQueue;
    private BlockingQueue<Boolean> tcpStartUpNotificationQueue;
    private int port;
    private Thread serverThread;

    public TcpListener(BlockingQueue<String> authorizationCodeQueue,
                       BlockingQueue<Boolean> tcpStartUpNotificationQueue){
        this.authorizationCodeQueue = authorizationCodeQueue;
        this.tcpStartUpNotificationQueue = tcpStartUpNotificationQueue;
    }

    public void startServer(){
        Runnable serverTask = () -> {
            try(ServerSocket serverSocket = createSocket()) {
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

    public ServerSocket createSocket() throws IOException {
        int[] ports = { 3843,4584, 4843, 60000 };
        for (int port : ports) {
            try {
                return new ServerSocket(port);
            } catch (IOException ex) {
                LOG.warn("Port: " + port + "is blocked");
            }
        }
        throw new IOException("no free port found");
    }

    public int getPort() {
        return port;
    }

    public void close(){
        serverThread.interrupt();
    }
}
