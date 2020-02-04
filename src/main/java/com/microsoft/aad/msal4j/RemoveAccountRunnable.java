// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Set;
import java.util.concurrent.CompletionException;

class RemoveAccountRunnable implements Runnable {

    private RequestContext requestContext;
    private ClientApplicationBase clientApplication;
    IAccount account;

    RemoveAccountRunnable(MsalRequest msalRequest, IAccount account) {
        this.clientApplication = msalRequest.application();
        this.requestContext = msalRequest.requestContext();
        this.account = account;
    }

    @Override
    public void run() {
        try {
            Set<String> aliases = AadInstanceDiscoveryProvider.getAliases(
                    clientApplication.authenticationAuthority.host());

            clientApplication.tokenCache.removeAccount
                    (clientApplication.clientId(), account, aliases);

        } catch (Exception ex) {
            clientApplication.log.error(
                    LogHelper.createMessage("Execution of " + this.getClass() + " failed.",
                            requestContext.correlationId()), ex);

            throw new CompletionException(ex);
        }
    }
}
