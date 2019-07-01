// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.testng.annotations.Test;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

/**
 *
 */

@Test(groups = { "checkin" })
public class HttpHelperTest extends AbstractMsalTests {

    @Test(expectedExceptions = AuthenticationException.class,
            expectedExceptionsMessageRegExp = "Server returned HTTP response code: 403 for URL : https://some.url, Error details : error info")
    public void testReadResponseFromConnection_ResponseCodeNot200()
            throws Exception {
        final HttpsURLConnection connection = PowerMock
                .createMock(HttpsURLConnection.class);
        EasyMock.expect(connection.getResponseCode()).andReturn(403).times(2);
        EasyMock.expect(connection.getURL()).andReturn(new URL("https://some.url"));

        String testInput = "error info";
        //StringReader reader = new StringReader(testInput);
        InputStream is = new ByteArrayInputStream(testInput.getBytes());

        EasyMock.expect(connection.getErrorStream()).andReturn(is).times(1);

        PowerMock.replayAll(connection);

        HttpEvent httpEvent = new HttpEvent();
        HttpHelper.readResponseFromConnection(connection, httpEvent);
    }
}
