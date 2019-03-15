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

import java.util.concurrent.TimeUnit;

import static com.microsoft.aad.msal4j.AuthenticationErrorCode.AUTHORIZATION_PENDING;

class AcquireTokenByDeviceCodeFlowSupplier extends AuthenticationResultSupplier {

    private DeviceCodeRequest deviceCodeRequest;

    AcquireTokenByDeviceCodeFlowSupplier(PublicClientApplication clientApplication,
                                         DeviceCodeRequest deviceCodeRequest) {
        super(clientApplication, deviceCodeRequest.getHeaders());
        this.deviceCodeRequest = deviceCodeRequest;
    }

    AuthenticationResult execute() throws Exception {
        doInstanceDiscovery();
        DeviceCode deviceCode = getDeviceCode();
        return acquireTokenWithDeviceCode(deviceCode);
    }

    private void doInstanceDiscovery() throws Exception{
        this.clientApplication.authenticationAuthority.doInstanceDiscovery(
                this.clientApplication.isValidateAuthority(),
                deviceCodeRequest.getHeaders().getReadonlyHeaderMap(),
                this.clientApplication.getServiceBundle());
    }

    private DeviceCode getDeviceCode() throws Exception{
        DeviceCode deviceCode = deviceCodeRequest.acquireDeviceCode(
                this.clientApplication.authenticationAuthority.getDeviceCodeEndpoint(),
                deviceCodeRequest.getClientAuthentication().getClientID().toString(),
                deviceCodeRequest.getHeaders().getReadonlyHeaderMap(),
                this.clientApplication.getServiceBundle());

        deviceCodeRequest.getDeviceCodeConsumer().accept(deviceCode);

        return deviceCode;
    }

    private AuthenticationResult acquireTokenWithDeviceCode(DeviceCode deviceCode) throws Exception {
        deviceCodeRequest.createAuthenticationGrant(deviceCode);

        long expirationTimeInSeconds = getCurrentSystemTimeInSeconds() + deviceCode.getExpiresIn();

        AcquireTokenByAuthorizationGrantSupplier acquireTokenByAuthorisationGrantSupplier =
                new AcquireTokenByAuthorizationGrantSupplier(
                        this.clientApplication,
                        deviceCodeRequest);

        while (getCurrentSystemTimeInSeconds() < expirationTimeInSeconds) {
            if(deviceCodeRequest.getFutureReference().get().isCancelled()){
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

    private Long getCurrentSystemTimeInSeconds(){
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    }
}
