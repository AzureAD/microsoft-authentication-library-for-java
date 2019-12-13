// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@PrepareForTest(AadInstanceDiscovery.class)
public class InstanceDiscoveryTest extends PowerMockTestCase {

    @BeforeMethod
    public void setup(){
        AadInstanceDiscovery.cache.clear();
    }

    @Test
    public void instanceDiscoveryMetadataTest_useDataInTokenCache() throws Exception {
        // properly formatted instance discovery metadata in token cache
        String tokenCacheData = TestHelper.readResource(
                this.getClass(),
                "/Instance_discovery_metadata_cache_data/cache_instance_discovery_metadata_valid.json");

        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .correlationId("correlation_id")
                .authority("https://login.microsoftonline.com/my_tenant")
                .build();

        app.tokenCache().deserialize(tokenCacheData);

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(
                "code", new URI("http://my.redirect.com")).build();

        MsalRequest msalRequest = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        URL authority = new URL(app.authority());

        PowerMock.mockStaticPartial(AadInstanceDiscovery.class, "sendInstanceDiscoveryRequest");

        PowerMock.expectPrivate(
                AadInstanceDiscovery.class,
                "sendInstanceDiscoveryRequest",
                authority,
                msalRequest,
                app.getServiceBundle()).andThrow(new AssertionError()).anyTimes();

        PowerMock.replay(AadInstanceDiscovery.class);

        InstanceDiscoveryMetadataEntry entry = AadInstanceDiscovery.GetMetadataEntry(
               authority,
                app,
                msalRequest);

        Assert.assertEquals(entry.preferredNetwork(), "login.microsoftonline.com");
        Assert.assertEquals(entry.preferredCache(), "login.windows.net");
        Assert.assertEquals(entry.aliases().size(), 4);
        Assert.assertTrue(entry.aliases().contains("login.microsoftonline.com"));
        Assert.assertTrue(entry.aliases().contains("login.windows.net"));
        Assert.assertTrue(entry.aliases().contains("login.microsoft.com"));
        Assert.assertTrue(entry.aliases().contains("sts.windows.net"));
    }

    @Test
    public void instanceDiscoveryMetadataTest_cacheDataExpired() throws Exception {
        // expired instance discovery metadata in token cache
        String tokenCacheData = TestHelper.readResource(
                this.getClass(),
                "/Instance_discovery_metadata_cache_data/cache_instance_discovery_metadata_expired.json");

        assertInstanceDiscoveryMetadataReturnedFromNetworkAndCached(tokenCacheData);
    }

    @Test
    public void instanceDiscoveryMetadataTest_noInstanceMetadataInCache() throws Exception {
        // token cache does not contain any instance discovery metadata
        String tokenCacheData = TestHelper.readResource(
                this.getClass(),
                "/Instance_discovery_metadata_cache_data/cache_no_instance_discovery_metadata.json");

        assertInstanceDiscoveryMetadataReturnedFromNetworkAndCached(tokenCacheData);
    }

    private void assertInstanceDiscoveryMetadataReturnedFromNetworkAndCached(String tokenCacheData)
            throws Exception {

        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .correlationId("correlation_id")
                .authority("https://login.microsoftonline.com/my_tenant")
                .build();

        app.tokenCache().deserialize(tokenCacheData);

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(
                "code", new URI("http://my.redirect.com")).build();

        MsalRequest msalRequest = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        URL authority = new URL(app.authority());

        String instanceDiscoveryData = TestHelper.readResource(
                this.getClass(),
                "/Instance_discovery_metadata_cache_data/Instance_discovery_response.json");

        InstanceDiscoveryResponse expectedResponse = JsonHelper.convertJsonToObject(
                instanceDiscoveryData,
                InstanceDiscoveryResponse.class);

        PowerMock.mockStaticPartial(AadInstanceDiscovery.class, "sendInstanceDiscoveryRequest");

        PowerMock.expectPrivate(
                AadInstanceDiscovery.class,
                "sendInstanceDiscoveryRequest",
                authority,
                msalRequest,
                app.getServiceBundle()).andReturn(expectedResponse);

        PowerMock.replay(AadInstanceDiscovery.class);

        InstanceDiscoveryMetadataEntry entry = AadInstanceDiscovery.GetMetadataEntry(
                new URL(app.authority()),
                app,
                msalRequest);

        PowerMock.verify(AadInstanceDiscovery.class);

        Assert.assertEquals(entry.preferredNetwork(), "login.microsoftonline.com");
        Assert.assertEquals(entry.preferredCache(), "login.windows.net");
        Assert.assertEquals(entry.aliases().size(), 4);
        Assert.assertTrue(entry.aliases().contains("login.microsoftonline.com"));
        Assert.assertTrue(entry.aliases().contains("login.windows.net"));
        Assert.assertTrue(entry.aliases().contains("login.microsoft.com"));
        Assert.assertTrue(entry.aliases().contains("sts.windows.net"));
        Assert.assertTrue(entry.expiresOn() >
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));

        // Assert token cache populated with fresh data
        Assert.assertTrue(
                app.tokenCache().instanceDiscoveryMetadata.get("login.microsoftonline.com").expiresOn() >
                        TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) );
    }
}
