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

import java.net.MalformedURLException;
import java.net.URL;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = { "checkin" })
@PrepareForTest({ AADAuthority.class, HttpHelper.class,
        JsonHelper.class, InstanceDiscoveryResponse.class })
public class AuthorityTest extends AbstractMsalTests {

    @Test
    public void testDetectAuthorityType_AAD() throws Exception {
        URL url = new URL(TestConfiguration.AAD_TENANT_ENDPOINT);
        Assert.assertEquals(Authority.detectAuthorityType(url), AuthorityType.AAD);
    }

    //TODO uncomment when ADFS support is added
//    @Test
//    public void testDetectAuthorityType_ADFS() throws Exception {
//        URL url = new URL(TestConfiguration.ADFS_TENANT_ENDPOINT);
//        Assert.assertEquals(Authority.detectAuthorityType(url), AuthorityType.ADFS);
//    }

    @Test
    public void testDetectAuthorityType_B2C() throws Exception {
        URL url = new URL(TestConfiguration.B2C_AUTHORITY);
        Assert.assertEquals(Authority.detectAuthorityType(url), AuthorityType.B2C);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp =
                    "'authority' Uri should have at least one segment in the path \\(i.e. https://<host>/<path>/...\\)")
    public void testAADAuthorityConstructor_NoPathAuthority() throws MalformedURLException {
        new AADAuthority(new URL("https://something.com/"));

    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp =
                    "B2C 'authority' Uri should have at least 3 segments in the path \\(i.e. https://<host>/tfp/<tenant>/<policy>/...\\)")
    public void testB2CAuthorityConstructor_NotEnoughSegments() throws MalformedURLException {
        new B2CAuthority(new URL("https://something.com/tfp/somethingelse/"));
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "canonicalAuthorityUrl")
    public void testAADAuthorityConstructor_NullAuthority() {
        new AADAuthority(null);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "canonicalAuthorityUrl")
    public void testB2CAuthorityConstructor_NullAuthority() {
        new B2CAuthority(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = AuthenticationErrorMessage.AUTHORITY_URI_INSECURE)
    public void testAADAuthorityConstructor_HttpAuthority() throws MalformedURLException {
        new AADAuthority(new URL("http://I.com/not/h/t/t/p/s/"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "authority is invalid format \\(contains fragment\\)")
    public void testAADAuthorityConstructor_UrlHasFragment() throws MalformedURLException {
        new AADAuthority(new URL("https://I.com/something/#haha"));
    }


    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "authority cannot contain query parameters")
    public void testAADAuthorityConstructor_AuthorityHasQuery()
            throws MalformedURLException {
        new AADAuthority(new URL("https://I.com/not/?query=not-allowed"));
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
        final B2CAuthority aa = new B2CAuthority (new URL(TestConfiguration.B2C_AUTHORITY));
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

    //TODO uncomment when ADFS support is added
//    @Test
//    public void testConstructor_ADFSAuthority() throws MalformedURLException {
//        final ADFSAuthority aa = new ADFSAuthority(new URL(TestConfiguration.ADFS_TENANT_ENDPOINT));
//        Assert.assertNotNull(aa);
//        Assert.assertEquals(aa.getAuthority(),
//                TestConfiguration.ADFS_TENANT_ENDPOINT);
//        Assert.assertEquals(aa.getHost(), TestConfiguration.ADFS_HOST_NAME);
//        Assert.assertEquals(aa.getSelfSignedJwtAudience(),
//                TestConfiguration.ADFS_TENANT_ENDPOINT + "oauth2/v2.0/token");
//        Assert.assertEquals(aa.getTokenEndpoint(),
//                TestConfiguration.ADFS_TENANT_ENDPOINT + "oauth2/v2.0/token");
//        Assert.assertEquals(aa.getAuthorityType(), AuthorityType.ADFS);
//        Assert.assertEquals(aa.getTokenEndpoint(),
//                TestConfiguration.ADFS_TENANT_ENDPOINT + "oauth2/v2.0/token");
//        Assert.assertFalse(aa.isTenantless());
//    }

    @Test
    public void testB2CAuthority_SameCanonicalAuthority() throws MalformedURLException{

        PublicClientApplication pca =  PublicClientApplication.builder("client_id").
                b2cAuthority(TestConfiguration.B2C_AUTHORITY_CUSTOM_PORT).build();
        Assert.assertEquals(pca.authenticationAuthority.authority,
                TestConfiguration.B2C_AUTHORITY_CUSTOM_PORT_TAIL_SLASH);

        PublicClientApplication pca2 =  PublicClientApplication.builder("client_id").
                b2cAuthority(TestConfiguration.B2C_AUTHORITY_CUSTOM_PORT_TAIL_SLASH).build();
        Assert.assertEquals(pca2.authenticationAuthority.authority,
                TestConfiguration.B2C_AUTHORITY_CUSTOM_PORT_TAIL_SLASH);
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
}
