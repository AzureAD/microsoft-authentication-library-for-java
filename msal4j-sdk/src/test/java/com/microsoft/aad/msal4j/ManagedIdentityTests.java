// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;


import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import org.apache.http.HttpStatus;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;

@PowerMockIgnore({"javax.net.ssl.*"})
@PrepareForTest({EnvironmentVariables.class})
public class ManagedIdentityTests extends PowerMockTestCase {

    final static String Resource = "https://management.azure.com";

    static ManagedIdentityParameters managedIdentityParameters = ManagedIdentityParameters.builder(Resource).forceRefresh(false).build();

    final static String ResourceDefaultSuffix = "https://management.azure.com/.default";
    final static String AppServiceEndpoint = "http://127.0.0.1:41564/msi/token";
    final static String IMDS_ENDPOINT = "http://169.254.169.254/metadata/identity/oauth2/token";
    final static String AzureArcEndpoint = "http://localhost:40342/metadata/identity/oauth2/token";
    final static String CloudShellEndpoint = "http://localhost:40342/metadata/identity/oauth2/token";
    final static String ServiceFabricEndpoint = "http://localhost:40342/metadata/identity/oauth2/token";

    ApacheHttpClientAdapter apacheHttpClientAdapter = new ApacheHttpClientAdapter();

    @DataProvider(name = "endpointScopeSource")
    public static Object[][] endpointScopeSource() {
        return new Object[][]{{AppServiceEndpoint, Resource, ManagedIdentitySourceType.AppService.toString()},
                {AppServiceEndpoint, Resource, ManagedIdentitySourceType.AppService.toString()},
                {AppServiceEndpoint, ResourceDefaultSuffix, ManagedIdentitySourceType.AppService.toString()},
                {IMDS_ENDPOINT, Resource, ManagedIdentitySourceType.Imds.toString()},
                {null, Resource, ManagedIdentitySourceType.Imds.toString()},
                {AzureArcEndpoint, Resource, ManagedIdentitySourceType.AzureArc.toString()},
                {AzureArcEndpoint, ResourceDefaultSuffix, ManagedIdentitySourceType.AzureArc.toString()},
                {CloudShellEndpoint, Resource, ManagedIdentitySourceType.CloudShell.toString()},
                {CloudShellEndpoint, ResourceDefaultSuffix, ManagedIdentitySourceType.CloudShell.toString()},
                {ServiceFabricEndpoint, Resource, ManagedIdentitySourceType.ServiceFabric.toString()},
                {ServiceFabricEndpoint, ResourceDefaultSuffix, ManagedIdentitySourceType.ServiceFabric.toString()}};
    }

