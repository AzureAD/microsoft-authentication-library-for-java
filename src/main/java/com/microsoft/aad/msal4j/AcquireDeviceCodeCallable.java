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

import java.util.Set;

class AcquireDeviceCodeCallable extends MsalCallable<DeviceCode> {
    private String clientId;
    private String scopes;

    AcquireDeviceCodeCallable(PublicClientApplication clientApplication,
                              String clientId, Set<String> scopes) {
        super(clientApplication);
        this.headers = new ClientDataHttpHeaders(clientApplication.getCorrelationId());
        this.clientId = clientId;
        this.scopes = String.join(" ", scopes);
    }

    DeviceCode execute() throws Exception {
        clientApplication.authenticationAuthority.doInstanceDiscovery(clientApplication.isValidateAuthority(),
                headers.getReadonlyHeaderMap(), clientApplication.getProxy(), clientApplication.getSslSocketFactory());
        return DeviceCodeRequest.execute(clientApplication.authenticationAuthority.getDeviceCodeEndpoint(),
                clientId, scopes, headers.getReadonlyHeaderMap(), clientApplication.getProxy(),
                clientApplication.getSslSocketFactory());
    }
}
