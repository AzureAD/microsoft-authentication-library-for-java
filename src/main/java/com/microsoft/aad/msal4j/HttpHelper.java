// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpHelper {

    private final static Logger log = LoggerFactory.getLogger(HttpHelper.class);

    static IHttpResponse executeHttpRequest(final HttpRequest httpRequest,
                                            final RequestContext requestContext,
                                            final ServiceBundle serviceBundle) {

        HttpEvent httpEvent = new HttpEvent(); // for tracking http telemetry
        IHttpResponse httpResponse;

        try(TelemetryHelper telemetryHelper = serviceBundle.getTelemetryManager().createTelemetryHelper(
                requestContext.getTelemetryRequestId(),
                requestContext.getClientId(),
                httpEvent,
                false)){

            addRequestInfoToTelemetry(httpRequest, httpEvent);

            try{
                IHttpClient httpClient = serviceBundle.getHttpClient();
                httpResponse = httpClient.send(httpRequest);
            } catch(Exception e){
                httpEvent.setOauthErrorCode(AuthenticationErrorCode.UNKNOWN);
                throw new MsalException(e);
            }

            addResponseInfoToTelemetry(httpResponse, httpEvent);

            if (httpResponse.getHeaders() != null) {
                HttpHelper.verifyReturnedCorrelationId(httpRequest, httpResponse);
            }
        }
        return httpResponse;
    }

    private static void addRequestInfoToTelemetry(final HttpRequest httpRequest, HttpEvent httpEvent){
        try{
            httpEvent.setHttpPath(httpRequest.getUrl().toURI());
            httpEvent.setHttpMethod(httpRequest.getHttpMethod().toString());
            if(!StringHelper.isBlank(httpRequest.getUrl().getQuery())){
                httpEvent.setQueryParameters(httpRequest.getUrl().getQuery());
            }
        } catch(Exception ex){
            String correlationId = httpRequest.getHeaderValue(
                    ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME);

            log.warn(LogHelper.createMessage("Setting URL telemetry fields failed: " +
                            LogHelper.getPiiScrubbedDetails(ex),
                    correlationId != null ? correlationId : ""));
        }
    }

    private static void addResponseInfoToTelemetry(IHttpResponse httpResponse, HttpEvent httpEvent){
        if(!StringHelper.isBlank(httpResponse.getHeaderValue("User-Agent"))){
            httpEvent.setUserAgent(httpResponse.getHeaderValue("User-Agent"));
        }
        httpEvent.setHttpResponseStatus(httpResponse.getStatusCode());

        if(httpResponse.getHeaderValue("x-ms-request-id") != null){
            httpEvent.setRequestIdHeader(httpResponse.getHeaderValue("x-ms-request-id"));
        }

        if(httpResponse.getHeaderValue("x-ms-clitelem") != null){
            XmsClientTelemetryInfo xmsClientTelemetryInfo =
                    XmsClientTelemetryInfo.parseXmsTelemetryInfo(
                            httpResponse.getHeaderValue("x-ms-clitelem"));
            if(xmsClientTelemetryInfo != null){
                httpEvent.setXmsClientTelemetryInfo(xmsClientTelemetryInfo);
            }
        }
    }

    private static void verifyReturnedCorrelationId(final HttpRequest httpRequest,
                                                    IHttpResponse httpResponse) {

        String sentCorrelationId = httpRequest.getHeaderValue(
                ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME);

        String returnedCorrelationId = httpResponse.getHeaderValue(
                ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME);

        if (StringHelper.isBlank(returnedCorrelationId) ||
                !returnedCorrelationId.equals(sentCorrelationId)) {

            String msg = LogHelper.createMessage(
                    String.format(
                            "Sent (%s) Correlation Id is not same as received (%s).",
                            sentCorrelationId,
                            returnedCorrelationId),
                    sentCorrelationId);

            log.info(msg);
        }
    }
}