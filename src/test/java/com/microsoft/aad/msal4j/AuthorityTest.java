// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = {"checkin"})
@PrepareForTest({AADAuthority.class, HttpHelper.class,
        JsonHelper.class, AadInstanceDiscoveryResponse.class})
public class AuthorityTest extends AbstractMsalTests {

    @Test
    public void testDetectAuthorityType_AAD() throws Exception {
        URL url = new URL(TestConfiguration.AAD_TENANT_ENDPOINT);
        Assert.assertEquals(Authority.detectAuthorityType(url), AuthorityType.AAD);
    }

    @Test
    public void testDetectAuthorityType_ADFS() throws Exception {
        URL url = new URL(TestConfiguration.ADFS_TENANT_ENDPOINT);
        Assert.assertEquals(Authority.detectAuthorityType(url), AuthorityType.ADFS);
    }

    @Test
    public void testDetectAuthorityType_B2C() throws Exception {
        URL url = new URL(TestConfiguration.B2C_AUTHORITY);
        Assert.assertEquals(Authority.detectAuthorityType(url), AuthorityType.B2C);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp =
                    "B2C 'authority' Uri should have at least 3 segments in the path \\(i.e. https://<host>/tfp/<tenant>/<policy>/...\\)")
    public void testB2CAuthorityConstructor_NotEnoughSegments() throws MalformedURLException {
        new B2CAuthority(new URL("https://something.com/tfp/somethingelse/"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "authority should use the 'https' scheme")
    public void testAADAuthorityConstructor_HttpAuthority() throws MalformedURLException {
        Authority.validateAuthority(new URL("http://I.com/not/h/t/t/p/s/"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "authority is invalid format \\(contains fragment\\)")
    public void testAADAuthorityConstructor_UrlHasFragment() throws MalformedURLException {
        Authority.validateAuthority(new URL("https://I.com/something/#haha"));
    }


    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "authority cannot contain query parameters")
    public void testAADAuthorityConstructor_AuthorityHasQuery()
            throws MalformedURLException {
        Authority.validateAuthority(new URL("https://I.com/not/?query=not-allowed"));
    }


    @Test
    public void testConstructor_AADAuthority() throws MalformedURLException {
        final AADAuthority aa = new AADAuthority(new URL(TestConfiguration.AAD_TENANT_ENDPOINT));
        Assert.assertNotNull(aa);
        Assert.assertEquals(aa.authority(),
                TestConfiguration.AAD_TENANT_ENDPOINT);
        Assert.assertEquals(aa.host(), TestConfiguration.AAD_HOST_NAME);
        Assert.assertEquals(aa.tokenEndpoint(),
                TestConfiguration.AAD_TENANT_ENDPOINT + "oauth2/v2.0/token");
        Assert.assertEquals(aa.selfSignedJwtAudience(),
                TestConfiguration.AAD_TENANT_ENDPOINT + "oauth2/v2.0/token");
        Assert.assertEquals(aa.tokenEndpoint(),
                TestConfiguration.AAD_TENANT_ENDPOINT + "oauth2/v2.0/token");
        Assert.assertEquals(aa.authorityType(), AuthorityType.AAD);
        Assert.assertFalse(aa.isTenantless());
        Assert.assertEquals(aa.deviceCodeEndpoint(),
                TestConfiguration.AAD_TENANT_ENDPOINT + "oauth2/v2.0/devicecode");
    }

    @Test
    public void testConstructor_B2CAuthority() throws MalformedURLException {
        final B2CAuthority aa = new B2CAuthority(new URL(TestConfiguration.B2C_AUTHORITY));
        Assert.assertNotNull(aa);
        Assert.assertEquals(aa.authority(),
                TestConfiguration.B2C_AUTHORITY + "/");
        Assert.assertEquals(aa.host(), TestConfiguration.B2C_HOST_NAME);
        Assert.assertEquals(aa.selfSignedJwtAudience(),
                TestConfiguration.B2C_AUTHORITY_ENDPOINT + "/oauth2/v2.0/token?p=" + TestConfiguration.B2C_SIGN_IN_POLICY);
        Assert.assertEquals(aa.tokenEndpoint(),
                TestConfiguration.B2C_AUTHORITY_ENDPOINT + "/oauth2/v2.0/token?p=" + TestConfiguration.B2C_SIGN_IN_POLICY);
        Assert.assertEquals(aa.authorityType(), AuthorityType.B2C);
        Assert.assertEquals(aa.tokenEndpoint(),
                TestConfiguration.B2C_AUTHORITY_ENDPOINT + "/oauth2/v2.0/token?p=" + TestConfiguration.B2C_SIGN_IN_POLICY);
        Assert.assertFalse(aa.isTenantless());
    }

    @Test
    public void testConstructor_ADFSAuthority() throws MalformedURLException {
        final ADFSAuthority a = new ADFSAuthority(new URL(TestConfiguration.ADFS_TENANT_ENDPOINT));
        Assert.assertNotNull(a);
        Assert.assertEquals(a.authority(), TestConfiguration.ADFS_TENANT_ENDPOINT);
        Assert.assertEquals(a.host(), TestConfiguration.ADFS_HOST_NAME);
        Assert.assertEquals(a.selfSignedJwtAudience(),
                TestConfiguration.ADFS_TENANT_ENDPOINT + ADFSAuthority.TOKEN_ENDPOINT);

        Assert.assertEquals(a.authorityType(), AuthorityType.ADFS);

        Assert.assertEquals(a.tokenEndpoint(),
                TestConfiguration.ADFS_TENANT_ENDPOINT + ADFSAuthority.TOKEN_ENDPOINT);
        Assert.assertFalse(a.isTenantless());
    }

    @Test
    public void testB2CAuthority_SameCanonicalAuthority() throws MalformedURLException {

        PublicClientApplication pca = PublicClientApplication.builder("client_id").
                b2cAuthority(TestConfiguration.B2C_AUTHORITY_CUSTOM_PORT).build();
        Assert.assertEquals(pca.authenticationAuthority.authority,
                TestConfiguration.B2C_AUTHORITY_CUSTOM_PORT_TAIL_SLASH);

        PublicClientApplication pca2 = PublicClientApplication.builder("client_id").
                b2cAuthority(TestConfiguration.B2C_AUTHORITY_CUSTOM_PORT_TAIL_SLASH).build();
        Assert.assertEquals(pca2.authenticationAuthority.authority,
                TestConfiguration.B2C_AUTHORITY_CUSTOM_PORT_TAIL_SLASH);
    }

    @Test
    public void testNoAuthorityPassedIn_DefaultsToCommonAuthority() {
        PublicClientApplication pca = PublicClientApplication.builder("client_id").build();

        Assert.assertEquals(pca.authority(), TestConfiguration.AAD_COMMON_AUTHORITY);
        Assert.assertNotNull(pca.authenticationAuthority);
    }

    @Test
    public void testDoStaticInstanceDiscovery_ValidateTrue_TrustedAuthority()
            throws MalformedURLException, Exception {
        final AADAuthority aa = new AADAuthority(new URL(TestConfiguration.AAD_TENANT_ENDPOINT));
        //PS Assert.assertTrue(aa.doStaticInstanceDiscovery(true));
    }

    @Test
    public void testDoStaticInstanceDiscovery_ValidateTrue_UntrustedAuthority()
            throws MalformedURLException, Exception {
        final AADAuthority aa = new AADAuthority(new URL(TestConfiguration.AAD_UNKNOWN_TENANT_ENDPOINT));
        //PS Assert.assertFalse(aa.doStaticInstanceDiscovery(true));
    }

    @Test
    public void testDoStaticInstanceDiscovery_ValidateFalse_TrustedAuthority()
            throws MalformedURLException, Exception {
        final AADAuthority aa = new AADAuthority(new URL(TestConfiguration.AAD_UNKNOWN_TENANT_ENDPOINT));
        //PS Assert.assertTrue(aa.doStaticInstanceDiscovery(false));
    }

    @Test
    public void testValidateAuthorityPath() {
        String[] authoritiesWithInvalidPath = {
                "https://login.microsoftonline.com/",
                "https://login.microsoftonline.com//",
                "https://login.microsoftonline.com//tenant",
                "https://login.microsoftonline.com/tenant//",
                "https://login.microsoftonline.com////tenant//path1"};

        for (String testAuthority : authoritiesWithInvalidPath) {
            try {
                URL authorityUrl = new URL(testAuthority);
                Authority.validateAuthority(authorityUrl);
            } catch (Exception ex) {
                Assert.assertTrue(ex instanceof IllegalArgumentException);
                Assert.assertEquals
                        (ex.getMessage(), "authority Uri should not have empty path segments");
            }
        }

        authoritiesWithInvalidPath = new String[]{
                "https://login.microsoftonline.com"};

        for (String testAuthority : authoritiesWithInvalidPath) {
            try {
                URL authorityUrl = new URL(testAuthority);
                Authority.validateAuthority(authorityUrl);
            } catch (Exception ex) {
                Assert.assertTrue(ex instanceof IllegalArgumentException);
                Assert.assertEquals(ex.getMessage(),
                        "authority Uri should have at least one segment in the path (i.e. https://<host>/<path>/...)");
            }
        }
    }
}