    @Test(dataProvider = "endpointScopeSource")
    public void ValidNotOkHttpResponse(String endpoint, String scope, String sourceType) throws Exception {

        mockEnvironmentVariables(endpoint, sourceType);

        TokenRequestExecutor request = PowerMock.createPartialMock(
                TokenRequestExecutor.class, "createOauthHttpRequest");

        OAuthHttpRequest msalOAuthHttpRequest = PowerMock.createMock(OAuthHttpRequest.class);

        HTTPResponse httpResponse = new HTTPResponse(HTTPResponse.SC_OK);

        httpResponse.setContent("content");
        httpResponse.setContentType(HTTPContentType.ApplicationJSON.contentType);

        EasyMock.expect(request.createOauthHttpRequest()).andReturn(msalOAuthHttpRequest).times(1);
        EasyMock.expect(msalOAuthHttpRequest.send()).andReturn(httpResponse).times(1);

        PowerMock.replay(request, msalOAuthHttpRequest);

        ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication.builder(ManagedIdentityId.SystemAssigned())
                .httpClient(apacheHttpClientAdapter);

        ManagedIdentityApplication mi = miBuilder.build();

        ManagedIdentityParameters managedIdentityParameters1 = ManagedIdentityParameters.builder(scope).forceRefresh(false).build();

        try{
            IAuthenticationResult result = mi.acquireTokenForManagedIdentity(managedIdentityParameters1).get();
            Assert.assertNotNull(result);
            Assert.assertNotNull(result.accessToken());

        }catch(ExecutionException ex){
            System.out.println(ex.getMessage());
        }
    }
    private void mockEnvironmentVariables(String endpoint, String managedIdentitySource){
        PowerMock.mockStatic(EnvironmentVariables.class);
        String secret = "secret";
        if(managedIdentitySource.equals(ManagedIdentitySourceType.AppService.toString())){
            EasyMock.expect(
                    EnvironmentVariables.getIdentityEndpoint()).andReturn(endpoint).anyTimes();
            EasyMock.expect(
                    EnvironmentVariables.getIdentityHeader()).andReturn(secret).anyTimes();
            EasyMock.expect(
                    EnvironmentVariables.getIdentityServerThumbprint()).andReturn(null).anyTimes();
        }else if(managedIdentitySource.equals(ManagedIdentitySourceType.Imds.toString())) {
            EasyMock.expect(
                    EnvironmentVariables.getAzurePodIdentityAuthorityHost()).andReturn(endpoint).anyTimes();
        }else if(managedIdentitySource.equals(ManagedIdentitySourceType.AzureArc.toString())) {
            EasyMock.expect(
                    EnvironmentVariables.getIdentityEndpoint()).andReturn(endpoint).anyTimes();
            EasyMock.expect(
                    EnvironmentVariables.getImdsEndpoint()).andReturn("http://localhost:40342").anyTimes();
        }else if(managedIdentitySource.equals(ManagedIdentitySourceType.CloudShell.toString())){
            EasyMock.expect(
                    EnvironmentVariables.getMsiEndpoint()).andReturn(endpoint).anyTimes();
        }else if(managedIdentitySource.equals(ManagedIdentitySourceType.ServiceFabric.toString())){
            EasyMock.expect(
                    EnvironmentVariables.getIdentityEndpoint()).andReturn(endpoint).anyTimes();
            EasyMock.expect(
                    EnvironmentVariables.getIdentityHeader()).andReturn(secret).anyTimes();
            EasyMock.expect(
                    EnvironmentVariables.getIdentityServerThumbprint()).andReturn("thumbprint").anyTimes();
        }

        PowerMock.replay(EnvironmentVariables.class);
    }

    @DataProvider(name = "endpointSourceId")
    public static Object[][] endpointSourceId() {
        return new Object[][]
                {{AppServiceEndpoint, ManagedIdentitySourceType.AppService, TestConstants.CLIENT_ID, ManagedIdentityIdType.ClientId},
                        {AppServiceEndpoint, ManagedIdentitySourceType.AppService, TestConstants.MI_RESOURCE_ID, ManagedIdentityIdType.ResourceId},
                        {IMDS_ENDPOINT, ManagedIdentitySourceType.Imds, TestConstants.CLIENT_ID, ManagedIdentityIdType.ClientId},
                        {IMDS_ENDPOINT, ManagedIdentitySourceType.Imds, TestConstants.MI_RESOURCE_ID, ManagedIdentityIdType.ResourceId},
                        {ServiceFabricEndpoint, ManagedIdentitySourceType.ServiceFabric, TestConstants.CLIENT_ID, ManagedIdentityIdType.ClientId},
                        {ServiceFabricEndpoint, ManagedIdentitySourceType.ServiceFabric, TestConstants.MI_RESOURCE_ID, ManagedIdentityIdType.ResourceId}};
    }

