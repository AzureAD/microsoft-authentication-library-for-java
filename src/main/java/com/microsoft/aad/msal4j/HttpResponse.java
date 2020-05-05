// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP response
 */
@Accessors(fluent=true)
@Getter
public class HttpResponse implements IHttpResponse {

    /**
     * HTTP response status code
     */
    @Setter
    private int statusCode;

    /**
     * HTTP response headers
     */
    private Map<String, List<String>> headers =  new HashMap<>();

    /**
     * HTTP response body
     */
    @Setter
    private String body;

    /**
     * @param responseHeaders Map of HTTP headers returned from HTTP client
     */
    public void addHeaders(Map<String, List<String>> responseHeaders) {
        for(Map.Entry<String, List<String>> entry: responseHeaders.entrySet()){
            if(entry.getKey() == null){
                continue;
            }

            List<String> values = entry.getValue();
            if (values == null || values.isEmpty() || values.get(0) == null) {
                continue;
            }

            addHeader(entry.getKey(), values.toArray(new String[]{}));
        }
    }

    private void addHeader(final String name, final String ... values){
        if (values != null && values.length > 0) {
            headers.put(name, Arrays.asList(values));
        } else {
            headers.remove(name);
        }
    }
}
