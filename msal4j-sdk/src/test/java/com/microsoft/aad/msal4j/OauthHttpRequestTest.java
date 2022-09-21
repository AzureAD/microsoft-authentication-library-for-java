// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertNotNull;

@Test(groups = {"checkin"})
@PrepareForTest({OAuthHttpRequest.class})
public class OauthHttpRequestTest extends AbstractMsalTests {

    @Test
    public void testConstructor() throws MalformedURLException {
        final OAuthHttpRequest request = new OAuthHttpRequest(
                Method.POST,
                new URL("http://login.windows.net"),
                null,
                null,
                null);
        assertNotNull(request);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "Couldn't parse Content-Type header: Invalid Content-Type value: Invalid content type string")
    public void testCreateResponseContentTypeParsingFailure() throws Exception {

        final OAuthHttpRequest request = new OAuthHttpRequest(
                Method.GET,
                new URL("https://" + TestConfiguration.AAD_HOST_NAME),
                null,
                null,
                null);

        final HttpResponse httpResponse = PowerMock
                .createMock(HttpResponse.class);

        EasyMock.expect(httpResponse.statusCode()).andReturn(200).times(1);

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Location", Collections.singletonList("https://location.pl"));
        headers.put("Content-Type", Collections.singletonList("invalid-content"));

        EasyMock.expect(httpResponse.headers()).andReturn(headers).times(3);
        EasyMock.expect(httpResponse.body()).andReturn("").times(1);
        PowerMock.replay(httpResponse);

        final HTTPResponse response = Whitebox.invokeMethod(request,
                "createOauthHttpResponseFromHttpResponse", httpResponse);
    }

    @Test
    public void testCreateResponse() throws Exception {
        final OAuthHttpRequest request = new OAuthHttpRequest(
                Method.GET,
                new URL("https://" + TestConfiguration.AAD_HOST_NAME),
                null,
                null,
                null);

        final HttpResponse httpResponse = PowerMock
                .createMock(HttpResponse.class);

        EasyMock.expect(httpResponse.statusCode()).andReturn(200).times(1);

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Location", Collections.singletonList("https://location.pl"));
        headers.put("Content-Type", Collections.singletonList("application/x-www-form-urlencoded"));

        EasyMock.expect(httpResponse.headers()).andReturn(headers).times(3);
        EasyMock.expect(httpResponse.body()).andReturn("").times(1);
        PowerMock.replay(httpResponse);

        final HTTPResponse response = Whitebox.invokeMethod(request,
                "createOauthHttpResponseFromHttpResponse", httpResponse);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getLocation().getAuthority(),
                "location.pl");
        Assert.assertEquals(response.getLocation().getScheme(), "https");
        Assert.assertNull(response.getContent());
        PowerMock.verifyAll();
    }
}
