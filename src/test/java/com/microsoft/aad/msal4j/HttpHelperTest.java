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

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;


import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.testng.annotations.Test;

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

        HttpHelper.readResponseFromConnection(connection);
    }
}
