// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

final class ClientDataHttpHeaders {

    public final static String PRODUCT_HEADER_NAME = "x-client-SKU";
    public final static String PRODUCT_HEADER_VALUE = "MSAL.Java";

    public final static String PRODUCT_VERSION_HEADER_NAME = "x-client-VER";
    public final static String PRODUCT_VERSION_HEADER_VALUE = getProductVersion();

    public final static String CPU_HEADER_NAME = "x-client-CPU";
    public final static String CPU_HEADER_VALUE = System.getProperty("os.arch");

    public final static String OS_HEADER_NAME = "x-client-OS";
    public final static String OS_HEADER_VALUE = System.getProperty("os.name");

    public final static String CORRELATION_ID_HEADER_NAME = "client-request-id";
    public final String correlationIdHeaderValue;

    public final static String REQUEST_CORRELATION_ID_IN_RESPONSE_HEADER_NAME = "return-client-request-id";
    public final static String REQUEST_CORRELATION_ID_IN_RESPONSE_HEADER_VALUE = "true";
    private final String headerValues;
    private final Map<String, String> headerMap = new HashMap<>();

    ClientDataHttpHeaders(final String correlationId) {
        if (!StringHelper.isBlank(correlationId)) {
            this.correlationIdHeaderValue = correlationId;
        }
        else {
            this.correlationIdHeaderValue = RequestContext.generateNewCorrelationId();
        }
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
        if (ClientDataHttpHeaders.class.getPackage().getImplementationVersion() == null) {
            return "1.0";
        }
        return ClientDataHttpHeaders.class.getPackage()
                .getImplementationVersion();
    }
}