    @Test(dataProvider = "endpointSourceId")
    public void managedIdentityUserAssignedHappyPathAsync(
            String endpoint,
            String managedIdentitySource,
            String userAssignedId,
            String userAssignedIdentityId) throws Exception {

        {
            mockEnvironmentVariables(managedIdentitySource, endpoint);

            ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication.builder(
                    userAssignedIdentityId.equals(ManagedIdentityIdType.ClientId.toString()) ?
                            ManagedIdentityId.UserAssignedClientId(userAssignedId) :
                            ManagedIdentityId.UserAssignedResourceId(userAssignedId))
                            .httpClient(apacheHttpClientAdapter);

            ManagedIdentityApplication mi = miBuilder.build();

//            httpManager.AddManagedIdentityMockHandler(
//                    endpoint,
//                    Resource,
//                    MockHelpers.GetMsiSuccessfulResponse(),
//                    managedIdentitySource,
//                    userAssignedClientIdOrResourceId: userAssignedId,
//                userAssignedIdentityId: userAssignedIdentityId);

            IAuthenticationResult result = mi.acquireTokenForManagedIdentity(managedIdentityParameters).get();

            Assert.assertNotNull(result);
            Assert.assertNotNull(result.accessToken());
//            Assert.assertEquals(TokenSource.IdentityProvider, result.AuthenticationResultMetadata.TokenSource);

            result = mi.acquireTokenForManagedIdentity(managedIdentityParameters)
                    .get();

            Assert.assertNotNull(result);
            Assert.assertNotNull(result.accessToken());
//            Assert.assertEquals(TokenSource.Cache, result.AuthenticationResultMetadata.TokenSource);
        }
    }

    @DataProvider(name = "endpointScopeSourceType")
    public static Object[][] endpointScopeSourceType() {
        return new Object[][]
                {{AppServiceEndpoint, Resource, "https://graph.microsoft.com", ManagedIdentitySourceType.AppService},
                        {IMDS_ENDPOINT, Resource, "https://graph.microsoft.com", ManagedIdentitySourceType.Imds},
                        {AzureArcEndpoint, Resource, "https://graph.microsoft.com", ManagedIdentitySourceType.AzureArc},
                        {CloudShellEndpoint, Resource, "https://graph.microsoft.com", ManagedIdentitySourceType.CloudShell},
                        {ServiceFabricEndpoint, Resource, "https://graph.microsoft.com", ManagedIdentitySourceType.ServiceFabric}};
    }

    @Test(dataProvider = "endpointScopeSourceType")
    public void ManagedIdentityDifferentScopesTestAsync(
            String endpoint,
            String scope,
            String anotherScope,
            String managedIdentitySource) throws Exception {

        {
            mockEnvironmentVariables(managedIdentitySource, endpoint);

            ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication.builder(ManagedIdentityId.SystemAssigned());
            // .httpClient();

            // Disabling shared cache options to avoid cross test pollution.
            // miBuilder.Config.AccessorOptions = null;

            ManagedIdentityApplication mi = miBuilder.build();

//            httpManager.AddManagedIdentityMockHandler(
//                    endpoint,
//                    Resource,
//                    MockHelpers.GetMsiSuccessfulResponse(),
//                    managedIdentitySource);

            ManagedIdentityParameters managedIdentityParameters1 = ManagedIdentityParameters.builder(scope).forceRefresh(false).build();

            IAuthenticationResult result = mi.acquireTokenForManagedIdentity(managedIdentityParameters1).get();

            Assert.assertNotNull(result);
            Assert.assertNotNull(result.accessToken());
//            Assert.assertEquals(TokenSource.IdentityProvider, result.AuthenticationResultMetadata.TokenSource);

            // Acquire token for same scope
            result = mi.acquireTokenForManagedIdentity(managedIdentityParameters1)
                    .get();

            Assert.assertNotNull(result);
            Assert.assertNotNull(result.accessToken());
//            Assert.assertEquals(TokenSource.Cache, result.AuthenticationResultMetadata.TokenSource);

//            httpManager.AddManagedIdentityMockHandler(
//                    endpoint,
//                    anotherScope,
//                    MockHelpers.GetMsiSuccessfulResponse(),
//                    managedIdentitySource);

            ManagedIdentityParameters managedIdentityParameters2 = ManagedIdentityParameters.builder(anotherScope).forceRefresh(false).build();

            // Acquire token for another scope
            result = mi.acquireTokenForManagedIdentity(managedIdentityParameters2).get();

            Assert.assertNotNull(result);
            Assert.assertNotNull(result.accessToken());
//            Assert.assertEquals(TokenSource.IdentityProvider, result.AuthenticationResultMetadata.TokenSource);
        }
    }

