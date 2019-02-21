package Infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpListener {

    private final static Logger LOG = LoggerFactory.getLogger(SeleniumExtensions.class);

    private BlockingQueue<String> queue;
    private int port;

    public TcpListener(BlockingQueue<String> queue){
        this.queue = queue;
    }

    public void startServer(){
        final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(2);

        Runnable serverTask = () -> {
            try {
                ServerSocket serverSocket = new ServerSocket(0);
                port = serverSocket.getLocalPort();
                Socket clientSocket = serverSocket.accept();
                clientProcessingPool.submit(new ClientTask(clientSocket));

            } catch (IOException e) {
                LOG.error("Unable to process client request: " + e.getMessage());
                throw new RuntimeException("Unable to process client request: " + e.getMessage());
            }
        };

        Thread serverThread = new Thread(serverTask);
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
                while(in.ready()){
                   builder.append(in.readLine());
                }
                queue.put(builder.toString());
            } catch (Exception e) {
                LOG.error("Error reading response from socket: " + e.getMessage());
                throw new RuntimeException("Error reading response from socket: " + e.getMessage());
            }

            try {
                clientSocket.close();
            } catch (IOException e) {
                LOG.error("Error closing socket: " + e.getMessage());
            }
        }
    }

    public int getPort() {
        return port;
    }
}
