package com.microsoft.aad.msal4j;

import java.util.List;
import java.util.Map;

public interface IHttpResponse {

    int getStatusCode();
    String getHeaderValue(String headerName);
    Map<String, List<String>> getHeaders();
    String getBody();
}



