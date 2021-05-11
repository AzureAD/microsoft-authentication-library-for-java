// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Test(groups = {"checkin"})
public class HttpHeaderTest {

    @Test
    public void testHttpHeaderConstructor() {

        PublicClientApplication app = PublicClientApplication
                .builder("client-id")
                .correlationId("correlation-id")
                .applicationName("app-name")
                .applicationVersion("app-version")
                .build();

        IApiParameters parameters = UserNamePasswordParameters
                .builder(Collections.singleton("scopes"), "username", "password".toCharArray())
                .build();

        RequestContext requestContext = new RequestContext(
                app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters);

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
    public void testHttpHeaderConstructor_valuesNotSet() {

        PublicClientApplication app = PublicClientApplication
                .builder("client-id")
                .build();

        IApiParameters parameters = UserNamePasswordParameters
                .builder(Collections.singleton("scopes"), "username", "password".toCharArray())
                .build();

        RequestContext requestContext = new RequestContext(
                app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters);

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

    @Test
    public void testHttpHeaderConstructor_userIdentifierUPN() {

        PublicClientApplication app = PublicClientApplication
                .builder("client-id")
                .build();

        IApiParameters parameters = UserNamePasswordParameters
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

        String expectedValue = String.format("%s:%s", "upn", upn);
        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.X_ANCHOR_MAILBOX), expectedValue);
    }

    @Test
    public void testHttpHeaderConstructor_userIdentifierHomeAccountId() {

        PublicClientApplication app = PublicClientApplication
                .builder("client-id")
                .build();

        IApiParameters parameters = UserNamePasswordParameters
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

        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.X_ANCHOR_MAILBOX), "oid:userObjectId@userTenantId");
    }

    @Test
    public void testHttpHeaderConstructor_extraHttpHeadersOverwriteLibraryHeaders() {

        PublicClientApplication app = PublicClientApplication
                .builder("client-id")
                .correlationId("correlation-id")
                .applicationName("app-name")
                .applicationVersion("app-version")
                .build();

        String uniqueHeaderKey = "uniqueHeader";
        String uniqueHeaderValue = "uniqueValue";
        String uniqueAppName = "my-unique-app-name";
        Map<String, String> extraHttpHeaders = new HashMap<>();
        extraHttpHeaders.put(uniqueHeaderKey, uniqueHeaderValue);
        extraHttpHeaders.put(HttpHeaders.APPLICATION_NAME_HEADER_NAME, uniqueAppName);

        IApiParameters parameters = UserNamePasswordParameters
                .builder(Collections.singleton("scopes"), "username", "password".toCharArray())
                .extraHttpHeaders(extraHttpHeaders)
                .build();

        RequestContext requestContext = new RequestContext(
                app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters);

        HttpHeaders httpHeaders = new HttpHeaders(requestContext);

        Map<String, String> httpHeaderMap = httpHeaders.getReadonlyHeaderMap();

        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.PRODUCT_HEADER_NAME), HttpHeaders.PRODUCT_HEADER_VALUE);
        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.PRODUCT_VERSION_HEADER_NAME), HttpHeaders.PRODUCT_VERSION_HEADER_VALUE);
        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.OS_HEADER_NAME), HttpHeaders.OS_HEADER_VALUE);
        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.CPU_HEADER_NAME), HttpHeaders.CPU_HEADER_VALUE);
        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.APPLICATION_NAME_HEADER_NAME), uniqueAppName);
        Assert.assertEquals(httpHeaderMap.get(uniqueHeaderKey), uniqueHeaderValue);
        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.APPLICATION_VERSION_HEADER_NAME), "app-version");
        Assert.assertEquals(httpHeaderMap.get(HttpHeaders.CORRELATION_ID_HEADER_NAME), "correlation-id");
    }
}