    @DataProvider(name = "endpointScopeSourceType1")
    public static Object[][] endpointScopeSourceType1() {
        return new Object[][] {{AppServiceEndpoint, Resource, ManagedIdentitySourceType.AppService},
                {IMDS_ENDPOINT, Resource, ManagedIdentitySourceType.Imds},
                {AzureArcEndpoint, Resource, ManagedIdentitySourceType.AzureArc},
                {CloudShellEndpoint, Resource, ManagedIdentitySourceType.CloudShell},
                {ServiceFabricEndpoint, Resource, ManagedIdentitySourceType.ServiceFabric}};
    }

    @Test(dataProvider = "endpointScopeSourceType1")
    public void ManagedIdentityForceRefreshTestAsync(
            String endpoint,
            String scope,
            String managedIdentitySourceType) throws Exception {

            mockEnvironmentVariables(managedIdentitySourceType, endpoint);

            ManagedIdentityApplication mi = ManagedIdentityApplication
                    .builder(ManagedIdentityId.SystemAssigned())
                        .httpClient(apacheHttpClientAdapter)
                    .build();

            // Disabling shared cache options to avoid cross test pollution.
            // miBuilder.Config.AccessorOptions = null;

//            httpManager.AddManagedIdentityMockHandler(
//                    endpoint,
//                    Resource,
//                    MockHelpers.GetMsiSuccessfulResponse(),
//                    managedIdentitySource);

            ManagedIdentityParameters managedIdentityParameters1 = ManagedIdentityParameters.builder(scope).forceRefresh(false).build();

            IAuthenticationResult result = mi.acquireTokenForManagedIdentity(managedIdentityParameters1).get();

            Assert.assertNotNull(result);
            Assert.assertNotNull(result.accessToken());
//            Assert.assertEquals(TokenSource.IdentityProvider, result.AuthenticationResultMetadata.TokenSource);

            // Acquire token from cache
            result = mi.acquireTokenForManagedIdentity(managedIdentityParameters1)
                    .get();

            Assert.assertNotNull(result);
            Assert.assertNotNull(result.accessToken());
//            Assert.assertEquals(TokenSource.Cache, result.AuthenticationResultMetadata.TokenSource);

//            httpManager.AddManagedIdentityMockHandler(
//                    endpoint,
//                    scope,
//                    MockHelpers.GetMsiSuccessfulResponse(),
//                    managedIdentitySource);

            // Acquire token with force refresh
            result = mi.acquireTokenForManagedIdentity(managedIdentityParameters1)//.WithForceRefresh(true)
                    .get();

            Assert.assertNotNull(result);
            Assert.assertNotNull(result.accessToken());
//            Assert.assertEquals(TokenSource.IdentityProvider, result.AuthenticationResultMetadata.TokenSource);
    }

    @DataProvider(name = "scopes")
    public static Object[][] scopes(){
        return new Object[][]{{"user.read",ManagedIdentitySourceType.AppService,AppServiceEndpoint},
                {"https://management.core.windows.net//user_impersonation",ManagedIdentitySourceType.AppService,AppServiceEndpoint},
                {"s",ManagedIdentitySourceType.AppService,AppServiceEndpoint},
                {"user.read",ManagedIdentitySourceType.Imds,IMDS_ENDPOINT},
                {"https://management.core.windows.net//user_impersonation",ManagedIdentitySourceType.Imds,IMDS_ENDPOINT},
                {"s",ManagedIdentitySourceType.Imds,IMDS_ENDPOINT},
                {"user.read",ManagedIdentitySourceType.AzureArc,AzureArcEndpoint},
                {"https://management.core.windows.net//user_impersonation",ManagedIdentitySourceType.AzureArc,AzureArcEndpoint},
                {"s",ManagedIdentitySourceType.AzureArc,AzureArcEndpoint},
                {"user.read",ManagedIdentitySourceType.CloudShell,CloudShellEndpoint},
                {"https://management.core.windows.net//user_impersonation",ManagedIdentitySourceType.CloudShell,CloudShellEndpoint},
                {"s",ManagedIdentitySourceType.CloudShell,CloudShellEndpoint},
                {"user.read",ManagedIdentitySourceType.ServiceFabric,ServiceFabricEndpoint},
                {"https://management.core.windows.net//user_impersonation",ManagedIdentitySourceType.ServiceFabric,ServiceFabricEndpoint},
                {"s",ManagedIdentitySourceType.ServiceFabric,ServiceFabricEndpoint}};
    }

