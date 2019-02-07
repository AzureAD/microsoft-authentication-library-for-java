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

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.Future;

import static org.testng.Assert.*;

@PowerMockIgnore({"javax.net.ssl.*"})
@Test(groups = { "checkin" })
@PrepareForTest({ ConfidentialClientApplication.class,
        AsymmetricKeyCredential.class, UserDiscoveryRequest.class })
public class ConfidentialClientApplicationTest extends PowerMockTestCase {

    private ConfidentialClientApplication app = null;

    @Test
    public void testAcquireTokenAuthCode_ClientCredential() throws Exception {
        app = PowerMock.createPartialMock(ConfidentialClientApplication.class,
                new String[] { "acquireTokenCommon" },
                new ConfidentialClientApplication.Builder(TestConfiguration.AAD_CLIENT_ID,
                ClientCredentialFactory.create(TestConfiguration.AAD_CLIENT_SECRET))
                        .authority(TestConfiguration.AAD_TENANT_ENDPOINT)
        );

        PowerMock.expectPrivate(app, "acquireTokenCommon",
                EasyMock.isA(MsalOAuthAuthorizationGrant.class),
                EasyMock.isA(ClientAuthentication.class),
                EasyMock.isA(ClientDataHttpHeaders.class)).andReturn(
                new AuthenticationResult("bearer", "accessToken",
                        "refreshToken", new Date().getTime(), "idToken", null,
                        false));
        PowerMock.replay(app);
        Future<AuthenticationResult> result = app
                .acquireTokenByAuthorizationCode(null, "auth_code",
                        new URI(TestConfiguration.AAD_DEFAULT_REDIRECT_URI));
        AuthenticationResult ar = result.get();
        Assert.assertNotNull(ar);
        PowerMock.verifyAll();
    }

    @Test
    public void testAcquireTokenAuthCode_KeyCredential() throws Exception {
        final KeyStore keystore = KeyStore.getInstance("PKCS12", "SunJSSE");
        keystore.load(
                new FileInputStream(this.getClass()
                        .getResource(TestConfiguration.AAD_CERTIFICATE_PATH)
                        .getFile()),
                TestConfiguration.AAD_CERTIFICATE_PASSWORD.toCharArray());
        final String alias = keystore.aliases().nextElement();
        final PrivateKey key = (PrivateKey) keystore.getKey(alias,
                TestConfiguration.AAD_CERTIFICATE_PASSWORD.toCharArray());
        final X509Certificate cert = (X509Certificate) keystore
                .getCertificate(alias);

        IClientCredential clientCredential =  ClientCredentialFactory.create(key, cert);

        app = PowerMock.createPartialMock(ConfidentialClientApplication.class,
                new String[] { "acquireTokenCommon" },
                new ConfidentialClientApplication.Builder(TestConfiguration.AAD_CLIENT_ID, clientCredential)
                        .authority(TestConfiguration.AAD_TENANT_ENDPOINT));

        PowerMock.expectPrivate(app, "acquireTokenCommon",
                EasyMock.isA(MsalOAuthAuthorizationGrant.class),
                EasyMock.isA(ClientAuthentication.class),
                EasyMock.isA(ClientDataHttpHeaders.class)).andReturn(
                new AuthenticationResult("bearer", "accessToken",
                        "refreshToken", new Date().getTime(), "idToken", null,
                        false));

        PowerMock.replay(app);
        Future<AuthenticationResult> result = app
                .acquireTokenByAuthorizationCode(null, "auth_code",
                        new URI(TestConfiguration.AAD_DEFAULT_REDIRECT_URI));
        AuthenticationResult ar = result.get();
        Assert.assertNotNull(ar);
        PowerMock.verifyAll();
        PowerMock.resetAll(app);
    }

    public void testAcquireToken_KeyCred() throws Exception {
        final KeyStore keystore = KeyStore.getInstance("PKCS12", "SunJSSE");
        keystore.load(
                new FileInputStream(this.getClass()
                        .getResource(TestConfiguration.AAD_CERTIFICATE_PATH)
                        .getFile()),
                TestConfiguration.AAD_CERTIFICATE_PASSWORD.toCharArray());
        final String alias = keystore.aliases().nextElement();
        final PrivateKey key = (PrivateKey) keystore.getKey(alias,
                TestConfiguration.AAD_CERTIFICATE_PASSWORD.toCharArray());
        final X509Certificate cert = (X509Certificate) keystore
                .getCertificate(alias);

        IClientCredential clientCredential =  ClientCredentialFactory.create(key, cert);

        app = PowerMock.createPartialMock(ConfidentialClientApplication.class,
                new String[] { "acquireTokenCommon" },
                new ConfidentialClientApplication.Builder(TestConfiguration.AAD_CLIENT_ID, clientCredential)
                        .authority(TestConfiguration.AAD_TENANT_ENDPOINT));

        PowerMock.expectPrivate(app, "acquireTokenCommon",
                EasyMock.isA(MsalOAuthAuthorizationGrant.class),
                EasyMock.isA(ClientAuthentication.class),
                EasyMock.isA(ClientDataHttpHeaders.class)).andReturn(
                new AuthenticationResult("bearer", "accessToken", null,
                        new Date().getTime(), null, null, false));

        PowerMock.replay(app);
        final Future<AuthenticationResult> result = app.acquireTokenForClient(
                TestConfiguration.AAD_RESOURCE_ID);
        final AuthenticationResult ar = result.get();
        assertNotNull(ar);
        assertFalse(StringHelper.isBlank(result.get().getAccessToken()));
        assertTrue(StringHelper.isBlank(result.get().getRefreshToken()));
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
