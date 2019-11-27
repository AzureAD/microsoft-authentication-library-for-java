// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

final class HttpHeaders {

    private final static String PRODUCT_HEADER_NAME = "x-client-SKU";
    private final static String PRODUCT_HEADER_VALUE = "MSAL.Java";

    private  final static String PRODUCT_VERSION_HEADER_NAME = "x-client-VER";
    private  final static String PRODUCT_VERSION_HEADER_VALUE = getProductVersion();

    private final static String CPU_HEADER_NAME = "x-client-CPU";
    private final static String CPU_HEADER_VALUE = System.getProperty("os.arch");

    private final static String OS_HEADER_NAME = "x-client-OS";
    private final static String OS_HEADER_VALUE = System.getProperty("os.name");

    private final static String APPLICATION_NAME_HEADER_NAME = "x-app-name";
    private final String applicationNameHeaderValue;

    private final static String APPLICATION_VERSION_HEADER_NAME = "x-app-ver";
    private final String applicationVersionHeaderValue;

    final static String CORRELATION_ID_HEADER_NAME = "client-request-id";
    private final String correlationIdHeaderValue;

    private  final static String REQUEST_CORRELATION_ID_IN_RESPONSE_HEADER_NAME = "return-client-request-id";
    private final static String REQUEST_CORRELATION_ID_IN_RESPONSE_HEADER_VALUE = "true";

    private final String headerValues;
    private final Map<String, String> headerMap = new HashMap<>();

    HttpHeaders(final RequestContext requestContext) {
        correlationIdHeaderValue = requestContext.correlationId();
        applicationNameHeaderValue = requestContext.applicationName();
        applicationVersionHeaderValue = requestContext.applicationVersion();

        this.headerValues = initHeaderMap();
    }

    private String initHeaderMap() {
        StringBuilder sb = new StringBuilder();

        BiConsumer<String, String> init = (String key, String val) -> {
            headerMap.put(key, val);
            sb.append(key).append("=").append(val).append(";");
        };
        init.accept(PRODUCT_HEADER_NAME, PRODUCT_HEADER_VALUE);
        init.accept(PRODUCT_VERSION_HEADER_NAME, PRODUCT_VERSION_HEADER_VALUE);
        init.accept(OS_HEADER_NAME, OS_HEADER_VALUE);
        init.accept(CPU_HEADER_NAME, CPU_HEADER_VALUE);
        init.accept(REQUEST_CORRELATION_ID_IN_RESPONSE_HEADER_NAME, REQUEST_CORRELATION_ID_IN_RESPONSE_HEADER_VALUE);
        init.accept(CORRELATION_ID_HEADER_NAME, this.correlationIdHeaderValue);
        init.accept(APPLICATION_NAME_HEADER_NAME, this.applicationNameHeaderValue);
        init.accept(APPLICATION_VERSION_HEADER_NAME, this.applicationVersionHeaderValue);

        return sb.toString();
    }

    Map<String, String> getReadonlyHeaderMap() {
        return Collections.unmodifiableMap(this.headerMap);
    }

    String getHeaderCorrelationIdValue() {
        return this.correlationIdHeaderValue;
    }

    @Override
    public String toString() {
        return this.headerValues;
    }

    private static String getProductVersion() {
        if (HttpHeaders.class.getPackage().getImplementationVersion() == null) {
            return "1.0";
        }
        return HttpHeaders.class.getPackage().getImplementationVersion();
    }
}
