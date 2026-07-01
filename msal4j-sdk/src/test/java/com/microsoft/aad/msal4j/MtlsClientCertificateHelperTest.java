// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.net.ssl.SSLSocketFactory;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MtlsClientCertificateHelperTest {

    private static final String PKCS12_RESOURCE = "/mtls_test_cert.p12";
    private static final String PKCS12_PASSWORD = "password";
    private static final String AUTHORITY = "https://login.microsoftonline.com/contoso.onmicrosoft.com/";

    private IClientCertificate certificate;

    @BeforeAll
    void setUp() throws Exception {
        try (InputStream pkcs12 = getClass().getResourceAsStream(PKCS12_RESOURCE)) {
            assertNotNull(pkcs12, "Test PKCS12 resource " + PKCS12_RESOURCE + " should be present");
            certificate = ClientCredentialFactory.createFromCertificate(pkcs12, PKCS12_PASSWORD);
        }
    }

    @Test
    void createMtlsSocketFactory_buildsFactoryFromCertificate() {
        SSLSocketFactory factory = MtlsClientCertificateHelper.createMtlsSocketFactory(certificate);
        assertNotNull(factory);
    }

    @Test
    void createMtlsSocketFactory_nullCertificate_throwsMtlsPopError() {
        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                MtlsClientCertificateHelper.createMtlsSocketFactory(null));
        assertEquals(AuthenticationErrorCode.MTLS_POP_ERROR, ex.errorCode());
    }

    @Test
    void computeThumbprintSha256_matchesBase64UrlSha256OfLeafDer() throws Exception {
        String encodedLeaf = certificate.getEncodedPublicKeyCertificateChain().get(0);
        byte[] der = Base64.getDecoder().decode(encodedLeaf);
        X509Certificate leaf = (X509Certificate) java.security.cert.CertificateFactory
                .getInstance("X.509")
                .generateCertificate(new java.io.ByteArrayInputStream(der));

        String expected = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256").digest(leaf.getEncoded()));

        assertEquals(expected, MtlsClientCertificateHelper.computeThumbprintSha256(leaf));
        assertEquals(expected, MtlsClientCertificateHelper.computeCertificateKeyId(certificate));
    }

    @Test
    void buildBindingCertificate_exposesPublicMaterialOnly() {
        BindingCertificate binding = MtlsClientCertificateHelper.buildBindingCertificate(certificate);

        assertNotNull(binding);
        List<X509Certificate> chain = binding.certificateChain();
        assertFalse(chain.isEmpty());
        assertEquals(MtlsClientCertificateHelper.computeCertificateKeyId(certificate), binding.thumbprintSha256());

        // BindingCertificate must never surface a private key. It only exposes the chain and thumbprint.
        for (java.lang.reflect.Method m : BindingCertificate.class.getMethods()) {
            assertFalse(m.getName().toLowerCase().contains("private"),
                    "BindingCertificate must not expose private key material via " + m.getName());
        }
    }

    @Test
    void resolveBindingCertificate_certCredential_returnsThatCertificate() throws Exception {
        ConfidentialClientApplication app = ConfidentialClientApplication.builder("clientId", certificate)
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .build();

        assertEquals(certificate, MtlsClientCertificateHelper.resolveBindingCertificate(app, null));
    }

    @Test
    void resolveBindingCertificate_assertionWithBindingCert_returnsBindingCert() throws Exception {
        ConfidentialClientApplication app = ConfidentialClientApplication.builder("clientId",
                        ClientCredentialFactory.createFromClientAssertion(TestHelper.signedAssertion))
                .authority(AUTHORITY)
                .mtlsBindingCertificate(certificate)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .build();

        assertEquals(certificate, MtlsClientCertificateHelper.resolveBindingCertificate(app, null));
    }

    @Test
    void resolveBindingCertificate_assertionWithoutBindingCert_throwsMtlsPopError() throws Exception {
        ConfidentialClientApplication app = ConfidentialClientApplication.builder("clientId",
                        ClientCredentialFactory.createFromClientAssertion(TestHelper.signedAssertion))
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .build();

        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                MtlsClientCertificateHelper.resolveBindingCertificate(app, null));
        assertEquals(AuthenticationErrorCode.MTLS_POP_ERROR, ex.errorCode());
    }
}
