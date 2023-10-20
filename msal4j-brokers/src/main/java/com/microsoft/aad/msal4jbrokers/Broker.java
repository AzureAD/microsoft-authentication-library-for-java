// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jbrokers;

import com.microsoft.aad.msal4j.*;
import com.microsoft.azure.javamsalruntime.Account;
import com.microsoft.azure.javamsalruntime.AuthParameters;
import com.microsoft.azure.javamsalruntime.AuthResult;
import com.microsoft.azure.javamsalruntime.MsalInteropException;
import com.microsoft.azure.javamsalruntime.MsalRuntimeInterop;
import com.microsoft.azure.javamsalruntime.ReadAccountResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Broker implements IBroker {
    private static final Logger LOG = LoggerFactory.getLogger(Broker.class);

    private static MsalRuntimeInterop interop;
    private static Boolean brokerAvailable;

    private boolean supportWindows;

    static {
        try {
            //MsalRuntimeInterop performs various initialization steps in a similar static block,
            // so when an MsalRuntimeBroker is created this will cause the interop layer to initialize
            interop = new MsalRuntimeInterop();
        } catch (MsalInteropException e) {
            throw new MsalClientException(String.format("Could not initialize MSALRuntime: %s", e.getErrorMessage()), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        }
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(PublicClientApplication application, SilentParameters parameters) {
        String correlationID = application.correlationId() == null ? generateCorrelationID() : application.correlationId();
        Account accountResult = null;

        //If request has an account ID, MSALRuntime likely has data cached for that account that we can retrieve
        if (parameters.account() != null) {
            try {
                accountResult = ((ReadAccountResult) interop.readAccountById(parameters.account().homeAccountId().split("\\.")[0], correlationID).get()).getAccount();
            } catch (InterruptedException | ExecutionException ex) {
                throw new MsalClientException(String.format("MSALRuntime async operation interrupted when waiting for result: %s", ex.getMessage()), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
            }
        }

        try {
            AuthParameters.AuthParametersBuilder authParamsBuilder = new AuthParameters.
                    AuthParametersBuilder(application.clientId(),
                    application.authority(),
                    String.join(" ", parameters.scopes()))
                    .additionalParameters(parameters.extraQueryParameters());

            //If POP auth scheme configured, set parameters to get MSALRuntime to return POP tokens
            if (parameters.proofOfPossession() != null) {
                authParamsBuilder.popParameters(parameters.proofOfPossession().getHttpMethod().methodName,
                        parameters.proofOfPossession().getUri(),
                        parameters.proofOfPossession().getNonce());
            }

            AuthParameters authParameters = authParamsBuilder.build();

            if (accountResult == null) {
                return interop.signInSilently(authParameters, correlationID)
                        .thenCompose(acctResult -> interop.acquireTokenSilently(authParameters, correlationID, ((AuthResult) acctResult).getAccount()))
                        .thenApply(authResult -> parseBrokerAuthResult(
                                application.authority(),
                                ((AuthResult) authResult).getIdToken(),
                                ((AuthResult) authResult).getAccessToken(),
                                ((AuthResult) authResult).getAccount().getAccountId(),
                                ((AuthResult) authResult).getAccount().getClientInfo(),
                                ((AuthResult) authResult).getAccessTokenExpirationTime(),
                                ((AuthResult) authResult).isPopAuthorization()));
            } else {
                return interop.acquireTokenSilently(authParameters, correlationID, accountResult)
                        .thenApply(authResult -> parseBrokerAuthResult(application.authority(),
                                ((AuthResult) authResult).getIdToken(),
                                ((AuthResult) authResult).getAccessToken(),
                                ((AuthResult) authResult).getAccount().getAccountId(),
                                ((AuthResult) authResult).getAccount().getClientInfo(),
                                ((AuthResult) authResult).getAccessTokenExpirationTime(),
                                ((AuthResult) authResult).isPopAuthorization()));
            }
        } catch (MsalInteropException interopException) {
            throw new MsalClientException(interopException.getErrorMessage(), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        }
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(PublicClientApplication application, InteractiveRequestParameters parameters) {
        String correlationID = application.correlationId() == null ? generateCorrelationID() : application.correlationId();

        try {
            AuthParameters.AuthParametersBuilder authParamsBuilder = new AuthParameters.
                    AuthParametersBuilder(application.clientId(),
                    application.authority(),
                    String.join(" ", parameters.scopes()))
                    .redirectUri(parameters.redirectUri().toString())
                    .additionalParameters(parameters.extraQueryParameters());

            //If POP auth scheme configured, set parameters to get MSALRuntime to return POP tokens
            if (parameters.proofOfPossession() != null) {
                authParamsBuilder.popParameters(parameters.proofOfPossession().getHttpMethod().methodName,
                        parameters.proofOfPossession().getUri(),
                        parameters.proofOfPossession().getNonce());
            }

            AuthParameters authParameters = authParamsBuilder.build();

            return interop.signInInteractively(parameters.windowHandle(), authParameters, correlationID, parameters.loginHint())
                    .thenCompose(acctResult -> interop.acquireTokenInteractively(parameters.windowHandle(), authParameters, correlationID, ((AuthResult) acctResult).getAccount()))
                    .thenApply(authResult -> parseBrokerAuthResult(
                            application.authority(),
                            ((AuthResult) authResult).getIdToken(),
                            ((AuthResult) authResult).getAccessToken(),
                            ((AuthResult) authResult).getAccount().getAccountId(),
                            ((AuthResult) authResult).getAccount().getClientInfo(),
                            ((AuthResult) authResult).getAccessTokenExpirationTime(),
                            ((AuthResult) authResult).isPopAuthorization()));
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
        String correlationID = application.correlationId() == null ? generateCorrelationID() : application.correlationId();

        try {
            AuthParameters.AuthParametersBuilder authParamsBuilder = new AuthParameters.
                    AuthParametersBuilder(application.clientId(),
                    application.authority(),
                    String.join(" ", parameters.scopes()))
                    .additionalParameters(parameters.extraQueryParameters());

            //If POP auth scheme configured, set parameters to get MSALRuntime to return POP tokens
            if (parameters.proofOfPossession() != null) {
                authParamsBuilder.popParameters(parameters.proofOfPossession().getHttpMethod().methodName,
                        parameters.proofOfPossession().getUri(),
                        parameters.proofOfPossession().getNonce());
            }

            AuthParameters authParameters = authParamsBuilder.build();

            return interop.signInSilently(authParameters, correlationID)
                    .thenCompose(acctResult -> interop.acquireTokenSilently(authParameters, correlationID, ((AuthResult) acctResult).getAccount()))
                    .thenApply(authResult -> parseBrokerAuthResult(
                            application.authority(),
                            ((AuthResult) authResult).getIdToken(),
                            ((AuthResult) authResult).getAccessToken(),
                            ((AuthResult) authResult).getAccount().getAccountId(),
                            ((AuthResult) authResult).getAccount().getClientInfo(),
                            ((AuthResult) authResult).getAccessTokenExpirationTime(),
                            ((AuthResult) authResult).isPopAuthorization()));
        } catch (MsalInteropException interopException) {
            throw new MsalClientException(interopException.getErrorMessage(), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        }
    }

    @Override
    public void removeAccount(PublicClientApplication application, IAccount msalJavaAccount) {
        String correlationID = application.correlationId() == null ? generateCorrelationID() : application.correlationId();

        try {
            Account msalRuntimeAccount = ((ReadAccountResult) interop.readAccountById(msalJavaAccount.homeAccountId().split("\\.")[0], correlationID).get()).getAccount();

            if (msalRuntimeAccount != null) {
                interop.signOutSilently(application.clientId(), correlationID, msalRuntimeAccount);
            }
        } catch (MsalInteropException interopException) {
            throw new MsalClientException(interopException.getErrorMessage(), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        } catch (InterruptedException | ExecutionException ex) {
            throw new MsalClientException(String.format("MSALRuntime async operation interrupted when waiting for result: %s", ex.getMessage()), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        }
    }

    /**
     * Calls MSALRuntime's startup API. If MSALRuntime started successfully, we can assume that the broker is available for use.
     *
     * If an exception is thrown when trying to start MSALRuntime, we assume that we cannot use the broker and will not make any more attempts to do so.
     *
     * @return boolean representing whether or not MSALRuntime started successfully
     */
    @Override
    public boolean isBrokerAvailable() {
        //brokerAvailable is only set after the first attempt to call MSALRuntime's startup API
        if (brokerAvailable == null) {
            try {
                interop.startupMsalRuntime();

                LOG.info("MSALRuntime started successfully. MSAL Java will use MSALRuntime in all supported broker flows.");

                brokerAvailable = true;
            } catch (MsalInteropException e) {
                LOG.warn("Exception thrown when trying to start MSALRuntime: {}", e.getErrorMessage());
                LOG.warn("MSALRuntime could not be started. MSAL Java will fall back to non-broker flows.");

                brokerAvailable = false;
            }
        }

        return brokerAvailable;
    }

    /**
     * Toggles whether or not detailed MSALRuntime logs will appear in MSAL Java's normal logging framework.
     *
     * If enabled, you will see logs directly from MSALRuntime, containing verbose information relating to telemetry, API calls,successful/failed requests, and more.
     * These logs will appear alongside MSAL Java's logs (with a message indicating they came from MSALRuntime), and will follow the same log level as MSAL Java's logs (info/debug/error/etc.).
     *
     * If disabled (default), MSAL Java will still produce some logs related to MSALRuntime, particularly in error messages, but will be much less verbose.
     *
     * @param enableLogging true to enable MSALRuntime logs, false to disable it
     */
    public void enableBrokerLogging(boolean enableLogging) {
        try {
            MsalRuntimeInterop.enableLogging(enableLogging);
        } catch (Exception ex) {
            throw new MsalClientException(String.format("Error occurred when calling MSALRuntime logging API: %s", ex.getMessage()), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        }
    }

    /**
     * If enabled, Personal Identifiable Information (PII) can appear in logs and error messages produced by MSALRuntime.
     *
     * If disabled (default), PII will not be shown, and you will simply see "(PII)" or similar notes in places where PII data would have appeared.
     *
     * @param enablePII true to allow PII to appear in logs and error messages, false to disallow it
     */
    public void enableBrokerPIILogging(boolean enablePII) {
        try {
            MsalRuntimeInterop.enableLoggingPii(enablePII);
        } catch (Exception ex) {
            throw new MsalClientException(String.format("Error occurred when calling MSALRuntime PII logging API: %s", ex.getMessage()), AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        }
    }

    //Generates a random correlation ID, used when a correlation ID was not set at the application level
    private String generateCorrelationID() {
        return UUID.randomUUID().toString();
    }

    public static class Builder {
        private boolean supportWindows = false;

        public Builder() {
        }

        public Builder supportWindows(boolean val) {
            supportWindows = val;
            return this;
        }

        public Broker build() {
            return new Broker(this);
        }
    }

    private Broker(Builder builder) {
        supportWindows = builder.supportWindows;
    }
}
