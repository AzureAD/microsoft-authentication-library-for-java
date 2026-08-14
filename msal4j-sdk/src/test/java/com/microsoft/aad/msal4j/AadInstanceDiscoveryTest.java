// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.net.URL;
import java.util.Collections;

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

    @ParameterizedTest
    @ValueSource(strings = {
            "east.us", "east/us", "../evil", "eastus:443", "east@us", "east us", "east\tus", "EastUS",
            "east%2eus", "\uFF45astus", "eastus-",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void aadInstanceDiscoveryTest_RegionSetByDeveloper_invalidRegion_throws(String region) {
        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                ConfidentialClientApplication.builder("client_id", ClientCredentialFactory.createFromSecret("secret"))
                        .aadInstanceDiscoveryResponse(instanceDiscoveryValidResponse)
                        .azureRegion(region));

        assertEquals(AuthenticationErrorCode.INVALID_REGION, ex.errorCode());
    }

    @Test
    void aadInstanceDiscoveryTest_AutoDetectRegion_invalidRegionDetected_fallsBackToGlobal() throws Exception {

        ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                "client_id", ClientCredentialFactory.createFromSecret("secret"))
                .aadInstanceDiscoveryResponse(instanceDiscoveryValidResponse)
                .autoDetectRegion(true)
                .build();

        MsalRequest msalRequest = clientCredentialRequest(app);
        URL authority = new URL(app.authority());

        //discoverRegion() is responsible for validating autodetected values and never returns an invalid one, so it
        //  is mocked here to return null, matching how it would behave for an invalid autodetected region
        try (MockedStatic<AadInstanceDiscoveryProvider> mocked = mockStatic(AadInstanceDiscoveryProvider.class, CALLS_REAL_METHODS)) {

            mocked.when(() -> AadInstanceDiscoveryProvider.discoverRegion(msalRequest,
                    app.serviceBundle())).thenReturn(null);

            InstanceDiscoveryMetadataEntry entry = AadInstanceDiscoveryProvider.getMetadataEntry(
                    authority, false, msalRequest, app.serviceBundle());

            assertValidResponse(entry);
            //An unusable region must not be stored on the application, where it would affect every later request
            assertNull(app.azureRegion());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "east.us", "east/us", "../evil", "eastus:443", "east@us", "east us", "EastUS", "east%2eus",
            "\uFF45astus", "eastus-",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void aadInstanceDiscoveryTest_InvalidEnvironmentRegion_fallsBackToGlobalWithoutCaching(String region)
            throws Exception {
        IHttpClient httpClient = mock(IHttpClient.class);
        org.mockito.Mockito.when(httpClient.send(any(HttpRequest.class))).thenReturn(instanceDiscoveryResponse());

        ConfidentialClientApplication app = autoDetectApplication(httpClient);
        MsalRequest msalRequest = clientCredentialRequest(app);
        URL authority = new URL(app.authority());

        try (MockedStatic<AadInstanceDiscoveryProvider> mocked = mockStatic(AadInstanceDiscoveryProvider.class,
                CALLS_REAL_METHODS)) {
            mocked.when(AadInstanceDiscoveryProvider::getRegionName).thenReturn(region);

            InstanceDiscoveryMetadataEntry entry = AadInstanceDiscoveryProvider.getMetadataEntry(
                    authority, false, msalRequest, app.serviceBundle());

            verify(httpClient).send(argThat(request -> request.url().getHost().equals("login.microsoftonline.com")));
            assertGlobalFallback(app, entry, region);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "east.us", "east/us", "../evil", "eastus:443", "east@us", "east us", "EastUS", "east%2eus",
            "\uFF45astus", "eastus-",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void aadInstanceDiscoveryTest_InvalidImdsRegion_fallsBackToGlobalWithoutCaching(String region) throws Exception {
        IHttpClient httpClient = mock(IHttpClient.class);
        HttpResponse imdsResponse = new HttpResponse().statusCode(HttpStatus.HTTP_OK)
                .body("{\"location\":\"" + region + "\"}");
        mockImdsAndInstanceDiscoveryResponses(httpClient, imdsResponse);

        ConfidentialClientApplication app = autoDetectApplication(httpClient);
        MsalRequest msalRequest = clientCredentialRequest(app);
        URL authority = new URL(app.authority());

        try (MockedStatic<AadInstanceDiscoveryProvider> mocked = mockStatic(AadInstanceDiscoveryProvider.class,
                CALLS_REAL_METHODS)) {
            mocked.when(AadInstanceDiscoveryProvider::getRegionName).thenReturn(null);

            InstanceDiscoveryMetadataEntry entry = AadInstanceDiscoveryProvider.getMetadataEntry(
                    authority, false, msalRequest, app.serviceBundle());

            verify(httpClient).send(argThat(request -> request.url().toString().startsWith(
                    "http://169.254.169.254/metadata/instance/compute")));
            assertGlobalFallback(app, entry, region);
        }
    }

    @Test
    void aadInstanceDiscoveryTest_MalformedImdsResponse_fallsBackToGlobalWithoutCaching() throws Exception {
        IHttpClient httpClient = mock(IHttpClient.class);
        HttpResponse imdsResponse = new HttpResponse().statusCode(HttpStatus.HTTP_OK).body("{ invalid json");
        mockImdsAndInstanceDiscoveryResponses(httpClient, imdsResponse);

        ConfidentialClientApplication app = autoDetectApplication(httpClient);
        MsalRequest msalRequest = clientCredentialRequest(app);
        URL authority = new URL(app.authority());

        try (MockedStatic<AadInstanceDiscoveryProvider> mocked = mockStatic(AadInstanceDiscoveryProvider.class,
                CALLS_REAL_METHODS)) {
            mocked.when(AadInstanceDiscoveryProvider::getRegionName).thenReturn(null);

            InstanceDiscoveryMetadataEntry entry = AadInstanceDiscoveryProvider.getMetadataEntry(
                    authority, false, msalRequest, app.serviceBundle());

            verify(httpClient).send(argThat(request -> request.url().toString().startsWith(
                    "http://169.254.169.254/metadata/instance/compute")));
            assertGlobalFallback(app, entry, null);
        }
    }

    @Test
    void aadInstanceDiscoveryTest_RegionSetByDeveloper_validRegion_buildsRegionalHost() throws Exception {

        ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                "client_id", ClientCredentialFactory.createFromSecret("secret"))
                .authority("https://login.microsoftonline.com/my_tenant")
                .azureRegion("eastus")
                .build();

        MsalRequest msalRequest = clientCredentialRequest(app);
        URL authority = new URL(app.authority());
        AadInstanceDiscoveryResponse expectedResponse = JsonHelper.convertJsonStringToJsonSerializableObject(
                instanceDiscoveryValidResponse, AadInstanceDiscoveryResponse::fromJson);

        try (MockedStatic<AadInstanceDiscoveryProvider> mocked = mockStatic(AadInstanceDiscoveryProvider.class, CALLS_REAL_METHODS)) {

            mocked.when(() -> AadInstanceDiscoveryProvider.discoverRegion(msalRequest,
                    app.serviceBundle())).thenReturn(null);
            mocked.when(() -> AadInstanceDiscoveryProvider.sendInstanceDiscoveryRequest(authority,
                    msalRequest, app.serviceBundle())).thenReturn(expectedResponse);

            InstanceDiscoveryMetadataEntry entry = AadInstanceDiscoveryProvider.getMetadataEntry(
                    authority, false, msalRequest, app.serviceBundle());

            assertEquals("eastus.login.microsoft.com", entry.preferredNetwork());
            assertEquals("login.microsoftonline.com", entry.preferredCache());
        }
    }

    @ParameterizedTest
    @CsvSource(value = {
            "https://login.chinacloudapi.cn/my_tenant|eastus.login.chinacloudapi.cn",
            "https://login.microsoftonline.us/my_tenant|eastus.login.microsoftonline.us"}, delimiter = '|')
    void aadInstanceDiscoveryTest_RegionSetByDeveloper_sovereignAuthorityBuildsRegionalHost(
            String authorityValue, String expectedHost) throws Exception {
        ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                "client_id", ClientCredentialFactory.createFromSecret("secret"))
                .authority(authorityValue)
                .azureRegion("eastus")
                .build();

        MsalRequest msalRequest = clientCredentialRequest(app);
        URL authority = new URL(app.authority());
        AadInstanceDiscoveryResponse expectedResponse = JsonHelper.convertJsonStringToJsonSerializableObject(
                instanceDiscoveryValidResponse, AadInstanceDiscoveryResponse::fromJson);

        try (MockedStatic<AadInstanceDiscoveryProvider> mocked = mockStatic(AadInstanceDiscoveryProvider.class,
                CALLS_REAL_METHODS)) {
            mocked.when(() -> AadInstanceDiscoveryProvider.discoverRegion(msalRequest,
                    app.serviceBundle())).thenReturn(null);
            mocked.when(() -> AadInstanceDiscoveryProvider.sendInstanceDiscoveryRequest(authority,
                    msalRequest, app.serviceBundle())).thenReturn(expectedResponse);

            InstanceDiscoveryMetadataEntry entry = AadInstanceDiscoveryProvider.getMetadataEntry(
                    authority, false, msalRequest, app.serviceBundle());

            assertEquals(expectedHost, entry.preferredNetwork());
            assertEquals(authority.getHost(), entry.preferredCache());
        }
    }

    private ConfidentialClientApplication autoDetectApplication(IHttpClient httpClient) {
        ConfidentialClientApplication.Builder builder = ConfidentialClientApplication.builder(
                        "client_id", ClientCredentialFactory.createFromSecret("secret"))
                .autoDetectRegion(true);
        if (httpClient != null) {
            builder.httpClient(httpClient);
        }
        return builder.build();
    }

    private void mockImdsAndInstanceDiscoveryResponses(IHttpClient httpClient, HttpResponse imdsResponse)
            throws Exception {
        org.mockito.Mockito.when(httpClient.send(any(HttpRequest.class))).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            return request.url().toString().startsWith("http://169.254.169.254/metadata/instance/compute") ?
                    imdsResponse : instanceDiscoveryResponse();
        });
    }

    private HttpResponse instanceDiscoveryResponse() {
        return new HttpResponse().statusCode(HttpStatus.HTTP_OK).body(instanceDiscoveryValidResponse);
    }

    private void assertGlobalFallback(ConfidentialClientApplication app, InstanceDiscoveryMetadataEntry entry,
                                      String invalidRegion) {
        assertValidResponse(entry);
        assertNull(app.azureRegion());
        if (invalidRegion != null) {
            assertFalse(AadInstanceDiscoveryProvider.cache.containsKey(invalidRegion));
            assertFalse(AadInstanceDiscoveryProvider.cache.keySet().stream()
                    .anyMatch(host -> host.contains(invalidRegion)));
        }
    }

    private MsalRequest clientCredentialRequest(ConfidentialClientApplication app) {
        //Regions are only used by the client credential flow, see AadInstanceDiscoveryProvider.shouldUseRegionalEndpoint
        ClientCredentialParameters parameters = ClientCredentialParameters.builder(
                Collections.singleton("scopes")).build();

        return new ClientCredentialRequest(parameters, app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_FOR_CLIENT, parameters), null);
    }

    @Test
    void instanceDiscoveryEndpointDoesNotCarryPortToDefaultTrustedHost() throws Exception {
        URL authority = new URL("https://custom.example:8443/tenant");

        String endpoint = AadInstanceDiscoveryProvider.getInstanceDiscoveryEndpoint(authority);

        assertEquals("https://login.microsoftonline.com/common/discovery/instance", endpoint);
    }

    @Test
    void instanceDiscoveryEndpointPreservesPortForTrustedHost() throws Exception {
        URL authority = new URL("https://login.microsoftonline.com:8443/tenant");

        String endpoint = AadInstanceDiscoveryProvider.getInstanceDiscoveryEndpoint(authority);

        assertEquals("https://login.microsoftonline.com:8443/common/discovery/instance", endpoint);
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
