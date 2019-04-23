// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

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
        EasyMock.expect(conn.getHeaderField("Cache-Control")).andReturn("cc")
                .times(1);
        EasyMock.expect(conn.getHeaderField("Pragma")).andReturn("pragma")
                .times(1);
        EasyMock.expect(conn.getHeaderField("WWW-Authenticate"))
                .andReturn("www-a").times(1);
        PowerMock.replay(conn);
        final HTTPResponse response = Whitebox.invokeMethod(request,
                "createResponse", conn, "content");
        PowerMock.verifyAll();
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getCacheControl(), "cc");
        Assert.assertEquals(response.getPragma(), "pragma");
        Assert.assertEquals(response.getWWWAuthenticate(), "www-a");
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
        EasyMock.expect(conn.getHeaderField("Cache-Control")).andReturn("cc")
                .times(1);
        EasyMock.expect(conn.getHeaderField("Pragma")).andReturn("pragma")
                .times(1);
        EasyMock.expect(conn.getHeaderField("WWW-Authenticate"))
                .andReturn("www-a").times(1);
        PowerMock.replay(conn);
        final HTTPResponse response = Whitebox.invokeMethod(request,
                "createResponse", conn, null);
        PowerMock.verifyAll();
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getCacheControl(), "cc");
        Assert.assertEquals(response.getPragma(), "pragma");
        Assert.assertEquals(response.getWWWAuthenticate(), "www-a");
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
