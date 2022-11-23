// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.Optional;
import java.lang.module.ModuleDescriptor;

final class HttpHeaders {

    static final String PRODUCT_HEADER_NAME = "x-client-SKU";
    static final String PRODUCT_HEADER_VALUE = "MSAL.Java";

    static final String PRODUCT_VERSION_HEADER_NAME = "x-client-VER";
    static final String PRODUCT_VERSION_HEADER_VALUE = getProductVersion();

    static final String CPU_HEADER_NAME = "x-client-CPU";
    static final String CPU_HEADER_VALUE = System.getProperty("os.arch");

    static final String OS_HEADER_NAME = "x-client-OS";
    static final String OS_HEADER_VALUE = System.getProperty("os.name");

    static final String APPLICATION_NAME_HEADER_NAME = "x-app-name";
    private final String applicationNameHeaderValue;

    static final String APPLICATION_VERSION_HEADER_NAME = "x-app-ver";
    private final String applicationVersionHeaderValue;

    static final String CORRELATION_ID_HEADER_NAME = "client-request-id";
    private final String correlationIdHeaderValue;

    private static final String REQUEST_CORRELATION_ID_IN_RESPONSE_HEADER_NAME = "return-client-request-id";
    private static final String REQUEST_CORRELATION_ID_IN_RESPONSE_HEADER_VALUE = "true";

    private static final String X_MS_LIB_CAPABILITY_NAME = "x-ms-lib-capability";
    private static final String X_MS_LIB_CAPABILITY_VALUE = "retry-after, h429";

    // Used for CCS routing
    static final String X_ANCHOR_MAILBOX = "X-AnchorMailbox";
    static final String X_ANCHOR_MAILBOX_OID_FORMAT = "oid:%s";
    static final String X_ANCHOR_MAILBOX_UPN_FORMAT = "upn:%s";
    private String anchorMailboxHeaderValue = null;

    private String headerValues;
    private Map<String, String> headerMap = new HashMap<>();

    HttpHeaders(final RequestContext requestContext) {
        correlationIdHeaderValue = requestContext.correlationId();
        applicationNameHeaderValue = requestContext.applicationName();
        applicationVersionHeaderValue = requestContext.applicationVersion();

        if (requestContext.userIdentifier() != null) {
            String upn = requestContext.userIdentifier().upn();
            String oid = requestContext.userIdentifier().oid();
            if (!StringHelper.isBlank(upn)) {
                anchorMailboxHeaderValue = String.format(X_ANCHOR_MAILBOX_UPN_FORMAT, upn);
            } else if (!StringHelper.isBlank(oid)) {
                anchorMailboxHeaderValue = String.format(X_ANCHOR_MAILBOX_OID_FORMAT, oid);
            }
        }

        Map<String, String> extraHttpHeaders = requestContext.apiParameters() == null ?
                null :
                requestContext.apiParameters().extraHttpHeaders();
        this.initializeHeaders(extraHttpHeaders);
    }

    private void initializeHeaders(Map<String, String> extraHttpHeaders) {
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

        if (!StringHelper.isBlank(this.applicationNameHeaderValue)) {
            init.accept(APPLICATION_NAME_HEADER_NAME, this.applicationNameHeaderValue);
        }
        if (!StringHelper.isBlank(this.applicationVersionHeaderValue)) {
            init.accept(APPLICATION_VERSION_HEADER_NAME, this.applicationVersionHeaderValue);
        }
        if (!StringHelper.isBlank(this.anchorMailboxHeaderValue)) {
            init.accept(X_ANCHOR_MAILBOX, this.anchorMailboxHeaderValue);
        }

        init.accept(X_MS_LIB_CAPABILITY_NAME, X_MS_LIB_CAPABILITY_VALUE);

        if (extraHttpHeaders != null) {
            extraHttpHeaders.forEach(init);
        }

        this.headerValues = sb.toString();
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
        return Optional
                .ofNullable(HttpHeaders.class.getModule().getDescriptor())
                .flatMap(ModuleDescriptor::version)
                .map(ModuleDescriptor.Version::toString)
                .orElse("1.0");
    }
}
