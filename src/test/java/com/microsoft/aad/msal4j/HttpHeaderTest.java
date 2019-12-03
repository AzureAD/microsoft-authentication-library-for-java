// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

@Test(groups = { "checkin" })
public class HttpHeaderTest {

    @Test
    public void testHttpHeaderConstructor(){

        PublicClientApplication app = PublicClientApplication
                .builder("client-id")
                .correlationId("correlation-id")
                .applicationName("app-name")
                .applicationVersion("app-version")
                .build();

        RequestContext requestContext = new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE);

        HttpHeaders httpHeaders = new HttpHeaders(requestContext);

        Map<String, String> httpHeaderMap = httpHeaders.getReadonlyHeaderMap();

        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.PRODUCT_HEADER_NAME), HttpHeaders.PRODUCT_HEADER_VALUE);
        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.PRODUCT_VERSION_HEADER_NAME), HttpHeaders.PRODUCT_VERSION_HEADER_VALUE);
        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.OS_HEADER_NAME), HttpHeaders.OS_HEADER_VALUE);
        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.CPU_HEADER_NAME), HttpHeaders.CPU_HEADER_VALUE);
        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.APPLICATION_NAME_HEADER_NAME), "app-name");
        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.APPLICATION_VERSION_HEADER_NAME), "app-version");
        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.CORRELATION_ID_HEADER_NAME), "correlation-id");
    }

    @Test
    public void testHttpHeaderConstructor_valuesNotSet(){

        PublicClientApplication app = PublicClientApplication
                .builder("client-id")
                .build();

        RequestContext requestContext = new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE);

        HttpHeaders httpHeaders = new HttpHeaders(requestContext);

        Map<String, String> httpHeaderMap = httpHeaders.getReadonlyHeaderMap();

        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.PRODUCT_HEADER_NAME), HttpHeaders.PRODUCT_HEADER_VALUE);
        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.PRODUCT_VERSION_HEADER_NAME), HttpHeaders.PRODUCT_VERSION_HEADER_VALUE);
        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.OS_HEADER_NAME), HttpHeaders.OS_HEADER_VALUE);
        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.CPU_HEADER_NAME), HttpHeaders.CPU_HEADER_VALUE);
        Assert.assertNull(httpHeaderMap.get(HttpHeaders.APPLICATION_NAME_HEADER_NAME));
        Assert.assertNull(httpHeaderMap.get(HttpHeaders.APPLICATION_VERSION_HEADER_NAME));
        Assert.assertNotNull(httpHeaderMap.get(HttpHeaders.CORRELATION_ID_HEADER_NAME));
    }
}
