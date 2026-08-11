// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.microsoft.aad.msal4j.ManagedIdentitySourceType.*;
import static com.microsoft.aad.msal4j.MsalError.MANAGED_IDENTITY_FILE_READ_ERROR;
import static com.microsoft.aad.msal4j.MsalError.MANAGED_IDENTITY_REQUEST_FAILED;
import static com.microsoft.aad.msal4j.MsalErrorMessage.*;
import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ManagedIdentityTests {

    private static final String EXPECTED_SKU = "MSAL.Java";
    private static final String TEST_CORRELATION_ID = "00000000-0000-0000-0000-000000000001";

    @BeforeAll
    static void setupRetryPolicies() {
        // Set retry delays to 1ms for faster test execution
        ManagedIdentityRetryPolicy.setRetryDelayMs(1);
        IMDSRetryPolicy.setRetryDelayMs(1);
    }

    @AfterAll
    static void resetRetryPolicies() {
        // Reset retry delays to default values
        ManagedIdentityRetryPolicy.resetToDefaults();
        IMDSRetryPolicy.resetToDefaults();
    }

    @AfterAll
    static void resetServiceFabricHttpClient() {
        ServiceFabricManagedIdentitySource.resetHttpClient();
    }

    private String getSuccessfulResponse(String resource) {
        long expiresOn = (System.currentTimeMillis() / 1000) + (24 * 3600);//A long-lived, 24 hour token
        return "{\"access_token\":\"accesstoken\",\"expires_on\":\"" + expiresOn + "\",\"resource\":\"" + resource + "\",\"token_type\":" +
                "\"Bearer\",\"client_id\":\"client_id\"}";
    }

    private String getSuccessfulResponseWithISOExpiresOn(String resource) {
        String expiresOn = DateTimeFormatter.ISO_INSTANT.format(Instant.now().plus(24, ChronoUnit.HOURS));//A long-lived, 24 hour token
        return "{\"access_token\":\"accesstoken\",\"expires_on\":\"" + expiresOn + "\",\"resource\":\"" + resource + "\",\"token_type\":" +
                "\"Bearer\",\"client_id\":\"client_id\"}";
    }

    private HttpRequest expectedRequest(ManagedIdentitySourceType source, String resource) {
        return expectedRequest(source, resource, ManagedIdentityId.systemAssigned(), false, false, null);
    }

    private HttpRequest expectedRequest(ManagedIdentitySourceType source, String resource, ManagedIdentityId id) {
        return expectedRequest(source, resource, id, false, false, null);
    }

    private HttpRequest expectedRequest(ManagedIdentitySourceType source, String resource,
                                        boolean hasClaims, boolean hasCapabilities, String expectedTokenHash) {
        return expectedRequest(source, resource, ManagedIdentityId.systemAssigned(), hasClaims, hasCapabilities, expectedTokenHash);
    }

    private HttpRequest expectedRequest(ManagedIdentitySourceType source, String resource,
                                        ManagedIdentityId id, boolean hasClaims, boolean hasCapabilities, String expectedTokenHash) {
        // Create maps for headers and query parameters
        Map<String, String> headers = new HashMap<>();
        Map<String, String> queryParameters = new HashMap<>();

        // Add resource to query parameters (common for all sources)
        queryParameters.put("resource", resource);

        // Handle claims and capabilities if supported
        if (Constants.TOKEN_REVOCATION_SUPPORTED_ENVIRONMENTS.contains(source)) {
            if (hasCapabilities) {
                queryParameters.put(Constants.CLIENT_CAPABILITY_REQUEST_PARAM, "cp1");
            }
            if (hasClaims) {
                queryParameters.put(Constants.TOKEN_HASH_CLAIM, expectedTokenHash);
            }
        }

        // Configure source-specific parameters
        String endpoint = configureSourceSpecificParameters(source, headers, queryParameters);

        // Configure idType-specific parameters
        if (id.getIdType() != ManagedIdentityId.systemAssigned().getIdType()) {
            configureIdentitySpecificParameters(id, queryParameters);
        }

        if (!queryParameters.isEmpty()) {
            endpoint = endpoint + "?" + StringHelper.serializeQueryParameters(queryParameters);
        }

        return new HttpRequest(HttpMethod.GET, endpoint, headers);
    }

    private String configureSourceSpecificParameters(ManagedIdentitySourceType source,
                                                     Map<String, String> headers,
                                                     Map<String, String> queryParameters) {
        switch (source) {
            case APP_SERVICE:
                queryParameters.put("api-version", "2019-08-01");
                headers.put("X-IDENTITY-HEADER", "secret");
                return ManagedIdentityTestConstants.APP_SERVICE_ENDPOINT;

            case CLOUD_SHELL:
                headers.put("ContentType", "application/x-www-form-urlencoded");
                headers.put("Metadata", "true");
                return ManagedIdentityTestConstants.CLOUDSHELL_ENDPOINT;

            case AZURE_ARC:
                queryParameters.put("api-version", "2020-06-01");
                headers.put("Metadata", "true");
                return ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT;

            case SERVICE_FABRIC:
                queryParameters.put("api-version", "2019-07-01-preview");
                headers.put("secret", "secret");
                return ManagedIdentityTestConstants.SERVICE_FABRIC_ENDPOINT;

            case IMDS:
            case NONE:
            case DEFAULT_TO_IMDS:
            default:
                queryParameters.put("api-version", "2018-02-01");
                headers.put("Metadata", "true");
                headers.put("x-client-SKU", EXPECTED_SKU);
                headers.put("x-client-VER", HttpHeaders.PRODUCT_VERSION_HEADER_VALUE);
                headers.put("x-ms-client-request-id", TEST_CORRELATION_ID);
                return ManagedIdentityTestConstants.IMDS_ENDPOINT;
        }
    }

    private void configureIdentitySpecificParameters(ManagedIdentityId id, Map<String, String> queryParameters) {
        switch (id.getIdType()) {
            case SYSTEM_ASSIGNED:
                break;
            case CLIENT_ID:
                queryParameters.put("client_id", id.getUserAssignedId());
                break;
            case RESOURCE_ID:
                if (ManagedIdentityClient.getManagedIdentitySource() == ManagedIdentitySourceType.IMDS
                        || ManagedIdentityClient.getManagedIdentitySource() == ManagedIdentitySourceType.AZURE_ARC) {
                    queryParameters.put(Constants.MANAGED_IDENTITY_RESOURCE_ID_IMDS, id.getUserAssignedId());
                } else {
                    queryParameters.put(Constants.MANAGED_IDENTITY_RESOURCE_ID, id.getUserAssignedId());
                }
                break;
            case OBJECT_ID:
                queryParameters.put("object_id", id.getUserAssignedId());
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + id.getIdType());
        }
    }

    private HttpResponse expectedResponse(int statusCode, String response) {
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.statusCode(statusCode);
        httpResponse.body(response);

        return httpResponse;
    }

    abstract class BaseManagedIdentityTest {
        protected ManagedIdentityApplication miApp;
        protected DefaultHttpClient httpClientMock;
        protected IEnvironmentVariables environmentVariables;

        void setUpCommonTest(ManagedIdentitySourceType source, String endpoint, ManagedIdentityId idType) {
            initEnvironmentVariables(source, endpoint);
            initHttpClientMock(source);
            initManagedIdentityApplication(idType);
        }

        void initEnvironmentVariables(ManagedIdentitySourceType source, String endpoint) {
            environmentVariables = new EnvironmentVariablesHelper(source, endpoint);
            ManagedIdentityApplication.setEnvironmentVariables(environmentVariables);
        }

        void initHttpClientMock(ManagedIdentitySourceType source) {
            httpClientMock = mock(DefaultHttpClient.class);
            if (source == SERVICE_FABRIC) {
                ServiceFabricManagedIdentitySource.setHttpClient(httpClientMock);
            }
        }

        void initManagedIdentityApplication(ManagedIdentityId idType) {
            miApp = ManagedIdentityApplication
                    .builder(idType)
                    .httpClient(httpClientMock)
                    .correlationId(TEST_CORRELATION_ID)
                    .build();

            // ManagedIdentityApplication uses a static token cache, avoid cross test pollution by clearing it
            miApp.tokenCache().accessTokens.clear();
        }

        void setUpTestWithoutHttpClientMock(ManagedIdentitySourceType source, String endpoint) {
            initEnvironmentVariables(source, endpoint);

            miApp = ManagedIdentityApplication
                    .builder(ManagedIdentityId.systemAssigned())
                    .build();

            // ManagedIdentityApplication uses a static token cache, avoid cross test pollution by clearing it
            miApp.tokenCache().accessTokens.clear();
        }

        void assertTokenFromIdentityProvider(IAuthenticationResult result) {
            assertNotNull(result.accessToken());
            assertEquals(TokenSource.IDENTITY_PROVIDER, result.metadata().tokenSource());
        }

        void assertTokenFromCache(IAuthenticationResult result) {
            assertNotNull(result.accessToken());
            assertEquals(TokenSource.CACHE, result.metadata().tokenSource());
        }

        void assertMsalServiceException(CompletableFuture<IAuthenticationResult> future,
                                        ManagedIdentitySourceType expectedSource,
                                        String expectedErrorCode) {
            ExecutionException ex = assertThrows(ExecutionException.class, future::get);
            assertInstanceOf(MsalServiceException.class, ex.getCause());

            MsalServiceException msalException = (MsalServiceException) ex.getCause();
            assertEquals(expectedSource.name(), msalException.managedIdentitySource());
            assertEquals(expectedErrorCode, msalException.errorCode());
        }

        void assertMsalClientException(CompletableFuture<IAuthenticationResult> future,
                                       String expectedErrorCode) {
            ExecutionException ex = assertThrows(ExecutionException.class, future::get);
            assertInstanceOf(MsalClientException.class, ex.getCause());



            MsalClientException msalException = (MsalClientException) ex.getCause();
            assertEquals(expectedErrorCode, msalException.errorCode());
        }

        CompletableFuture<IAuthenticationResult> acquireTokenCommon(String resource) throws Exception {
            return miApp.acquireTokenForManagedIdentity(
                    ManagedIdentityParameters.builder(resource)
                            .build());
        }
    }

    @Nested
    class TokenAcquisitionAndCachingTests extends BaseManagedIdentityTest {

        @ParameterizedTest
        @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createData")
        void managedIdentityTest_SystemAssigned_SuccessfulResponse(ManagedIdentitySourceType source, String endpoint, String resource) throws Exception {
            setUpCommonTest(source, endpoint, ManagedIdentityId.systemAssigned());

            when(httpClientMock.send(any())).thenReturn(expectedResponse(HttpStatus.HTTP_OK, getSuccessfulResponse(resource)));

            IAuthenticationResult result = acquireTokenCommon(resource).get();

            assertTokenFromIdentityProvider(result);

            result = acquireTokenCommon(resource).get();

            assertTokenFromCache(result);
            verify(httpClientMock, times(1)).send(any());
        }

        @ParameterizedTest
        @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataUserAssigned")
        void managedIdentityTest_UserAssigned_SuccessfulResponse(ManagedIdentitySourceType source, String endpoint, ManagedIdentityId id) throws Exception {
            setUpCommonTest(source, endpoint, id);

            when(httpClientMock.send(expectedRequest(source, ManagedIdentityTestConstants.RESOURCE, id)))
                    .thenReturn(expectedResponse(HttpStatus.HTTP_OK, getSuccessfulResponse(ManagedIdentityTestConstants.RESOURCE)));

            IAuthenticationResult result = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();

            assertTokenFromIdentityProvider(result);
            verify(httpClientMock, times(1)).send(any());
        }

        @ParameterizedTest
        @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataError")
        void managedIdentity_SharedCache(ManagedIdentitySourceType source, String endpoint) throws Exception {
            setUpCommonTest(source, endpoint, ManagedIdentityId.systemAssigned());

            when(httpClientMock.send(any())).thenReturn(expectedResponse(HttpStatus.HTTP_OK, getSuccessfulResponse(ManagedIdentityTestConstants.RESOURCE)));

            ManagedIdentityApplication miApp2 = ManagedIdentityApplication
                    .builder(ManagedIdentityId.systemAssigned())
                    .httpClient(httpClientMock)
                    .build();

            IAuthenticationResult resultMiApp1 = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();

            assertTokenFromIdentityProvider(resultMiApp1);

            IAuthenticationResult resultMiApp2 = miApp2.acquireTokenForManagedIdentity(
                    ManagedIdentityParameters.builder(ManagedIdentityTestConstants.RESOURCE)
                            .build()).get();

            assertTokenFromCache(resultMiApp2);

            //acquireTokenForManagedIdentity does a cache lookup by default, and all ManagedIdentityApplication's share a cache,
            // so calling acquireTokenForManagedIdentity with the same parameters in two different ManagedIdentityApplications
            // should return the same token
            assertEquals(resultMiApp1.accessToken(), resultMiApp2.accessToken());
            verify(httpClientMock, times(1)).send(any());
        }

        @ParameterizedTest
        @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createData")
        void managedIdentityTest_DifferentScopes_RequestsNewToken(ManagedIdentitySourceType source, String endpoint) throws Exception {
            setUpCommonTest(source, endpoint, ManagedIdentityId.systemAssigned());

            when(httpClientMock.send(any())).thenReturn(expectedResponse(HttpStatus.HTTP_OK, getSuccessfulResponse(ManagedIdentityTestConstants.RESOURCE)));

            String anotherResource = "https://graph.microsoft.com";

            when(httpClientMock.send(expectedRequest(source, anotherResource))).thenReturn(expectedResponse(HttpStatus.HTTP_OK, getSuccessfulResponse(ManagedIdentityTestConstants.RESOURCE)));

            IAuthenticationResult result = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();

            assertTokenFromIdentityProvider(result);

            result = acquireTokenCommon(anotherResource).get();

            assertTokenFromIdentityProvider(result);
            verify(httpClientMock, times(2)).send(any());
        }
    }

    @Nested
    class ManagedIdentityBehaviorTests extends BaseManagedIdentityTest {
        //Tests covering specific behavior/scenarios/use cases/etc. for Managed Identity flows

        @ParameterizedTest
        @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataError")
        void managedIdentityTest_WithClaims(ManagedIdentitySourceType source, String endpoint) throws Exception {
            setUpCommonTest(source, endpoint, ManagedIdentityId.systemAssigned());

            when(httpClientMock.send(any())).thenReturn(expectedResponse(HttpStatus.HTTP_OK, getSuccessfulResponse(ManagedIdentityTestConstants.RESOURCE)));

            // First call, get the token from the identity provider.
            IAuthenticationResult result = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();

            assertTokenFromIdentityProvider(result);

            // Second call, get the token from the cache without passing the claims.
            result = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();

            assertTokenFromCache(result);

            String expectedTokenHash = StringHelper.createSha256HashHexString(result.accessToken());
            when(httpClientMock.send(expectedRequest(source, ManagedIdentityTestConstants.RESOURCE, true, false, expectedTokenHash)))
                    .thenReturn(expectedResponse(HttpStatus.HTTP_OK, getSuccessfulResponse(ManagedIdentityTestConstants.RESOURCE)));

            // Third call, when claims are passed bypass the cache.
            result = miApp.acquireTokenForManagedIdentity(
                    ManagedIdentityParameters.builder(ManagedIdentityTestConstants.RESOURCE)
                            .claims(TestConfiguration.CLAIMS_REQUEST)
                            .build()).get();

            assertTokenFromIdentityProvider(result);

            verify(httpClientMock, times(2)).send(any());
        }

        @ParameterizedTest
        @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataError")
        void managedIdentityTest_WithCapabilitiesOnly(ManagedIdentitySourceType source, String endpoint) throws Exception {
            initEnvironmentVariables(source, endpoint);
            initHttpClientMock(source);

            when(httpClientMock.send(expectedRequest(source, ManagedIdentityTestConstants.RESOURCE, false, true, null)))
                    .thenReturn(expectedResponse(HttpStatus.HTTP_OK, getSuccessfulResponse(ManagedIdentityTestConstants.RESOURCE)));

            miApp = ManagedIdentityApplication
                    .builder(ManagedIdentityId.systemAssigned())
                    .httpClient(httpClientMock)
                    .clientCapabilities(singletonList("cp1"))
                    .correlationId(TEST_CORRELATION_ID)
                    .build();

            miApp.tokenCache.accessTokens.clear();

            // First call, get the token from the identity provider.
            IAuthenticationResult result = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();

            assertTokenFromIdentityProvider(result);

            // Second call, get the token from the cache without passing the claims.
            result = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();

            assertTokenFromCache(result);

            verify(httpClientMock, times(1)).send(any());
        }

        @ParameterizedTest
        @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataError")
        void managedIdentity_ClaimsAndCapabilities(ManagedIdentitySourceType source, String endpoint) throws Exception {
            setUpCommonTest(source, endpoint, ManagedIdentityId.systemAssigned());

            when(httpClientMock.send(expectedRequest(source, ManagedIdentityTestConstants.RESOURCE, false, true, null)))
                    .thenReturn(expectedResponse(HttpStatus.HTTP_OK, getSuccessfulResponse(ManagedIdentityTestConstants.RESOURCE)));

            miApp = ManagedIdentityApplication
                    .builder(ManagedIdentityId.systemAssigned())
                    .clientCapabilities(singletonList("cp1"))
                    .httpClient(httpClientMock)
                    .correlationId(TEST_CORRELATION_ID)
                    .build();

            // First call, get the token from the identity provider.
            IAuthenticationResult result = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();

            assertTokenFromIdentityProvider(result);

            // Second call, get the token from the cache without passing the claims.
            result = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();

            assertTokenFromCache(result);

            String expectedTokenHash = StringHelper.createSha256HashHexString(result.accessToken());
            when(httpClientMock.send(expectedRequest(source, ManagedIdentityTestConstants.RESOURCE, true, true, expectedTokenHash)))
                    .thenReturn(expectedResponse(HttpStatus.HTTP_OK, getSuccessfulResponse(ManagedIdentityTestConstants.RESOURCE)));

            // Third call, when claims are passed bypass the cache.
            result = miApp.acquireTokenForManagedIdentity(
                    ManagedIdentityParameters.builder(ManagedIdentityTestConstants.RESOURCE)
                            .claims(TestConfiguration.CLAIMS_REQUEST)
                            .build()).get();

            assertTokenFromIdentityProvider(result);
        }

        @ParameterizedTest
        @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataGetSource")
        void managedIdentity_GetManagedIdentitySource(ManagedIdentitySourceType source, String endpoint, ManagedIdentitySourceType expectedSource) {
            setUpTestWithoutHttpClientMock(source, endpoint);

            ManagedIdentitySourceType miClientSourceType = ManagedIdentityClient.getManagedIdentitySource();
            ManagedIdentitySourceType miAppSourceType = ManagedIdentityApplication.getManagedIdentitySource();
            assertEquals(expectedSource, miClientSourceType);
            assertEquals(expectedSource, miAppSourceType);
        }

        @Test
        void managedIdentityTest_RefreshOnHalfOfExpiresOn() throws Exception {
            //All managed identity flows use the same AcquireTokenByManagedIdentitySupplier where refreshOn is set,
            //  so any of the MI options should let us verify that it's being set correctly
            setUpCommonTest(APP_SERVICE, ManagedIdentityTestConstants.APP_SERVICE_ENDPOINT, ManagedIdentityId.systemAssigned());

            when(httpClientMock.send(expectedRequest(APP_SERVICE, ManagedIdentityTestConstants.RESOURCE))).thenReturn(expectedResponse(HttpStatus.HTTP_OK, getSuccessfulResponse(ManagedIdentityTestConstants.RESOURCE)));

            AuthenticationResult result = (AuthenticationResult) acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();

            long timestampSeconds = (System.currentTimeMillis() / 1000);
            long expectedRefreshIn = result.refreshOn() - timestampSeconds;
            long actualRefreshIn = (result.expiresOn() - timestampSeconds)/2;

            assertTokenFromIdentityProvider(result);
            //Allow a few seconds of difference to account for execution time
            assertTrue((actualRefreshIn - expectedRefreshIn) <= 5);

            verify(httpClientMock, times(1)).send(any());
        }

        @Test
        void managedIdentityTest_ISOExpiresOn() throws Exception {
            //All managed identity flows use the same AcquireTokenByManagedIdentitySupplier where refreshOn is set,
            //  so any of the MI options should let us verify that it's being set correctly
            setUpCommonTest(APP_SERVICE, ManagedIdentityTestConstants.APP_SERVICE_ENDPOINT, ManagedIdentityId.systemAssigned());

            when(httpClientMock.send(expectedRequest(ManagedIdentitySourceType.APP_SERVICE, ManagedIdentityTestConstants.RESOURCE))).thenReturn(expectedResponse(HttpStatus.HTTP_OK, getSuccessfulResponseWithISOExpiresOn(ManagedIdentityTestConstants.RESOURCE)));

            AuthenticationResult result = (AuthenticationResult) miApp.acquireTokenForManagedIdentity(
                    ManagedIdentityParameters.builder(ManagedIdentityTestConstants.RESOURCE)
                            .build()).get();

            // Calculate what the expected expiration time should be
            long expectedExpiresOn = System.currentTimeMillis() / 1000 + (24 * 3600); // 24 hours from now, used in getSuccessfulResponseWithISOExpiresOn

            assertTokenFromIdentityProvider(result);
            //Allow a few seconds of difference to account for execution time
            assertTrue((result.expiresOn() - expectedExpiresOn) <= 5);

            verify(httpClientMock, times(1)).send(any());
        }
    }

    @Nested
    class ErrorHandlingTests extends BaseManagedIdentityTest {

        @ParameterizedTest
        @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createData")
        void managedIdentityTest_SuccessfulResponse_WithInvalidJson(ManagedIdentitySourceType source, String endpoint, String resource) throws Exception {
            setUpCommonTest(source, endpoint, ManagedIdentityId.systemAssigned());

            when(httpClientMock.send(expectedRequest(source, resource))).thenReturn(expectedResponse(HttpStatus.HTTP_OK, ManagedIdentityTestConstants.RESPONSE_MALFORMED_JSON));

            assertMsalServiceException(acquireTokenCommon(resource), source, MsalError.MANAGED_IDENTITY_RESPONSE_PARSE_FAILURE);
        }

        @ParameterizedTest
        @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataUserAssignedNotSupported")
        void managedIdentityTest_UserAssigned_NotSupported(ManagedIdentitySourceType source, String endpoint, ManagedIdentityId id) throws Exception {
            setUpCommonTest(source, endpoint, id);

            assertMsalServiceException(acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE), source, MsalError.USER_ASSIGNED_MANAGED_IDENTITY_NOT_SUPPORTED);
        }

        @ParameterizedTest
        @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataWrongScope")
        void managedIdentityTest_WrongScopes(ManagedIdentitySourceType source, String endpoint, String resource) throws Exception {
            setUpCommonTest(source, endpoint, ManagedIdentityId.systemAssigned());

            if (environmentVariables.getEnvironmentVariable("SourceType").equals(CLOUD_SHELL.toString())) {
                when(httpClientMock.send(expectedRequest(source, resource))).thenReturn(expectedResponse(HttpStatus.HTTP_INTERNAL_ERROR, ManagedIdentityTestConstants.CLOUDSHELL_ERROR_RESPONSE));
            } else {
                when(httpClientMock.send(expectedRequest(source, resource))).thenReturn(expectedResponse(HttpStatus.HTTP_INTERNAL_ERROR, ManagedIdentityTestConstants.MSI_ERROR_RESPONSE_500));
            }

            assertMsalServiceException(acquireTokenCommon(resource), source, MsalError.MANAGED_IDENTITY_REQUEST_FAILED);
        }

        @ParameterizedTest
        @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataWrongScope")
        void managedIdentityTest_Retry(ManagedIdentitySourceType source, String endpoint, String resource) throws Exception {
            IEnvironmentVariables environmentVariables = new EnvironmentVariablesHelper(source, endpoint);
            ManagedIdentityApplication.setEnvironmentVariables(environmentVariables);

            DefaultHttpClientManagedIdentity httpClientMock = mock(DefaultHttpClientManagedIdentity.class);
            if (source == SERVICE_FABRIC) {
                ServiceFabricManagedIdentitySource.setHttpClient(httpClientMock);
            }

            miApp = ManagedIdentityApplication
                    .builder(ManagedIdentityId.systemAssigned())
                    .httpClient(httpClientMock)
                    .correlationId(TEST_CORRELATION_ID)
                    .build();

            //Several specific 4xx and 5xx errors, such as 500, should trigger MSAL's retry logic
            when(httpClientMock.send(expectedRequest(source, resource))).thenReturn(expectedResponse(HttpStatus.HTTP_INTERNAL_ERROR, ManagedIdentityTestConstants.MSI_ERROR_RESPONSE_500));

            try {
                acquireTokenCommon(resource).get();
            } catch (Exception exception) {
                assert(exception.getCause() instanceof MsalServiceException);

                //There should be three retries for certain MSI error codes, so there will be four invocations of
                // HttpClient's send method: the original call, and the three retries
                verify(httpClientMock, times(4)).send(any());
            }

            clearInvocations(httpClientMock);
            //Status codes that aren't on the list, such as 123, should not cause a retry
            when(httpClientMock.send(expectedRequest(source, resource))).thenReturn(expectedResponse(123, ManagedIdentityTestConstants.MSI_ERROR_RESPONSE_NORETRY));

            try {
                acquireTokenCommon(resource).get();
            } catch (Exception exception) {
                assert(exception.getCause() instanceof MsalServiceException);

                //Because there was no retry, there should only be one invocation of HttpClient's send method
                verify(httpClientMock, times(1)).send(any());

                return;
            }

            fail("MsalServiceException is expected but not thrown.");
        }

        @ParameterizedTest
        @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataWrongScope")
        void managedIdentityTest_RetriesDisabled(ManagedIdentitySourceType source, String endpoint, String resource) throws Exception {
            IEnvironmentVariables environmentVariables = new EnvironmentVariablesHelper(source, endpoint);
            ManagedIdentityApplication.setEnvironmentVariables(environmentVariables);

            DefaultHttpClientManagedIdentity httpClientMock = mock(DefaultHttpClientManagedIdentity.class);
            if (source == SERVICE_FABRIC) {
                ServiceFabricManagedIdentitySource.setHttpClient(httpClientMock);
            }

            miApp = ManagedIdentityApplication
                    .builder(ManagedIdentityId.systemAssigned())
                    .httpClient(httpClientMock)
                    .disableInternalRetries()
                    .correlationId(TEST_CORRELATION_ID)
                    .build();

            //Several specific 4xx and 5xx errors, such as 500, should trigger MSAL's retry logic
            when(httpClientMock.send(expectedRequest(source, resource))).thenReturn(expectedResponse(500, ManagedIdentityTestConstants.MSI_ERROR_RESPONSE_500));

            try {
                acquireTokenCommon(resource).get();
            } catch (Exception exception) {
                assert(exception.getCause() instanceof MsalServiceException);

                //But because retries are disabled at the app level, there should be no retries for any source except Service Fabric, which uses its own HttpClient
                if (source == SERVICE_FABRIC) {
                    verify(httpClientMock, times(4)).send(any());
                } else {
                    verify(httpClientMock, times(1)).send(any());
                }
            }
        }

        @Test
        void managedIdentityTest_IMDSRetry() throws Exception {
            IEnvironmentVariables environmentVariables = new EnvironmentVariablesHelper(IMDS, ManagedIdentityTestConstants.IMDS_ENDPOINT);
            ManagedIdentityApplication.setEnvironmentVariables(environmentVariables);

            DefaultHttpClientManagedIdentity httpClientMock = mock(DefaultHttpClientManagedIdentity.class);

            miApp = ManagedIdentityApplication
                    .builder(ManagedIdentityId.systemAssigned())
                    .httpClient(httpClientMock)
                    .correlationId(TEST_CORRELATION_ID)
                    .build();

            // IMDS has different retry logic for certain status codes, such as 410
            when(httpClientMock.send(expectedRequest(IMDS, ManagedIdentityTestConstants.RESOURCE))).thenReturn(expectedResponse(HttpStatus.HTTP_GONE, ManagedIdentityTestConstants.MSI_ERROR_RESPONSE_500));

            try {
                acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();
            } catch (Exception exception) {
                assert(exception.getCause() instanceof MsalServiceException);

                //A 410 status code should trigger the linear retry policy, with a total of 8 attempts (1 original + 7 retries)
                verify(httpClientMock, times(8)).send(any());
            }

            clearInvocations(httpClientMock);
            when(httpClientMock.send(expectedRequest(IMDS, ManagedIdentityTestConstants.RESOURCE))).thenReturn(expectedResponse(HttpStatus.HTTP_INTERNAL_ERROR, ManagedIdentityTestConstants.MSI_ERROR_RESPONSE_500));

            try {
                acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();
            } catch (Exception exception) {
                assert(exception.getCause() instanceof MsalServiceException);

                //A 500 status code should trigger the exponential retry policy, with a total of 4 attempts (1 original + 3 retries)
                verify(httpClientMock, times(4)).send(any());

                return;
            }

            fail("MsalServiceException is expected but not thrown.");
        }

        @Test
        void managedIdentityTest_RetrySucceedsAfterFailure() throws Exception {
            IEnvironmentVariables environmentVariables = new EnvironmentVariablesHelper(IMDS, ManagedIdentityTestConstants.IMDS_ENDPOINT);
            ManagedIdentityApplication.setEnvironmentVariables(environmentVariables);

            DefaultHttpClientManagedIdentity httpClientMock = mock(DefaultHttpClientManagedIdentity.class);

            miApp = ManagedIdentityApplication
                    .builder(ManagedIdentityId.systemAssigned())
                    .httpClient(httpClientMock)
                    .correlationId(TEST_CORRELATION_ID)
                    .build();

            // First call returns 500, subsequent calls return 200
            when(httpClientMock.send(expectedRequest(IMDS, ManagedIdentityTestConstants.RESOURCE)))
                    .thenReturn(expectedResponse(HttpStatus.HTTP_INTERNAL_ERROR, ManagedIdentityTestConstants.MSI_ERROR_RESPONSE_500))
                    .thenReturn(expectedResponse(HttpStatus.HTTP_OK, getSuccessfulResponse(ManagedIdentityTestConstants.RESOURCE)));

            IAuthenticationResult result = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();

            assertTokenFromIdentityProvider(result);
            assertNotNull(result.accessToken());

            // Verify that the client was called exactly twice (first attempt + one retry)
            ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(httpClientMock, times(2)).send(captor.capture());

            // Verify IMDS client metadata headers on the captured request
            HttpRequest capturedRequest = captor.getValue();
            assertEquals(EXPECTED_SKU, capturedRequest.headers().get("x-client-SKU"));
            assertNotNull(capturedRequest.headers().get("x-client-VER"));
            assertEquals(TEST_CORRELATION_ID, capturedRequest.headers().get("x-ms-client-request-id"));
            assertDoesNotThrow(() -> UUID.fromString(capturedRequest.headers().get("x-ms-client-request-id")));
        }

        @Test
        void managedIdentityTest_NonRetryableError() throws Exception {
            IEnvironmentVariables environmentVariables = new EnvironmentVariablesHelper(IMDS, ManagedIdentityTestConstants.IMDS_ENDPOINT);
            ManagedIdentityApplication.setEnvironmentVariables(environmentVariables);

            DefaultHttpClientManagedIdentity httpClientMock = mock(DefaultHttpClientManagedIdentity.class);

            miApp = ManagedIdentityApplication
                    .builder(ManagedIdentityId.systemAssigned())
                    .httpClient(httpClientMock)
                    .correlationId(TEST_CORRELATION_ID)
                    .build();

            // Use a status code that doesn't trigger retries (499)
            when(httpClientMock.send(expectedRequest(IMDS, ManagedIdentityTestConstants.RESOURCE)))
                    .thenReturn(expectedResponse(499, ManagedIdentityTestConstants.MSI_ERROR_RESPONSE_NORETRY));

            CompletableFuture<IAuthenticationResult> future = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE);

            // Verify that an exception is thrown
            ExecutionException ex = assertThrows(ExecutionException.class, future::get);
            assertInstanceOf(MsalServiceException.class, ex.getCause());

            MsalServiceException msalException = (MsalServiceException) ex.getCause();
            assertEquals(MsalError.MANAGED_IDENTITY_REQUEST_FAILED, msalException.errorCode());

            // Verify the client was called exactly once (no retries attempted)
            verify(httpClientMock, times(1)).send(any());
        }

        @Test
        void managedIdentityTest_RetriesARePerRequest() throws Exception {
            IEnvironmentVariables environmentVariables = new EnvironmentVariablesHelper(IMDS, ManagedIdentityTestConstants.IMDS_ENDPOINT);
            ManagedIdentityApplication.setEnvironmentVariables(environmentVariables);

            DefaultHttpClientManagedIdentity httpClientMock = mock(DefaultHttpClientManagedIdentity.class);

            miApp = ManagedIdentityApplication
                    .builder(ManagedIdentityId.systemAssigned())
                    .httpClient(httpClientMock)
                    .correlationId(TEST_CORRELATION_ID)
                    .build();

            // First call returns 500, subsequent calls return 200
            when(httpClientMock.send(expectedRequest(IMDS, ManagedIdentityTestConstants.RESOURCE)))
                    .thenReturn(expectedResponse(HttpStatus.HTTP_INTERNAL_ERROR, ManagedIdentityTestConstants.MSI_ERROR_RESPONSE_500))
                    .thenReturn(expectedResponse(HttpStatus.HTTP_OK, getSuccessfulResponse(ManagedIdentityTestConstants.RESOURCE)));

            IAuthenticationResult result = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();

            assertTokenFromIdentityProvider(result);
            assertNotNull(result.accessToken());

            // Verify that the client was called exactly twice (first attempt + one retry)
            verify(httpClientMock, times(2)).send(any());

            //All calls return 500
            when(httpClientMock.send(expectedRequest(IMDS, "otherResource")))
                    .thenReturn(expectedResponse(HttpStatus.HTTP_INTERNAL_ERROR, ManagedIdentityTestConstants.MSI_ERROR_RESPONSE_500));

            CompletableFuture<IAuthenticationResult> future = acquireTokenCommon("otherResource");

            // Verify that an exception is thrown
            ExecutionException ex = assertThrows(ExecutionException.class, future::get);
            assertInstanceOf(MsalServiceException.class, ex.getCause());

            MsalServiceException msalException = (MsalServiceException) ex.getCause();
            assertEquals(MsalError.MANAGED_IDENTITY_REQUEST_FAILED, msalException.errorCode());

            // Verify that the client was called four more times (new first attempt + three new retries)
            verify(httpClientMock, times(6)).send(any());
        }

        @ParameterizedTest
        @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataError")
        void managedIdentity_RequestFailed_NoPayload(ManagedIdentitySourceType source, String endpoint) throws Exception {
            setUpCommonTest(source, endpoint, ManagedIdentityId.systemAssigned());

            when(httpClientMock.send(expectedRequest(source, ManagedIdentityTestConstants.RESOURCE))).thenReturn(expectedResponse(500, ""));

            assertMsalServiceException(acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE), source, MsalError.MANAGED_IDENTITY_REQUEST_FAILED);
        }

        @ParameterizedTest
        @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataError")
        void managedIdentity_RequestFailed_NullResponse(ManagedIdentitySourceType source, String endpoint) throws Exception {
            setUpCommonTest(source, endpoint, ManagedIdentityId.systemAssigned());

            when(httpClientMock.send(expectedRequest(source, ManagedIdentityTestConstants.RESOURCE))).thenReturn(expectedResponse(HttpStatus.HTTP_OK, ""));

            assertMsalServiceException(acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE), source, MsalError.MANAGED_IDENTITY_REQUEST_FAILED);

            verify(httpClientMock, times(1)).send(any());
        }

        @ParameterizedTest
        @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createDataError")
        void managedIdentity_RequestFailed_UnreachableNetwork(ManagedIdentitySourceType source, String endpoint) throws Exception {
            setUpCommonTest(source, endpoint, ManagedIdentityId.systemAssigned());

            when(httpClientMock.send(expectedRequest(source, ManagedIdentityTestConstants.RESOURCE))).thenThrow(new SocketException("A socket operation was attempted to an unreachable network."));

            assertMsalServiceException(acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE), source, MsalError.MANAGED_IDENTITY_UNREACHABLE_NETWORK);

            verify(httpClientMock, times(1)).send(any());
        }

        @ParameterizedTest
        @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createInvalidClaimsData")
        void managedIdentity_InvalidClaims(String claimsJson) throws Exception {
            setUpCommonTest(APP_SERVICE, ManagedIdentityTestConstants.APP_SERVICE_ENDPOINT, ManagedIdentityId.systemAssigned());

            CompletableFuture<IAuthenticationResult> future = miApp.acquireTokenForManagedIdentity(
                    ManagedIdentityParameters.builder(ManagedIdentityTestConstants.RESOURCE)
                            .claims(claimsJson)
                            .build());

            assertMsalClientException(future, AuthenticationErrorCode.INVALID_JSON);

            // Verify no HTTP requests were made for invalid claims
            verify(httpClientMock, never()).send(any());
        }

        @Test
        void managedIdentityTest_WithEmptyClaims() throws Exception {
            setUpCommonTest(APP_SERVICE, ManagedIdentityTestConstants.APP_SERVICE_ENDPOINT, ManagedIdentityId.systemAssigned());

            try {
                miApp.acquireTokenForManagedIdentity(
                        ManagedIdentityParameters.builder(ManagedIdentityTestConstants.RESOURCE)
                                .claims("")
                                .build());
            } catch (Exception exception) {
                assert(exception instanceof IllegalArgumentException);
            }

            try {
                miApp.acquireTokenForManagedIdentity(
                        ManagedIdentityParameters.builder(ManagedIdentityTestConstants.RESOURCE)
                                .claims(null)
                                .build());
            } catch (Exception exception) {
                assert(exception instanceof IllegalArgumentException);
            }

            // Verify no HTTP requests were made for invalid claims
            verify(httpClientMock, never()).send(any());
        }
    }

    @Nested
    class AzureArc extends BaseManagedIdentityTest{

        @Test
        void missingAuthHeader() throws Exception {
            mockHttpResponse(emptyMap());

            assertMsalServiceException(MANAGED_IDENTITY_REQUEST_FAILED, MANAGED_IDENTITY_NO_CHALLENGE_ERROR);
        }

        @ParameterizedTest
        @ValueSource(strings = {"WWW-Authenticate", "Www-Authenticate"})
        void invalidAuthHeader(String authHeaderKey) throws Exception {
            mockHttpResponse(singletonMap(authHeaderKey, singletonList("xyz")));

            assertMsalServiceException(MANAGED_IDENTITY_REQUEST_FAILED,
                    MANAGED_IDENTITY_INVALID_CHALLENGE);
        }

        @ParameterizedTest
        @ValueSource(strings = {"WWW-Authenticate", "Www-Authenticate"})
        void validPathWithMissingFile(String authHeaderKey)
                throws Exception {
            Path validPathWithMissingFile = Paths.get(
                    System.getenv("ProgramData") + "/AzureConnectedMachineAgent/Tokens/secret.key");

            mockHttpResponse(singletonMap(authHeaderKey, singletonList("Basic realm=" + validPathWithMissingFile)));

            assertMsalServiceException(MANAGED_IDENTITY_FILE_READ_ERROR,
                    MANAGED_IDENTITY_INVALID_FILEPATH);
        }

        @ParameterizedTest
        @ValueSource(strings = {"WWW-Authenticate", "Www-Authenticate"})
        void invalidPathWithRealFile(String authHeaderKey)
                throws Exception {
            Path invalidPathWithRealFile = Paths.get(
                    this.getClass().getResource("/msi-azure-arc-secret.txt").toURI());

            mockHttpResponse(singletonMap(authHeaderKey, singletonList("Basic realm=" + invalidPathWithRealFile)));

            assertMsalServiceException(MANAGED_IDENTITY_FILE_READ_ERROR,
                    MANAGED_IDENTITY_INVALID_FILEPATH);
        }

        @Test
        void userAssignedClientId_Honored() throws Exception {
            ManagedIdentityId id = ManagedIdentityId.userAssignedClientId(ManagedIdentityTestConstants.CLIENT_ID);
            setUpCommonTest(AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT, id);

            when(httpClientMock.send(expectedRequest(AZURE_ARC, ManagedIdentityTestConstants.RESOURCE, id)))
                    .thenReturn(expectedResponse(HttpStatus.HTTP_OK,
                            getSuccessfulResponseWithUserAssignedId(ManagedIdentityTestConstants.RESOURCE,
                                    "client_id", ManagedIdentityTestConstants.CLIENT_ID)));

            IAuthenticationResult result = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();

            assertTokenFromIdentityProvider(result);
            verify(httpClientMock, times(1)).send(any());
        }

        @Test
        void userAssignedObjectId_Honored() throws Exception {
            ManagedIdentityId id = ManagedIdentityId.userAssignedObjectId(ManagedIdentityTestConstants.OBJECT_ID);
            setUpCommonTest(AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT, id);

            when(httpClientMock.send(expectedRequest(AZURE_ARC, ManagedIdentityTestConstants.RESOURCE, id)))
                    .thenReturn(expectedResponse(HttpStatus.HTTP_OK,
                            getSuccessfulResponseWithUserAssignedId(ManagedIdentityTestConstants.RESOURCE,
                                    "object_id", ManagedIdentityTestConstants.OBJECT_ID)));

            IAuthenticationResult result = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();

            assertTokenFromIdentityProvider(result);
            verify(httpClientMock, times(1)).send(any());
        }

        @Test
        void userAssignedResourceId_ForwardsMsiResIdAndHonored() throws Exception {
            ManagedIdentityId id = ManagedIdentityId.userAssignedResourceId(ManagedIdentityTestConstants.RESOURCE_ID);
            setUpCommonTest(AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT, id);

            // expectedRequest asserts the resource id is forwarded as msi_res_id (not mi_res_id) for Azure Arc.
            when(httpClientMock.send(expectedRequest(AZURE_ARC, ManagedIdentityTestConstants.RESOURCE, id)))
                    .thenReturn(expectedResponse(HttpStatus.HTTP_OK,
                            getSuccessfulResponseWithUserAssignedId(ManagedIdentityTestConstants.RESOURCE,
                                    Constants.MANAGED_IDENTITY_RESOURCE_ID_IMDS, ManagedIdentityTestConstants.RESOURCE_ID)));

            IAuthenticationResult result = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();

            assertTokenFromIdentityProvider(result);
            verify(httpClientMock, times(1)).send(any());
        }

        @Test
        void userAssigned_NotConfirmed_FailsClosed() throws Exception {
            ManagedIdentityId id = ManagedIdentityId.userAssignedClientId(ManagedIdentityTestConstants.CLIENT_ID);
            setUpCommonTest(AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT, id);

            // A legacy Azure Arc agent ignores the selector and returns the system-assigned identity, so the
            // response does not echo the requested client_id. MSAL must fail closed rather than hand back a
            // token that may belong to a different identity than the one requested.
            when(httpClientMock.send(expectedRequest(AZURE_ARC, ManagedIdentityTestConstants.RESOURCE, id)))
                    .thenReturn(expectedResponse(HttpStatus.HTTP_OK,
                            getSuccessfulResponse(ManagedIdentityTestConstants.RESOURCE)));

            CompletableFuture<IAuthenticationResult> future = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE);

            ExecutionException ex = assertThrows(ExecutionException.class, future::get);
            assertInstanceOf(MsalServiceException.class, ex.getCause());
            MsalServiceException msalException = (MsalServiceException) ex.getCause();
            assertEquals(AZURE_ARC.name(), msalException.managedIdentitySource());
            assertEquals(MsalError.USER_ASSIGNED_MANAGED_IDENTITY_NOT_CONFIRMED, msalException.errorCode());
        }

        @Test
        void userAssigned_MissingEcho_FailsClosed() throws Exception {
            ManagedIdentityId id = ManagedIdentityId.userAssignedClientId(ManagedIdentityTestConstants.CLIENT_ID);
            setUpCommonTest(AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT, id);

            // A legacy agent may return a token with no identity echo at all. MSAL cannot confirm the
            // requested identity, so it must fail closed.
            when(httpClientMock.send(expectedRequest(AZURE_ARC, ManagedIdentityTestConstants.RESOURCE, id)))
                    .thenReturn(expectedResponse(HttpStatus.HTTP_OK,
                            getSuccessfulResponseWithoutEcho(ManagedIdentityTestConstants.RESOURCE)));

            assertAcquireTokenFailsClosed();
        }

        @Test
        void userAssignedResourceId_AcceptsMiResIdAlias_Honored() throws Exception {
            ManagedIdentityId id = ManagedIdentityId.userAssignedResourceId(ManagedIdentityTestConstants.RESOURCE_ID);
            setUpCommonTest(AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT, id);

            // The response echoes the resource id only under the alternate "mi_res_id" spelling; accept it.
            when(httpClientMock.send(expectedRequest(AZURE_ARC, ManagedIdentityTestConstants.RESOURCE, id)))
                    .thenReturn(expectedResponse(HttpStatus.HTTP_OK,
                            getSuccessfulResponseWithUserAssignedId(ManagedIdentityTestConstants.RESOURCE,
                                    Constants.MANAGED_IDENTITY_RESOURCE_ID, ManagedIdentityTestConstants.RESOURCE_ID)));

            IAuthenticationResult result = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();

            assertTokenFromIdentityProvider(result);
        }

        @Test
        void userAssignedResourceId_ConflictingAliasesMsiResIdFirst_FailsClosed() throws Exception {
            assertConflictingResourceIdAliasesFailClosed(true);
        }

        @Test
        void userAssignedResourceId_ConflictingAliasesMiResIdFirst_FailsClosed() throws Exception {
            assertConflictingResourceIdAliasesFailClosed(false);
        }

        // The requested resource id matches only the mi_res_id alias while the canonical msi_res_id echoes a
        // different identity. A matching alias must not rescue a contradictory canonical echo, regardless of
        // JSON field order, so the response must fail closed.
        private void assertConflictingResourceIdAliasesFailClosed(boolean msiResIdFirst) throws Exception {
            ManagedIdentityId id = ManagedIdentityId.userAssignedResourceId(ManagedIdentityTestConstants.RESOURCE_ID);
            setUpCommonTest(AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT, id);

            String conflictingMsiResId =
                    "/subscriptions/other/resourcegroups/x/providers/Microsoft.ManagedIdentity/userAssignedIdentities/other";
            when(httpClientMock.send(expectedRequest(AZURE_ARC, ManagedIdentityTestConstants.RESOURCE, id)))
                    .thenReturn(expectedResponse(HttpStatus.HTTP_OK,
                            getSuccessfulResponseWithBothResourceIdAliases(ManagedIdentityTestConstants.RESOURCE,
                                    conflictingMsiResId, ManagedIdentityTestConstants.RESOURCE_ID, msiResIdFirst)));

            assertAcquireTokenFailsClosed();
        }

        @Test
        @EnabledOnOs(OS.WINDOWS)
        void userAssignedResourceId_ChallengeFlow_RetainsMsiResIdOnAuthenticatedRequest_Honored() throws Exception {
            ManagedIdentityId id = ManagedIdentityId.userAssignedResourceId(ManagedIdentityTestConstants.RESOURCE_ID);
            setUpCommonTest(AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT, id);

            Path secretFile = createArcSecretKeyFile();
            try {
                when(httpClientMock.send(any()))
                        .thenReturn(challengeResponse(secretFile))
                        .thenReturn(expectedResponse(HttpStatus.HTTP_OK,
                                getSuccessfulResponseWithUserAssignedId(ManagedIdentityTestConstants.RESOURCE,
                                        Constants.MANAGED_IDENTITY_RESOURCE_ID_IMDS,
                                        ManagedIdentityTestConstants.RESOURCE_ID)));

                IAuthenticationResult result = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE).get();
                assertTokenFromIdentityProvider(result);

                // The authenticated (second) request must still carry the selector, using the msi_res_id
                // spelling Azure Arc honors (not mi_res_id).
                ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
                verify(httpClientMock, times(2)).send(captor.capture());
                String authenticatedRequestUrl = captor.getAllValues().get(1).url().toString();
                assertTrue(authenticatedRequestUrl.contains("msi_res_id="),
                        "authenticated request should retain the msi_res_id selector");
                assertFalse(authenticatedRequestUrl.contains("mi_res_id="),
                        "authenticated request must not use the mi_res_id spelling");
            } finally {
                Files.deleteIfExists(secretFile);
            }
        }

        @Test
        @EnabledOnOs(OS.WINDOWS)
        void userAssigned_ChallengeFlow_MismatchedEcho_FailsClosed() throws Exception {
            ManagedIdentityId id = ManagedIdentityId.userAssignedClientId(ManagedIdentityTestConstants.CLIENT_ID);
            setUpCommonTest(AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT, id);

            Path secretFile = createArcSecretKeyFile();
            try {
                // The authenticated (second) response echoes a different client_id -> fail closed.
                when(httpClientMock.send(any()))
                        .thenReturn(challengeResponse(secretFile))
                        .thenReturn(expectedResponse(HttpStatus.HTTP_OK,
                                getSuccessfulResponse(ManagedIdentityTestConstants.RESOURCE)));

                assertAcquireTokenFailsClosed();
            } finally {
                Files.deleteIfExists(secretFile);
            }
        }

        private void assertAcquireTokenFailsClosed() throws Exception {
            CompletableFuture<IAuthenticationResult> future = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE);

            ExecutionException ex = assertThrows(ExecutionException.class, future::get);
            assertInstanceOf(MsalServiceException.class, ex.getCause());
            MsalServiceException msalException = (MsalServiceException) ex.getCause();
            assertEquals(AZURE_ARC.name(), msalException.managedIdentitySource());
            assertEquals(MsalError.USER_ASSIGNED_MANAGED_IDENTITY_NOT_CONFIRMED, msalException.errorCode());
        }

        private HttpResponse challengeResponse(Path secretFile) {
            HttpResponse response = new HttpResponse();
            response.statusCode(HttpStatus.HTTP_UNAUTHORIZED);
            response.headers().put("WWW-Authenticate", singletonList("Basic realm=" + secretFile));
            return response;
        }

        private Path createArcSecretKeyFile() throws IOException {
            Path tokensDir = Paths.get(System.getenv("ProgramData"), "AzureConnectedMachineAgent", "Tokens");
            Files.createDirectories(tokensDir);
            Path secretFile = tokensDir.resolve("msal4j-uami-test.key");
            Files.write(secretFile, "secret".getBytes(StandardCharsets.UTF_8));
            return secretFile;
        }

        private String getSuccessfulResponseWithoutEcho(String resource) {
            long expiresOn = (System.currentTimeMillis() / 1000) + (24 * 3600);
            return "{\"access_token\":\"accesstoken\",\"expires_on\":\"" + expiresOn + "\",\"resource\":\""
                    + resource + "\",\"token_type\":\"Bearer\"}";
        }

        private String getSuccessfulResponseWithBothResourceIdAliases(
                String resource, String msiResId, String miResId, boolean msiResIdFirst) {
            long expiresOn = (System.currentTimeMillis() / 1000) + (24 * 3600);
            String aliases = msiResIdFirst
                    ? "\"msi_res_id\":\"" + msiResId + "\",\"mi_res_id\":\"" + miResId + "\""
                    : "\"mi_res_id\":\"" + miResId + "\",\"msi_res_id\":\"" + msiResId + "\"";
            return "{\"access_token\":\"accesstoken\",\"expires_on\":\"" + expiresOn + "\",\"resource\":\""
                    + resource + "\",\"token_type\":\"Bearer\"," + aliases + "}";
        }

        private String getSuccessfulResponseWithUserAssignedId(String resource, String fieldName, String value) {
            long expiresOn = (System.currentTimeMillis() / 1000) + (24 * 3600);
            return "{\"access_token\":\"accesstoken\",\"expires_on\":\"" + expiresOn + "\",\"resource\":\""
                    + resource + "\",\"token_type\":\"Bearer\",\"" + fieldName + "\":\"" + value + "\"}";
        }

        private void mockHttpResponse(Map<String, ? extends List<String>> responseHeaders) throws Exception {
            setUpCommonTest(AZURE_ARC, ManagedIdentityTestConstants.AZURE_ARC_ENDPOINT, ManagedIdentityId.systemAssigned());

            HttpResponse response = new HttpResponse();
            response.statusCode(HttpStatus.HTTP_UNAUTHORIZED);
            response.headers().putAll(responseHeaders);

            when(httpClientMock.send(
                    expectedRequest(AZURE_ARC, ManagedIdentityTestConstants.RESOURCE))).thenReturn(
                    response);
        }

        private void assertMsalServiceException(String errorCode, String message) throws Exception {
            CompletableFuture<IAuthenticationResult> future = acquireTokenCommon(ManagedIdentityTestConstants.RESOURCE);

            ExecutionException ex = assertThrows(ExecutionException.class, future::get);
            assertInstanceOf(MsalServiceException.class, ex.getCause());
            MsalServiceException msalException = (MsalServiceException) ex.getCause();
            assertEquals(AZURE_ARC.name(),
                    msalException.managedIdentitySource());
            assertEquals(errorCode, msalException.errorCode());
            assertTrue(ex.getMessage().contains(message));
        }
    }
}
