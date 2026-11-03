// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.net.ssl.SSLContext;
import java.net.URL;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthHttpRequestMtlsTest {

    @Test
    void requestSpecificSslContextDisablesRedirects() throws Exception {
        ServiceBundle serviceBundle = mock(ServiceBundle.class);
        HttpHelper httpHelper = mock(HttpHelper.class);
        IHttpResponse response = mock(IHttpResponse.class);
        ServerSideTelemetry serverSideTelemetry = mock(ServerSideTelemetry.class);
        when(serviceBundle.getHttpHelper()).thenReturn(httpHelper);
        when(serviceBundle.getServerSideTelemetry()).thenReturn(serverSideTelemetry);
        when(serverSideTelemetry.getServerTelemetryHeaderMap())
                .thenReturn(Collections.emptyMap());
        when(httpHelper.executeHttpRequest(
                any(HttpRequest.class),
                any(RequestContext.class),
                any(ServiceBundle.class))).thenReturn(response);
        when(response.headers()).thenReturn(Collections.emptyMap());
        when(response.body()).thenReturn("");

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, null);
        OAuthHttpRequest request = new OAuthHttpRequest(
                HttpMethod.POST,
                new URL("https://login.example/token"),
                Collections.emptyMap(),
                mock(RequestContext.class),
                serviceBundle).sslContext(sslContext);

        request.send();

        ArgumentCaptor<HttpRequest> requestCaptor =
                ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpHelper).executeHttpRequest(
                requestCaptor.capture(),
                any(RequestContext.class),
                any(ServiceBundle.class));
        assertFalse(requestCaptor.getValue().followRedirects());
    }
}