    @Test(dataProvider = "scopes", expectedExceptions = MsalManagedIdentityException.class , expectedExceptionsMessageRegExp = MsalError.MANAGED_IDENTITY_REQUEST_FAILED)
    public void ManagedIdentityTestWrongScopeAsync(String resource, String managedIdentitySource, String endpoint) throws Exception {
        {
            mockEnvironmentVariables(managedIdentitySource, endpoint);

            ManagedIdentityApplication mi = ManagedIdentityApplication.
                    builder(ManagedIdentityId.SystemAssigned())
                       .httpClient(apacheHttpClientAdapter).build();

//            httpManager.AddManagedIdentityMockHandler(endpoint, resource, MockHelpers.GetMsiErrorResponse(),
//                    managedIdentitySource, statusCode: HttpStatusCode.InternalServerError);
//            httpManager.AddManagedIdentityMockHandler(endpoint, resource, MockHelpers.GetMsiErrorResponse(),
//                    managedIdentitySource, statusCode: HttpStatusCode.InternalServerError);

            mi.acquireTokenForManagedIdentity(managedIdentityParameters)
                    .get();
//            Assert.assertEquals(managedIdentitySource, ex.ManagedIdentitySourceType);
//            Assert.assertEquals(MsalError.ManagedIdentityRequestFailed, ex.ErrorCode);
        }
    }

    @DataProvider(name = "sourceTypeEndpoint")
    public static Object[][] sourceTypeEndpoint() {
        return new Object[][]{{ManagedIdentitySourceType.AppService, AppServiceEndpoint},
                {ManagedIdentitySourceType.Imds, IMDS_ENDPOINT},
                {ManagedIdentitySourceType.AzureArc, AzureArcEndpoint},
                {ManagedIdentitySourceType.CloudShell, CloudShellEndpoint},
                {ManagedIdentitySourceType.ServiceFabric, ServiceFabricEndpoint}};
    }

    @Test(dataProvider = "sourceTypeEndpoint", expectedExceptions = MsalManagedIdentityException.class, expectedExceptionsMessageRegExp = MsalError.MANAGED_IDENTITY_REQUEST_FAILED)
    public void ManagedIdentityErrorResponseNoPayloadTestAsync(String managedIdentitySource, String endpoint) throws Exception {
        mockEnvironmentVariables(managedIdentitySource, endpoint);

        ManagedIdentityApplication mi = ManagedIdentityApplication
                .builder(ManagedIdentityId.SystemAssigned())
                    .httpClient(apacheHttpClientAdapter)
                .build();

//        httpManager.AddManagedIdentityMockHandler(endpoint, "scope", "",
//                managedIdentitySource, statusCode: HttpStatusCode.InternalServerError);
//        httpManager.AddManagedIdentityMockHandler(endpoint, "scope", "",
//                managedIdentitySource, statusCode: HttpStatusCode.InternalServerError);

        ManagedIdentityParameters managedIdentityParameters1 = ManagedIdentityParameters.builder("scope")
                .forceRefresh(false).build();
        mi.acquireTokenForManagedIdentity(managedIdentityParameters1)
                .get();

//        Assert.assertEquals(managedIdentitySource, ex.ManagedIdentitySourceType);
//        Assert.assertEquals(MsalError.ManagedIdentityRequestFailed, ex.ErrorCode);
//        Assert.assertEquals(MsalErrorMessage.ManagedIdentityNoResponseReceived, ex.Message);
    }

