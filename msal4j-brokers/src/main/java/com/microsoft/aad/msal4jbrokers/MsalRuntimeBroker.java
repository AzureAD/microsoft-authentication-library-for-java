// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jbrokers;

import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IBroker;
import com.microsoft.aad.msal4j.InteractiveRequestParameters;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.SilentParameters;
import com.microsoft.aad.msal4j.UserNamePasswordParameters;
import com.microsoft.aad.msal4j.MsalException;
import com.microsoft.aad.msal4j.AuthenticationErrorCode;
import com.microsoft.aad.msal4j.IAccount;
import com.microsoft.azure.javamsalruntime.Account;
import com.microsoft.azure.javamsalruntime.AuthParameters;
import com.microsoft.azure.javamsalruntime.AuthResult;
import com.microsoft.azure.javamsalruntime.MsalInteropException;
import com.microsoft.azure.javamsalruntime.MsalRuntimeFuture;
import com.microsoft.azure.javamsalruntime.MsalRuntimeInterop;
import com.microsoft.azure.javamsalruntime.ReadAccountResult;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CancellationException;
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
    public void acquireToken(PublicClientApplication application, SilentParameters parameters, CompletableFuture<IAuthenticationResult> future) {
        Account accountResult = null;
        //If request has an account ID, MSALRuntime data cached for that account
        //  try to get account info from MSALRuntime
        if (parameters.account() != null) {
            try {
                accountResult = ((ReadAccountResult) interop.readAccountById(parameters.account().homeAccountId(), application.correlationId()).get()).getAccount();
            } catch (InterruptedException | ExecutionException e) {
                //TODO: these exceptions can occur when waiting on a result from MSALRuntime. Not possible to continue? Rethrow exception or create MSAL exception?
            }
        }

        try (AuthParameters authParameters =
                     new AuthParameters
                             .AuthParametersBuilder(application.clientId(),
                             application.authority(),
                             parameters.scopes().toString())
                             .build()) {
            MsalRuntimeFuture currentAsyncOperation;

            //Account was not cached in MSALRuntime, must perform sign in first to populate account info
            if (accountResult == null) {
                currentAsyncOperation = interop.signInSilently(authParameters, application.correlationId());
                setFutureToCancel(future, currentAsyncOperation);
                accountResult = ((AuthResult) currentAsyncOperation.get()).getAccount();
            }
            //Either account was already cached or silent sign in was successful, so we can retrieve tokens from MSALRuntime,
            // parse the result into an MSAL Java AuthenticationResult, and complete the future
            currentAsyncOperation = interop.acquireTokenSilently(authParameters, application.correlationId(), accountResult);
            setFutureToCancel(future, currentAsyncOperation);
            currentAsyncOperation.thenApply(authResult -> future.complete(parseBrokerAuthResult(
                            application.authority(),
                            ((AuthResult) authResult).getIdToken(),
                            ((AuthResult) authResult).getAccessToken(),
                            ((AuthResult) authResult).getAccount().getAccountId(),
                            ((AuthResult) authResult).getAccount().getClientInfo(),
                            ((AuthResult) authResult).getAccessTokenExpirationTime())));
        } catch (MsalInteropException interopException) {
            throw new MsalException(interopException.getErrorMessage(), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        } catch (InterruptedException | ExecutionException ex) {
            //TODO: these exceptions can occur when waiting on a result from MSALRuntime. Not possible to continue? Rethrow exception or create MSAL exception?
        }
    }

    @Override
    public void acquireToken(PublicClientApplication application, InteractiveRequestParameters parameters, CompletableFuture<IAuthenticationResult> future) {

        try (AuthParameters authParameters =
                     new AuthParameters
                             .AuthParametersBuilder(application.clientId(),
                             application.authority(),
                             parameters.scopes().toString())
                             .claims(parameters.claims().toString())
                             .build()) {

            MsalRuntimeFuture currentMsalRuntimeFuture;

            currentMsalRuntimeFuture = interop.signInInteractively(parameters.windowHandle(), authParameters, application.correlationId(), parameters.loginHint());
            setFutureToCancel(future, currentMsalRuntimeFuture);
            Account accountResult = ((AuthResult) currentMsalRuntimeFuture.get()).getAccount();

            currentMsalRuntimeFuture = interop.acquireTokenInteractively(parameters.windowHandle(), authParameters, application.correlationId(), accountResult);
            setFutureToCancel(future, currentMsalRuntimeFuture);
            currentMsalRuntimeFuture.thenApply(authResult -> future.complete(parseBrokerAuthResult(
                            application.authority(),
                            ((AuthResult) authResult).getIdToken(),
                            ((AuthResult) authResult).getAccessToken(),
                            ((AuthResult) authResult).getAccount().getAccountId(),
                            ((AuthResult) authResult).getAccount().getClientInfo(),
                            ((AuthResult) authResult).getAccessTokenExpirationTime())));
        } catch (MsalInteropException interopException) {
            throw new MsalException(interopException.getErrorMessage(), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        } catch (InterruptedException | ExecutionException ex) {
            //TODO: these exceptions can occur when waiting on a result from MSALRuntime. Not possible to continue? Retrow exception or create MSAL exception?
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public void acquireToken(PublicClientApplication application, UserNamePasswordParameters parameters, CompletableFuture<IAuthenticationResult> future) {

        try (AuthParameters authParameters =
                     new AuthParameters
                             .AuthParametersBuilder(application.clientId(),
                             application.authority(),
                             parameters.scopes().toString())
                             .claims(parameters.claims().toString())
                             .build()) {

            MsalRuntimeFuture currentMsalRuntimeFuture;

            authParameters.setUsernamePassword(parameters.username(), new String(parameters.password()));

            currentMsalRuntimeFuture = interop.signInSilently(authParameters, application.correlationId());
            setFutureToCancel(future, currentMsalRuntimeFuture);
            Account accountResult = ((AuthResult) currentMsalRuntimeFuture.get()).getAccount();

            //Either account was already cached or silent sign in was successful, so we can retrieve tokens from MSALRuntime,
            // parse the result into an MSAL Java AuthenticationResult, and complete the future
            currentMsalRuntimeFuture = interop.acquireTokenSilently(authParameters, application.correlationId(), accountResult);
            setFutureToCancel(future, currentMsalRuntimeFuture);
            currentMsalRuntimeFuture.thenApply(authResult -> future.complete(parseBrokerAuthResult(
                            application.authority(),
                            ((AuthResult) authResult).getIdToken(),
                            ((AuthResult) authResult).getAccessToken(),
                            ((AuthResult) authResult).getAccount().getAccountId(),
                            ((AuthResult) authResult).getAccount().getClientInfo(),
                            ((AuthResult) authResult).getAccessTokenExpirationTime())));
        } catch (MsalInteropException interopException) {
            throw new MsalException(interopException.getErrorMessage(), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        } catch (InterruptedException | ExecutionException ex) {
            //TODO: these exceptions can occur when waiting on a result from MSALRuntime. Not possible to continue? Retrow exception or create MSAL exception?
        }
    }

    @Override
    public void removeAccount(PublicClientApplication application, IAccount msalJavaAccount) {
        try  {

            Account msalRuntimeAccount = ((ReadAccountResult) interop.readAccountById(msalJavaAccount.homeAccountId(), application.correlationId()).get()).getAccount();

            if (msalRuntimeAccount != null) {
                interop.signOutSilently(application.clientId(), application.correlationId(), msalRuntimeAccount);
            }
        } catch (MsalInteropException interopException) {
            throw new MsalException(interopException.getErrorMessage(), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        } catch (InterruptedException | ExecutionException ex) {
            //TODO: these exceptions can occur when waiting on a result from MSALRuntime. Not possible to continue? Retrow exception or create MSAL exception?
        }
    }

    /**
     * If the future returned by MSAL Java is canceled before we can complete it using a result from MSALRuntime, we must cancel the async operations MSALRuntime is performing
     *
     * However, there are multiple sequential calls that need to be made to MSALRuntime, each of which returns an MsalRuntimeFuture which we'd need to cancel
     *
     * This utility method encapsulates the logic for swapping which MsalRuntimeFuture gets canceled if the main future is canceled
     */
    public void setFutureToCancel(CompletableFuture<IAuthenticationResult> future, MsalRuntimeFuture futureToCancel) {
        future.whenComplete((result, ex) -> {if (ex instanceof CancellationException) futureToCancel.cancelAsyncOperation();});
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
