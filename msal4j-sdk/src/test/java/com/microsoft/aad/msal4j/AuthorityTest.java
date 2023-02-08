// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.powermock.core.classloader.annotations.PrepareForTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// @Test(groups = {"checkin"})
////@PrepareForTest({AADAuthority.class, HttpHelper.class,
//        JsonHelper.class, AadInstanceDiscoveryResponse.class})
public class AuthorityTest extends AbstractMsalTests {

    @Test
    public void testDetectAuthorityType_AAD() throws Exception {
        URL url = new URL(TestConfiguration.AAD_TENANT_ENDPOINT);
        assertEquals(Authority.detectAuthorityType(url), AuthorityType.AAD);
    }

    @Test
    public void testDetectAuthorityType_ADFS() throws Exception {
        URL url = new URL(TestConfiguration.ADFS_TENANT_ENDPOINT);
        assertEquals(Authority.detectAuthorityType(url), AuthorityType.ADFS);
    }

    @Test
    public void testDetectAuthorityType_B2C() throws Exception {
        URL url = new URL(TestConfiguration.B2C_AUTHORITY);
        assertEquals(Authority.detectAuthorityType(url), AuthorityType.B2C);
    }

    @Test
    public void testB2CAuthorityConstructor_NotEnoughSegments(){

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new B2CAuthority(new URL("https://something.com/tfp/somethingelse/"));
        });

        assertEquals("B2C 'authority' Uri should have at least 3 segments in the path (i.e. https://<host>/tfp/<tenant>/<policy>/...)", exception.getMessage());
    }

    @Test
    public void testAADAuthorityConstructor_HttpAuthority(){
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Authority.validateAuthority(new URL("http://I.com/not/h/t/t/p/s/"));
        });

        assertEquals("authority should use the 'https' scheme", exception.getMessage());


    }

    @Test
    public void testAADAuthorityConstructor_UrlHasFragment() throws MalformedURLException {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Authority.validateAuthority(new URL("https://I.com/something/#haha"));
        });

        assertEquals("authority is invalid format (contains fragment)", exception.getMessage());
    }


    @Test
    public void testAADAuthorityConstructor_AuthorityHasQuery()
            throws MalformedURLException {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Authority.validateAuthority(new URL("https://I.com/not/?query=not-allowed"));
        });

        assertEquals("authority cannot contain query parameters", exception.getMessage());

    }


    @Test
    public void testConstructor_AADAuthority() throws MalformedURLException {
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
    public void testConstructor_B2CAuthority() throws MalformedURLException {
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
    public void testConstructor_ADFSAuthority() throws MalformedURLException {
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
    public void testB2CAuthority_SameCanonicalAuthority() throws MalformedURLException {

        PublicClientApplication pca = PublicClientApplication.builder("client_id").
                b2cAuthority(TestConfiguration.B2C_AUTHORITY_CUSTOM_PORT).build();
        assertEquals(pca.authenticationAuthority.authority,
                TestConfiguration.B2C_AUTHORITY_CUSTOM_PORT_TAIL_SLASH);

        PublicClientApplication pca2 = PublicClientApplication.builder("client_id").
                b2cAuthority(TestConfiguration.B2C_AUTHORITY_CUSTOM_PORT_TAIL_SLASH).build();
        assertEquals(pca2.authenticationAuthority.authority,
                TestConfiguration.B2C_AUTHORITY_CUSTOM_PORT_TAIL_SLASH);
    }

    @Test
    public void testNoAuthorityPassedIn_DefaultsToCommonAuthority() {
        PublicClientApplication pca = PublicClientApplication.builder("client_id").build();

        assertEquals(pca.authority(), TestConfiguration.AAD_COMMON_AUTHORITY);
        assertNotNull(pca.authenticationAuthority);
    }

    @Test
    public void testDoStaticInstanceDiscovery_ValidateTrue_TrustedAuthority()
            throws Exception {
        final AADAuthority aa = new AADAuthority(new URL(TestConfiguration.AAD_TENANT_ENDPOINT));
        //PS assertTrue(aa.doStaticInstanceDiscovery(true));
    }

    @Test
    public void testDoStaticInstanceDiscovery_ValidateTrue_UntrustedAuthority()
            throws Exception {
        final AADAuthority aa = new AADAuthority(new URL(TestConfiguration.AAD_UNKNOWN_TENANT_ENDPOINT));
        //PS assertFalse(aa.doStaticInstanceDiscovery(true));
    }

    @Test
    public void testDoStaticInstanceDiscovery_ValidateFalse_TrustedAuthority()
            throws Exception {
        final AADAuthority aa = new AADAuthority(new URL(TestConfiguration.AAD_UNKNOWN_TENANT_ENDPOINT));
        //PS assertTrue(aa.doStaticInstanceDiscovery(false));
    }


    public static Object[][] createData() {
        return new Object[][]{{"https://login.microsoftonline.com/"},
                {"https://login.microsoftonline.com//tenant"},
                {"https://login.microsoftonline.com////tenant//path1"}};
    }

    @ParameterizedTest
    @MethodSource("createData")
    public void testValidateAuthorityEmptyPathSegments(String authority) throws MalformedURLException {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Authority.validateAuthority(new URL(authority));
        });

        assertEquals(IllegalArgumentExceptionMessages.AUTHORITY_URI_EMPTY_PATH_SEGMENT, exception.getMessage());

    }

    @Test
    public void testValidateAuthorityEmptyPath() throws MalformedURLException {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Authority.validateAuthority(new URL("https://login.microsoftonline.com"));
        });

        assertEquals(IllegalArgumentExceptionMessages.AUTHORITY_URI_EMPTY_PATH, exception.getMessage());

    }
}
