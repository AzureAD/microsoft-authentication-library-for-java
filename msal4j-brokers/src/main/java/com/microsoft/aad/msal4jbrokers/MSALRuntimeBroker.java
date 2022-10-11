package com.microsoft.aad.msal4jbrokers;

import com.microsoft.aad.msal4j.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class MSALRuntimeBroker implements IBroker {

    @Override
    public IAuthenticationResult acquireToken(PublicClientApplication application, SilentParameters requestParameters) {
        log.debug("Should not call this API if msal runtime init failed");
        throw new MsalClientException("Broker implementation missing", "missing_broker");
    }

    @Override
    public IAuthenticationResult acquireToken(PublicClientApplication application, InteractiveRequestParameters requestParameters) {
        throw new MsalClientException("Broker implementation missing", "missing_broker");
    }

    @Override
    public IAuthenticationResult acquireToken(PublicClientApplication application, UserNamePasswordParameters requestParameters) {
        throw new MsalClientException("Broker implementation missing", "missing_broker");
    }

    @Override
    public CompletableFuture removeAccount(IAccount account) {
        throw new MsalClientException("Broker implementation missing", "missing_broker");
    }
}
