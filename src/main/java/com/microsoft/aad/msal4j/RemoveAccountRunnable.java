// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.msal4j;

import java.util.concurrent.CompletionException;

class RemoveAccountRunnable implements Runnable {

    ClientDataHttpHeaders headers;
    ClientApplicationBase clientApplication;
    IAccount account;

    RemoveAccountRunnable
            (ClientApplicationBase clientApplication, IAccount account) {
        this.clientApplication = clientApplication;
        this.headers = new ClientDataHttpHeaders(clientApplication.correlationId());
        this.account = account;
    }

    @Override
    public void run() {
        try {
            InstanceDiscoveryMetadataEntry instanceDiscoveryData =
                    AadInstanceDiscovery.cache.get(clientApplication.authenticationAuthority.host());

            clientApplication.tokenCache.removeAccount
                    (clientApplication.clientId(), account, instanceDiscoveryData.aliases());

        } catch (Exception ex) {
            clientApplication.log.error(
                    LogHelper.createMessage("Execution of " + this.getClass() + " failed.",
                            this.headers.getHeaderCorrelationIdValue()), ex);

            throw new CompletionException(ex);
        }
    }
}
