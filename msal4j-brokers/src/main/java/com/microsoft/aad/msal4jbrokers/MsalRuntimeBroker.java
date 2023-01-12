// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jbrokers;

import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IBroker;
import com.microsoft.aad.msal4j.InteractiveRequestParameters;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.SilentParameters;
import com.microsoft.aad.msal4j.UserNamePasswordParameters;
import com.microsoft.aad.msal4j.MsalClientException;
import com.microsoft.aad.msal4j.AuthenticationErrorCode;
import com.microsoft.aad.msal4j.IAccount;
import com.microsoft.azure.javamsalruntime.Account;
import com.microsoft.azure.javamsalruntime.AuthParameters;
import com.microsoft.azure.javamsalruntime.AuthResult;
import com.microsoft.azure.javamsalruntime.MsalInteropException;
import com.microsoft.azure.javamsalruntime.MsalRuntimeInterop;
import com.microsoft.azure.javamsalruntime.ReadAccountResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MsalRuntimeBroker implements IBroker {
    private static MsalRuntimeInterop interop;

    static {
        try {
            //MsalRuntimeInterop performs various initialization steps in a similar static block,
            // so when an MsalRuntimeBroker is created this will cause the interop layer to initialize
            interop = new MsalRuntimeInterop();
            interop.startupMsalRuntime();
        } catch (MsalInteropException e) {
            throw new MsalClientException(String.format("Could not initialize MSALRuntime: %s", e.getErrorMessage()), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        }
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(PublicClientApplication application, SilentParameters parameters) {
        Account accountResult = null;

        //If request has an account ID, MSALRuntime likely has data cached for that account that we can retrieve
        if (parameters.account() != null) {
            try {
                accountResult = ((ReadAccountResult) interop.readAccountById(parameters.account().homeAccountId(), application.correlationId()).get()).getAccount();
            } catch (InterruptedException | ExecutionException ex) {
                throw new MsalClientException(String.format("MSALRuntime async operation interrupted when waiting for result: %s", ex.getMessage()), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
            }
        }

        try {
            AuthParameters authParameters = new AuthParameters
                    .AuthParametersBuilder(application.clientId(),
                    application.authority(),
                    String.join(" ", parameters.scopes()))
                    .build();

            if (accountResult == null) {
                return interop.signInSilently(authParameters, application.correlationId())
                        .thenCompose(acctResult -> interop.acquireTokenSilently(authParameters, application.correlationId(), ((AuthResult) acctResult).getAccount()))
                        .thenApply(authResult -> parseBrokerAuthResult(
                                application.authority(),
                                ((AuthResult) authResult).getIdToken(),
                                ((AuthResult) authResult).getAccessToken(),
                                ((AuthResult) authResult).getAccount().getAccountId(),
                                ((AuthResult) authResult).getAccount().getClientInfo(),
                                ((AuthResult) authResult).getAccessTokenExpirationTime()));
            } else {
                return interop.acquireTokenSilently(authParameters, application.correlationId(), accountResult)
                        .thenApply(authResult -> parseBrokerAuthResult(application.authority(),
                                ((AuthResult) authResult).getIdToken(),
                                ((AuthResult) authResult).getAccessToken(),
                                ((AuthResult) authResult).getAccount().getAccountId(),
                                ((AuthResult) authResult).getAccount().getClientInfo(),
                                ((AuthResult) authResult).getAccessTokenExpirationTime())

                        );
            }
        } catch (MsalInteropException interopException) {
            throw new MsalClientException(interopException.getErrorMessage(), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        }
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(PublicClientApplication application, InteractiveRequestParameters parameters) {
        try {
            AuthParameters authParameters = new AuthParameters
                    .AuthParametersBuilder(application.clientId(),
                    application.authority(),
                    String.join(" ", parameters.scopes()))
                    .build();

            return interop.signInInteractively(parameters.windowHandle(), authParameters, application.correlationId(), parameters.loginHint())
                    .thenCompose(acctResult -> interop.acquireTokenInteractively(parameters.windowHandle(), authParameters, application.correlationId(), ((AuthResult) acctResult).getAccount()))
                    .thenApply(authResult -> parseBrokerAuthResult(
                            application.authority(),
                            ((AuthResult) authResult).getIdToken(),
                            ((AuthResult) authResult).getAccessToken(),
                            ((AuthResult) authResult).getAccount().getAccountId(),
                            ((AuthResult) authResult).getAccount().getClientInfo(),
                            ((AuthResult) authResult).getAccessTokenExpirationTime())
                    );
        } catch (MsalInteropException interopException) {
            throw new MsalClientException(interopException.getErrorMessage(), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(PublicClientApplication application, UserNamePasswordParameters parameters) {
        try (AuthParameters authParameters =
                     new AuthParameters
                             .AuthParametersBuilder(application.clientId(),
                             application.authority(),
                             String.join(" ", parameters.scopes()))
                             .build()) {

            authParameters.setUsernamePassword(parameters.username(), new String(parameters.password()));

            return interop.signInSilently(authParameters, application.correlationId())
                    .thenCompose(acctResult -> interop.acquireTokenSilently(authParameters, application.correlationId(), ((AuthResult) acctResult).getAccount()))
                    .thenApply(authResult -> parseBrokerAuthResult(
                            application.authority(),
                            ((AuthResult) authResult).getIdToken(),
                            ((AuthResult) authResult).getAccessToken(),
                            ((AuthResult) authResult).getAccount().getAccountId(),
                            ((AuthResult) authResult).getAccount().getClientInfo(),
                            ((AuthResult) authResult).getAccessTokenExpirationTime()));
        } catch (MsalInteropException interopException) {
            throw new MsalClientException(interopException.getErrorMessage(), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        }
    }

    @Override
    public void removeAccount(PublicClientApplication application, IAccount msalJavaAccount) {
        try {
            Account msalRuntimeAccount = ((ReadAccountResult) interop.readAccountById(msalJavaAccount.homeAccountId(), application.correlationId()).get()).getAccount();

            if (msalRuntimeAccount != null) {
                interop.signOutSilently(application.clientId(), application.correlationId(), msalRuntimeAccount);
            }
        } catch (MsalInteropException interopException) {
            throw new MsalClientException(interopException.getErrorMessage(), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        } catch (InterruptedException | ExecutionException ex) {
            throw new MsalClientException(String.format("MSALRuntime async operation interrupted when waiting for result: %s", ex.getMessage()), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        }
    }

    public boolean isBrokerAvailable() {
        return MsalRuntimeInterop.isPlatformSupported();
    }
}
