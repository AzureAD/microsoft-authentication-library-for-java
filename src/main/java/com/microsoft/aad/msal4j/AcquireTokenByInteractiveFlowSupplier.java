package com.microsoft.aad.msal4j;

import java.awt.*;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AcquireTokenByInteractiveFlowSupplier extends AuthenticationResultSupplier {

    private BlockingQueue<String> AuthorizationCodeQueue;
    private TcpListener tcpListener;

    private PublicClientApplication clientApplication;
    private InteractiveRequest interactiveRequest;

    AcquireTokenByInteractiveFlowSupplier(PublicClientApplication clientApplication,
                                          InteractiveRequest request){
        super(clientApplication, request);
        this.clientApplication = clientApplication;
        this.interactiveRequest = request;
    }

    // This where the high level logic for the flow will go
    AuthenticationResult execute() {
        String authorizationCode = getAuthorizationCode();
        acquireTokenWithAuthorizationCode(authorizationCode);

    }

    private String getAuthorizationCode(){

        BlockingQueue<Boolean> tcpStartUpNotificationQueue = new LinkedBlockingQueue<>();
        startTcpListener(tcpStartUpNotificationQueue);

        String authServerResponse;
        try{

            Boolean tcpListenerStarted = tcpStartUpNotificationQueue.poll(
                    30,
                    TimeUnit.SECONDS);
            if (tcpListenerStarted == null || !tcpListenerStarted){
                throw new RuntimeException("Could not start TCP listener");
            }

            if(interactiveRequest.interactiveRequestParameters.systemBrowserOptions().openBrowserAction() != null){
                interactiveRequest.interactiveRequestParameters.systemBrowserOptions().openBrowserAction().openBrowser(
                        interactiveRequest.authorizationURI);
            } else {
                openDefaultSystemBrowser(interactiveRequest.authorizationURI);
            }

            authServerResponse = getResponseFromTcpListener();
        } catch(Exception e){
            throw new MsalClientException("Error", AuthenticationErrorCode.AUTHORIZATION_CODE_BLANK);
        }

        return parseServerResponse(authServerResponse, clientApplication.authenticationAuthority.authorityType);
    }

    private AuthenticationResult acquireTokenWithAuthorizationCode(String authorizationCode){

    }

    private void startTcpListener(BlockingQueue<Boolean> tcpStartUpNotifierQueue){
        AuthorizationCodeQueue = new LinkedBlockingQueue<>();

        tcpListener = new TcpListener(
                AuthorizationCodeQueue,
                tcpStartUpNotifierQueue);

        // if port is unspecified, set to 0, which will cause socket to find a free port
        int port = interactiveRequest.interactiveRequestParameters.redirectUri().getPort() == -1 ?
                0 :
                interactiveRequest.interactiveRequestParameters.redirectUri().getPort();
        tcpListener.startServer(new int[] {port});
    }

    private void openDefaultSystemBrowser(URI uri){

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(uri);
        }
    }

    private String getResponseFromTcpListener(){
        String response;
        try {
            response = AuthorizationCodeQueue.poll(300, TimeUnit.SECONDS);
            if (StringHelper.isBlank(response)){
                throw new MsalClientException("No Authorization code was returned from the server",
                        AuthenticationErrorCode.AUTHORIZATION_CODE_BLANK);
            }
        } catch(Exception e){
            throw new MsalClientException(e);
        }
        return response;
    }

    //TODO: Bring up difference in between auth code response and ask B2C team if it's possible to standardize
    private String parseServerResponse(String serverResponse, AuthorityType authorityType){
        // Response will be a GET request with query parameter ?code=authCode
        String regexp;
        if(authorityType == AuthorityType.B2C){
            regexp = "(?<=code=)(?:(?! HTTP).)*";
        } else {
            regexp = "(?<=code=)(?:(?!&).)*";
        }

        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(serverResponse);

        if(!matcher.find()){
            throw new MsalClientException("No authorization code in server response",
                    AuthenticationErrorCode.AUTHORIZATION_CODE_BLANK);
        }
        return matcher.group(0);
    }

}