    @Test(dataProvider = "sourceTypeEndpoint", expectedExceptions = MsalManagedIdentityException.class, expectedExceptionsMessageRegExp = MsalError.MANAGED_IDENTITY_REQUEST_FAILED)
    public void ManagedIdentityNullResponseAsync(String managedIdentitySource, String endpoint) throws Exception {
        mockEnvironmentVariables(managedIdentitySource, endpoint);

        ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication
                .builder(ManagedIdentityId.SystemAssigned())
                 .httpClient(apacheHttpClientAdapter);

        ManagedIdentityApplication mi = miBuilder.build();

//            httpManager.AddManagedIdentityMockHandler(
//                    endpoint,
//                    Resource,
//                    "",
//                    managedIdentitySource,
//                    statusCode: HttpStatusCode.OK);

        mi.acquireTokenForManagedIdentity(managedIdentityParameters)
                .get();

//            Assert.assertEquals(managedIdentitySource, ex.ManagedIdentitySourceType);
//            Assert.assertEquals(MsalError.ManagedIdentityRequestFailed, ex.ErrorCode);
//            Assert.assertEquals(MsalErrorMessage.ManagedIdentityInvalidResponse, ex.Message);
    }

    @Test(dataProvider = "sourceTypeEndpoint", expectedExceptions = MsalManagedIdentityException.class, expectedExceptionsMessageRegExp = MsalError.MANAGED_IDENTITY_REQUEST_FAILED)
    public void ManagedIdentityUnreachableNetworkAsync(String managedIdentitySource, String endpoint) throws Exception {

        {
            mockEnvironmentVariables(managedIdentitySource, endpoint);

            ManagedIdentityApplication mi = ManagedIdentityApplication
                    .builder(ManagedIdentityId.SystemAssigned())
                        .httpClient(apacheHttpClientAdapter)
                        .build();

//            httpManager.AddFailingRequest(new HttpRequestException("A socket operation was attempted to an unreachable network.",
//                    new SocketException(10051)));

            mi.acquireTokenForManagedIdentity(managedIdentityParameters)
                    .get();

//            Assert.assertEquals(managedIdentitySource, ex.ManagedIdentitySourceType);
//            Assert.assertEquals(MsalError.ManagedIdentityUnreachableNetwork, ex.ErrorCode);
//            Assert.assertEquals("A socket operation was attempted to an unreachable network.", ex.Message);
        }
    }

    @DataProvider(name = "sourceEndpointHttpStatus")
    public static Object[][] sourceEndpointHttpStatus(){
        return new Object[][]{{ManagedIdentitySourceType.AppService,AppServiceEndpoint, HttpURLConnection.HTTP_CLIENT_TIMEOUT},
                {ManagedIdentitySourceType.AppService,AppServiceEndpoint,HttpURLConnection.HTTP_INTERNAL_ERROR},
                {ManagedIdentitySourceType.AppService,AppServiceEndpoint,HttpURLConnection.HTTP_UNAVAILABLE},
                {ManagedIdentitySourceType.AppService,AppServiceEndpoint,HttpURLConnection.HTTP_GATEWAY_TIMEOUT},
                {ManagedIdentitySourceType.AppService,AppServiceEndpoint,HttpURLConnection.HTTP_NOT_FOUND},
                {ManagedIdentitySourceType.Imds,IMDS_ENDPOINT,HttpURLConnection.HTTP_NOT_FOUND},
                {ManagedIdentitySourceType.AzureArc,AzureArcEndpoint,HttpURLConnection.HTTP_NOT_FOUND},
                {ManagedIdentitySourceType.CloudShell,CloudShellEndpoint,HttpURLConnection.HTTP_NOT_FOUND},
                {ManagedIdentitySourceType.ServiceFabric,ServiceFabricEndpoint,HttpURLConnection.HTTP_NOT_FOUND}};
    }

