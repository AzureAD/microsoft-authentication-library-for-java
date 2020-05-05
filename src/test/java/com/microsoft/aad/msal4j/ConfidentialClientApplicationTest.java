// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

@PowerMockIgnore({"javax.net.ssl.*"})
@Test(groups = {"checkin"})
@PrepareForTest({ConfidentialClientApplication.class,
        ClientCertificate.class, UserDiscoveryRequest.class, JwtHelper.class})
public class ConfidentialClientApplicationTest extends PowerMockTestCase {

    private ConfidentialClientApplication app = null;

    @Test
    public void testAcquireTokenAuthCode_ClientCredential() throws Exception {
        app = PowerMock.createPartialMock(ConfidentialClientApplication.class,
                new String[]{"acquireTokenCommon"},
                ConfidentialClientApplication.builder(TestConfiguration.AAD_CLIENT_ID,
                        ClientCredentialFactory.createFromSecret(TestConfiguration.AAD_CLIENT_SECRET))
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

        IClientCredential clientCredential = ClientCredentialFactory.createFromCertificate(key, cert);

        app = PowerMock.createPartialMock(ConfidentialClientApplication.class,
                new String[]{"acquireTokenCommon"},
                ConfidentialClientApplication.builder(TestConfiguration.AAD_CLIENT_ID, clientCredential)
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

        IClientCredential clientCredential = ClientCredentialFactory.createFromCertificate(key, cert);

        app = PowerMock.createPartialMock(ConfidentialClientApplication.class,
                new String[]{"acquireTokenCommon"},
                ConfidentialClientApplication.builder(TestConfiguration.AAD_CLIENT_ID, clientCredential)
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

        ClientCertificate clientCredential = (ClientCertificate) ClientCredentialFactory.createFromCertificate(key, cert);

        PowerMock.mockStaticPartial(JwtHelper.class, new String[]{"buildJwt"});
        long jwtExperiationPeriodMilli = 2000;
        ClientAssertion shortExperationJwt = buildShortJwt(TestConfiguration.AAD_CLIENT_ID, clientCredential, TestConfiguration.AAD_TENANT_ENDPOINT, jwtExperiationPeriodMilli);
        PowerMock.expectPrivate(
                JwtHelper.class,
                "buildJwt",
                EasyMock.isA(String.class),
                EasyMock.isA(ClientCertificate.class),
                EasyMock.isA(String.class))
                .andReturn(shortExperationJwt)
                .times(2); // By this being called twice we ensure the client assertion is rebuilt once it has expired

        PowerMock.replay(JwtHelper.class);
        app = ConfidentialClientApplication.builder(TestConfiguration.AAD_CLIENT_ID, clientCredential)
                .authority(TestConfiguration.AAD_TENANT_ENDPOINT).build();
        Thread.sleep(jwtExperiationPeriodMilli + 1000); //Have to sleep to ensure that the time period has passed
        final PrivateKeyJWT clientAuthentication = (PrivateKeyJWT) app.clientAuthentication();
        assertNotNull(clientAuthentication);
        PowerMock.verifyAll();
    }

    private ClientAssertion buildShortJwt(String clientId,
                                          ClientCertificate credential,
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
            List<Base64> certs = new ArrayList<>();
            certs.add(new Base64(credential.publicCertificate()));
            JWSHeader.Builder builder = new JWSHeader.Builder(JWSAlgorithm.RS256);
            builder.x509CertChain(certs);
            builder.x509CertThumbprint(new Base64URL(credential.publicCertificateHash()));
            jwt = new SignedJWT(builder.build(), claimsSet);
            final RSASSASigner signer = new RSASSASigner(credential.key());
            jwt.sign(signer);
        } catch (final Exception e) {
            throw new MsalClientException(e);
        }
        return new ClientAssertion(jwt.serialize());
    }
}
