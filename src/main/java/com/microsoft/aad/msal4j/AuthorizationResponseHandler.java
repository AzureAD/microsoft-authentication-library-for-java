// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.sun.net.httpserver.Headers;
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

    private final static String DEFAULT_FAILURE_MESSAGE = "<html><head><title>Authentication Failed</title></head> " +
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
            authorizationResultQueue.put(result);
            sendResponse(httpExchange, result);

        } catch (InterruptedException ex){
            LOG.error("Error reading response from socket: " + ex.getMessage());
            throw new MsalClientException(ex);
        } finally {
            httpExchange.close();
        }
    }

    private void sendResponse(HttpExchange httpExchange, AuthorizationResult result)
            throws IOException{

        switch (result.status()){
            case Success:
                sendSuccessResponse(httpExchange, getSuccessfulResponseMessage());
                break;
            case ProtocolError:
            case UnknownError:
                sendErrorResponse(httpExchange, getErrorResponseMessage());
                break;
        }
    }

    private void sendSuccessResponse(HttpExchange httpExchange, String response) throws IOException {
        if (systemBrowserOptions().browserRedirectSuccess() != null) {
            send302Response(httpExchange, systemBrowserOptions().browserRedirectSuccess().toString());
        } else {
            send200Response(httpExchange, response);
        }
    }

    private void sendErrorResponse(HttpExchange httpExchange, String response) throws IOException {
        if(systemBrowserOptions().browserRedirectError() != null){
            send302Response(httpExchange, systemBrowserOptions().browserRedirectError().toString());
        } else {
            send200Response(httpExchange, response);
        }
    }

    private void send302Response(HttpExchange httpExchange, String redirectUri) throws IOException{
        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("Location", redirectUri);
        httpExchange.sendResponseHeaders(302, 0);
    }

    private void send200Response(HttpExchange httpExchange, String response) throws IOException{
        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private String getSuccessfulResponseMessage(){
        return systemBrowserOptions().htmlMessageSuccess() != null ?
                systemBrowserOptions().htmlMessageSuccess() :
                DEFAULT_SUCCESS_MESSAGE;
    }

    private String getErrorResponseMessage(){
        return systemBrowserOptions().htmlMessageError() != null ?
                systemBrowserOptions().htmlMessageError() :
                DEFAULT_FAILURE_MESSAGE;
    }
}
