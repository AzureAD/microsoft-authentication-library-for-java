package com.microsoft.aad.msal4j;

public interface IHttpClient {
    IHttpResponse send(HttpRequest request) throws Exception;
}
