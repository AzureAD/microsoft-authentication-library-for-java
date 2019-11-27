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

        Assert.assertEquals(httpHeaderMap.get("x-client-SKU"), "MSAL.Java");
        Assert.assertEquals(httpHeaderMap.get("x-client-VER"), "1.0");
        Assert.assertEquals(httpHeaderMap.get("x-client-CPU"), System.getProperty("os.arch"));
        Assert.assertEquals(httpHeaderMap.get("x-client-OS"), System.getProperty("os.name"));
        Assert.assertEquals(httpHeaderMap.get("x-app-name"), "app-name");
        Assert.assertEquals(httpHeaderMap.get("x-app-ver"), "app-version");
        Assert.assertEquals(httpHeaderMap.get("client-request-id"), "correlation-id");
    }

    @Test
    public void testHttpHeaderConstructor_valuesNotSet(){

        PublicClientApplication app = PublicClientApplication
                .builder("client-id")
                .build();

        RequestContext requestContext = new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE);

        HttpHeaders httpHeaders = new HttpHeaders(requestContext);

        Map<String, String> httpHeaderMap = httpHeaders.getReadonlyHeaderMap();

        Assert.assertEquals(httpHeaderMap.get("x-client-SKU"), "MSAL.Java");
        Assert.assertEquals(httpHeaderMap.get("x-client-VER"), "1.0");
        Assert.assertEquals(httpHeaderMap.get("x-client-CPU"), System.getProperty("os.arch"));
        Assert.assertEquals(httpHeaderMap.get("x-client-OS"), System.getProperty("os.name"));
        Assert.assertEquals(httpHeaderMap.get("x-app-name"), "");
        Assert.assertEquals(httpHeaderMap.get("x-app-ver"), "");
        Assert.assertNotNull(httpHeaderMap.get("client-request-id"));
    }
}
