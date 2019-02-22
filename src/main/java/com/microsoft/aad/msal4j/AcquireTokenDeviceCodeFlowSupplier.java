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

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.microsoft.aad.msal4j.AdalErrorCode.AUTHORIZATION_PENDING;

public class AcquireTokenDeviceCodeFlowSupplier extends AuthenticationResultSupplier {

    private ClientAuthentication clientAuth;
    private String scopes;
    private Consumer<DeviceCode> deviceCodeConsumer;
    private AtomicReference<CompletableFuture<AuthenticationResult>> futureReference;

    AcquireTokenDeviceCodeFlowSupplier(PublicClientApplication clientApplication, ClientAuthentication clientAuth,
                                       Set<String> scopes, Consumer<DeviceCode> deviceCodeConsumer,
                                       AtomicReference<CompletableFuture<AuthenticationResult>> futureReference)
    {
        super(clientApplication);
        this.headers = new ClientDataHttpHeaders(clientApplication.getCorrelationId());
        this.clientAuth = clientAuth;
        this.scopes = String.join(" ", scopes);
        this.deviceCodeConsumer = deviceCodeConsumer;

        this.futureReference = futureReference;
    }

    AuthenticationResult execute() throws Exception {

        clientApplication.authenticationAuthority.doInstanceDiscovery(clientApplication.isValidateAuthority(),
                headers.getReadonlyHeaderMap(), clientApplication.getProxy(), clientApplication.getSslSocketFactory());

        DeviceCode deviceCode = DeviceCodeRequest.execute(clientApplication.authenticationAuthority.getDeviceCodeEndpoint(),
                clientAuth.getClientID().toString(), scopes, headers.getReadonlyHeaderMap(), clientApplication.getProxy(),
                clientApplication.getSslSocketFactory());

        deviceCodeConsumer.accept(deviceCode);

        MsalDeviceCodeAuthorizationGrant deviceCodeGrant =
                new MsalDeviceCodeAuthorizationGrant(deviceCode, deviceCode.getScopes());

        long expirationTimeInSeconds =
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + deviceCode.getExpiresIn();

        AcquireTokenByAuthorisationGrantSupplier acquireTokenByAuthorisationGrantSupplier =
                new AcquireTokenByAuthorisationGrantSupplier(clientApplication, deviceCodeGrant, clientAuth);

        while (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) < expirationTimeInSeconds) {
            if(futureReference.get().isCancelled()){
                throw new InterruptedException("Acquire token Device Code Flow was interrupted");
            }
            try {
                return acquireTokenByAuthorisationGrantSupplier.execute();
            }
            catch (AuthenticationException ex) {
                if (ex.getErrorCode().equals(AUTHORIZATION_PENDING))
                {
                    TimeUnit.SECONDS.sleep(deviceCode.getInterval());
                } else {
                    throw ex;
                }
            }
        }
        throw new AuthenticationException("Expired Device code");
    }
}
