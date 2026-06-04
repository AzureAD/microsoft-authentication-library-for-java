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
class AuthorityTest {

    @Test
    void testDetectAuthorityType_AAD() throws Exception {
        URL url = new URL(TestConfiguration.AAD_TENANT_ENDPOINT);
        assertEquals(AuthorityType.AAD, Authority.detectAuthorityType(url));
    }

    @Test
    void testDetectAuthorityType_ADFS() throws Exception {
        URL url = new URL(TestConfiguration.ADFS_TENANT_ENDPOINT);
        assertEquals(AuthorityType.ADFS, Authority.detectAuthorityType(url));
    }

    @Test
    void testDetectAuthorityType_B2C() throws Exception {
        URL url = new URL(TestConfiguration.B2C_AUTHORITY);
        assertEquals(AuthorityType.B2C, Authority.detectAuthorityType(url));
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.AuthorityTest#ciamAuthorities")
    void testDetectAuthorityType_CIAM(URL authority) throws Exception {
        assertEquals(AuthorityType.CIAM, Authority.detectAuthorityType(authority));
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.AuthorityTest#validCiamAuthoritiesAndTransformedAuthority")
    void testCiamAuthorityTransformation(URL authority, URL transformedAuthority) throws Exception {
        assertEquals(transformedAuthority, CIAMAuthority.transformAuthority(authority));
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
        assertEquals(TestConfiguration.AAD_TENANT_ENDPOINT,
                aa.authority());
        assertEquals(TestConfiguration.AAD_HOST_NAME, aa.host());
        assertEquals(TestConfiguration.AAD_TENANT_ENDPOINT + "oauth2/v2.0/token",
                aa.tokenEndpoint());
        assertEquals(TestConfiguration.AAD_TENANT_ENDPOINT + "oauth2/v2.0/token",
                aa.selfSignedJwtAudience());
        assertEquals(TestConfiguration.AAD_TENANT_ENDPOINT + "oauth2/v2.0/token",
                aa.tokenEndpoint());
        assertEquals(AuthorityType.AAD, aa.authorityType());
        assertFalse(aa.isTenantless());
        assertEquals(TestConfiguration.AAD_TENANT_ENDPOINT + "oauth2/v2.0/devicecode",
                aa.deviceCodeEndpoint());
    }

    @Test
    void testConstructor_B2CAuthority() throws MalformedURLException {
        final B2CAuthority aa = new B2CAuthority(new URL(TestConfiguration.B2C_AUTHORITY));
        assertNotNull(aa);
        assertEquals(TestConfiguration.B2C_AUTHORITY + "/",
                aa.authority());
        assertEquals(TestConfiguration.B2C_HOST_NAME, aa.host());
        assertEquals(TestConfiguration.B2C_AUTHORITY_ENDPOINT + "/oauth2/v2.0/token?p=" + TestConfiguration.B2C_SIGN_IN_POLICY,
                aa.selfSignedJwtAudience());
        assertEquals(TestConfiguration.B2C_AUTHORITY_ENDPOINT + "/oauth2/v2.0/token?p=" + TestConfiguration.B2C_SIGN_IN_POLICY,
                aa.tokenEndpoint());
        assertEquals(AuthorityType.B2C, aa.authorityType());
        assertEquals(TestConfiguration.B2C_AUTHORITY_ENDPOINT + "/oauth2/v2.0/token?p=" + TestConfiguration.B2C_SIGN_IN_POLICY,
                aa.tokenEndpoint());
        assertFalse(aa.isTenantless());
    }

    @Test
    void testConstructor_ADFSAuthority() throws MalformedURLException {
        final ADFSAuthority a = new ADFSAuthority(new URL(TestConfiguration.ADFS_TENANT_ENDPOINT));
        assertNotNull(a);
        assertEquals(TestConfiguration.ADFS_TENANT_ENDPOINT, a.authority());
        assertEquals(TestConfiguration.ADFS_HOST_NAME, a.host());
        assertEquals(TestConfiguration.ADFS_TENANT_ENDPOINT + ADFSAuthority.TOKEN_ENDPOINT,
                a.selfSignedJwtAudience());

        assertEquals(AuthorityType.ADFS, a.authorityType());

        assertEquals(TestConfiguration.ADFS_TENANT_ENDPOINT + ADFSAuthority.TOKEN_ENDPOINT,
                a.tokenEndpoint());
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
