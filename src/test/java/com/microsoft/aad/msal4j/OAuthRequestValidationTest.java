// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import static com.microsoft.aad.msal4j.TestConfiguration.INSTANCE_DISCOVERY_RESPONSE;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.replace;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.lang3.StringUtils;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.slf4j.Logger;

@PowerMockIgnore({"javax.net.ssl.*"})
@PrepareForTest({com.microsoft.aad.msal4j.OAuthHttpRequest.class, HttpHelper.class})
public class OAuthRequestValidationTest extends PowerMockTestCase {

    private final static String AUTHORITY = "https://loginXXX.windows.net/path/";

    private final static String CLIENT_ID = "ClientId";
    private final static String CLIENT_SECRET = "ClientPassword";

    private final static String SCOPES = "https://SomeResource.azure.net";
    private final static String DEFAULT_SCOPES = "openid profile offline_access";

    private final static String GRANT_TYPE_JWT = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    private final static String CLIENT_ASSERTION_TYPE_JWT = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private final static String ON_BEHALF_OF_USE_JWT = "on_behalf_of";

    private final static String CLIENT_CREDENTIALS_GRANT_TYPE = "client_credentials";

    private final static String CLIENT_INFO_VALUE = "1";

    private final static String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6Ikpva" +
            "G4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";

    private static String query;

    public OAuthRequestValidationTest() throws MalformedURLException {
    }

