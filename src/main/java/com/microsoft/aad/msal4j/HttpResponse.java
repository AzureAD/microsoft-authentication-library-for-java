package com.microsoft.aad.msal4j;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HttpResponse implements IHttpResponse {

    private int statusCode;
    private Map<String, List<String>> headers =  new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private String body;

    HttpResponse(){
    }

    public int getStatusCode(){
        return this.statusCode;
    }

    public Map<String, List<String>> getHeaders(){
        return this.headers;
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

    public String getBody(){
        return this.body;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setHeaders(Map<String, List<String>> responseHeaders) {
        for(Map.Entry<String, List<String>> entry: responseHeaders.entrySet()){
            if(entry.getKey() == null){
                continue;
            }

            List<String> values = entry.getValue();
            if (values == null || values.isEmpty() || values.get(0) == null) {
                continue;
            }

            setHeader(entry.getKey(), values.toArray(new String[]{}));
        }
    }

    private void setHeader(final String name, final String ... values){
        if (values != null && values.length > 0) {
            headers.put(name, Arrays.asList(values));
        } else {
            headers.remove(name);
        }
    }

    public void setBody(String body) {
        this.body = body;
    }
}
