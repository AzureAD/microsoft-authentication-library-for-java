package com.microsoft.aad.msal4j;

import lombok.Getter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Getter
public class HttpRequest {

    private HttpMethod httpMethod;
    private URL url;
    private Map<String, List<String>> headers;
    private String postData;

    public HttpRequest(HttpMethod httpMethod, String url){
        this.httpMethod = httpMethod;
        this.url = createUrlFromString(url);
    }

    public HttpRequest(HttpMethod httpMethod, String url, Map<String, List<String>> headers){
        this.httpMethod = httpMethod;
        this.url = createUrlFromString(url);
        this.headers = headers;
    }

    public HttpRequest(HttpMethod httpMethod, String url, String postData){
        this.httpMethod = httpMethod;
        this.url = createUrlFromString(url);
        this.postData = postData;
    }

    public HttpRequest(HttpMethod httpMethod,
                       String url, Map<String, List<String>> headers,
                       String postData){
        this.httpMethod = httpMethod;
        this.url = createUrlFromString(url);
        this.headers = headers;
        this.postData = postData;
    }

    public String getHeaderValue(String headerName){

        if(headerName == null || headers == null){
            return null;
        }

        List<String> headerValue = headers.get(headerName);

        if(headerValue == null || headerValue.isEmpty()){
            return null;
        }

        return headerValue.get(0);
    }

    private URL createUrlFromString(String stringUrl){
        URL url;
        try{
            url = new URL(stringUrl);
        } catch(MalformedURLException e){
            throw new MsalClientException(e);
        }

        return url;
    }
}