// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

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

        DeviceCode deviceCode = getDeviceCode(requestAuthority);

        return acquireTokenWithDeviceCode(deviceCode, requestAuthority);
    }

    private DeviceCode getDeviceCode(Authority requestAuthority) {

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
                throw new InterruptedException("Device code flow was cancelled before acquiring a token");
            }
            if (deviceCodeFlowRequest.futureReference().get().isCompletedExceptionally()) {
                throw new InterruptedException("Device code flow had an exception before acquiring a token");
            }
            try {
                return acquireTokenByAuthorisationGrantSupplier.execute();
            } catch (MsalServiceException ex) {
                if (ex.errorCode() != null && ex.errorCode().equals(AUTHORIZATION_PENDING)) {
                    TimeUnit.SECONDS.sleep(deviceCode.interval());
                } else {
                    throw ex;
                }
            }
        }
        throw new MsalClientException("Expired Device code", AuthenticationErrorCode.CODE_EXPIRED);
    }

    private Long getCurrentSystemTimeInSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    }
}
