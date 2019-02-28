package Infrastructure;

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

    private BlockingQueue<String> queue;
    private int port;
    private Thread serverThread;
    public TcpListener(BlockingQueue<String> queue){
        this.queue = queue;
    }

    public void startServer(){

        Runnable serverTask = () -> {
            try(ServerSocket serverSocket = new ServerSocket(0)) {
                port = serverSocket.getLocalPort();
                Socket clientSocket = serverSocket.accept();
                new ClientTask(clientSocket).run();
            } catch (IOException e) {
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
                queue.put(builder.toString());
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

    public int getPort() {
        return port;
    }

    public void close(){
        serverThread.interrupt();
    }
}
