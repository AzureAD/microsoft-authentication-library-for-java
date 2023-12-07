// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Set;
import java.util.concurrent.CompletionException;

class RemoveAccountRunnable implements Runnable {

    private RequestContext requestContext;
    private AbstractClientApplicationBase clientApplication;
    IAccount account;

    RemoveAccountRunnable(MsalRequest msalRequest, IAccount account) {
        this.clientApplication = msalRequest.application();
        this.requestContext = msalRequest.requestContext();
        this.account = account;
    }

    @Override
    public void run() {
        try {
            clientApplication.tokenCache.removeAccount
                    (clientApplication.clientId(), account);

        } catch (Exception ex) {
            clientApplication.log.warn(
                    LogHelper.createMessage(
                            String.format("Execution of %s failed: %s", this.getClass(), ex.getMessage()),
                            requestContext.correlationId()));

            throw new CompletionException(ex);
        }
    }
}
