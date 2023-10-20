// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.net.URI;
import java.net.URL;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AadInstanceDiscoveryTest {

    String instanceDiscoveryValidResponse;
    AuthorizationCodeParameters parameters;

    @BeforeAll
    public void init() throws Exception {
        instanceDiscoveryValidResponse = TestHelper.readResource(
                this.getClass(),
                "/instance_discovery_data/aad_instance_discovery_response_valid.json");

        parameters = AuthorizationCodeParameters.builder(
                "code", new URI("http://my.redirect.com")).build();
    }

    @BeforeEach
    public void setup() {
        AadInstanceDiscoveryProvider.cache.clear();
    }

    @Test
    void aadInstanceDiscoveryTest_NotSetByDeveloper() throws Exception {
        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .correlationId("correlation_id")
                .authority("https://login.microsoftonline.com/my_tenant")
                .build();

        MsalRequest msalRequest = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        URL authority = new URL(app.authority());

        AadInstanceDiscoveryResponse expectedResponse = JsonHelper.convertJsonToObject(
                instanceDiscoveryValidResponse,
                AadInstanceDiscoveryResponse.class);

        try (MockedStatic<AadInstanceDiscoveryProvider> mockedInstanceDiscoveryProvider = mockStatic(AadInstanceDiscoveryProvider.class, CALLS_REAL_METHODS)) {

            mockedInstanceDiscoveryProvider.when(() -> AadInstanceDiscoveryProvider.sendInstanceDiscoveryRequest(authority,
                    msalRequest,
                    app.getServiceBundle())).thenReturn(expectedResponse);

            InstanceDiscoveryMetadataEntry entry = AadInstanceDiscoveryProvider.getMetadataEntry(
                    authority,
                    false,
                    msalRequest,
                    app.getServiceBundle());

            assertValidResponse(entry);
        }
    }

    @Test
    void aadInstanceDiscoveryTest_responseSetByDeveloper_validResponse() throws Exception {

        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .aadInstanceDiscoveryResponse(instanceDiscoveryValidResponse)
                .build();

        MsalRequest msalRequest = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        URL authority = new URL(app.authority());

        InstanceDiscoveryMetadataEntry entry = AadInstanceDiscoveryProvider.getMetadataEntry(
                authority,
                false,
                msalRequest,
                app.getServiceBundle());

        assertValidResponse(entry);
    }

    @Test
    void aadInstanceDiscoveryTest_responseSetByDeveloper_invalidJson() throws Exception {

        String instanceDiscoveryResponse = TestHelper.readResource(
                this.getClass(),
                "/instance_discovery_data/aad_instance_discovery_response_invalid_json.json");

        assertThrows(MsalClientException.class, () -> PublicClientApplication.builder("client_id")
                .aadInstanceDiscoveryResponse(instanceDiscoveryResponse)
                .build());
    }

    @Test
    void aadInstanceDiscoveryTest_AutoDetectRegion_NoRegionDetected() throws Exception {

        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .aadInstanceDiscoveryResponse(instanceDiscoveryValidResponse)
                .autoDetectRegion(true)
                .build();

        MsalRequest msalRequest = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        URL authority = new URL(app.authority());

        try (MockedStatic<AadInstanceDiscoveryProvider> mocked = mockStatic(AadInstanceDiscoveryProvider.class, CALLS_REAL_METHODS)) {

            mocked.when(() -> AadInstanceDiscoveryProvider.discoverRegion(msalRequest,
                    app.getServiceBundle())).thenReturn(null);

            InstanceDiscoveryMetadataEntry entry = AadInstanceDiscoveryProvider.getMetadataEntry(
                    authority,
                    false,
                    msalRequest,
                    app.getServiceBundle());

            assertValidResponse(entry);
        }
    }

    void assertValidResponse(InstanceDiscoveryMetadataEntry entry) {
        assertEquals(entry.preferredNetwork(), "login.microsoftonline.com");
        assertEquals(entry.preferredCache(), "login.windows.net");
        assertEquals(entry.aliases().size(), 4);
        assertTrue(entry.aliases().contains("login.microsoftonline.com"));
        assertTrue(entry.aliases().contains("login.windows.net"));
        assertTrue(entry.aliases().contains("login.microsoft.com"));
        assertTrue(entry.aliases().contains("sts.windows.net"));
    }
}
