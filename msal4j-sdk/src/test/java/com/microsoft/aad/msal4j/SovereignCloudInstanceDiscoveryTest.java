// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Acceptance tests for sovereign cloud instance discovery behavior.
 *
 * These tests create a real ConfidentialClientApplication with a sovereign authority,
 * mock only the HTTP layer, and verify that all HTTP requests are routed to the
 * correct sovereign host — not to login.microsoftonline.com or any other host.
 */
class SovereignCloudInstanceDiscoveryTest {

    private static final String SOVEREIGN_HOST = "login.sovcloud-identity.fr";
    private static final String SOVEREIGN_AUTHORITY = "https://" + SOVEREIGN_HOST + "/my-tenant";
    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";

    // A valid instance discovery response for the sovereign cloud
    private static final String SOVEREIGN_INSTANCE_DISCOVERY_RESPONSE = "{"
            + "\"tenant_discovery_endpoint\":\"https://" + SOVEREIGN_HOST + "/my-tenant/.well-known/openid-configuration\","
            + "\"api-version\":\"1.1\","
            + "\"metadata\":[{"
            + "\"preferred_network\":\"" + SOVEREIGN_HOST + "\","
            + "\"preferred_cache\":\"" + SOVEREIGN_HOST + "\","
            + "\"aliases\":[\"" + SOVEREIGN_HOST + "\"]"
            + "}]}";

    @BeforeEach
    void setup() {
        AadInstanceDiscoveryProvider.cache.clear();
    }

    @Test
    void sovereignAuthority_allRequestsRouteToSovereignHost() throws Exception {
        // Arrange — mock HTTP client that captures all request URLs
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            String url = request.url().toString();

            if (url.contains("discovery/instance")) {
                return TestHelper.expectedResponse(200, SOVEREIGN_INSTANCE_DISCOVERY_RESPONSE);
            }
            if (url.contains("oauth2/v2.0/token")) {
                return TestHelper.expectedResponse(200,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>()));
            }

