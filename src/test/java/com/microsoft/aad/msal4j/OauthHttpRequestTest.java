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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import static org.testng.Assert.assertNotNull;

@Test(groups = { "checkin" })
@PrepareForTest({ OAuthHttpRequest.class })
public class OauthHttpRequestTest extends AbstractMsalTests {

    @Test
    public void testConstructor() throws MalformedURLException {
        final OAuthHttpRequest request = new OAuthHttpRequest(Method.POST,
                new URL("http://login.windows.net"), null, null);
        assertNotNull(request);
    }


    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "Couldn't parse Content-Type header: Invalid Content-Type value: In Content-Type string <invalid-content>, expected '/', got null")
    public void testCreateResponseContentTypeParsingFailure()
            throws Exception {

        final OAuthHttpRequest request = new OAuthHttpRequest(Method.GET,
                new URL("https://" + TestConfiguration.AAD_HOST_NAME), null, null);
        final HttpURLConnection conn = PowerMock
                .createMock(HttpURLConnection.class);
        EasyMock.expect(conn.getResponseCode()).andReturn(200).times(1);
        EasyMock.expect(conn.getHeaderField("Location"))
                .andReturn("https://location.pl").times(1);
        EasyMock.expect(conn.getContentType()).andReturn("invalid-content")
                .times(1);
        PowerMock.replay(conn);
        Whitebox.invokeMethod(request, "createResponse", conn, null);
    }

    @Test
    public void testCreateResponseLocationNull()
            throws Exception {
        final OAuthHttpRequest request = new OAuthHttpRequest(Method.GET,
                new URL("https://" + TestConfiguration.AAD_HOST_NAME), null, null);
        final HttpURLConnection conn = PowerMock
                .createMock(HttpURLConnection.class);
        EasyMock.expect(conn.getResponseCode()).andReturn(200).times(1);
        EasyMock.expect(conn.getHeaderField("Location")).andReturn(null)
                .times(1);
        EasyMock.expect(conn.getContentType())
                .andReturn("application/x-www-form-urlencoded").times(1);
        EasyMock.expect(conn.getHeaderFields()).andReturn(new HashMap<>());
        PowerMock.replay(conn);
        final HTTPResponse response = Whitebox.invokeMethod(request,
                "createResponse", conn, "content");
        PowerMock.verifyAll();
        Assert.assertNotNull(response);
        Assert.assertNull(response.getLocation(), "location.pl");
        Assert.assertEquals(response.getContent(), "content");
    }

    @Test
    public void testCreateResponse() throws Exception {
        final OAuthHttpRequest request = new OAuthHttpRequest(Method.GET,
                new URL("https://" + TestConfiguration.AAD_HOST_NAME), null, null);
        final HttpURLConnection conn = PowerMock
                .createMock(HttpURLConnection.class);
        EasyMock.expect(conn.getResponseCode()).andReturn(200).times(1);
        EasyMock.expect(conn.getHeaderField("Location"))
                .andReturn("https://location.pl").times(1);
        EasyMock.expect(conn.getContentType())
                .andReturn("application/x-www-form-urlencoded").times(1);
        EasyMock.expect(conn.getHeaderFields()).andReturn(new HashMap<>());
        PowerMock.replay(conn);
        final HTTPResponse response = Whitebox.invokeMethod(request,
                "createResponse", conn, null);
        PowerMock.verifyAll();
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getLocation().getAuthority(),
                "location.pl");
        Assert.assertEquals(response.getLocation().getScheme(), "https");
        Assert.assertNull(response.getContent());
    }
    
    @Test
    public void testCreateResponseFor404() throws Exception {
        final OAuthHttpRequest request = new OAuthHttpRequest(Method.GET,
                new URL("https://" + TestConfiguration.AAD_HOST_NAME), null, null);
        final HttpURLConnection conn = PowerMock
                .createMock(HttpURLConnection.class);
        EasyMock.expect(conn.getResponseCode()).andReturn(404);
        EasyMock.expect(conn.getErrorStream()).andReturn(null);

        InputStream stream = new ByteArrayInputStream("stream".getBytes());

        EasyMock.expect(conn.getInputStream()).andReturn(stream);
        PowerMock.replay(conn);
        final String response = Whitebox.invokeMethod(request,
                "processAndReadResponse", conn);
        Assert.assertEquals(response, "stream");
        PowerMock.verifyAll();
    }
}
