// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpHeaderTest {

    @Test
    void testHttpHeaderConstructor() {

        PublicClientApplication app = PublicClientApplication
                .builder("client-id")
                .correlationId("correlation-id")
                .applicationName("app-name")
                .applicationVersion("app-version")
                .build();

        IAcquireTokenParameters parameters = UserNamePasswordParameters
                .builder(Collections.singleton("scopes"), "username", "password".toCharArray())
                .build();

        RequestContext requestContext = new RequestContext(
                app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters);

        HttpHeaders httpHeaders = new HttpHeaders(requestContext);

        Map<String, String> httpHeaderMap = httpHeaders.getReadonlyHeaderMap();

        assertEquals(httpHeaderMap.get(HttpHeaders.PRODUCT_HEADER_NAME), HttpHeaders.PRODUCT_HEADER_VALUE);
        assertEquals(httpHeaderMap.get(HttpHeaders.PRODUCT_VERSION_HEADER_NAME), HttpHeaders.PRODUCT_VERSION_HEADER_VALUE);
        assertEquals(httpHeaderMap.get(HttpHeaders.OS_HEADER_NAME), HttpHeaders.OS_HEADER_VALUE);
        assertEquals(httpHeaderMap.get(HttpHeaders.CPU_HEADER_NAME), HttpHeaders.CPU_HEADER_VALUE);
        assertEquals(httpHeaderMap.get(HttpHeaders.APPLICATION_NAME_HEADER_NAME), "app-name");
        assertEquals(httpHeaderMap.get(HttpHeaders.APPLICATION_VERSION_HEADER_NAME), "app-version");
        assertEquals(httpHeaderMap.get(HttpHeaders.CORRELATION_ID_HEADER_NAME), "correlation-id");
    }

    @Test
    void testHttpHeaderConstructor_valuesNotSet() {

        PublicClientApplication app = PublicClientApplication
                .builder("client-id")
                .build();

        IAcquireTokenParameters parameters = UserNamePasswordParameters
                .builder(Collections.singleton("scopes"), "username", "password".toCharArray())
                .build();

        RequestContext requestContext = new RequestContext(
                app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters);

        HttpHeaders httpHeaders = new HttpHeaders(requestContext);

        Map<String, String> httpHeaderMap = httpHeaders.getReadonlyHeaderMap();

        assertEquals(httpHeaderMap.get(HttpHeaders.PRODUCT_HEADER_NAME), HttpHeaders.PRODUCT_HEADER_VALUE);
        assertEquals(httpHeaderMap.get(HttpHeaders.PRODUCT_VERSION_HEADER_NAME), HttpHeaders.PRODUCT_VERSION_HEADER_VALUE);
        assertEquals(httpHeaderMap.get(HttpHeaders.OS_HEADER_NAME), HttpHeaders.OS_HEADER_VALUE);
        assertEquals(httpHeaderMap.get(HttpHeaders.CPU_HEADER_NAME), HttpHeaders.CPU_HEADER_VALUE);
        assertNull(httpHeaderMap.get(HttpHeaders.APPLICATION_NAME_HEADER_NAME));
        assertNull(httpHeaderMap.get(HttpHeaders.APPLICATION_VERSION_HEADER_NAME));
        assertNotNull(httpHeaderMap.get(HttpHeaders.CORRELATION_ID_HEADER_NAME));
    }

    @Test
    void testHttpHeaderConstructor_userIdentifierUPN() {

        PublicClientApplication app = PublicClientApplication
                .builder("client-id")
                .build();

        IAcquireTokenParameters parameters = UserNamePasswordParameters
                .builder(Collections.singleton("scopes"), "username", "password".toCharArray())
                .build();

        String upn = "testuser@microsoft.com";
        RequestContext requestContext = new RequestContext(
                app,
                PublicApi.ACQUIRE_TOKEN_BY_USERNAME_PASSWORD,
                parameters,
                UserIdentifier.fromUpn(upn));

        HttpHeaders httpHeaders = new HttpHeaders(requestContext);

        Map<String, String> httpHeaderMap = httpHeaders.getReadonlyHeaderMap();

        String expectedValue = String.format(HttpHeaders.X_ANCHOR_MAILBOX_UPN_FORMAT, upn);
        assertEquals(httpHeaderMap.get(HttpHeaders.X_ANCHOR_MAILBOX), expectedValue);
    }

    @Test
    void testHttpHeaderConstructor_userIdentifierHomeAccountId() {

        PublicClientApplication app = PublicClientApplication
                .builder("client-id")
                .build();

        IAcquireTokenParameters parameters = UserNamePasswordParameters
                .builder(Collections.singleton("scopes"), "username", "password".toCharArray())
                .build();

        String homeAccountId = "userObjectId.userTenantId";
        RequestContext requestContext = new RequestContext(
                app,
                PublicApi.ACQUIRE_TOKEN_SILENTLY,
                parameters,
                UserIdentifier.fromHomeAccountId(homeAccountId));

        HttpHeaders httpHeaders = new HttpHeaders(requestContext);

        Map<String, String> httpHeaderMap = httpHeaders.getReadonlyHeaderMap();

        assertEquals(httpHeaderMap.get(HttpHeaders.X_ANCHOR_MAILBOX), "oid:userObjectId@userTenantId");
    }

    @Test
    void testHttpHeaderConstructor_extraHttpHeadersOverwriteLibraryHeaders() {

        PublicClientApplication app = PublicClientApplication
                .builder("client-id")
                .correlationId("correlation-id")
                .applicationName("app-name")
                .applicationVersion("app-version")
                .build();

        // Adding extra header
        String uniqueHeaderKey = "uniqueHeader";
        String uniqueHeaderValue = "uniqueValue";
        Map<String, String> extraHttpHeaders = new HashMap<>();
        extraHttpHeaders.put(uniqueHeaderKey, uniqueHeaderValue);

        // Overwriting standard header
        String uniqueAppName = "my-unique-app-name";
        extraHttpHeaders.put(HttpHeaders.APPLICATION_NAME_HEADER_NAME, uniqueAppName);

        IAcquireTokenParameters parameters = UserNamePasswordParameters
                .builder(Collections.singleton("scopes"), "username", "password".toCharArray())
                .extraHttpHeaders(extraHttpHeaders)
                .build();

        RequestContext requestContext = new RequestContext(
                app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters);

        HttpHeaders httpHeaders = new HttpHeaders(requestContext);

        Map<String, String> httpHeaderMap = httpHeaders.getReadonlyHeaderMap();

        // Standard headers
        assertEquals(httpHeaderMap.get(HttpHeaders.PRODUCT_HEADER_NAME), HttpHeaders.PRODUCT_HEADER_VALUE);
        assertEquals(httpHeaderMap.get(HttpHeaders.PRODUCT_VERSION_HEADER_NAME), HttpHeaders.PRODUCT_VERSION_HEADER_VALUE);
        assertEquals(httpHeaderMap.get(HttpHeaders.OS_HEADER_NAME), HttpHeaders.OS_HEADER_VALUE);
        assertEquals(httpHeaderMap.get(HttpHeaders.CPU_HEADER_NAME), HttpHeaders.CPU_HEADER_VALUE);
        assertEquals(httpHeaderMap.get(HttpHeaders.APPLICATION_VERSION_HEADER_NAME), "app-version");
        assertEquals(httpHeaderMap.get(HttpHeaders.CORRELATION_ID_HEADER_NAME), "correlation-id");

        // Overwritten standard header
        assertEquals(httpHeaderMap.get(HttpHeaders.APPLICATION_NAME_HEADER_NAME), uniqueAppName);

        // Extra header
        assertEquals(httpHeaderMap.get(uniqueHeaderKey), uniqueHeaderValue);
    }
}
