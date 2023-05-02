// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.powermock.api.easymock.PowerMock.expectPrivate;

@PrepareForTest({DefaultHttpClient.class})
public class DefaultHttpClientTest extends PowerMockTestCase {

    @Test
    public void ValidNotOkHttpResponse() throws Exception {
        String TEST_URL = "https://somehost.com";

        HttpsURLConnection mockCon = PowerMock.createMock(HttpsURLConnection.class);

        EasyMock.expect(mockCon.getResponseCode())
                .andReturn(HttpURLConnection.HTTP_INTERNAL_ERROR).times(1);

        String errorResponse = "Error Message";
        InputStream inputStream = IOUtils.toInputStream(errorResponse, "UTF-8");
        EasyMock.expect(mockCon.getErrorStream()).andReturn(inputStream).times(1);

        Map<String, List<String>> expectedHeaders = new HashMap<>();
        expectedHeaders.put("header1", Arrays.asList("val1", "val2"));

        EasyMock.expect(mockCon.getHeaderFields()).andReturn(expectedHeaders).times(1);

        mockCon.setReadTimeout(0);
        mockCon.setConnectTimeout(0);

        DefaultHttpClient httpClient =
                PowerMock.createPartialMock(DefaultHttpClient.class, "openConnection");

        expectPrivate(httpClient, "openConnection", EasyMock.isA(URL.class)).andReturn(mockCon);

        PowerMock.replayAll(mockCon, httpClient);


        HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, TEST_URL);
        IHttpResponse response = httpClient.send(httpRequest);


        Assert.assertEquals(response.body(), errorResponse);
        Assert.assertEquals(response.headers(), expectedHeaders);
    }
}
