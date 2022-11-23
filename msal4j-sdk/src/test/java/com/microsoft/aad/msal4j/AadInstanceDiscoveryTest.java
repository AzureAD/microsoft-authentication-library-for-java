// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URL;
import java.util.Collections;

@PrepareForTest(AadInstanceDiscoveryProvider.class)
public class AadInstanceDiscoveryTest extends PowerMockTestCase {

    @BeforeMethod
    public void setup() {
        AadInstanceDiscoveryProvider.cache.clear();
    }

    @Test
    public void aadInstanceDiscoveryTest_NotSetByDeveloper() throws Exception {
        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .correlationId("correlation_id")
                .authority("https://login.microsoftonline.com/my_tenant")
                .build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(
                "code", new URI("http://my.redirect.com")).build();

        MsalRequest msalRequest = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        URL authority = new URL(app.authority());

        String instanceDiscoveryData = TestHelper.readResource(
                this.getClass(),
                "/instance_discovery_data/aad_instance_discovery_response_valid.json");

        AadInstanceDiscoveryResponse expectedResponse = JsonHelper.convertJsonToObject(
                instanceDiscoveryData,
                AadInstanceDiscoveryResponse.class);

        PowerMock.mockStaticPartial(AadInstanceDiscoveryProvider.class, "sendInstanceDiscoveryRequest");

        PowerMock.expectPrivate(
                AadInstanceDiscoveryProvider.class,
                "sendInstanceDiscoveryRequest",
                authority,
                msalRequest,
                app.getServiceBundle()).andReturn(expectedResponse);

        PowerMock.replay(AadInstanceDiscoveryProvider.class);

        InstanceDiscoveryMetadataEntry entry = AadInstanceDiscoveryProvider.getMetadataEntry(
                new URL(app.authority()),
                false,
                msalRequest,
                app.getServiceBundle());

        PowerMock.verify(AadInstanceDiscoveryProvider.class);

        Assert.assertEquals(entry.preferredNetwork(), "login.microsoftonline.com");
        Assert.assertEquals(entry.preferredCache(), "login.windows.net");
        Assert.assertEquals(entry.aliases().size(), 4);
        Assert.assertTrue(entry.aliases().contains("login.microsoftonline.com"));
        Assert.assertTrue(entry.aliases().contains("login.windows.net"));
        Assert.assertTrue(entry.aliases().contains("login.microsoft.com"));
        Assert.assertTrue(entry.aliases().contains("sts.windows.net"));
    }

    @Test
    public void aadInstanceDiscoveryTest_responseSetByDeveloper_validResponse() throws Exception {

        String instanceDiscoveryResponse = TestHelper.readResource(
                this.getClass(),
                "/instance_discovery_data/aad_instance_discovery_response_valid.json");

        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .aadInstanceDiscoveryResponse(instanceDiscoveryResponse)
                .build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(
                "code", new URI("http://my.redirect.com")).build();

        MsalRequest msalRequest = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        URL authority = new URL(app.authority());

        PowerMock.mockStaticPartial(
                AadInstanceDiscoveryProvider.class,
                "sendInstanceDiscoveryRequest");

        // throw exception if we try to get metadata from network.
        PowerMock.expectPrivate(
                AadInstanceDiscoveryProvider.class,
                "sendInstanceDiscoveryRequest",
                authority,
                msalRequest,
                app.getServiceBundle()).andThrow(new AssertionError()).anyTimes();

        PowerMock.replay(AadInstanceDiscoveryProvider.class);

        InstanceDiscoveryMetadataEntry entry = AadInstanceDiscoveryProvider.getMetadataEntry(
                authority,
                false,
                msalRequest,
                app.getServiceBundle());

        Assert.assertEquals(entry.preferredNetwork(), "login.microsoftonline.com");
        Assert.assertEquals(entry.preferredCache(), "login.windows.net");
        Assert.assertEquals(entry.aliases().size(), 4);
        Assert.assertTrue(entry.aliases().contains("login.microsoftonline.com"));
        Assert.assertTrue(entry.aliases().contains("login.windows.net"));
        Assert.assertTrue(entry.aliases().contains("login.microsoft.com"));
        Assert.assertTrue(entry.aliases().contains("sts.windows.net"));
    }

    @Test(expectedExceptions = MsalClientException.class)
    public void aadInstanceDiscoveryTest_responseSetByDeveloper_invalidJson() throws Exception {

        String instanceDiscoveryResponse = TestHelper.readResource(
                this.getClass(),
                "/instance_discovery_data/aad_instance_discovery_response_invalid_json.json");

        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .aadInstanceDiscoveryResponse(instanceDiscoveryResponse)
                .build();
    }

