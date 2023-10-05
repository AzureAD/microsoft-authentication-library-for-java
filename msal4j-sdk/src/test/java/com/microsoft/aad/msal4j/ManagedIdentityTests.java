// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.util.URLUtils;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.SocketException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ManagedIdentityTests {

    static final String resource = "https://management.azure.com";
    final static String resourceDefaultSuffix = "https://management.azure.com/.default";
    final static String appServiceEndpoint = "http://127.0.0.1:41564/msi/token";
    final static String IMDS_ENDPOINT = "http://169.254.169.254/metadata/identity/oauth2/token";
    final static String azureArcEndpoint = "http://localhost:40342/metadata/identity/oauth2/token";
    final static String cloudShellEndpoint = "http://localhost:40342/metadata/identity/oauth2/token";
    final static String serviceFabricEndpoint = "http://localhost:40342/metadata/identity/oauth2/token";

    private String getSuccessfulResponse(String resource) {
        long expiresOn = Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond();
        return "{\"access_token\":\"accesstoken\",\"expires_on\":\"" + expiresOn + "\",\"resource\":\"" + resource + "\",\"token_type\":" +
                "\"Bearer\",\"client_id\":\"client_id\"}";
    }

    private String getMsiErrorResponse() {
        return "{\"statusCode\":\"500\",\"message\":\"An unexpected error occured while fetching the AAD Token.\",\"correlationId\":\"7d0c9763-ff1d-4842-a3f3-6d49e64f4513\"}";
    }

    private HttpRequest expectedRequest(ManagedIdentitySourceType source, String resource) {
        return expectedRequest(source, resource, ManagedIdentityId.systemAssigned());
    }

    private HttpRequest expectedRequest(ManagedIdentitySourceType source, String resource,
            ManagedIdentityId id) {
        String endpoint = null;
        Map<String, String> headers = new HashMap<>();
        Map<String, List<String>> queryParameters = new HashMap<>();
        Map<String, List<String>> bodyParameters = new HashMap<>();

        switch (source) {
            case APP_SERVICE: {
                endpoint = appServiceEndpoint;

                queryParameters.put("api-version", Collections.singletonList("2019-08-01"));
                queryParameters.put("resource", Collections.singletonList(resource));

                headers.put("X-IDENTITY-HEADER", "secret");
                break;
            }
            case CLOUD_SHELL: {
                endpoint = cloudShellEndpoint;

                headers.put("ContentType", "application/x-www-form-urlencoded");
                headers.put("Metadata", "true");

                bodyParameters.put("resource", Collections.singletonList(resource));
                return new HttpRequest(HttpMethod.POST, computeUri(endpoint, queryParameters), headers, URLUtils.serializeParameters(bodyParameters));
            }
            case IMDS: {
                endpoint = IMDS_ENDPOINT;
                queryParameters.put("api-version", Collections.singletonList("2018-02-01"));
                queryParameters.put("resource", Collections.singletonList(resource));
                headers.put("Metadata", "true");
                break;
            }
        }

        switch (id.getIdType()) {
            case CLIENT_ID:
                queryParameters.put("client_id", Collections.singletonList(id.getUserAssignedId()));
                break;
            case RESOURCE_ID:
                queryParameters.put("mi_res_id", Collections.singletonList(id.getUserAssignedId()));
                break;
        }

        return new HttpRequest(HttpMethod.GET, computeUri(endpoint, queryParameters), headers);
    }

    private String computeUri(String endpoint, Map<String, List<String>> queryParameters) {
        if (queryParameters.isEmpty()) {
            return endpoint;
        }

        String queryString = URLUtils.serializeParameters(queryParameters);

        return endpoint + "?" + queryString;
    }

    private HttpResponse expectedResponse(int statusCode, String response) {
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.statusCode(statusCode);
        httpResponse.body(response);

        return httpResponse;
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createData")
    void managedIdentityTest_SystemAssigned_SuccessfulResponse(ManagedIdentitySourceType source, String endpoint, String resource) throws Exception {
        IEnvironmentVariables environmentVariables = new EnvironmentVariablesHelper(source, endpoint);
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        lenient().when(httpClientMock.send(eq(expectedRequest(source, resource)))).thenReturn(expectedResponse(200, getSuccessfulResponse(resource)));

        ManagedIdentityApplication miApp = ManagedIdentityApplication
                .builder(ManagedIdentityId.systemAssigned())
                .httpClient(httpClientMock)
                .build();

        IAuthenticationResult result = miApp.acquireTokenForManagedIdentity(
                ManagedIdentityParameters.builder(resource)
                        .environmentVariables(environmentVariables)
                        .build()).get();

        assertNotNull(result.accessToken());

        String accessToken = result.accessToken();

        result = miApp.acquireTokenForManagedIdentity(
                ManagedIdentityParameters.builder(resource)
                        .environmentVariables(environmentVariables)
                        .build()).get();

        assertNotNull(result.accessToken());
        assertEquals(accessToken, result.accessToken());
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataUserAssigned")
    void managedIdentityTest_UserAssigned_SuccessfulResponse(ManagedIdentitySourceType source, String endpoint, ManagedIdentityId id) throws Exception {
        IEnvironmentVariables environmentVariables = new EnvironmentVariablesHelper(source, endpoint);
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(eq(expectedRequest(source, resource, id)))).thenReturn(expectedResponse(200, getSuccessfulResponse(resource)));

        ManagedIdentityApplication miApp = ManagedIdentityApplication
                .builder(id)
                .httpClient(httpClientMock)
                .build();

        IAuthenticationResult result = miApp.acquireTokenForManagedIdentity(
                ManagedIdentityParameters.builder(resource)
                        .environmentVariables(environmentVariables)
                        .build()).get();

        assertNotNull(result.accessToken());
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataUserAssignedNotSupported")
    void managedIdentityTest_UserAssigned_NotSupported(ManagedIdentitySourceType source, String endpoint, ManagedIdentityId id) throws Exception {
        IEnvironmentVariables environmentVariables = new EnvironmentVariablesHelper(source, endpoint);
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        ManagedIdentityApplication miApp = ManagedIdentityApplication
                .builder(id)
                .httpClient(httpClientMock)
                .build();

        try {
            IAuthenticationResult result = miApp.acquireTokenForManagedIdentity(
                    ManagedIdentityParameters.builder(resource)
                            .environmentVariables(environmentVariables)
                            .build()).get();
        } catch (Exception e) {
            assertNotNull(e);
            assertInstanceOf(MsalManagedIdentityException.class, e.getCause());

            MsalManagedIdentityException msalMsiException = (MsalManagedIdentityException) e.getCause();
            assertEquals(ManagedIdentitySourceType.CLOUD_SHELL, msalMsiException.managedIdentitySourceType);
            assertEquals(MsalError.USER_ASSIGNED_MANAGED_IDENTITY_NOT_SUPPORTED, msalMsiException.errorCode());
            return;
        }

        fail("MsalManagedIdentityException is expected but not thrown.");
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createData")
    void managedIdentityTest_DifferentScopes_RequestsNewToken(ManagedIdentitySourceType source, String endpoint) throws Exception {
        String resource = "https://management.azure.com";
        String anotherResource = "https://graph.microsoft.com";

        IEnvironmentVariables environmentVariables = new EnvironmentVariablesHelper(source, endpoint);
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        lenient().when(httpClientMock.send(eq(expectedRequest(source, resource)))).thenReturn(expectedResponse(200, getSuccessfulResponse(resource)));
        lenient().when(httpClientMock.send(eq(expectedRequest(source, anotherResource)))).thenReturn(expectedResponse(200, getSuccessfulResponse(resource)));

        ManagedIdentityApplication miApp = ManagedIdentityApplication
                .builder(ManagedIdentityId.systemAssigned())
                .httpClient(httpClientMock)
                .build();

        IAuthenticationResult result = miApp.acquireTokenForManagedIdentity(
                ManagedIdentityParameters.builder(resource)
                        .environmentVariables(environmentVariables)
                        .build()).get();

        assertNotNull(result.accessToken());

        result = miApp.acquireTokenForManagedIdentity(
                ManagedIdentityParameters.builder(anotherResource)
                        .environmentVariables(environmentVariables)
                        .build()).get();

        assertNotNull(result.accessToken());
        // TODO: Assert token source to check the token source is IDP and not Cache.
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataWrongScope")
    void managedIdentityTest_WrongScopes(ManagedIdentitySourceType source, String endpoint, String resource) throws Exception {
        IEnvironmentVariables environmentVariables = new EnvironmentVariablesHelper(source, endpoint);
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        lenient().when(httpClientMock.send(eq(expectedRequest(source, resource)))).thenReturn(expectedResponse(500, getMsiErrorResponse()));
        lenient().when(httpClientMock.send(eq(expectedRequest(source, resource)))).thenReturn(expectedResponse(500, getMsiErrorResponse()));

        ManagedIdentityApplication miApp = ManagedIdentityApplication
                .builder(ManagedIdentityId.systemAssigned())
                .httpClient(httpClientMock)
                .build();

        try {
            miApp.acquireTokenForManagedIdentity(
                    ManagedIdentityParameters.builder(resource)
                            .environmentVariables(environmentVariables)
                            .build()).get();
        } catch (Exception exception) {
            assert(exception.getCause() instanceof MsalManagedIdentityException);

            MsalManagedIdentityException miException = (MsalManagedIdentityException) exception.getCause();
            assertEquals(source, miException.managedIdentitySourceType);
            assertEquals(AuthenticationErrorCode.MANAGED_IDENTITY_REQUEST_FAILED, miException.errorCode());
            return;
        }

        fail("MsalManagedIdentityException is expected but not thrown.");
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataError")
    void managedIdentity_RequestFailed_NoPayload(ManagedIdentitySourceType source, String endpoint) throws Exception {
        IEnvironmentVariables environmentVariables = new EnvironmentVariablesHelper(source, endpoint);
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        lenient().when(httpClientMock.send(eq(expectedRequest(source, resource)))).thenReturn(expectedResponse(500, ""));
        lenient().when(httpClientMock.send(eq(expectedRequest(source, resource)))).thenReturn(expectedResponse(500, ""));

        ManagedIdentityApplication miApp = ManagedIdentityApplication
                .builder(ManagedIdentityId.systemAssigned())
                .httpClient(httpClientMock)
                .build();

        try {
            miApp.acquireTokenForManagedIdentity(
                    ManagedIdentityParameters.builder(resource)
                            .environmentVariables(environmentVariables)
                            .build()).get();
        } catch (Exception exception) {
            assert(exception.getCause() instanceof MsalManagedIdentityException);

            MsalManagedIdentityException miException = (MsalManagedIdentityException) exception.getCause();
            assertEquals(source, miException.managedIdentitySourceType);
            assertEquals(AuthenticationErrorCode.MANAGED_IDENTITY_REQUEST_FAILED, miException.errorCode());
            return;
        }

        fail("MsalManagedIdentityException is expected but not thrown.");
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataError")
    void managedIdentity_RequestFailed_NullResponse(ManagedIdentitySourceType source, String endpoint) throws Exception {
        IEnvironmentVariables environmentVariables = new EnvironmentVariablesHelper(source, endpoint);
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        lenient().when(httpClientMock.send(eq(expectedRequest(source, resource)))).thenReturn(expectedResponse(200, ""));
        lenient().when(httpClientMock.send(eq(expectedRequest(source, resource)))).thenReturn(expectedResponse(200, ""));

        ManagedIdentityApplication miApp = ManagedIdentityApplication
                .builder(ManagedIdentityId.systemAssigned())
                .httpClient(httpClientMock)
                .build();

        try {
            miApp.acquireTokenForManagedIdentity(
                    ManagedIdentityParameters.builder(resource)
                            .environmentVariables(environmentVariables)
                            .build()).get();
        } catch (Exception exception) {
            assert(exception.getCause() instanceof MsalManagedIdentityException);

            MsalManagedIdentityException miException = (MsalManagedIdentityException) exception.getCause();
            assertEquals(source, miException.managedIdentitySourceType);
            assertEquals(AuthenticationErrorCode.MANAGED_IDENTITY_REQUEST_FAILED, miException.errorCode());
            return;
        }

        fail("MsalManagedIdentityException is expected but not thrown.");
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataError")
    void managedIdentity_RequestFailed_UnreachableNetwork(ManagedIdentitySourceType source, String endpoint) throws Exception {
        IEnvironmentVariables environmentVariables = new EnvironmentVariablesHelper(source, endpoint);
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        lenient().when(httpClientMock.send(eq(expectedRequest(source, resource)))).thenThrow(new SocketException("A socket operation was attempted to an unreachable network."));

        ManagedIdentityApplication miApp = ManagedIdentityApplication
                .builder(ManagedIdentityId.systemAssigned())
                .httpClient(httpClientMock)
                .build();

        try {
            miApp.acquireTokenForManagedIdentity(
                    ManagedIdentityParameters.builder(resource)
                            .environmentVariables(environmentVariables)
                            .build()).get();
        } catch (Exception exception) {
            assert(exception.getCause() instanceof MsalManagedIdentityException);

            MsalManagedIdentityException miException = (MsalManagedIdentityException) exception.getCause();
            assertEquals(source, miException.managedIdentitySourceType);
            assertEquals(MsalError.MANAGED_IDENTITY_UNREACHABLE_NETWORK, miException.errorCode());
            return;
        }

        fail("MsalManagedIdentityException is expected but not thrown.");
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataError")
    void managedIdentity_SharedCache(ManagedIdentitySourceType source, String endpoint) throws Exception {
        IEnvironmentVariables environmentVariables = new EnvironmentVariablesHelper(source, endpoint);
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        lenient().when(httpClientMock.send(eq(expectedRequest(source, resource)))).thenReturn(expectedResponse(200, getSuccessfulResponse(resource)));

        ManagedIdentityApplication miApp1 = ManagedIdentityApplication
                .builder(ManagedIdentityId.systemAssigned())
                .httpClient(httpClientMock)
                .build();

        ManagedIdentityApplication miApp2 = ManagedIdentityApplication
                .builder(ManagedIdentityId.systemAssigned())
                .httpClient(httpClientMock)
                .build();

        IAuthenticationResult resultMiApp1 = miApp1.acquireTokenForManagedIdentity(
                ManagedIdentityParameters.builder(resource)
                        .environmentVariables(environmentVariables)
                        .build()).get();

        assertNotNull(resultMiApp1.accessToken());

        IAuthenticationResult resultMiApp2 = miApp2.acquireTokenForManagedIdentity(
                ManagedIdentityParameters.builder(resource)
                        .environmentVariables(environmentVariables)
                        .build()).get();

        assertNotNull(resultMiApp2.accessToken());

        //acquireTokenForManagedIdentity does a cache lookup by default, and all ManagedIdentityApplication's share a cache,
        // so calling acquireTokenForManagedIdentity with the same parameters in two different ManagedIdentityApplications
        // should return the same token
        assertEquals(resultMiApp1.accessToken(), resultMiApp2.accessToken());
    }
}
