// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jbrokers;

import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IBroker;
import com.microsoft.aad.msal4j.InteractiveRequestParameters;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.SilentParameters;
import com.microsoft.aad.msal4j.UserNamePasswordParameters;
import com.microsoft.azure.javamsalruntime.Account;
import com.microsoft.azure.javamsalruntime.AuthParameters;
import com.microsoft.azure.javamsalruntime.AuthResult;
import com.microsoft.azure.javamsalruntime.MsalInteropException;
import com.microsoft.azure.javamsalruntime.MsalRuntimeInterop;
import com.microsoft.azure.javamsalruntime.ReadAccountResult;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MsalRuntimeBroker implements IBroker {
    private static MsalRuntimeInterop interop;

    static {
        try {
            //MsalRuntimeInterop performs various initialization steps in a similar static block,
            // so when an MsalRuntimeBroker is created this will cause the interop layer to initialize
            interop = new MsalRuntimeInterop();
        } catch (MsalInteropException e) {
            //TODO: Meaningful error message explaining why MSALRuntime failed to initialize (unsupported platform, error from MSALRuntime, etc.)
        }
    }

    @Override
    public void acquireToken(PublicClientApplication application, SilentParameters parameters, CompletableFuture<IAuthenticationResult> future) throws ExecutionException, InterruptedException {
        Account accountResult = null;

        //If request has an account ID, MSALRuntime data cached for that account
        //  try to get account info from MSALRuntime
        if (parameters.account() != null) {
            accountResult = ((ReadAccountResult) interop.readAccountById(parameters.account().homeAccountId(), application.correlationId()).get()).getAccount();
        }

        try (AuthParameters authParameters =
                     new AuthParameters
                             .AuthParametersBuilder(application.clientId(),
                             application.authority(),
                             parameters.scopes().toString())
                             .build()) {

            //Account was not cached in MSALRuntime, must perform sign in first to populate account info
            if (accountResult == null) {
                accountResult = ((AuthResult) interop.signInSilently(authParameters, application.correlationId()).get()).getAccount();
            }

            //Either account was already cached or silent sign in was successful, so we can retrieve tokens from MSALRuntime,
            // parse the result into an MSAL Java AuthenticationResult, and complete the future
            interop.acquireTokenSilently(authParameters, application.correlationId(), accountResult)
                    .thenApply(authResult -> future.complete(parseBrokerAuthResult(
                            application.authority(),
                            ((AuthResult) authResult).getIdToken(),
                            ((AuthResult) authResult).getAccessToken(),
                            ((AuthResult) authResult).getAccount().getAccountId(),
                            ((AuthResult) authResult).getAccount().getClientInfo(),
                            ((AuthResult) authResult).getAccessTokenExpirationTime())));
        }
    }

    @Override
    public void acquireToken(PublicClientApplication application, InteractiveRequestParameters parameters, CompletableFuture<IAuthenticationResult> future) throws ExecutionException, InterruptedException {
        try (AuthParameters authParameters =
                     new AuthParameters
                             .AuthParametersBuilder(application.clientId(),
                             application.authority(),
                             parameters.scopes().toString())
                             .claims(parameters.claims().toString())
                             .build()) {

            Account accountResult = ((AuthResult) interop.signInInteractively(parameters.windowHandle(), authParameters, application.correlationId(), parameters.loginHint()).get()).getAccount();

            interop.acquireTokenInteractively(parameters.windowHandle(), authParameters, application.correlationId(), accountResult)
                    .thenApply(authResult -> future.complete(parseBrokerAuthResult(
                            application.authority(),
                            ((AuthResult) authResult).getIdToken(),
                            ((AuthResult) authResult).getAccessToken(),
                            ((AuthResult) authResult).getAccount().getAccountId(),
                            ((AuthResult) authResult).getAccount().getClientInfo(),
                            ((AuthResult) authResult).getAccessTokenExpirationTime())));
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public void acquireToken(PublicClientApplication application, UserNamePasswordParameters parameters, CompletableFuture<IAuthenticationResult> future) throws ExecutionException, InterruptedException {

        try (AuthParameters authParameters =
                     new AuthParameters
                             .AuthParametersBuilder(application.clientId(),
                             application.authority(),
                             parameters.scopes().toString())
                             .claims(parameters.claims().toString())
                             .build()) {

            authParameters.setUsernamePassword(parameters.username(), new String(parameters.password()));

            Account accountResult = ((AuthResult) interop.signInSilently(authParameters, application.correlationId()).get()).getAccount();

            //Either account was already cached or silent sign in was successful, so we can retrieve tokens from MSALRuntime,
            // parse the result into an MSAL Java AuthenticationResult, and complete the future
            interop.acquireTokenSilently(authParameters, application.correlationId(), accountResult)
                    .thenApply(authResult -> future.complete(parseBrokerAuthResult(
                            application.authority(),
                            ((AuthResult) authResult).getIdToken(),
                            ((AuthResult) authResult).getAccessToken(),
                            ((AuthResult) authResult).getAccount().getAccountId(),
                            ((AuthResult) authResult).getAccount().getClientInfo(),
                            ((AuthResult) authResult).getAccessTokenExpirationTime())));
        }
    }

    //Simple manual test for early development/testing
    public static void main(String args[]) throws MalformedURLException, ExecutionException, InterruptedException {
        String clientId = "903c8a8a-9e74-415e-9921-711a293d90cb";
        String authority = "https://login.microsoftonline.com/common";
        String scopes = "https://graph.microsoft.com/.default";


        MsalRuntimeBroker broker = new MsalRuntimeBroker();

        PublicClientApplication pca = PublicClientApplication.builder(
                clientId).
                authority(authority).
                correlationId(UUID.randomUUID().toString()).
                build();

        SilentParameters parameters = SilentParameters.builder(Collections.singleton(scopes)).build();

        CompletableFuture<IAuthenticationResult> future = new CompletableFuture<>();

        broker.acquireToken(pca, parameters, future);

        IAuthenticationResult result = future.get();

        System.out.println(result.idToken());
        System.out.println(result.accessToken());
    }
}
