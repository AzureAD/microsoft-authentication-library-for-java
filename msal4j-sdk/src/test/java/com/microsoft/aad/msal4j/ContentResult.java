package com.microsoft.aad.msal4j;

public class ContentResult {

    String content;
    String contentEncoding;
    int statusCode;

    public ContentResult(String content, String contentEncoding, int statusCode) {
        this.content = content;
        this.contentEncoding = contentEncoding;
        this.statusCode = statusCode;
    }
}
