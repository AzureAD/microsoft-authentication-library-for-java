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
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Future;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

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
                EasyMock.isA(MsalRequest.class),
                EasyMock.isA(AuthenticationAuthority.class)).andReturn(
                AuthenticationResult.builder().
                        accessToken("accessToken").
                        expiresOn(new Date().getTime() + 100).
                        refreshToken("refreshToken").
                        idToken("idToken").environment("environment").build());

        PowerMock.replay(app);

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder
                ("auth_code",
                        new URI(TestConfiguration.AAD_DEFAULT_REDIRECT_URI))
                .scopes(Collections.singleton("default-scope"))
                .build();

        Future<AuthenticationResult> result = app.acquireToken(parameters);

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
                EasyMock.isA(MsalRequest.class),
                EasyMock.isA(AuthenticationAuthority.class)).andReturn(
                AuthenticationResult.builder().
                        accessToken("accessToken").
                        expiresOn(new Date().getTime() + 100).
                        refreshToken("refreshToken").
                        idToken("idToken").environment("environment").build());

        PowerMock.replay(app);

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder
                ("auth_code",
                        new URI(TestConfiguration.AAD_DEFAULT_REDIRECT_URI))
                .scopes(Collections.singleton("default-scope"))
                .build();

        Future<AuthenticationResult> result = app.acquireToken(parameters);

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
                EasyMock.isA(MsalRequest.class),
                EasyMock.isA(AuthenticationAuthority.class)).andReturn(
                AuthenticationResult.builder().
                        accessToken("accessToken").
                        expiresOn(new Date().getTime() + 100).
                        refreshToken("refreshToken").
                        idToken("idToken").environment("environment").build());

        PowerMock.replay(app);

        ClientCredentialParameters parameters = ClientCredentialParameters.builder(
                Collections.singleton(TestConfiguration.AAD_RESOURCE_ID))
                .build();

        final Future<AuthenticationResult> result = app.acquireToken(parameters);

        final AuthenticationResult ar = result.get();
        assertNotNull(ar);
        assertFalse(StringHelper.isBlank(result.get().accessToken()));
        PowerMock.verifyAll();
        PowerMock.resetAll(app);
    }
}
