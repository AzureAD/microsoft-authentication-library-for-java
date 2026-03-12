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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import java.lang.reflect.Method;
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
        AadInstanceDiscoveryResponse expectedResponse= JsonHelper.convertJsonStringToJsonSerializableObject(instanceDiscoveryValidResponse, AadInstanceDiscoveryResponse::fromJson);

        try (MockedStatic<AadInstanceDiscoveryProvider> mockedInstanceDiscoveryProvider = mockStatic(AadInstanceDiscoveryProvider.class, CALLS_REAL_METHODS)) {

            mockedInstanceDiscoveryProvider.when(() -> AadInstanceDiscoveryProvider.sendInstanceDiscoveryRequest(authority,
                    msalRequest,
                    app.serviceBundle())).thenReturn(expectedResponse);

            InstanceDiscoveryMetadataEntry entry = AadInstanceDiscoveryProvider.getMetadataEntry(
                    authority,
                    false,
                    msalRequest,
                    app.serviceBundle());

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
                app.serviceBundle());

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
                    app.serviceBundle())).thenReturn(null);

            InstanceDiscoveryMetadataEntry entry = AadInstanceDiscoveryProvider.getMetadataEntry(
                    authority,
                    false,
                    msalRequest,
                    app.serviceBundle());

            assertValidResponse(entry);
        }
    }

    @Test
    void discoveryEndpoint_routesToSovereignHost() throws Exception {
        // Arrange
        URL sovereignUrl = new URL("https://login.sovcloud-identity.fr/my_tenant");
        Method method = AadInstanceDiscoveryProvider.class.getDeclaredMethod("getInstanceDiscoveryEndpoint", URL.class);
        method.setAccessible(true);

        // Act
        String endpoint = (String) method.invoke(null, sovereignUrl);

        // Assert
        assertTrue(endpoint.contains("login.sovcloud-identity.fr"),
                "Discovery endpoint should use the sovereign host, got: " + endpoint);
    }

    @Test
    void regionalEndpoint_usesSovereignTemplate() throws Exception {
        // Arrange
        Method method = AadInstanceDiscoveryProvider.class.getDeclaredMethod("getRegionalizedHost", String.class, String.class);
        method.setAccessible(true);

        // Act
        String result = (String) method.invoke(null, "login.sovcloud-identity.fr", "westeurope");

        // Assert
        assertEquals("westeurope.login.sovcloud-identity.fr", result);
    }

    @Test
    void networkException_cachesFallbackAndDoesNotPropagate() throws Exception {
        // Arrange
        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .correlationId("correlation_id")
                .authority("https://login.microsoftonline.com/my_tenant")
                .build();

        MsalRequest msalRequest = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        URL authority = new URL(app.authority());

        try (MockedStatic<AadInstanceDiscoveryProvider> mocked = mockStatic(AadInstanceDiscoveryProvider.class, CALLS_REAL_METHODS)) {

            mocked.when(() -> AadInstanceDiscoveryProvider.sendInstanceDiscoveryRequest(authority,
                    msalRequest,
                    app.serviceBundle())).thenThrow(new MsalClientException("Network timeout", AuthenticationErrorCode.UNKNOWN));

            // Act — should not throw
            InstanceDiscoveryMetadataEntry entry = assertDoesNotThrow(() ->
                    AadInstanceDiscoveryProvider.getMetadataEntry(
                            authority,
                            false,
                            msalRequest,
                            app.serviceBundle()));

            // Assert — cache should contain a self-entry
            assertNotNull(entry);
            String host = authority.getHost();
            InstanceDiscoveryMetadataEntry cached = AadInstanceDiscoveryProvider.cache.get(host);
            assertNotNull(cached, "Fallback entry should be cached");
            assertEquals(host, cached.preferredNetwork);
            assertEquals(host, cached.preferredCache);
            assertTrue(cached.aliases.contains(host));
        }
    }

    @Test
    void subsequentCallAfterNetworkFailure_usesCacheNoRetry() throws Exception {
        // Arrange
        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .correlationId("correlation_id")
                .authority("https://login.microsoftonline.com/my_tenant")
                .build();

        MsalRequest msalRequest = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        URL authority = new URL(app.authority());

        try (MockedStatic<AadInstanceDiscoveryProvider> mocked = mockStatic(AadInstanceDiscoveryProvider.class, CALLS_REAL_METHODS)) {

            mocked.when(() -> AadInstanceDiscoveryProvider.sendInstanceDiscoveryRequest(any(URL.class),
                    any(MsalRequest.class),
                    any(ServiceBundle.class))).thenThrow(new MsalClientException("Network timeout", AuthenticationErrorCode.UNKNOWN));

            // Act — first call triggers network failure + fallback cache
            AadInstanceDiscoveryProvider.getMetadataEntry(authority, false, msalRequest, app.serviceBundle());

            // Act — second call should hit cache, not retry network
            InstanceDiscoveryMetadataEntry entry = AadInstanceDiscoveryProvider.getMetadataEntry(authority, false, msalRequest, app.serviceBundle());

            // Assert — sendInstanceDiscoveryRequest should have been called only once
            mocked.verify(() -> AadInstanceDiscoveryProvider.sendInstanceDiscoveryRequest(any(URL.class),
                    any(MsalRequest.class),
                    any(ServiceBundle.class)), times(1));
            assertNotNull(entry);
        }
    }

    @Test
    void invalidInstanceException_stillPropagates() throws Exception {
        // Arrange
        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .correlationId("correlation_id")
                .authority("https://login.microsoftonline.com/my_tenant")
                .build();

        MsalRequest msalRequest = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        URL authority = new URL(app.authority());

        try (MockedStatic<AadInstanceDiscoveryProvider> mocked = mockStatic(AadInstanceDiscoveryProvider.class, CALLS_REAL_METHODS)) {

            mocked.when(() -> AadInstanceDiscoveryProvider.sendInstanceDiscoveryRequest(authority,
                    msalRequest,
                    app.serviceBundle())).thenThrow(new MsalServiceException("invalid_instance", "invalid_instance"));

            // Act / Assert — MsalServiceException should propagate
            assertThrows(MsalServiceException.class, () ->
                    AadInstanceDiscoveryProvider.getMetadataEntry(
                            authority,
                            false,
                            msalRequest,
                            app.serviceBundle()));

            // Assert — nothing should be cached
            assertNull(AadInstanceDiscoveryProvider.cache.get(authority.getHost()));
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
