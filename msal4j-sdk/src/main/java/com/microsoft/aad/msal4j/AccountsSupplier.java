// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.URL;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

class AccountsSupplier implements Supplier<Set<IAccount>> {

    AbstractClientApplicationBase clientApplication;
    MsalRequest msalRequest;

    AccountsSupplier(AbstractClientApplicationBase clientApplication, MsalRequest msalRequest) {

        this.clientApplication = clientApplication;
        this.msalRequest = msalRequest;
    }

    @Override
    public Set<IAccount> get() {
        try {
            return clientApplication.tokenCache.getAccounts
                    (clientApplication.clientId());

        } catch (Exception ex) {
            clientApplication.log.warn(
                    LogHelper.createMessage(
                            String.format("Execution of %s failed: %s", this.getClass(), ex.getMessage()),
                            msalRequest.headers().getHeaderCorrelationIdValue()));

            throw new CompletionException(ex);
        }
    }
}
