// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

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
                                 Map<String, List<String>> clientDataHeaders,
                                 ServiceBundle serviceBundle) throws Exception {

        String urlWithQueryParams = createQueryParamsAndAppendToURL(url, clientId);
        Map<String, List<String>> headers = appendToHeaders(clientDataHeaders);

        HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, urlWithQueryParams, headers);
        final IHttpResponse response = HttpHelper.executeHttpRequest(
                httpRequest,
                this.requestContext(),
                serviceBundle);

        return parseJsonToDeviceCodeAndSetParameters(response.getBody(), headers, clientId);
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

    private Map<String, List<String>> appendToHeaders(Map<String, List<String>> clientDataHeaders) {
        Map<String, List<String>> headers = new HashMap<>(clientDataHeaders);
        headers.put("Accept", Collections.singletonList("application/json"));

        return headers;
    }

    private DeviceCode parseJsonToDeviceCodeAndSetParameters(
            String json,
            Map<String, List<String>> headers,
            String clientId) {

        DeviceCode result;
        result = JsonHelper.convertJsonToObject(json, DeviceCode.class);

        List<String> correlationIdHeader = headers.get(
                ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME);
        if(correlationIdHeader != null && correlationIdHeader.size() > 0){
            result.correlationId(correlationIdHeader.get(0));
        }

        result.clientId(clientId);
        result.scopes(scopesStr);

        return result;
    }
}