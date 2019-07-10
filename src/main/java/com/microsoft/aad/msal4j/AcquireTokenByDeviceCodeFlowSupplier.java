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

    private DeviceCodeFlowRequest deviceCodeFlowRequest;

        AcquireTokenByDeviceCodeFlowSupplier(PublicClientApplication clientApplication,
                                         DeviceCodeFlowRequest deviceCodeFlowRequest) {
        super(clientApplication, deviceCodeFlowRequest);
        this.deviceCodeFlowRequest = deviceCodeFlowRequest;
    }

    AuthenticationResult execute() throws Exception {

        Authority requestAuthority = clientApplication.authenticationAuthority;
        requestAuthority = getAuthorityWithPrefNetworkHost(requestAuthority.authority());

        DeviceCode deviceCode = getDeviceCode((AADAuthority) requestAuthority);

        return acquireTokenWithDeviceCode(deviceCode, requestAuthority);
    }

    private DeviceCode getDeviceCode(AADAuthority requestAuthority) throws Exception{

        DeviceCode deviceCode = deviceCodeFlowRequest.acquireDeviceCode(
                requestAuthority.deviceCodeEndpoint(),
                clientApplication.clientId(),
                deviceCodeFlowRequest.headers().getReadonlyHeaderMap(),
                this.clientApplication.getServiceBundle());

        deviceCodeFlowRequest.parameters().deviceCodeConsumer().accept(deviceCode);

        return deviceCode;
    }

    private AuthenticationResult acquireTokenWithDeviceCode(DeviceCode deviceCode,
                                                            Authority requestAuthority) throws Exception {
        deviceCodeFlowRequest.createAuthenticationGrant(deviceCode);
        long expirationTimeInSeconds = getCurrentSystemTimeInSeconds() + deviceCode.expiresIn();

        AcquireTokenByAuthorizationGrantSupplier acquireTokenByAuthorisationGrantSupplier =
                new AcquireTokenByAuthorizationGrantSupplier(
                        clientApplication,
                        deviceCodeFlowRequest,
                        requestAuthority);

        while (getCurrentSystemTimeInSeconds() < expirationTimeInSeconds) {
            if (deviceCodeFlowRequest.futureReference().get().isCancelled()) {
                throw new InterruptedException("Acquire token Device Code Flow was interrupted");
            }
            try {
                return acquireTokenByAuthorisationGrantSupplier.execute();
            } catch (MsalServiceException ex) {
                if (ex.errorCode().equals(AUTHORIZATION_PENDING)) {
                    TimeUnit.SECONDS.sleep(deviceCode.interval());
                } else {
                    throw ex;
                }
            }
        }
        throw new MsalClientException("Expired Device code", AuthenticationErrorCode.CODE_EXPIRED);
    }

    private Long getCurrentSystemTimeInSeconds(){
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    }
}
