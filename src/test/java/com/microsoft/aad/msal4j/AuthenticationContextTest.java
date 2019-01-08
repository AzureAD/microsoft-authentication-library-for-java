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
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

@PowerMockIgnore({"javax.net.ssl.*"})
@Test(groups = { "checkin" })
//@PrepareForTest({ AuthenticationContext.class, AuthenticationCallback.class,
//		AsymmetricKeyCredential.class, UserDiscoveryRequest.class })
public class AuthenticationContextTest extends AbstractAdalTests {
/*
	private AuthenticationContext ctx = null;
	private ExecutorService service = null;

	@BeforeTest
	public void setup() {
		service = Executors.newFixedThreadPool(1);
	}

	@AfterTest
	public void cleanup() {
		if (service != null) {
			service.shutdown();
		}
	}

	@Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "authority is null or empty")
	public void testConstructor_NullAuthority() throws MalformedURLException {
		ctx = new AuthenticationContext(null, true, service);
	}

	@Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "service is null")
	public void testConstructor_NullService() throws MalformedURLException {
		ctx = new AuthenticationContext(TestConfiguration.AAD_TENANT_ENDPOINT,
				true, null);
	}

	@Test
	public void testCorrelationId() throws MalformedURLException {
		ctx = new AuthenticationContext(TestConfiguration.AAD_TENANT_ENDPOINT,
				true, service);
		ctx.setCorrelationId("correlationId");
		Assert.assertEquals(ctx.getCorrelationId(), "correlationId");
	}

	@Test
	public void testAcquireTokenAuthCode_ClientCredential() throws Exception {
		ctx = PowerMock.createPartialMock(AuthenticationContext.class,
				new String[] { "acquireTokenCommon" },
				TestConfiguration.AAD_TENANT_ENDPOINT, true, service);
		PowerMock.expectPrivate(ctx, "acquireTokenCommon",
				EasyMock.isA(MsalOAuthAuthorizationGrant.class),
				EasyMock.isA(ClientAuthentication.class),
				EasyMock.isA(ClientDataHttpHeaders.class)).andReturn(
				new AuthenticationResult("bearer", "accessToken",
						"refreshToken", new Date().getTime(), "idToken", null,
						false));
		PowerMock.replay(ctx);
		Future<AuthenticationResult> result = ctx
				.acquireTokenByAuthorizationCode("auth_code", new URI(
						TestConfiguration.AAD_DEFAULT_REDIRECT_URI),
						new ClientSecret("clientId", "clientSecret"), null);
		AuthenticationResult ar = result.get();
		Assert.assertNotNull(ar);
		PowerMock.verifyAll();
	}

	@Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "authorization code is null or empty")
	public void testAcquireTokenAuthCode_AuthCodeNull() throws Exception {
		ctx = new AuthenticationContext(TestConfiguration.AAD_TENANT_ENDPOINT,
				true, service);
		ctx.acquireTokenByAuthorizationCode(null, new URI(
				TestConfiguration.AAD_DEFAULT_REDIRECT_URI),
				new ClientSecret("clientId", "clientSecret"), null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "redirect uri is null")
	public void testAcquireTokenAuthCode_RedirectUriNull() throws Exception {
		ctx = new AuthenticationContext(TestConfiguration.AAD_TENANT_ENDPOINT,
				true, service);
		ctx.setLogPii(true);
		ctx.acquireTokenByAuthorizationCode("auth_code", null,
				new ClientSecret("clientId", "clientSecret"), null);
	}

	public void testAcquireToken_Username_Password() throws Exception {
		ctx = PowerMock.createPartialMock(AuthenticationContext.class,
				new String[] { "acquireTokenCommon" },
				TestConfiguration.AAD_TENANT_ENDPOINT, true, service);
		PowerMock.expectPrivate(ctx, "acquireTokenCommon",
				EasyMock.isA(MsalOAuthAuthorizationGrant.class),
				EasyMock.isA(ClientAuthentication.class),
				EasyMock.isA(ClientDataHttpHeaders.class))
				.andReturn(
						new AuthenticationResult("bearer", "accessToken",
								"refreshToken", new Date().getTime(), null,
								null, false));

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

		PowerMock.replay(ctx, response, UserDiscoveryRequest.class);
		Future<AuthenticationResult> result = ctx.acquireToken("resource",
				"clientId", "username", "password", null);

		AuthenticationResult ar = result.get();
		Assert.assertNotNull(ar);
		PowerMock.verifyAll();
		PowerMock.resetAll(ctx);
	}

	@Test
	public void testAcquireTokenAuthCode_KeyCredential() throws Exception {
		ctx = PowerMock.createPartialMock(AuthenticationContext.class,
				new String[] { "acquireTokenCommon" },
				TestConfiguration.AAD_TENANT_ENDPOINT, true, service);
		PowerMock.expectPrivate(ctx, "acquireTokenCommon",
				EasyMock.isA(MsalOAuthAuthorizationGrant.class),
				EasyMock.isA(ClientAuthentication.class),
				EasyMock.isA(ClientDataHttpHeaders.class)).andReturn(
				new AuthenticationResult("bearer", "accessToken",
						"refreshToken", new Date().getTime(), "idToken", null,
						false));
		final KeyStore keystore = KeyStore.getInstance("PKCS12", "SunJSSE");
		keystore.load(
				new FileInputStream(this.getClass()
						.getScopes(TestConfiguration.AAD_CERTIFICATE_PATH)
						.getFile()),
				TestConfiguration.AAD_CERTIFICATE_PASSWORD.toCharArray());
		final String alias = keystore.aliases().nextElement();
		final PrivateKey key = (PrivateKey) keystore.getKey(alias,
				TestConfiguration.AAD_CERTIFICATE_PASSWORD.toCharArray());
		final X509Certificate cert = (X509Certificate) keystore
				.getCertificate(alias);

		PowerMock.replay(ctx);
		Future<AuthenticationResult> result = ctx
				.acquireTokenByAuthorizationCode("auth_code", new URI(
						TestConfiguration.AAD_DEFAULT_REDIRECT_URI),
						AsymmetricKeyCredential.create(
								TestConfiguration.AAD_CLIENT_ID, key, cert),
						null);
		AuthenticationResult ar = result.get();
		Assert.assertNotNull(ar);
		PowerMock.verifyAll();
		PowerMock.resetAll(ctx);
	}

	public void testAcquireToken_KeyCred() throws Exception {

		ctx = PowerMock.createPartialMock(AuthenticationContext.class,
				new String[] { "acquireTokenCommon" },
				TestConfiguration.AAD_TENANT_ENDPOINT, true, service);
		PowerMock.expectPrivate(ctx, "acquireTokenCommon",
				EasyMock.isA(MsalOAuthAuthorizationGrant.class),
				EasyMock.isA(ClientAuthentication.class),
				EasyMock.isA(ClientDataHttpHeaders.class)).andReturn(
				new AuthenticationResult("bearer", "accessToken", null,
						new Date().getTime(), null, null, false));

		final KeyStore keystore = KeyStore.getInstance("PKCS12", "SunJSSE");
		keystore.load(
				new FileInputStream(this.getClass()
						.getScopes(TestConfiguration.AAD_CERTIFICATE_PATH)
						.getFile()),
				TestConfiguration.AAD_CERTIFICATE_PASSWORD.toCharArray());
		final String alias = keystore.aliases().nextElement();
		final PrivateKey key = (PrivateKey) keystore.getKey(alias,
				TestConfiguration.AAD_CERTIFICATE_PASSWORD.toCharArray());
		final X509Certificate cert = (X509Certificate) keystore
				.getCertificate(alias);

		PowerMock.replay(ctx);
		final Future<AuthenticationResult> result = ctx.acquireToken(
				TestConfiguration.AAD_RESOURCE_ID, AsymmetricKeyCredential
						.create(TestConfiguration.AAD_CLIENT_ID, key, cert),
				null);
		final AuthenticationResult ar = result.get();
		assertNotNull(ar);
		assertFalse(StringHelper.isBlank(result.get().getAccessToken()));
		assertTrue(StringHelper.isBlank(result.get().getRefreshToken()));
		PowerMock.verifyAll();
		PowerMock.resetAll(ctx);
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testInvalidClientAssertion() throws MalformedURLException {
		ctx = new AuthenticationContext(TestConfiguration.AAD_TENANT_ENDPOINT,
				true, service);
		ctx.acquireToken(TestConfiguration.AAD_RESOURCE_ID,
				new ClientAssertion("invalid_assertion"), null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "resource is null or empty")
	public void testValidateInput_ValidateNullResource()
			throws MalformedURLException {
		ctx = new AuthenticationContext(TestConfiguration.AAD_TENANT_ENDPOINT,
				true, service);
		ctx.acquireToken(null, new ClientAssertion("invalid_assertion"), null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "credential is null")
	public void testValidateInput_NullCredential() throws MalformedURLException {
		ctx = new AuthenticationContext(TestConfiguration.AAD_TENANT_ENDPOINT,
				true, service);
		ctx.acquireToken(TestConfiguration.AAD_RESOURCE_ID,
				(ClientAssertion) null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "refreshToken is null or empty")
	public void testValidateRefreshTokenRequestInput_NullRefreshToken()
			throws MalformedURLException {
		ctx = new AuthenticationContext(TestConfiguration.AAD_TENANT_ENDPOINT,
				true, service);
		ctx.acquireTokenByRefreshToken(null, "client_id", new ClientAssertion(
				"invalid_assertion"), null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "clientId is null or empty")
	public void testValidateRefreshTokenRequestInput_NullClientId()
			throws MalformedURLException {
		ctx = new AuthenticationContext(TestConfiguration.AAD_TENANT_ENDPOINT,
				true, service);
		ctx.acquireTokenByRefreshToken("refresh_token", null,
				new ClientAssertion("invalid_assertion"), null);
	}

	@Test
	public void testFailedAcquireTokenRequest_ExecuteCallback()
			throws Throwable {
		ctx = new AuthenticationContext(
				TestConfiguration.AAD_UNKNOWN_TENANT_ENDPOINT, true, service);
		AuthenticationCallback ac = PowerMock
				.createMock(AuthenticationCallback.class);
		ac.onFailure(EasyMock.isA(Throwable.class));
		EasyMock.expectLastCall();
		PowerMock.replay(ac);
		Future<AuthenticationResult> result = ctx.acquireTokenByRefreshToken(
				"refresh", new ClientSecret("clientId", "clientSecret"),
				"resource", ac);
		try {
			result.get();
		} catch (ExecutionException ee) {
			throw ee.getCause();
		}
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
	*/
}