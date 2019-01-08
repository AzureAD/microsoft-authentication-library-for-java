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

import com.nimbusds.oauth2.sdk.util.URLUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocketFactory;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

public class DeviceCodeRequest {

    private final static Logger log = LoggerFactory.getLogger(DeviceCodeRequest.class);

    static DeviceCode execute(String url, String clientId, String scopes, Map<String, String> clientDataHeaders,
                              final Proxy proxy, final SSLSocketFactory sslSocketFactory) throws Exception {
        Map<String, String> headers = new HashMap<>(clientDataHeaders);
        headers.put("Accept", "application/json");

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("client_id", clientId);

        String scopesParam = AbstractMsalAuthorizationGrant.COMMON_SCOPES_PARAM +
                AbstractMsalAuthorizationGrant.SCOPES_DELIMITER + scopes;

        queryParameters.put("scope", scopesParam);

        url = url + "?" + URLUtils.serializeParameters(queryParameters);

        final String json = HttpHelper.executeHttpGet(log, url, headers, proxy, sslSocketFactory);

        DeviceCode result;
        result = JsonHelper.convertJsonToObject(json, DeviceCode.class);

        result.setCorrelationId(headers.get(ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME));

        result.setClientId(clientId);
        result.setScopes(scopes);

        return result;
    }
}
