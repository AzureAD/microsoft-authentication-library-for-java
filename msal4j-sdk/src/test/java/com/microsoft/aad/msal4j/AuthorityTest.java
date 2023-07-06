// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthorityTest extends AbstractMsalTests {

    @Test
    void testDetectAuthorityType_AAD() throws Exception {
        URL url = new URL(TestConfiguration.AAD_TENANT_ENDPOINT);
        assertEquals(Authority.detectAuthorityType(url), AuthorityType.AAD);
    }

    @Test
    void testDetectAuthorityType_ADFS() throws Exception {
        URL url = new URL(TestConfiguration.ADFS_TENANT_ENDPOINT);
        assertEquals(Authority.detectAuthorityType(url), AuthorityType.ADFS);
    }

    @Test
    void testDetectAuthorityType_B2C() throws Exception {
        URL url = new URL(TestConfiguration.B2C_AUTHORITY);
        assertEquals(Authority.detectAuthorityType(url), AuthorityType.B2C);
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.AuthorityTest#ciamAuthorities")
    void testDetectAuthorityType_CIAM(URL authority) throws Exception {
        assertEquals(Authority.detectAuthorityType(authority), AuthorityType.CIAM);
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.AuthorityTest#validCiamAuthoritiesAndTransformedAuthority")
    void testCiamAuthorityTransformation(URL authority, URL transformedAuthority) throws Exception {
        assertEquals(CIAMAuthority.transformAuthority(authority), transformedAuthority);
    }

    @Test
    void testB2CAuthorityConstructor_NotEnoughSegments() {

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new B2CAuthority(new URL("https://something.com/somethingelse/")));

        assertTrue(ex.getMessage().contains("Valid B2C 'authority' URLs should follow either of these formats"));
    }

    @Test
    void testAADAuthorityConstructor_HttpAuthority() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                Authority.validateAuthority(new URL("http://I.com/not/h/t/t/p/s/")));

        assertTrue(ex.getMessage().contains("authority should use the 'https' scheme"));
    }

    @Test
    void testAADAuthorityConstructor_UrlHasFragment() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                Authority.validateAuthority(new URL("https://I.com/something/#haha")));

        assertTrue(ex.getMessage().contains("authority is invalid format (contains fragment)"));
    }

    @Test
    void testAADAuthorityConstructor_AuthorityHasQuery() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                Authority.validateAuthority(new URL("https://I.com/not/?query=not-allowed")));

        assertTrue(ex.getMessage().contains("authority cannot contain query parameters"));
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.AuthorityTest#authoritiesWithEmptyPath")
    void testValidateAuthorityEmptyPathSegments(String authority) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                Authority.validateAuthority(new URL(authority)));

        assertEquals(IllegalArgumentExceptionMessages.AUTHORITY_URI_EMPTY_PATH_SEGMENT, ex.getMessage());
    }

    @Test
    void testValidateAuthorityEmptyPath() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                Authority.validateAuthority(new URL("https://login.microsoftonline.com")));

        assertEquals(IllegalArgumentExceptionMessages.AUTHORITY_URI_EMPTY_PATH, ex.getMessage());
    }

    @Test
    void testConstructor_AADAuthority() throws MalformedURLException {
        final AADAuthority aa = new AADAuthority(new URL(TestConfiguration.AAD_TENANT_ENDPOINT));
        assertNotNull(aa);
        assertEquals(aa.authority(),
                TestConfiguration.AAD_TENANT_ENDPOINT);
        assertEquals(aa.host(), TestConfiguration.AAD_HOST_NAME);
        assertEquals(aa.tokenEndpoint(),
                TestConfiguration.AAD_TENANT_ENDPOINT + "oauth2/v2.0/token");
        assertEquals(aa.selfSignedJwtAudience(),
                TestConfiguration.AAD_TENANT_ENDPOINT + "oauth2/v2.0/token");
        assertEquals(aa.tokenEndpoint(),
                TestConfiguration.AAD_TENANT_ENDPOINT + "oauth2/v2.0/token");
        assertEquals(aa.authorityType(), AuthorityType.AAD);
        assertFalse(aa.isTenantless());
        assertEquals(aa.deviceCodeEndpoint(),
                TestConfiguration.AAD_TENANT_ENDPOINT + "oauth2/v2.0/devicecode");
    }

    @Test
    void testConstructor_B2CAuthority() throws MalformedURLException {
        final B2CAuthority aa = new B2CAuthority(new URL(TestConfiguration.B2C_AUTHORITY));
        assertNotNull(aa);
        assertEquals(aa.authority(),
                TestConfiguration.B2C_AUTHORITY + "/");
        assertEquals(aa.host(), TestConfiguration.B2C_HOST_NAME);
        assertEquals(aa.selfSignedJwtAudience(),
                TestConfiguration.B2C_AUTHORITY_ENDPOINT + "/oauth2/v2.0/token?p=" + TestConfiguration.B2C_SIGN_IN_POLICY);
        assertEquals(aa.tokenEndpoint(),
                TestConfiguration.B2C_AUTHORITY_ENDPOINT + "/oauth2/v2.0/token?p=" + TestConfiguration.B2C_SIGN_IN_POLICY);
        assertEquals(aa.authorityType(), AuthorityType.B2C);
        assertEquals(aa.tokenEndpoint(),
                TestConfiguration.B2C_AUTHORITY_ENDPOINT + "/oauth2/v2.0/token?p=" + TestConfiguration.B2C_SIGN_IN_POLICY);
        assertFalse(aa.isTenantless());
    }

    @Test
    void testConstructor_ADFSAuthority() throws MalformedURLException {
        final ADFSAuthority a = new ADFSAuthority(new URL(TestConfiguration.ADFS_TENANT_ENDPOINT));
        assertNotNull(a);
        assertEquals(a.authority(), TestConfiguration.ADFS_TENANT_ENDPOINT);
        assertEquals(a.host(), TestConfiguration.ADFS_HOST_NAME);
        assertEquals(a.selfSignedJwtAudience(),
                TestConfiguration.ADFS_TENANT_ENDPOINT + ADFSAuthority.TOKEN_ENDPOINT);

        assertEquals(a.authorityType(), AuthorityType.ADFS);

        assertEquals(a.tokenEndpoint(),
                TestConfiguration.ADFS_TENANT_ENDPOINT + ADFSAuthority.TOKEN_ENDPOINT);
        assertFalse(a.isTenantless());
    }

    @Test
    void testB2CAuthority_SameCanonicalAuthority() throws MalformedURLException {

        PublicClientApplication pca = PublicClientApplication.builder("client_id").
                b2cAuthority(TestConfiguration.B2C_AUTHORITY_CUSTOM_PORT).build();
        assertEquals(TestConfiguration.B2C_AUTHORITY_CUSTOM_PORT_TAIL_SLASH,
                pca.authenticationAuthority.authority);

        PublicClientApplication pca2 = PublicClientApplication.builder("client_id").
                b2cAuthority(TestConfiguration.B2C_AUTHORITY_CUSTOM_PORT_TAIL_SLASH).build();
        assertEquals(TestConfiguration.B2C_AUTHORITY_CUSTOM_PORT_TAIL_SLASH,
                pca2.authenticationAuthority.authority);
    }

    @Test
    void testNoAuthorityPassedIn_DefaultsToCommonAuthority() {
        PublicClientApplication pca = PublicClientApplication.builder("client_id").build();

        assertEquals(TestConfiguration.AAD_COMMON_AUTHORITY, pca.authority());
        assertNotNull(pca.authenticationAuthority);
    }

    //TODO: test if this test tests anything
    @Test
    void testDoStaticInstanceDiscovery_ValidateTrue_TrustedAuthority()
            throws Exception {
        new AADAuthority(new URL(TestConfiguration.AAD_TENANT_ENDPOINT));
    }

    //TODO: test if this test tests anything
    @Test
    void testDoStaticInstanceDiscovery_ValidateTrue_UntrustedAuthority()
            throws Exception {
        new AADAuthority(new URL(TestConfiguration.AAD_UNKNOWN_TENANT_ENDPOINT));
    }

    //TODO: test if this test tests anything
    @Test
    void testDoStaticInstanceDiscovery_ValidateFalse_TrustedAuthority()
            throws Exception {
        new AADAuthority(new URL(TestConfiguration.AAD_UNKNOWN_TENANT_ENDPOINT));
    }


    static Object[][] validCiamAuthoritiesAndTransformedAuthority() throws MalformedURLException {
        return new Object[][]{{new URL("https://msidlabciam1.ciamlogin.com/"), new URL("https://msidlabciam1.ciamlogin.com/msidlabciam1.onmicrosoft.com/")},
                {new URL("https://msidlabciam1.ciamlogin.com/d57fb3d4-4b5a-4144-9328-9c1f7d58179d"), new URL("https://msidlabciam1.ciamlogin.com/d57fb3d4-4b5a-4144-9328-9c1f7d58179d")},
                {new URL("https://msidlabciam1.ciamlogin.com/msidlabciam1.onmicrosoft.com"), new URL("https://msidlabciam1.ciamlogin.com/msidlabciam1.onmicrosoft.com")},
                {new URL("https://msidlabciam1.ciamlogin.com/aDomain"), new URL("https://msidlabciam1.ciamlogin.com/aDomain")}};
    }

    static Object[][] ciamAuthorities() throws MalformedURLException {
        return new Object[][]{{new URL("https://msidlabciam1.ciamlogin.com/")},
                {new URL("https://msidlabciam1.ciamlogin.com/d57fb3d4-4b5a-4144-9328-9c1f7d58179d/")},
                {new URL("https://msidlabciam1.ciamlogin.com/msidlabciam1.onmicrosoft.com/")},
                {new URL("https://msidlabciam1.ciamlogin.com/aDomain/")}};
    }

    static Object[][] authoritiesWithEmptyPath() {
        return new Object[][]{{"https://login.microsoftonline.com/"},
                {"https://login.microsoftonline.com//tenant"},
                {"https://login.microsoftonline.com////tenant//path1"}};
    }
}
