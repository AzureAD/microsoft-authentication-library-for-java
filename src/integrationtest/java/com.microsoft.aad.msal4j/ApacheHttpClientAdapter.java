// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ApacheHttpClientAdapter implements IHttpClient {

    private final CloseableHttpClient httpClient;

    ApacheHttpClientAdapter() {
        this.httpClient = HttpClients.createDefault();
    }

    @Override
    public IHttpResponse send(HttpRequest httpRequest) throws Exception {

        HttpRequestBase request = buildApacheRequestFromMsalRequest(httpRequest);
        CloseableHttpResponse response = httpClient.execute(request);

        return buildMsalResponseFromApacheResponse(response);
    }


    private HttpRequestBase buildApacheRequestFromMsalRequest(HttpRequest httpRequest) {

        if (httpRequest.httpMethod() == HttpMethod.GET) {
            return builGetRequest(httpRequest);
        } else if (httpRequest.httpMethod() == HttpMethod.POST) {
            return buildPostRequest(httpRequest);
        } else {
            throw new IllegalArgumentException("HttpRequest method should be either GET or POST");
        }
    }

    private HttpGet builGetRequest(HttpRequest httpRequest) {
        HttpGet httpGet = new HttpGet(httpRequest.url().toString());

        for (Map.Entry<String, String> entry : httpRequest.headers().entrySet()) {
            httpGet.setHeader(entry.getKey(), entry.getValue());
        }

        return httpGet;
    }

    private HttpPost buildPostRequest(HttpRequest httpRequest) {

        HttpPost httpPost = new HttpPost(httpRequest.url().toString());
        for (Map.Entry<String, String> entry : httpRequest.headers().entrySet()) {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        String contentTypeHeaderValue = httpRequest.headerValue("Content-Type");
        ContentType contentType = ContentType.getByMimeType(contentTypeHeaderValue);
        StringEntity stringEntity = new StringEntity(httpRequest.body(), contentType);

        httpPost.setEntity(stringEntity);
        return httpPost;
    }

    private IHttpResponse buildMsalResponseFromApacheResponse(CloseableHttpResponse apacheResponse)
            throws IOException {

        IHttpResponse httpResponse = new HttpResponse();
        ((HttpResponse) httpResponse).statusCode(apacheResponse.getStatusLine().getStatusCode());

        Map<String, List<String>> headers = new HashMap<>();
        for (Header header : apacheResponse.getAllHeaders()) {
            headers.put(header.getName(), Collections.singletonList(header.getValue()));
        }
        ((HttpResponse) httpResponse).addHeaders(headers);

        String responseBody = EntityUtils.toString(apacheResponse.getEntity(), "UTF-8");
        ((HttpResponse) httpResponse).body(responseBody);
        return httpResponse;
    }
}