            fail("Unexpected request URL: " + url);
            return null;
        });

        ConfidentialClientApplication cca = ConfidentialClientApplication
                .builder(CLIENT_ID, ClientCredentialFactory.createFromSecret(CLIENT_SECRET))
                .authority(SOVEREIGN_AUTHORITY)
                .validateAuthority(false)
                .httpClient(httpClientMock)
                .build();

        // Act
        IAuthenticationResult result = cca.acquireToken(
                ClientCredentialParameters.builder(Collections.singleton("https://resource/.default")).build()
        ).get();

        // Assert — token was acquired
        assertNotNull(result);
        assertNotNull(result.accessToken());

        // Assert — every HTTP request went to the sovereign host
        verify(httpClientMock, atLeastOnce()).send(requestCaptor.capture());
        List<HttpRequest> capturedRequests = requestCaptor.getAllValues();
        assertFalse(capturedRequests.isEmpty(), "At least one HTTP request should have been made");

        for (HttpRequest req : capturedRequests) {
            assertEquals(SOVEREIGN_HOST, req.url().getHost(),
                    "All requests must go to " + SOVEREIGN_HOST + ", but got: " + req.url());
        }
    }

    @Test
    void sovereignAuthority_instanceDiscoveryEndpointUsesOwnHost() throws Exception {
        // Arrange — track which URLs are called
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        List<String> capturedUrls = new ArrayList<>();

        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            String url = request.url().toString();
            capturedUrls.add(url);

            if (url.contains("discovery/instance")) {
                return TestHelper.expectedResponse(200, SOVEREIGN_INSTANCE_DISCOVERY_RESPONSE);
            }
            if (url.contains("oauth2/v2.0/token")) {
                return TestHelper.expectedResponse(200,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>()));
            }

            fail("Unexpected request URL: " + url);
            return null;
        });

        ConfidentialClientApplication cca = ConfidentialClientApplication
                .builder(CLIENT_ID, ClientCredentialFactory.createFromSecret(CLIENT_SECRET))
                .authority(SOVEREIGN_AUTHORITY)
                .validateAuthority(false)
                .httpClient(httpClientMock)
                .build();

        // Act
        cca.acquireToken(
                ClientCredentialParameters.builder(Collections.singleton("https://resource/.default")).build()
        ).get();

        // Assert — instance discovery endpoint was called on the sovereign host
        assertTrue(capturedUrls.stream().anyMatch(url ->
                        url.contains(SOVEREIGN_HOST) && url.contains("discovery/instance")),
                "Instance discovery should be sent to " + SOVEREIGN_HOST + ". Captured URLs: " + capturedUrls);

        // Assert — no request went to the public cloud
        assertTrue(capturedUrls.stream().noneMatch(url -> url.contains("login.microsoftonline.com")),
                "No requests should go to login.microsoftonline.com. Captured URLs: " + capturedUrls);
    }

    @Test
    void sovereignAuthority_instanceDiscoveryFailure_usesKnownMetadataFallback() throws Exception {
        // Arrange — instance discovery returns a server error (not invalid_instance)
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        List<String> capturedUrls = new ArrayList<>();

        String serverErrorResponse = "{\"error\":\"server_error\",\"error_description\":\"Internal server error\"}";

        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            String url = request.url().toString();
            capturedUrls.add(url);

            if (url.contains("discovery/instance")) {
                // Return 500 server error — should NOT throw, should fall back
                return TestHelper.expectedResponse(500, serverErrorResponse);
            }
            if (url.contains("oauth2/v2.0/token")) {
                return TestHelper.expectedResponse(200,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>()));
            }

            fail("Unexpected request URL: " + url);
            return null;
        });

        ConfidentialClientApplication cca = ConfidentialClientApplication
                .builder(CLIENT_ID, ClientCredentialFactory.createFromSecret(CLIENT_SECRET))
                .authority(SOVEREIGN_AUTHORITY)
                .validateAuthority(false)
                .httpClient(httpClientMock)
                .build();

        // Act — should succeed despite instance discovery failure
        IAuthenticationResult result = cca.acquireToken(
                ClientCredentialParameters.builder(Collections.singleton("https://resource/.default")).build()
        ).get();

        // Assert — token was still acquired
        assertNotNull(result);

        // Assert — all requests went to the sovereign host
        assertTrue(capturedUrls.stream().allMatch(url -> url.contains(SOVEREIGN_HOST)),
                "All requests must go to " + SOVEREIGN_HOST + ". Captured URLs: " + capturedUrls);

        // Assert — known metadata for the sovereign host was cached
        InstanceDiscoveryMetadataEntry cached = AadInstanceDiscoveryProvider.cache.get(SOVEREIGN_HOST);
        assertNotNull(cached, "Fallback metadata should be cached for " + SOVEREIGN_HOST);
        assertEquals(SOVEREIGN_HOST, cached.preferredNetwork());
    }

    @Test
    void sovereignAuthority_invalidInstance_throws() throws Exception {
        // Arrange — instance discovery returns invalid_instance error
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        String invalidInstanceResponse = "{\"error\":\"invalid_instance\","
                + "\"error_description\":\"AADSTS50049: Unknown or invalid instance.\","
                + "\"error_codes\":[50049]}";

        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            String url = request.url().toString();

            if (url.contains("discovery/instance")) {
                return TestHelper.expectedResponse(400, invalidInstanceResponse);
            }

            fail("No further requests should be made after invalid_instance. URL: " + url);
            return null;
        });

        ConfidentialClientApplication cca = ConfidentialClientApplication
                .builder(CLIENT_ID, ClientCredentialFactory.createFromSecret(CLIENT_SECRET))
                .authority(SOVEREIGN_AUTHORITY)
                .validateAuthority(false)
                .httpClient(httpClientMock)
                .build();

        // Act / Assert — should throw MsalServiceException
        Exception thrown = assertThrows(Exception.class, () ->
                cca.acquireToken(
                        ClientCredentialParameters.builder(Collections.singleton("https://resource/.default")).build()
                ).get()
        );

        // The CompletableFuture wraps the exception in ExecutionException
        Throwable cause = thrown.getCause();
        assertInstanceOf(MsalServiceException.class, cause,
                "Should throw MsalServiceException for invalid_instance, got: " + cause);
    }

    @Test
    void sovereignAuthority_networkException_cachesFallbackAndProceeds() throws Exception {
        // Arrange — instance discovery throws a network exception on first call,
        // then succeeds for the token request
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        List<String> capturedUrls = new ArrayList<>();

        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            String url = request.url().toString();
            capturedUrls.add(url);

            if (url.contains("discovery/instance")) {
                throw new java.net.SocketException("Network is unreachable");
            }
            if (url.contains("oauth2/v2.0/token")) {
                return TestHelper.expectedResponse(200,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>()));
            }

            fail("Unexpected request URL: " + url);
            return null;
        });

        ConfidentialClientApplication cca = ConfidentialClientApplication
                .builder(CLIENT_ID, ClientCredentialFactory.createFromSecret(CLIENT_SECRET))
                .authority(SOVEREIGN_AUTHORITY)
                .validateAuthority(false)
                .httpClient(httpClientMock)
                .build();

        // Act — should succeed despite network failure on instance discovery
        IAuthenticationResult result = cca.acquireToken(
                ClientCredentialParameters.builder(Collections.singleton("https://resource/.default")).build()
        ).get();

        // Assert — token was acquired
        assertNotNull(result);

        // Assert — all requests went to the sovereign host
        assertTrue(capturedUrls.stream().allMatch(url -> url.contains(SOVEREIGN_HOST)),
                "All requests must go to " + SOVEREIGN_HOST + ". Captured URLs: " + capturedUrls);

        // Assert — fallback entry was cached with known metadata
        InstanceDiscoveryMetadataEntry cached = AadInstanceDiscoveryProvider.cache.get(SOVEREIGN_HOST);
        assertNotNull(cached, "Fallback entry should be cached for " + SOVEREIGN_HOST);
        assertEquals(SOVEREIGN_HOST, cached.preferredNetwork());
    }

    @Test
    void sovereignAuthority_nonInvalidInstanceServiceError_doesNotThrow() throws Exception {
        // Arrange — instance discovery returns a 502 with a non-invalid_instance error.
        // This should NOT throw (unlike invalid_instance) — it should fall back gracefully.
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        String badGatewayResponse = "{\"error\":\"server_error\",\"error_description\":\"Bad Gateway\"}";

        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            String url = request.url().toString();

            if (url.contains("discovery/instance")) {
                return TestHelper.expectedResponse(502, badGatewayResponse);
            }
            if (url.contains("oauth2/v2.0/token")) {
                return TestHelper.expectedResponse(200,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>()));
            }

            fail("Unexpected request URL: " + url);
            return null;
        });

        ConfidentialClientApplication cca = ConfidentialClientApplication
                .builder(CLIENT_ID, ClientCredentialFactory.createFromSecret(CLIENT_SECRET))
                .authority(SOVEREIGN_AUTHORITY)
                .validateAuthority(false)
                .httpClient(httpClientMock)
                .build();

        // Act — should NOT throw, should fall back
        IAuthenticationResult result = assertDoesNotThrow(() ->
                cca.acquireToken(
                        ClientCredentialParameters.builder(Collections.singleton("https://resource/.default")).build()
                ).get()
        );

        // Assert
        assertNotNull(result);
        assertNotNull(result.accessToken());
    }
}
