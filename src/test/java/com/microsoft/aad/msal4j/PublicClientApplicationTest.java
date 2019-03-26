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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import javax.net.ssl.SSLSocketFactory;
import java.net.Proxy;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Future;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.Test;

@PowerMockIgnore({"javax.net.ssl.*"})
@Test(groups = { "checkin" })
@PrepareForTest({ PublicClientApplication.class,
        AsymmetricKeyCredential.class, UserDiscoveryRequest.class})
public class PublicClientApplicationTest extends PowerMockTestCase {

    private PublicClientApplication app = null;

    public void testAcquireToken_Username_Password() throws Exception {
        app = PowerMock.createPartialMock(PublicClientApplication.class,
                new String[] { "acquireTokenCommon" },
                new PublicClientApplication.Builder(TestConfiguration.AAD_CLIENT_ID)
                        .authority(TestConfiguration.AAD_TENANT_ENDPOINT));

        PowerMock.expectPrivate(app, "acquireTokenCommon",
                EasyMock.isA(MsalOAuthAuthorizationGrant.class),
                EasyMock.isA(ClientAuthentication.class),
                EasyMock.isA(ClientDataHttpHeaders.class),
                EasyMock.isA(AuthenticationAuthority.class))
                .andReturn(AuthenticationResult.builder().
                                accessToken("accessToken").
                                expiresOn(new Date().getTime() + 100).
                                refreshToken("refreshToken").
                                idToken("idToken").environment("environment").build()
                );

        UserDiscoveryResponse response = EasyMock
                .createMock(UserDiscoveryResponse.class);
        EasyMock.expect(response.isAccountFederated()).andReturn(false);

        PowerMock.mockStatic(UserDiscoveryRequest.class);
        EasyMock.expect(
                UserDiscoveryRequest.execute(
                        EasyMock.isA(String.class),
                        EasyMock.isA(Map.class),
                        EasyMock.isNull(Proxy.class),
                        EasyMock.isNull(SSLSocketFactory.class))).andReturn(response);

        PowerMock.replay(app, response, UserDiscoveryRequest.class);
        Future<AuthenticationResult> result =
                app.acquireTokenByUsernamePassword(Collections.singleton("scopes"), "username", "password");

        AuthenticationResult ar = result.get();
        Assert.assertNotNull(ar);
        PowerMock.verifyAll();
        PowerMock.resetAll(app);
    }

    static String getThumbPrint(final byte[] der)
            throws NoSuchAlgorithmException, CertificateEncodingException {
        final MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(der);
        final byte[] digest = md.digest();
        return hexify(digest);

    }

    static String hexify(final byte bytes[]) {

        final char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'a', 'b', 'c', 'd', 'e', 'f' };

        final StringBuffer buf = new StringBuffer(bytes.length * 2);

        for (int i = 0; i < bytes.length; ++i) {
            buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
            buf.append(hexDigits[bytes[i] & 0x0f]);
        }

        return buf.toString();
    }
}