    @Test(dataProvider = "sourceEndpointHttpStatus", expectedExceptions = MsalManagedIdentityException.class,
            expectedExceptionsMessageRegExp = MsalError.MANAGED_IDENTITY_REQUEST_FAILED)
    public void ManagedIdentityTestRetry(String managedIdentitySource, String endpoint, HttpStatus statusCode) throws Exception {

        {
            mockEnvironmentVariables(managedIdentitySource, endpoint);

            ManagedIdentityApplication mi = ManagedIdentityApplication
                    .builder(ManagedIdentityId.SystemAssigned())
                     .httpClient(apacheHttpClientAdapter)
                    .build();

//            httpManager.AddManagedIdentityMockHandler(
//                    endpoint,
//                    Resource,
//                    "",
//                    managedIdentitySource,
//                    statusCode: statusCode);
//
//            httpManager.AddManagedIdentityMockHandler(
//                    endpoint,
//                    Resource,
//                    "",
//                    managedIdentitySource,
//                    statusCode: statusCode);

            mi.acquireTokenForManagedIdentity(managedIdentityParameters)
                    .get();

//            Assert.assertEquals(MsalError.ManagedIdentityRequestFailed, ex.ErrorCode);
//            Assert.assertTrue(ex.IsRetryable);
        }
    }
    @Test
    public void systemAssignedManagedIdentityApiIdTest() throws Exception {

        {
            mockEnvironmentVariables(ManagedIdentitySourceType.AppService.toString(), AppServiceEndpoint);

            ManagedIdentityApplication mi = ManagedIdentityApplication
                    .builder(ManagedIdentityId.SystemAssigned())
                        .httpClient(apacheHttpClientAdapter)
                       .build();

//            httpManager.AddManagedIdentityMockHandler(
//                    AppServiceEndpoint,
//                    Resource,
//                    MockHelpers.GetMsiSuccessfulResponse(),
//                    ManagedIdentitySourceType.AppService);

            IAuthenticationResult result = mi.acquireTokenForManagedIdentity(managedIdentityParameters).get();

            Assert.assertNotNull(result);
            Assert.assertNotNull(result.accessToken());
//            Assert.assertEquals(TokenSource.IdentityProvider, result.AuthenticationResultMetadata.TokenSource);

//            Assert.assertEquals(ApiEvent.ApiIds.AcquireTokenForSystemAssignedManagedIdentity, builder.CommonParameters.ApiId);
        }
    }
    @Test
    public void userAssignedManagedIdentityApiIdTestAsync() throws Exception {
            mockEnvironmentVariables(ManagedIdentitySourceType.AppService.toString(), AppServiceEndpoint);

            ManagedIdentityApplication mi = ManagedIdentityApplication
                    .builder(ManagedIdentityId.UserAssignedClientId(TestConstants.CLIENT_ID))
             .httpClient(apacheHttpClientAdapter)
                    .build();

//            httpManager.AddManagedIdentityMockHandler(
//                    AppServiceEndpoint,
//                    Resource,
//                    MockHelpers.GetMsiSuccessfulResponse(),
//                    ManagedIdentitySourceType.AppService,
//                    userAssignedClientIdOrResourceId: TestConstants.ClientId,
//                userAssignedIdentityId: UserAssignedIdentityId.ClientId);

            IAuthenticationResult result = mi.acquireTokenForManagedIdentity(managedIdentityParameters).get();

            Assert.assertNotNull(result);
            Assert.assertNotNull(result.accessToken());
//            Assert.assertEquals(TokenSource.IdentityProvider, result.AuthenticationResultMetadata.TokenSource);

//            Assert.assertEquals(ApiEvent.ApiIds.AcquireTokenForUserAssignedManagedIdentity, builder.CommonParameters.ApiId);
    }
    @DataProvider(name = "expiresInForceRefresh")
    public static Object[][] expiresInForceRefresh() {
        return new Object[][]{{1, false},
                {2, false},
                {3, true}};
    }

