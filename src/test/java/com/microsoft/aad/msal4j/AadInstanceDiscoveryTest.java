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

@PrepareForTest(AadInstanceDiscoveryProvider.class)
public class AadInstanceDiscoveryTest extends PowerMockTestCase {

    @BeforeMethod
    public void setup(){
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
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

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
    public void aadInstanceDiscoveryTest_responseSetByDeveloper_validResponse() throws Exception{

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
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

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
    public void aadInstanceDiscoveryTest_responseSetByDeveloper_invalidJson() throws Exception{

        String instanceDiscoveryResponse = TestHelper.readResource(
                this.getClass(),
                "/instance_discovery_data/aad_instance_discovery_response_invalid_json.json");

        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .aadInstanceDiscoveryResponse(instanceDiscoveryResponse)
                .build();
    }
}
