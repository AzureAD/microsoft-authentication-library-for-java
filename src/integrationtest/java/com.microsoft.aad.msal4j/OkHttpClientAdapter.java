// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

class OkHttpClientAdapter implements IHttpClient{

    private final OkHttpClient client;

    OkHttpClientAdapter(){
        this.client = new OkHttpClient();
    }

    @Override
    public IHttpResponse send(HttpRequest httpRequest) throws IOException {

        Request request = buildOkRequestFromMsalRequest(httpRequest);

        Response okHttpResponse= client.newCall(request).execute();
        return buildMsalResponseFromOkResponse(okHttpResponse);
    }

    private Request buildOkRequestFromMsalRequest(HttpRequest httpRequest){

        if(httpRequest.httpMethod() == HttpMethod.GET){
            return buildGetRequest(httpRequest);
        } else if(httpRequest.httpMethod() == HttpMethod.POST){
            return buildPostRequest(httpRequest);
        } else {
            throw new IllegalArgumentException("HttpRequest method should be either GET or POST");
        }
    }

    private Request buildGetRequest(HttpRequest httpRequest){
        Headers headers = Headers.of(httpRequest.headers());

        return new Request.Builder()
                .url(httpRequest.url())
                .headers(headers)
                .build();
    }

    private Request buildPostRequest(HttpRequest httpRequest){
        Headers headers = Headers.of(httpRequest.headers());
        String contentType = httpRequest.headerValue("Content-Type");
        MediaType type = MediaType.parse(contentType);

        RequestBody requestBody = RequestBody.create(type, httpRequest.body());

        return new Request.Builder()
                .url(httpRequest.url())
                .post(requestBody)
                .headers(headers)
                .build();
    }

    private IHttpResponse buildMsalResponseFromOkResponse(Response okHttpResponse) throws IOException{

        IHttpResponse httpResponse = new HttpResponse();
        ((HttpResponse) httpResponse).statusCode(okHttpResponse.code());

        ResponseBody body = okHttpResponse.body();
        if(body != null){
            ((HttpResponse) httpResponse).body(body.string());
        }

        Headers headers = okHttpResponse.headers();
        if(headers != null){
            ((HttpResponse) httpResponse).addHeaders(headers.toMultimap());
        }
        return httpResponse;
    }
}
