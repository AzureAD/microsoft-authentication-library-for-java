package com.microsoft.aad.msal4j;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

public class ConfidentialClientApplicationTest {

    final String clientId = TestConfiguration.AAD_CLIENT_ID;
    String authority = "https://" + TestConfiguration.AAD_HOST_NAME + "/" + TestConfiguration.AAD_TENANT_NAME + "/";

    @Test
    public void testClientAssertion_noException() throws Exception{

        IClientCertificate certificate = TestClass.getClientCertificate();

        final ClientCertificate credential = (ClientCertificate) certificate;

        final JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer("issuer")
                .subject(clientId)
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

        ClientAssertion clientAssertion = new ClientAssertion(jwt.serialize());

        IClientCredential iClientCredential = ClientCredentialFactory.createFromClientAssertion(
                clientAssertion.assertion());

        ConfidentialClientApplication app = ConfidentialClientApplication.builder(clientId, iClientCredential).authority(authority).build();

        Assert.assertEquals(app.clientId(),clientId);
        Assert.assertEquals(app.authority(), authority);
        Assert.assertTrue(app.sendX5c());

    }

    @Test(expectedExceptions = MsalClientException.class)
    public void testClientAssertion_throwsException() throws Exception{

        IClientCertificate certificate = TestClass.getClientCertificate();
        final ClientCertificate credential = (ClientCertificate) certificate;

        final JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(null)
                .subject(clientId)
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

        ClientAssertion clientAssertion = new ClientAssertion(jwt.serialize());

        IClientCredential iClientCredential = ClientCredentialFactory.createFromClientAssertion(
                clientAssertion.assertion());

        ConfidentialClientApplication.builder(clientId, iClientCredential).authority(authority).build();

    }
}
