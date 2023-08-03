// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class AcquireTokenByInteractiveFlowSupplier extends AuthenticationResultSupplier {

    private static final Logger LOG = LoggerFactory.getLogger(AcquireTokenByInteractiveFlowSupplier.class);

    private PublicClientApplication clientApplication;
    private InteractiveRequest interactiveRequest;

    private BlockingQueue<AuthorizationResult> authorizationResultQueue;
    private HttpListener httpListener;

    /**MSAL tried to open the browser on Linux using the xdg-open, gnome-open, or kfmclient tools, but failed.
     Make sure you can open a page using xdg-open tool. See <a href="https://aka.ms/msal-net-os-browser">...</a> for details. */
    public static final String LINUX_XDG_OPEN = "linux_xdg_open_failed";

    public static final String LINUX_OPEN_AS_SUDO_NOT_SUPPORTED = "Unable to open a web page using xdg-open, gnome-open, kfmclient or wslview tools in sudo mode. Please run the process as non-sudo user.";

    AcquireTokenByInteractiveFlowSupplier(PublicClientApplication clientApplication,
                                          InteractiveRequest request) {
        super(clientApplication, request);
        this.clientApplication = clientApplication;
        this.interactiveRequest = request;
    }

    @Override
    AuthenticationResult execute() throws Exception {
        AuthorizationResult authorizationResult = getAuthorizationResult();
        validateState(authorizationResult);
        return acquireTokenWithAuthorizationCode(authorizationResult);
    }

    private AuthorizationResult getAuthorizationResult() {

        AuthorizationResult result;
        try {
            SystemBrowserOptions systemBrowserOptions =
                    interactiveRequest.interactiveRequestParameters().systemBrowserOptions();

            authorizationResultQueue = new LinkedBlockingQueue<>();
            AuthorizationResponseHandler authorizationResponseHandler =
                    new AuthorizationResponseHandler(
                            authorizationResultQueue,
                            systemBrowserOptions);

            startHttpListener(authorizationResponseHandler);

            if (systemBrowserOptions != null && systemBrowserOptions.openBrowserAction() != null) {
                interactiveRequest.interactiveRequestParameters().systemBrowserOptions().openBrowserAction()
                        .openBrowser(interactiveRequest.authorizationUrl());
            } else {
                openDefaultSystemBrowser(interactiveRequest.authorizationUrl());
            }

            result = getAuthorizationResultFromHttpListener();
        } finally {
            if (httpListener != null) {
                httpListener.stopListener();
            }
        }
        return result;
    }

    private void validateState(AuthorizationResult authorizationResult) {
        if (StringHelper.isBlank(authorizationResult.state()) ||
                !authorizationResult.state().equals(interactiveRequest.state())) {

            throw new MsalClientException("State returned in authorization result is blank or does " +
                    "not match state sent on outgoing request",
                    AuthenticationErrorCode.INVALID_AUTHORIZATION_RESULT);
        }
    }

    private void startHttpListener(AuthorizationResponseHandler handler) {
        // if port is unspecified, set to 0, which will cause socket to find a free port
        int port = interactiveRequest.interactiveRequestParameters().redirectUri().getPort() == -1 ?
                0 :
                interactiveRequest.interactiveRequestParameters().redirectUri().getPort();

        httpListener = new HttpListener();
        httpListener.startListener(port, handler);

        //If no port is passed, http listener finds a free one. We should update redirect URL to
        // point to this port.
        if (port != httpListener.port()) {
            updateRedirectUrl();
        }
    }

    private void updateRedirectUrl() {
        try {
            URI updatedRedirectUrl = new URI("http://localhost:" + httpListener.port());
            interactiveRequest.interactiveRequestParameters().redirectUri(updatedRedirectUrl);
            LOG.debug("Redirect URI updated to" + updatedRedirectUrl);
        } catch (URISyntaxException ex) {
            throw new MsalClientException("Error updating redirect URI. Not a valid URI format",
                    AuthenticationErrorCode.INVALID_REDIRECT_URI);
        }
    }
    private static List<String> getOpenToolsLinux() {
            return Arrays.asList("xdg-open", "gnome-open", "kfmclient", "microsoft-edge", "wslview");
    }

    private static String getExecutablePath(String executable) {
        String pathEnvVar = System.getenv("PATH");
        if (pathEnvVar != null) {
            String[] paths = pathEnvVar.split(File
                    .pathSeparator);
            for (String basePath : paths) {
                String path = basePath + File.separator + executable;
                if (new File(path).exists()) {
                    return path;
                }
            }
        }
        return null;
    }

    private void openDefaultSystemBrowser(URL url){
        if (OSHelper.isWindows()) { //windows
            openDefaultSystemBrowserInWindows(url);
        } else if (OSHelper.isMac()) { // mac os
            openDefaultSystemBrowserInMac(url);
        } else if (OSHelper.isLinux()) { //linux or unix os
            openDefaultSystemBrowserInLinux(url);
        } else {
            throw new UnsupportedOperationException(OSHelper.getOs() + "Operating system not supported exception.");
        }
    }

    private static void openDefaultSystemBrowserInWindows(URL url){
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(url.toURI());
                LOG.debug("Opened default system browser");
            } else {
                throw new MsalClientException("Unable to open default system browser",
                        AuthenticationErrorCode.DESKTOP_BROWSER_NOT_SUPPORTED);
            }
        } catch (URISyntaxException | IOException ex) {
            throw new MsalClientException(ex);
        }
    }

    private static void openDefaultSystemBrowserInMac(URL url){
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec("open " + url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void openDefaultSystemBrowserInLinux(URL url){
        String sudoUser = System.getenv("SUDO_USER");
        if (sudoUser != null && !sudoUser.isEmpty()) {
            throw new MsalClientException(LINUX_XDG_OPEN, LINUX_OPEN_AS_SUDO_NOT_SUPPORTED);
        }

        boolean opened = false;
        List<String> openTools = getOpenToolsLinux();
        for (String openTool : openTools) {
            String openToolPath = getExecutablePath(openTool);
            if (openToolPath != null) {
                Runtime runtime = Runtime.getRuntime();
                try {
                    runtime.exec(openTool + " " + url);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                opened = true;
                break;
            }
        }
        if (!opened) {
            throw new MsalClientException(LINUX_XDG_OPEN, LINUX_OPEN_AS_SUDO_NOT_SUPPORTED);
        }
    }

    private AuthorizationResult getAuthorizationResultFromHttpListener() {
        AuthorizationResult result = null;
        try {
            int timeFromParameters = interactiveRequest.interactiveRequestParameters().httpPollingTimeoutInSeconds();
            long expirationTime;

            if (timeFromParameters > 0) {
                LOG.debug(String.format("Listening for authorization result. Listener will timeout after %S seconds.", timeFromParameters));
                expirationTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + timeFromParameters;
            } else {
                LOG.warn("Listening for authorization result. Timeout configured to less than 1 second, listener will use a 1 second timeout instead.");
                expirationTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + 1;
            }

            while (result == null && !interactiveRequest.futureReference().get().isCancelled()) {
                if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) > expirationTime) {
                    LOG.warn(String.format("Listener timed out after %S seconds, no authorization code was returned from the server during that time.", timeFromParameters));
                    break;
                }

                result = authorizationResultQueue.poll(100, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            throw new MsalClientException(e);
        }

        if (result == null || StringHelper.isBlank(result.code())) {
            throw new MsalClientException("No Authorization code was returned from the server",
                    AuthenticationErrorCode.INVALID_AUTHORIZATION_RESULT);
        }
        return result;
    }

    private AuthenticationResult acquireTokenWithAuthorizationCode(AuthorizationResult authorizationResult)
            throws Exception {
        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder(authorizationResult.code(), interactiveRequest.interactiveRequestParameters().redirectUri())
                .scopes(interactiveRequest.interactiveRequestParameters().scopes())
                .codeVerifier(interactiveRequest.verifier())
                .claims(interactiveRequest.interactiveRequestParameters().claims())
                .build();

        RequestContext context = new RequestContext(
                clientApplication,
                PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE,
                parameters,
                interactiveRequest.requestContext().userIdentifier());

        AuthorizationCodeRequest authCodeRequest = new AuthorizationCodeRequest(
                parameters,
                clientApplication,
                context);

        Authority authority;

        //The result field of an AuthorizationResult object is only set if the response contained the 'cloud_instance_host_name' key,
        // which indicates that this token request is instance aware and should use that as the environment value
        //Otherwise, use the authority value from the client application
        if (authorizationResult.environment() != null) {
            authority = Authority.createAuthority(new URL(clientApplication.authenticationAuthority.canonicalAuthorityUrl.getProtocol(),
                    authorizationResult.environment(),
                    clientApplication.authenticationAuthority.canonicalAuthorityUrl.getFile()));
        } else {
            authority = clientApplication.authenticationAuthority;
        }

        AcquireTokenByAuthorizationGrantSupplier acquireTokenByAuthorizationGrantSupplier =
                new AcquireTokenByAuthorizationGrantSupplier(
                        clientApplication,
                        authCodeRequest,
                        authority);

        return acquireTokenByAuthorizationGrantSupplier.execute();
    }
}