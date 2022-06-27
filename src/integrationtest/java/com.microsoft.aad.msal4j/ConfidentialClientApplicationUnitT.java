// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.auth.JWTAuthentication;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;

import static com.microsoft.aad.msal4j.TestConstants.KEYVAULT_DEFAULT_SCOPE;
import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

@PowerMockIgnore({"javax.net.ssl.*"})
@PrepareForTest({ConfidentialClientApplication.class,
        ClientCertificate.class, UserDiscoveryRequest.class, JwtHelper.class})
public class ConfidentialClientApplicationUnitT extends PowerMockTestCase {

    private ConfidentialClientApplication app = null;
    private IClientCertificate clientCertificate;

    @BeforeClass
    private void init() throws
            KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException, NoSuchProviderException {

        clientCertificate = CertificateHelper.getClientCertificate();
    }

    @Test
    public void testAcquireTokenAuthCode_ClientCredential() throws Exception {
        app = PowerMock.createPartialMock(ConfidentialClientApplication.class,
                new String[]{"acquireTokenCommon"},
                ConfidentialClientApplication.builder(TestConfiguration.AAD_CLIENT_ID,
                        ClientCredentialFactory.createFromSecret(TestConfiguration.AAD_CLIENT_DUMMYSECRET))
                        .authority(TestConfiguration.AAD_TENANT_ENDPOINT)
        );

        PowerMock.expectPrivate(app, "acquireTokenCommon",
                EasyMock.isA(MsalRequest.class),
                EasyMock.isA(AADAuthority.class)).andReturn(
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

        Future<IAuthenticationResult> result = app.acquireToken(parameters);

        IAuthenticationResult ar = result.get();
        Assert.assertNotNull(ar);
        PowerMock.verifyAll();
    }

    @Test
    public void testAcquireTokenAuthCode_KeyCredential() throws Exception {
        app = PowerMock.createPartialMock(ConfidentialClientApplication.class,
                new String[]{"acquireTokenCommon"},
                ConfidentialClientApplication.builder(TestConfiguration.AAD_CLIENT_ID, clientCertificate)
                        .authority(TestConfiguration.AAD_TENANT_ENDPOINT));

        PowerMock.expectPrivate(app, "acquireTokenCommon",
                EasyMock.isA(MsalRequest.class),
                EasyMock.isA(AADAuthority.class)).andReturn(
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

        Future<IAuthenticationResult> result = app.acquireToken(parameters);

        IAuthenticationResult ar = result.get();
        Assert.assertNotNull(ar);
        PowerMock.verifyAll();
        PowerMock.resetAll(app);
    }

    @Test
    public void testAcquireToken_KeyCred() throws Exception {
        app = PowerMock.createPartialMock(ConfidentialClientApplication.class,
                new String[]{"acquireTokenCommon"},
                ConfidentialClientApplication.builder(TestConfiguration.AAD_CLIENT_ID, clientCertificate)
                        .authority(TestConfiguration.AAD_TENANT_ENDPOINT));

        PowerMock.expectPrivate(app, "acquireTokenCommon",
                EasyMock.isA(MsalRequest.class),
                EasyMock.isA(AADAuthority.class)).andReturn(
                AuthenticationResult.builder().
                        accessToken("accessToken").
                        expiresOn(new Date().getTime() + 100).
                        refreshToken("refreshToken").
                        idToken("idToken").environment("environment").build());

        PowerMock.replay(app);

        ClientCredentialParameters parameters = ClientCredentialParameters.builder(
                Collections.singleton(TestConfiguration.AAD_RESOURCE_ID))
                .build();

        Future<IAuthenticationResult> result = app.acquireToken(parameters);

        IAuthenticationResult ar = result.get();
        assertNotNull(ar);
        assertFalse(StringHelper.isBlank(result.get().accessToken()));
        PowerMock.verifyAll();
        PowerMock.resetAll(app);
    }

    @Test
    public void testClientCertificateRebuildsWhenExpired() throws Exception {
        PowerMock.mockStaticPartial(JwtHelper.class, "buildJwt");
        long jwtExperiationPeriodMilli = 2000;
        ClientAssertion shortExperationJwt = buildShortJwt(TestConfiguration.AAD_CLIENT_ID,
                clientCertificate,
                TestConfiguration.AAD_TENANT_ENDPOINT,
                jwtExperiationPeriodMilli);

        PowerMock.expectPrivate(
                JwtHelper.class,
                "buildJwt",
                EasyMock.isA(String.class),
                EasyMock.isA(ClientCertificate.class),
                EasyMock.isA(String.class),
                EasyMock.anyBoolean())
                .andReturn(shortExperationJwt)
                .times(2); // By this being called twice we ensure the client assertion is rebuilt once it has expired

        PowerMock.replay(JwtHelper.class);
        app = ConfidentialClientApplication.builder(TestConfiguration.AAD_CLIENT_ID, clientCertificate)
                .authority(TestConfiguration.AAD_TENANT_ENDPOINT).build();
        Thread.sleep(jwtExperiationPeriodMilli + 1000); //Have to sleep to ensure that the time period has passed
        final PrivateKeyJWT clientAuthentication = (PrivateKeyJWT) app.clientAuthentication();
        assertNotNull(clientAuthentication);
        PowerMock.verifyAll();
    }

    private ClientAssertion buildShortJwt(String clientId,
                                          IClientCertificate credential,
                                          String jwtAudience,
                                          long jwtExperiationPeriod) {
        final long time = System.currentTimeMillis();
        final JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .audience(Collections.singletonList(jwtAudience))
                .issuer(clientId)
                .jwtID(UUID.randomUUID().toString())
                .notBeforeTime(new Date(time))
                .expirationTime(new Date(time + jwtExperiationPeriod))
                .subject(clientId)
                .build();
        SignedJWT jwt;
        try {
            JWSHeader.Builder builder = new JWSHeader.Builder(JWSAlgorithm.RS256);

            List<Base64> certs = new ArrayList<>();
            for (String cert : credential.getEncodedPublicKeyCertificateChain()) {
                certs.add(new Base64(cert));
            }
            builder.x509CertChain(certs);

            builder.x509CertThumbprint(new Base64URL(credential.publicCertificateHash()));

            jwt = new SignedJWT(builder.build(), claimsSet);
            final RSASSASigner signer = new RSASSASigner(credential.privateKey());
            jwt.sign(signer);
        } catch (final Exception e) {
            throw new MsalClientException(e);
        }
        return new ClientAssertion(jwt.serialize());
    }

    @Test
    public void testClientAssertion_noException() throws Exception{
        SignedJWT jwt = createClientAssertion("issuer");

        ClientAssertion clientAssertion = new ClientAssertion(jwt.serialize());

        IClientCredential iClientCredential = ClientCredentialFactory.createFromClientAssertion(
                clientAssertion.assertion());

        ConfidentialClientApplication app = ConfidentialClientApplication
                .builder(TestConfiguration.AAD_CLIENT_ID, iClientCredential)
                .authority(TestConfiguration.AAD_TENANT_ENDPOINT)
                .build();

        Assert.assertEquals(app.clientId(),TestConfiguration.AAD_CLIENT_ID);
        Assert.assertTrue(app.sendX5c());

    }

    @Test
    public void testClientAssertion_acquireToken() throws Exception{
        SignedJWT jwt = createClientAssertion("issuer");

        ClientAssertion clientAssertion = new ClientAssertion(jwt.serialize());
        ConfidentialClientApplication app = ConfidentialClientApplication
                .builder(TestConfiguration.AAD_CLIENT_ID, ClientCredentialFactory.createFromClientAssertion(clientAssertion.assertion()))
                .authority(TestConfiguration.AAD_TENANT_ENDPOINT)
                .build();

        String scope = "requestedScope";
        ClientCredentialRequest clientCredentialRequest = getClientCredentialRequest(app, scope);

        IHttpClient httpClientMock = EasyMock.mock(IHttpClient.class);
        Capture<HttpRequest> captureSingleArgument = newCapture();
        expect(httpClientMock.send(capture(captureSingleArgument))).andReturn(new HttpResponse());
        EasyMock.replay(httpClientMock);

        TokenRequestExecutor tokenRequestExecutor = new TokenRequestExecutor(app.authenticationAuthority, clientCredentialRequest, mockedServiceBundle(httpClientMock));
        try {
            tokenRequestExecutor.executeTokenRequest();
        } catch(Exception e) {
            //Ignored, we only want to check the request that was send.
        }
        HttpRequest value = captureSingleArgument.getValue();
        String body = value.body();
        Assert.assertTrue(body.contains("grant_type=client_credentials"));
        Assert.assertTrue(body.contains("client_assertion=" + clientAssertion.assertion()));
        Assert.assertTrue(body.contains("client_assertion_type=" + URLEncoder.encode(JWTAuthentication.CLIENT_ASSERTION_TYPE, "utf-8")));
        Assert.assertTrue(body.contains("scope=" + URLEncoder.encode("openid profile offline_access " + scope, "utf-8")));
        Assert.assertTrue(body.contains("client_id=" + TestConfiguration.AAD_CLIENT_ID));
    }

    private ServiceBundle mockedServiceBundle(IHttpClient httpClientMock) {
        ServiceBundle serviceBundle = new ServiceBundle(
                null,
                httpClientMock,
                new TelemetryManager(null, false));
        return serviceBundle;
    }

    private ClientCredentialRequest getClientCredentialRequest(ConfidentialClientApplication app, String scope) {
        Set<String> scopes = new HashSet<>();
        scopes.add(scope);
        ClientCredentialParameters clientCredentials = ClientCredentialParameters.builder(scopes).tenant(IdToken.TENANT_IDENTIFIER).build();
        RequestContext requestContext = new RequestContext(
                app,
                PublicApi.ACQUIRE_TOKEN_FOR_CLIENT,
                clientCredentials);

        return new ClientCredentialRequest(
                        clientCredentials,
                        app,
                        requestContext);
    }

    @Test(expectedExceptions = MsalClientException.class)
    public void testClientAssertion_throwsException() throws Exception{
        SignedJWT jwt = createClientAssertion(null);

        ClientAssertion clientAssertion = new ClientAssertion(jwt.serialize());

        IClientCredential iClientCredential = ClientCredentialFactory.createFromClientAssertion(
                clientAssertion.assertion());

        ConfidentialClientApplication.builder(TestConfiguration.AAD_CLIENT_ID, iClientCredential).authority(TestConfiguration.AAD_TENANT_ENDPOINT).build();

    }

    @Test
    public void validateAppTokenProviderAsync() throws Exception{

        SignedJWT jwt = createClientAssertion("issuer");

        ClientAssertion clientAssertion = new ClientAssertion(jwt.serialize());

        IClientCredential iClientCredential = ClientCredentialFactory.createFromClientAssertion(
                clientAssertion.assertion());

        //builds client with AppTokenProvider
        ConfidentialClientApplication cca = ConfidentialClientApplication.
                builder(TestConfiguration.AAD_CLIENT_ID, iClientCredential)
                .appTokenProvider((parameters) -> {
                    Assert.assertNotNull(parameters.scopes);
                    Assert.assertNotNull(parameters.correlationId);
                    Assert.assertNotNull(parameters.tenantId);
                    return getAppTokenProviderResult("/default");
                })
                .build();

        IAuthenticationResult result1 = cca.acquireToken(ClientCredentialParameters
                        .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                        .tenant("tenant1")
                        .build())
                .get();

        Assert.assertNotNull(result1.accessToken());

        Assert.assertEquals(cca.tokenCache.accessTokens.size(), 1);

        //Acquire token from cache

        IAuthenticationResult result2 = cca.acquireToken(ClientCredentialParameters
                        .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                        .build())
                .get();

        Assert.assertEquals(result1.accessToken(), result2.accessToken());

        Assert.assertEquals(cca.tokenCache.accessTokens.size(), 1);

        cca = ConfidentialClientApplication.
                builder(TestConfiguration.AAD_CLIENT_ID, iClientCredential)
                .appTokenProvider((parameters) -> {
                    Assert.assertNotNull(parameters.scopes);
                    Assert.assertNotNull(parameters.correlationId);
                    Assert.assertNotNull(parameters.tenantId);
                    return getAppTokenProviderResult("/newScope");
                })
                .build();

        IAuthenticationResult result3 = cca.acquireToken(ClientCredentialParameters
                        .builder(Collections.singleton("/newScope"))
                                .tenant("tenant1")
//                        .claims(new ClaimsRequest().formatAsClaimsRequest(TestConstants.CLAIMS))
                        .build())
                .get();

        Assert.assertNotEquals(result2.accessToken(), result3.accessToken());
        Assert.assertEquals(cca.tokenCache.accessTokens.size(), 1);

    }

    private CompletableFuture<TokenProviderResult> getAppTokenProviderResult(String differentScopesForAt)
    {
        long currTimestampSec = new Date().getTime() / 1000;
        TokenProviderResult token = new TokenProviderResult();
        token.setAccessToken(TestConstants.DEFAULT_ACCESS_TOKEN + differentScopesForAt); //Used to indicate that there is a new access token for a different set of scopes
        token.setTenantId("tenantId");
        token.setExpiresInSeconds(currTimestampSec + 1000000);
        token.setRefreshInSeconds(currTimestampSec + 800000);

        return CompletableFuture.completedFuture(token);
    }

    private SignedJWT createClientAssertion(String issuer) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, NoSuchProviderException, JOSEException {
        IClientCertificate certificate = CertificateHelper.getClientCertificate();

        final ClientCertificate credential = (ClientCertificate) certificate;

        final JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("subject")
                .build();

        SignedJWT jwt;
        JWSHeader.Builder builder = new JWSHeader.Builder(JWSAlgorithm.RS256);

        List<Base64> certs = new ArrayList<>();
        for (String cert : credential.getEncodedPublicKeyCertificateChain()) {
            certs.add(new Base64(cert));
        }
        builder.x509CertChain(certs);

        jwt = new SignedJWT(builder.build(), claimsSet);
        final RSASSASigner signer = new RSASSASigner(credential.privateKey());

        jwt.sign(signer);
        return jwt;
    }

}