    @BeforeMethod
    public void init() {
        replace(method(OAuthHttpRequest.class, "send")).
                with(new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        OAuthRequestValidationTest.query = ((OAuthHttpRequest) proxy).getQuery();
                        throw new MsalException("", AuthenticationErrorCode.UNKNOWN);
                    }
                });
    }

    public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }

    private String getRSAjwt() throws NoSuchAlgorithmException, JOSEException {
        // RSA signatures require a public and private RSA key pair, the public key
        // must be made known to the JWS recipient in order to verify the signatures
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(2048);

        KeyPair kp = keyGenerator.genKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) kp.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();

        // Create RSA-signer with the private key
        JWSSigner signer = new RSASSASigner(privateKey);

        // Prepare JWT with claims set
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
        builder.issuer("alice");
        builder.subject("alice");
        List<String> aud = new ArrayList<String>();
        aud.add("https://app-one.com");
        aud.add("https://app-two.com");
        builder.audience(aud);
        // Set expiration in 10 minutes
        builder.expirationTime(new Date(new Date().getTime() + 1000 * 60 * 10));
        builder.notBeforeTime(new Date());
        builder.issueTime(new Date());
        builder.jwtID(UUID.randomUUID().toString());

        JWTClaimsSet jwtClaims = builder.build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.RS256),
                jwtClaims);

        // Compute the RSA signature
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    @Test
    public void oAuthRequest_for_acquireTokenByUserAssertion() throws Exception {
        ConfidentialClientApplication app =
                new ConfidentialClientApplication.Builder(CLIENT_ID, ClientCredentialFactory.create(CLIENT_SECRET))
                        .authority(AUTHORITY)
                        .validateAuthority(false).build();

        try {
            // Using UserAssertion as Authorization Grants
            OnBehalfOfParameters parameters =
                    OnBehalfOfParameters.builder(Collections.singleton(SCOPES), new UserAssertion(jwt))
                            .build();

            app.acquireToken(parameters).get();

        } catch (ExecutionException ex) {
            Assert.assertTrue(ex.getCause() instanceof MsalException);
        }

        Map<String, String> queryParams = splitQuery(query);
        Assert.assertEquals(7, queryParams.size());

        // validate Authorization Grants query params
        Assert.assertEquals(GRANT_TYPE_JWT, queryParams.get("grant_type"));
        Assert.assertEquals(jwt, queryParams.get("assertion"));

        // validate Client Authentication query params
        Assert.assertEquals(CLIENT_ID, queryParams.get("client_id"));
        Assert.assertEquals(CLIENT_SECRET, queryParams.get("client_secret"));

        // to do validate scopes
        Assert.assertEquals(SCOPES, queryParams.get("scope"));

        Assert.assertEquals("on_behalf_of", queryParams.get("requested_token_use"));

        Assert.assertEquals(CLIENT_INFO_VALUE, queryParams.get("client_info"));
    }

    @Test
    public void oAuthRequest_for_acquireTokenByAsymmetricKeyCredential() throws Exception {
        PowerMock.mockStatic(HttpHelper.class);

        EasyMock.expect(
                HttpHelper.executeHttpRequest(
                        EasyMock.isA(Logger.class),
                        EasyMock.isA(HttpMethod.class),
                        EasyMock.isA(String.class),
                        EasyMock.isA(Map.class),
                        EasyMock.isNull(),
                        EasyMock.isA(RequestContext.class),
                        EasyMock.isA(ServiceBundle.class)))
                .andReturn(INSTANCE_DISCOVERY_RESPONSE);


        PowerMock.replay(HttpHelper.class);

        try {
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

            IClientCredential clientCredential = ClientCredentialFactory.create(key, cert);

            ConfidentialClientApplication app = new ConfidentialClientApplication.Builder(CLIENT_ID, clientCredential)
                    .authority(AUTHORITY)
                    .validateAuthority(false).build();

            // Using UserAssertion as Authorization Grants
            OnBehalfOfParameters parameters =
                    OnBehalfOfParameters.builder(Collections.singleton(SCOPES), new UserAssertion(jwt))
                            .build();

            app.acquireToken(parameters).get();
        } catch (ExecutionException ex) {
            Assert.assertTrue(ex.getCause() instanceof MsalException);
        }

        Map<String, String> queryParams = splitQuery(query);
        Assert.assertEquals(7, queryParams.size());

        // validate Authorization Grants query params
        Assert.assertEquals(GRANT_TYPE_JWT, queryParams.get("grant_type"));
        Assert.assertEquals(jwt, queryParams.get("assertion"));

        // validate Client Authentication query params
        Assert.assertFalse(StringUtils.isEmpty(queryParams.get("client_assertion")));

        // to do validate scopes
        Assert.assertEquals(SCOPES, queryParams.get("scope"));

        Assert.assertEquals(CLIENT_ASSERTION_TYPE_JWT, queryParams.get("client_assertion_type"));
        Assert.assertEquals(ON_BEHALF_OF_USE_JWT, queryParams.get("requested_token_use"));

        Assert.assertEquals(CLIENT_INFO_VALUE, queryParams.get("client_info"));
    }

    @Test
    public void oAuthRequest_for_acquireTokenByClientAssertion() throws Exception {
        //String rsaJwt = getRSAjwt();
        try {
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

            ConfidentialClientApplication app =
                    new ConfidentialClientApplication.Builder(CLIENT_ID, ClientCredentialFactory.create(key, cert))
                            .authority(AUTHORITY)
                            .validateAuthority(false)
                            .build();

            // Using ClientAssertion for Client Authentication and as the authorization grant

            app.acquireToken(ClientCredentialParameters.builder(Collections.singleton(SCOPES))
                    .build())
                    .get();

        } catch (ExecutionException ex) {
            Assert.assertTrue(ex.getCause() instanceof MsalException);
        }

        Map<String, String> queryParams = splitQuery(query);

        Assert.assertEquals(5, queryParams.size());

        // validate Authorization Grants query params
        Assert.assertEquals(CLIENT_CREDENTIALS_GRANT_TYPE, queryParams.get("grant_type"));

        // validate Client Authentication query params
        Assert.assertTrue(StringUtils.isNotEmpty(queryParams.get("client_assertion")));
        Assert.assertEquals(CLIENT_ASSERTION_TYPE_JWT, queryParams.get("client_assertion_type"));

        // to do validate scopes
        Assert.assertEquals("openid profile offline_access https://SomeResource.azure.net", queryParams.get("scope"));

        Assert.assertEquals(CLIENT_INFO_VALUE, queryParams.get("client_info"));
    }
}