    @Test(dataProvider = "expiresInForceRefresh")
    public void ManagedIdentityExpiresOnTestAsync(int expiresInHours, boolean refreshIn) throws Exception {
            mockEnvironmentVariables(ManagedIdentitySourceType.AppService.toString(), AppServiceEndpoint);

            ManagedIdentityApplication mi = ManagedIdentityApplication
                    .builder(ManagedIdentityId.SystemAssigned())
                    .httpClient(apacheHttpClientAdapter)
                    .build();

//            httpManager.AddManagedIdentityMockHandler(
//                    AppServiceEndpoint,
//                    Resource,
//                    MockHelpers.GetMsiSuccessfulResponse(expiresInHours),
//                    ManagedIdentitySourceType.AppService);

            IAuthenticationResult result = mi.acquireTokenForManagedIdentity(managedIdentityParameters).get();

            Assert.assertNotNull(result);
            Assert.assertNotNull(result);
            Assert.assertNotNull(result.accessToken());
//            Assert.assertEquals(TokenSource.IdentityProvider, result.AuthenticationResultMetadata.TokenSource);
//            Assert.assertEquals(ApiEvent.ApiIds.AcquireTokenForSystemAssignedManagedIdentity, builder.CommonParameters.ApiId);
//            Assert.assertEquals(refreshOnHasValue, result.AuthenticationResultMetadata.RefreshOn.HasValue);
        }
    @Test(expectedExceptions = MsalClientException.class)
    public void ManagedIdentityInvalidRefreshOnThrowsAsync() throws Exception {
        mockEnvironmentVariables(ManagedIdentitySourceType.AppService.toString(), AppServiceEndpoint);

        ManagedIdentityApplication mi = ManagedIdentityApplication
                .builder(ManagedIdentityId.SystemAssigned())
                    .httpClient(apacheHttpClientAdapter)
                .build();

//            httpManager.AddManagedIdentityMockHandler(
//                    AppServiceEndpoint,
//                    Resource,
//                    MockHelpers.GetMsiSuccessfulResponse(0),
//                    ManagedIdentitySourceType.AppService);

        mi.acquireTokenForManagedIdentity(managedIdentityParameters).get();
    }
    @Test
    public void ManagedIdentityIsProactivelyRefreshedAsync() throws Exception {
        mockEnvironmentVariables(ManagedIdentitySourceType.AppService.toString(), AppServiceEndpoint);

        System.out.println("1. Setup an app with a token cache with one AT");

        ManagedIdentityApplication mi = ManagedIdentityApplication
                .builder(ManagedIdentityId.SystemAssigned())
                    .httpClient(apacheHttpClientAdapter)
                .build();

//            httpManager.AddManagedIdentityMockHandler(
//                    AppServiceEndpoint,
//                    Resource,
//                    MockHelpers.GetMsiSuccessfulResponse(),
//                    ManagedIdentitySourceType.AppService);

        IAuthenticationResult result = mi.acquireTokenForManagedIdentity(managedIdentityParameters)
                .get();

        System.out.println("2. Configure AT so that it shows it needs to be refreshed");
//        var refreshOn = TestCommon.UpdateATWithRefreshOn(mi.AppTokenCacheInternal.Accessor).RefreshOn;
//        TokenCacheAccessRecorder cacheAccess = mi.AppTokenCacheInternal.RecordAccess();

        System.out.println("3. Configure MSI to respond with a valid token");
//            httpManager.AddManagedIdentityMockHandler(
//                    AppServiceEndpoint,
//                    Resource,
//                    MockHelpers.GetMsiSuccessfulResponse(),
//                    ManagedIdentitySourceType.AppService);

        // Act
        System.out.println("4. ATM - should perform an RT refresh");
        result = mi.acquireTokenForManagedIdentity(managedIdentityParameters)
                .get()
        ;

        // Assert
//            TestCommon.YieldTillSatisfied(() => httpManager.QueueSize == 0);

        Assert.assertNotNull(result);
//        Assert.assertEquals(0, httpManager.QueueSize,
//                "MSAL should have refreshed the token because the original AT was marked for refresh");
//
//            cacheAccess.WaitTo_AssertAcessCounts(1, 1);
//
//            Assert.assertEquals(CacheRefreshReason.ProactivelyRefreshed, result.AuthenticationResultMetadata.CacheRefreshReason);
//
//            Assert.assertEquals(refreshOn, result.AuthenticationResultMetadata.RefreshOn);

        result = mi.acquireTokenForManagedIdentity(managedIdentityParameters)
                .get();

//            Assert.assertEquals(CacheRefreshReason.NotApplicable, result.AuthenticationResultMetadata.CacheRefreshReason);
    }
}
