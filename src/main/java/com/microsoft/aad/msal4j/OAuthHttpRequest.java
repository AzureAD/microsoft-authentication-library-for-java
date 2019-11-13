// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.CommonContentTypes;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import lombok.AccessLevel;
import lombok.Getter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class OAuthHttpRequest extends HTTPRequest {

    @Getter(AccessLevel.PACKAGE)
    private final Map<String, List<String>> extraHeaderParams;
    private final ServiceBundle serviceBundle;
    private final RequestContext requestContext;

    OAuthHttpRequest(final Method method,
                     final URL url,
                     final Map<String, List<String>> extraHeaderParams,
                     RequestContext requestContext,
                     final ServiceBundle serviceBundle) {
        super(method, url);
        this.extraHeaderParams = extraHeaderParams;
        this.requestContext = requestContext;
        this.serviceBundle = serviceBundle;
    }

    @Override
    public HTTPResponse send() throws IOException {

        Map<String, List<String>> httpHeaders = configureHttpHeaders();
        HttpRequest httpRequest = new HttpRequest(
                HttpMethod.POST,
                this.getURL().toString(),
                httpHeaders,
                this.getQuery());

        IHttpResponse httpResponse = HttpHelper.executeHttpRequest(
                httpRequest,
                this.requestContext,
                this.serviceBundle);

        return createOauthHttpResponseFromHttpResponse(httpResponse);
    }

    private Map<String, List<String>> configureHttpHeaders(){

        Map<String, List<String>> httpHeaders = new HashMap<>(extraHeaderParams);
        httpHeaders.put("Content-Type", Collections.singletonList(
                CommonContentTypes.APPLICATION_URLENCODED.toString()));

        if (this.getAuthorization() != null) {
            httpHeaders.put("Authorization", Collections.singletonList(this.getAuthorization()));
        }
        return httpHeaders;
    }

    private HTTPResponse createOauthHttpResponseFromHttpResponse(IHttpResponse httpResponse)
            throws IOException {

        final HTTPResponse response = new HTTPResponse(httpResponse.getStatusCode());

        final String location = httpResponse.getHeaderValue("Location");
        if (!StringHelper.isBlank(location)) {
            try {
                response.setLocation(new URI(location));
            } catch (URISyntaxException e) {
                throw new IOException("Invalid location URI " + location, e);
            }
        }

        try {
            String contentType = httpResponse.getHeaderValue("Content-Type");
            if(!StringHelper.isBlank(contentType)){
                response.setContentType(contentType);
            }
        } catch (final ParseException e) {
            throw new IOException("Couldn't parse Content-Type header: "
                    + e.getMessage(), e);
        }

        Map<String, List<String>> headers = httpResponse.getHeaders();
        for(Map.Entry<String, List<String>> header: headers.entrySet()){

            if(StringHelper.isBlank(header.getKey())){
                continue;
            }

            String headerValue = response.getHeaderValue(header.getKey());
            if(headerValue == null || StringHelper.isBlank(headerValue)){
                response.setHeader(header.getKey(), header.getValue().toArray(new String[0]));
            }
        }

        if (!StringHelper.isBlank(httpResponse.getBody())) {
            response.setContent(httpResponse.getBody());
        }
        return response;
    }
}