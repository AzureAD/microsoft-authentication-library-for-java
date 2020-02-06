package com.microsoft.aad.msal4j;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

@Getter
@Accessors(fluent = true)
class AuthorizationResponseHandler implements HttpHandler {

    private final static Logger LOG = LoggerFactory.getLogger(AuthorizationResponseHandler.class);
    private final static String DEFAULT_SUCCESS_MESSAGE = "<html><head><title>Authentication Complete</title></head>"+
            "  <body> Authentication complete. You can close the browser and return to the application."+
            "  </body></html>" ;

    private final static String DEFAULT_FAILURE_MESSAGE = "<html> " +
            "<head><title>Authentication Failed</title></head> " +
            "<body> Authentication failed. You can return to the application. Feel free to close this browser tab. " +
            "</br></br></br></br> Error details: error {0} error_description: {1} </body> </html>";

    private BlockingQueue<AuthorizationResult> authorizationResultQueue;
    private SystemBrowserOptions systemBrowserOptions;

    AuthorizationResponseHandler(BlockingQueue<AuthorizationResult> authorizationResultQueue,
                                 SystemBrowserOptions systemBrowserOptions){
        this.authorizationResultQueue = authorizationResultQueue;
        this.systemBrowserOptions = systemBrowserOptions;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try{
            if(!httpExchange.getRequestURI().getPath().equalsIgnoreCase("/")){
                httpExchange.sendResponseHeaders(200, 0);
                return;
            }
            String responseBody = new BufferedReader(new InputStreamReader(
                    httpExchange.getRequestBody())).lines().collect(Collectors.joining("\n"));

            AuthorizationResult result = AuthorizationResult.fromResponseBody(responseBody);
            sendResponse(httpExchange, result);
            authorizationResultQueue.put(result);

        } catch (InterruptedException ex){
            LOG.error("Error reading response from socket: " + ex.getMessage());
            throw new MsalClientException(ex);
        } finally {
            httpExchange.close();
        }
    }

    private void sendResponse(HttpExchange httpExchange, AuthorizationResult result)
            throws IOException{

        String response;
        switch (result.status()){
            case Success:
                response = DEFAULT_SUCCESS_MESSAGE;
                break;
            case ProtocolError:
                response = DEFAULT_FAILURE_MESSAGE;
                break;
            default:
                //TODO better exception
                throw new RuntimeException();
        }

        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