    @Test()
    public void aadInstanceDiscoveryTest_AutoDetectRegion_NoRegionDetected() throws Exception {

        String instanceDiscoveryResponse = TestHelper.readResource(
                this.getClass(),
                "/instance_discovery_data/aad_instance_discovery_response_valid.json");

        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .aadInstanceDiscoveryResponse(instanceDiscoveryResponse)
                .autoDetectRegion(true)
                .build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(
                "code", new URI("http://my.redirect.com")).build();

        MsalRequest msalRequest = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        URL authority = new URL(app.authority());

        PowerMock.mockStaticPartial(
                AadInstanceDiscoveryProvider.class,
                "discoverRegion");

        PowerMock.expectPrivate(
                AadInstanceDiscoveryProvider.class,
                "discoverRegion",
                msalRequest,
                app.getServiceBundle()).andThrow(new AssertionError()).anyTimes();

        PowerMock.replay(AadInstanceDiscoveryProvider.class);

        InstanceDiscoveryMetadataEntry entry = AadInstanceDiscoveryProvider.getMetadataEntry(
                authority,
                false,
                msalRequest,
                app.getServiceBundle());

        //Region detection will have been performed in the expected discoverRegion method, but these tests (likely) aren't
        // being run in an Azure VM and instance discovery will fall back to the global endpoint (login.microsoftonline.com)
        Assert.assertEquals(entry.preferredNetwork(), "login.microsoftonline.com");
        Assert.assertEquals(entry.preferredCache(), "login.windows.net");
        Assert.assertEquals(entry.aliases().size(), 4);
        Assert.assertTrue(entry.aliases().contains("login.microsoftonline.com"));
        Assert.assertTrue(entry.aliases().contains("login.windows.net"));
        Assert.assertTrue(entry.aliases().contains("login.microsoft.com"));
        Assert.assertTrue(entry.aliases().contains("sts.windows.net"));
    }

    @DataProvider(name = "aadClouds")
    private static Object[][] getAadClouds(){
        return new Object[][] {{"https://login.microsoftonline.com/common"} , // #Known to Microsoft
                {"https://private.cloud/foo"}//Private Cloud
                };
    }

    @DataProvider(name = "b2cAdfsClouds")
    private static Object[][] getNonAadClouds(){
        return new Object[][] {{"https://contoso.com/adfs"}//ADFS
//                {"https://login.b2clogin.com/contoso/b2c_policy"},//B2C
                };
    }

    /**
     * when instance_discovery flag is set to true (by default), an instance_discovery is performed for authorityType = AAD and
     *     hence, an exception is thrown while making a call to getMetaDataEntry() if instanceDiscoveryResponse is not mocked.
    */
    @Test(  dataProvider = "aadClouds",
            expectedExceptions = StringIndexOutOfBoundsException.class)
    public void aad_instance_discovery_true(String authority) throws Exception {

        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .authority(authority)
                .build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(
                "code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("scope")).build();

        MsalRequest msalRequest = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        URL authorityURL = new URL(authority);

       AadInstanceDiscoveryProvider.getMetadataEntry(
                authorityURL,
                false,
                msalRequest,
                app.getServiceBundle());

    }

    /**
     * when instance_discovery flag is set to true (by default), an instance_discovery is NOT performed for b2c.
     */
    @Test(  dataProvider = "b2cAdfsClouds")
    public void b2c_adfs_instance_discovery_true(String authority) throws Exception {

        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .authority(authority)
                .build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(
                        "code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("scope")).build();

        MsalRequest msalRequest = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        URL authorityURL = new URL(authority);

        AadInstanceDiscoveryProvider.getMetadataEntry(
                authorityURL,
                false,
                msalRequest,
                app.getServiceBundle());
    }

    @Test (dataProvider = "aadClouds")
    /**
     * when instance_discovery flag is set to false, instance_discovery is not performed and hence,
     * no exception is thrown while making a call to getMetaDataEntry() even when instanceDiscoveryResponse is not mocked.
     */
    public void aad_instance_discovery_false(String authority) throws Exception{

        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .authority(authority)
                .instanceDiscovery(false)
                .build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(
                        "code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("scope")).build();

        MsalRequest msalRequest = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        URL authorityURL = new URL(authority);

        AadInstanceDiscoveryProvider.getMetadataEntry(
                authorityURL,
                false,
                msalRequest,
                app.getServiceBundle());
    }

    @Test (dataProvider = "b2cAdfsClouds")
    /**
     * when instance_discovery flag is set to true, instance_discovery is not performed and hence,
     * no exception is thrown while making a call to getMetaDataEntry() even when instanceDiscoveryResponse is not mocked.
     */
    public void b2c_adfs_instance_discovery_false(String authority) throws Exception{

        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .authority(authority)
                .instanceDiscovery(false)
                .build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(
                        "code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("scope")).build();

        MsalRequest msalRequest = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        URL authorityURL = new URL(authority);

        AadInstanceDiscoveryProvider.getMetadataEntry(
                authorityURL,
                false,
                msalRequest,
                app.getServiceBundle());
    }
}
