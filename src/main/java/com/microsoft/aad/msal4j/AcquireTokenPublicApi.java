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

enum AcquireTokenPublicApi {
    // TODO finalize api ids
    ACQUIRE_TOKEN_BY_REFRESH_TOKEN(82),
    ACQUIRE_TOKEN_BY_USERNAME_PASSWORD(300),
    ACQUIRE_TOKEN_BY_INTEGRATED_WINDOWS_AUTH(400),
    ACQUIRE_TOKEN_ON_BEHALF_OF(522),
    ACQUIRE_TOKEN_BY_DEVICE_CODE_FLOW(620),
    ACQUIRE_TOKEN_FOR_CLIENT(729),
    ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE(831);

    private final int apiId;

    AcquireTokenPublicApi(int apiId){
        this.apiId = apiId;
    }

    int getApiId(){
        return apiId;
    }
}
