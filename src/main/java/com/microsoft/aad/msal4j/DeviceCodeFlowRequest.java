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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Accessors(fluent = true)
@Getter(AccessLevel.PACKAGE)
class DeviceCodeFlowRequest extends MsalRequest {
    private final Logger log = LoggerFactory.getLogger(DeviceCodeFlowRequest.class);

    private AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference;

    private DeviceCodeFlowParameters parameters;
    private String scopesStr;

    DeviceCodeFlowRequest(DeviceCodeFlowParameters parameters,
                          AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference,
                          PublicClientApplication application,
                          RequestContext requestContext) {

        super(application, null, requestContext);

        this.parameters = parameters;
        this.scopesStr = String.join(" ", parameters.scopes());
        this.futureReference = futureReference;
    }

    DeviceCode acquireDeviceCode(String url,
                                 String clientId,
                                 Map<String, String> clientDataHeaders,
                                 ServiceBundle serviceBundle) throws Exception {

        String urlWithQueryParams = createQueryParamsAndAppendToURL(url, clientId);
        Map<String, String> headers = appendToHeaders(clientDataHeaders);

        final String json = HttpHelper.executeHttpRequest(
                log,
                HttpMethod.GET,
                urlWithQueryParams,
                headers,
                null,
                this.requestContext(),
                serviceBundle);

        return parseJsonToDeviceCodeAndSetParameters(json, headers, clientId);
    }

    void createAuthenticationGrant(DeviceCode deviceCode) {
        msalAuthorizationGrant = new DeviceCodeAuthorizationGrant(deviceCode, deviceCode.scopes());
    }

    private String createQueryParamsAndAppendToURL(String url, String clientId) {
        Map<String, List<String>> queryParameters = new HashMap<>();
        queryParameters.put("client_id", Collections.singletonList(clientId));

        String scopesParam = AbstractMsalAuthorizationGrant.COMMON_SCOPES_PARAM +
                AbstractMsalAuthorizationGrant.SCOPES_DELIMITER + scopesStr;

        queryParameters.put("scope", Collections.singletonList(scopesParam));

        url = url + "?" + URLUtils.serializeParameters(queryParameters);
        return url;
    }

    private Map<String, String> appendToHeaders(Map<String, String> clientDataHeaders) {
        Map<String, String> headers = new HashMap<>(clientDataHeaders);
        headers.put("Accept", "application/json");

        return headers;
    }

    private DeviceCode parseJsonToDeviceCodeAndSetParameters(
            String json,
            Map<String, String> headers,
            String clientId) {

        DeviceCode result;
        result = JsonHelper.convertJsonToObject(json, DeviceCode.class);

        result.correlationId(headers.get(ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME));
        result.clientId(clientId);
        result.scopes(scopesStr);

        return result;
    }
}