// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jbrokers;

import com.microsoft.aad.msal4j.*;
import com.microsoft.azure.javamsalruntime.AuthParams;
import com.microsoft.azure.javamsalruntime.MsalRuntimeInterop;
import com.microsoft.azure.javamsalruntime.ResultHandler;

public class MsalRuntimeBroker implements IBroker {

    @Override
    public boolean isAvailable() {
        //TODO: ensure it's a supported platform/architecture
        //TODO: ensure an msalruntime.dll is available for the platform/architecture
        //TODO: check one method from msalruntime.dll to make sure it works?

        return true;
    }

    @Override
    public IAuthenticationResult acquireToken(PublicClientApplication application,
                                              SilentParameters parameters) throws Exception {
        MsalRuntimeInterop interop = new MsalRuntimeInterop();
        interop.initializeBroker();

        ResultHandler resultHandler = new ResultHandler();

        // If request has an account ID, try to get account info from MSALRuntime
        if (parameters.account() != null) {
            interop.readAccountById(resultHandler, parameters.account().homeAccountId());
        }

        AuthParams authParams = new AuthParams(
                interop.msalRuntimeLibrary, interop.errorHandler, application.clientId(), application.authority(),
                parameters.scopes().toString(),
                null, // No redirect url in a silent request
                parameters.claims().toString());

        // If request did not have an account ID or MSALRuntime did not return an account, attempt a silent sign in
        if (resultHandler.getAuthResult().getAccount() == null) {
            interop.signInSilently(resultHandler, authParams);
        }

        // Account information populated, attempt to acquire access token
        interop.acquireTokenSilently(resultHandler, authParams);

        // Parse the results of the MSALRuntime calls into an MSAL Java IAuthenticationResult
        return parseBrokerAuthResult(parameters.authorityUrl(), resultHandler.getAuthResult().getIdToken(),
                        resultHandler.getAuthResult().getAccessToken(), resultHandler.getAuthResult().getAccount().getAccountId(),
                        resultHandler.getAuthResult().getAccount().getAccountClientInfo(), resultHandler.getAuthResult().getAccessTokenExpirationTime());
    }

    @Override
    public IAuthenticationResult acquireToken(PublicClientApplication application,
                                              InteractiveRequestParameters parameters) throws Exception {
        MsalRuntimeInterop interop = new MsalRuntimeInterop();
        interop.initializeBroker();

        ResultHandler resultHandler = new ResultHandler();

        AuthParams authParams = new AuthParams(
                interop.msalRuntimeLibrary, interop.errorHandler, application.clientId(), application.authority(),
                parameters.scopes().toString(),
                parameters.redirectUri().toString(),
                parameters.claims().toString());

        // Perform an interactive sign in to get the user information
        interop.signInInteractively(resultHandler, authParams, parameters.loginHint());

        // Account information populated, attempt to acquire access token
        interop.acquireTokenInteractively(resultHandler, authParams);

        // Parse the results of the MSALRuntime calls into an MSAL Java IAuthenticationResult
        return parseBrokerAuthResult(application.authority(), resultHandler.getAuthResult().getIdToken(),
                resultHandler.getAuthResult().getAccessToken(), resultHandler.getAuthResult().getAccount().getAccountId(),
                resultHandler.getAuthResult().getAccount().getAccountClientInfo(), resultHandler.getAuthResult().getAccessTokenExpirationTime());
    }

    @Override
    public IAuthenticationResult acquireToken(PublicClientApplication application,
                                              UserNamePasswordParameters parameters) throws Exception {
        MsalRuntimeInterop interop = new MsalRuntimeInterop();
        interop.initializeBroker();

        ResultHandler resultHandler = new ResultHandler();

        AuthParams authParams = new AuthParams(
                interop.msalRuntimeLibrary, interop.errorHandler, application.clientId(), application.authority(),
                parameters.scopes().toString(),
                null, // No redirect url in a silent request
                parameters.claims().toString());

        authParams.setUsernamePassword(interop.msalRuntimeLibrary, interop.errorHandler,
                parameters.username(), String.valueOf(parameters.password()));

        //No interaction is needed for the username/password flow
        interop.signInSilently(resultHandler, authParams);

        // Account information populated, attempt to acquire access token
        interop.acquireTokenSilently(resultHandler, authParams);

        // Parse the results of the MSALRuntime calls into an MSAL Java IAuthenticationResult
        return parseBrokerAuthResult(application.authority(), resultHandler.getAuthResult().getIdToken(),
                resultHandler.getAuthResult().getAccessToken(), resultHandler.getAuthResult().getAccount().getAccountId(),
                resultHandler.getAuthResult().getAccount().getAccountClientInfo(), resultHandler.getAuthResult().getAccessTokenExpirationTime());

    }

    @Override
    public void removeAccount(PublicClientApplication application, IAccount account) throws Exception {
        MsalRuntimeInterop interop = new MsalRuntimeInterop();
        interop.initializeBroker();

        ResultHandler resultHandler = new ResultHandler();

        interop.signOutSilently(resultHandler, application.clientId(), account.homeAccountId());
    }
}
