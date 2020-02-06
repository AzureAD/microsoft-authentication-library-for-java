package com.microsoft.aad.msal4j;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class AcquireTokenByInteractiveFlowSupplier extends AuthenticationResultSupplier {

    private PublicClientApplication clientApplication;
    private InteractiveRequest interactiveRequest;

    private BlockingQueue<AuthorizationResult> authorizationCodeQueue;
    private HttpListener httpListener;
    private AuthorizationResponseHandler authorizationResponseHandler;

    AcquireTokenByInteractiveFlowSupplier(PublicClientApplication clientApplication,
                                          InteractiveRequest request){
        super(clientApplication, request);
        this.clientApplication = clientApplication;
        this.interactiveRequest = request;
    }

    @Override
    AuthenticationResult execute() throws Exception{
        AuthorizationResult authorizationResult = getAuthorizationResult();
        return acquireTokenWithAuthorizationCode(authorizationResult);
    }

    private AuthorizationResult getAuthorizationResult(){
        SystemBrowserOptions systemBrowserOptions=
                interactiveRequest.interactiveRequestParameters.systemBrowserOptions();

        authorizationCodeQueue = new LinkedBlockingQueue<>();
        authorizationResponseHandler = new AuthorizationResponseHandler(
                authorizationCodeQueue,
                systemBrowserOptions);
        startHttpListener(authorizationResponseHandler);

        if (systemBrowserOptions != null && systemBrowserOptions.openBrowserAction() != null) {
            interactiveRequest.interactiveRequestParameters.systemBrowserOptions().openBrowserAction()
                    .openBrowser(interactiveRequest.authorizationUrl());
        } else {
            openDefaultSystemBrowser(interactiveRequest.authorizationUrl());
        }

        return getAuthorizationResultFromHttpListener();
}

    private void startHttpListener(AuthorizationResponseHandler handler){
        // if port is unspecified, set to 0, which will cause socket to find a free port
        int port = interactiveRequest.interactiveRequestParameters.redirectUri().getPort() == -1 ?
                0 :
                interactiveRequest.interactiveRequestParameters.redirectUri().getPort();

        httpListener = new HttpListener();
        httpListener.startListener(port, handler);
    }

    private void openDefaultSystemBrowser(URL url){
        try{
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(url.toURI());
            }
        } catch(Exception e){
            throw new MsalClientException(e);
        }
    }

    private AuthorizationResult getAuthorizationResultFromHttpListener(){
        AuthorizationResult result = null;
        try {
            long expirationTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + 120;

            while(result == null && !interactiveRequest.futureReference.get().isCancelled() &&
                    TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) < expirationTime) {

                result = authorizationCodeQueue.poll(100, TimeUnit.MILLISECONDS);
            }
        } catch(Exception e){
            throw new MsalClientException(e);
        } finally {
            if(httpListener != null){
                httpListener.stopListener();
            }
        }

        if (result == null || StringHelper.isBlank(result.code())) {
            throw new MsalClientException("No Authorization code was returned from the server",
                    AuthenticationErrorCode.AUTHORIZATION_RESULT_BLANK);
        }
        return result;
    }

    private AuthenticationResult acquireTokenWithAuthorizationCode(AuthorizationResult authorizationResult)
            throws Exception{

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder(authorizationResult.code(), interactiveRequest.interactiveRequestParameters.redirectUri())
                .scopes(interactiveRequest.interactiveRequestParameters.scopes())
                .codeVerifier(interactiveRequest.verifier())
                .build();

        AuthorizationCodeRequest authCodeRequest = new AuthorizationCodeRequest(
                parameters,
                clientApplication,
                clientApplication.createRequestContext(PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        AcquireTokenByAuthorizationGrantSupplier acquireTokenByAuthorizationGrantSupplier =
            new AcquireTokenByAuthorizationGrantSupplier(
                    clientApplication,
                    authCodeRequest,
                    clientApplication.authenticationAuthority);

        return acquireTokenByAuthorizationGrantSupplier.execute();
    }
}