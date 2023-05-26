package com.microsoft.aad.msal4j;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.aad.msal4j.ManagedIdentityTestUtils.setEnvironmentVariables;
import static org.powermock.api.easymock.PowerMock.expectPrivate;

@PrepareForTest({EnvironmentVariables.class})
public class ManagedIdentityTest extends PowerMockTestCase {

    final static String Resource = "https://management.azure.com";

    static ManagedIdentityParameters managedIdentityParameters = ManagedIdentityParameters.builder(Resource).forceRefresh(false).build();
    
    final static String ResourceDefaultSuffix = "https://management.azure.com/.default";
    final static String AppServiceEndpoint = "http://127.0.0.1:41564/msi/token";
    final static String IMDS_ENDPOINT = "http://169.254.169.254/metadata/identity/oauth2/token";
    final static String AzureArcEndpoint = "http://localhost:40342/metadata/identity/oauth2/token";
    final static String CloudShellEndpoint = "http://localhost:40342/metadata/identity/oauth2/token";
    final static String ServiceFabricEndpoint = "http://localhost:40342/metadata/identity/oauth2/token";

    @DataProvider(name = "endpointScopeSource")
    public static Object[][] endpointScopeSource() {
        return new Object[][]{{"http://127.0.0.1:41564/msi/token/", Resource, ManagedIdentitySourceType.AppService},
                {AppServiceEndpoint, Resource, ManagedIdentitySourceType.AppService},
                {AppServiceEndpoint, ResourceDefaultSuffix, ManagedIdentitySourceType.AppService},
                {IMDS_ENDPOINT, Resource, ManagedIdentitySourceType.Imds},
                {null, Resource, ManagedIdentitySourceType.Imds},
                {AzureArcEndpoint, Resource, ManagedIdentitySourceType.AzureArc},
                {AzureArcEndpoint, ResourceDefaultSuffix, ManagedIdentitySourceType.AzureArc},
                {CloudShellEndpoint, Resource, ManagedIdentitySourceType.CloudShell},
                {CloudShellEndpoint, ResourceDefaultSuffix, ManagedIdentitySourceType.CloudShell},
                {ServiceFabricEndpoint, Resource, ManagedIdentitySourceType.ServiceFabric},
                {ServiceFabricEndpoint, ResourceDefaultSuffix, ManagedIdentitySourceType.ServiceFabric}};
    }

    @Test(dataProvider = "endpointScopeSource")
    public void ValidNotOkHttpResponse(String endpoint,
                                       String scope,
                                       ManagedIdentitySourceType managedIdentitySource) throws Exception {

        HttpsURLConnection mockCon = PowerMock.createMock(HttpsURLConnection.class);

        EasyMock.expect(mockCon.getResponseCode())
                .andReturn(HttpURLConnection.HTTP_INTERNAL_ERROR).times(1);

        String errorResponse = "Error Message";
        InputStream inputStream = IOUtils.toInputStream(errorResponse, "UTF-8");
        EasyMock.expect(mockCon.getErrorStream()).andReturn(inputStream).times(1);

        Map<String, List<String>> expectedHeaders = new HashMap<>();
        expectedHeaders.put("header1", Arrays.asList("val1", "val2"));

        EasyMock.expect(mockCon.getHeaderFields()).andReturn(expectedHeaders).times(1);

        mockCon.setReadTimeout(0);
        mockCon.setConnectTimeout(0);

        DefaultHttpClient httpClient =
                PowerMock.createPartialMock(DefaultHttpClient.class, "openConnection");

        expectPrivate(httpClient, "openConnection", EasyMock.isA(URL.class)).andReturn(mockCon);

        PowerMock.replayAll(mockCon, httpClient);

        ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication.builder(ManagedIdentityId.SystemAssigned())
                .httpClient(httpClient);

        // Disabling shared cache options to avoid cross test pollution.
        // miBuilder.Config.AccessorOptions = null;

        ManagedIdentityApplication mi = miBuilder.build();

        ManagedIdentityParameters managedIdentityParameters1 = ManagedIdentityParameters.builder("https://management.azure.com").forceRefresh(false).build();

        IAuthenticationResult result = mi.acquireTokenForManagedIdentity(managedIdentityParameters1).get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
    }

    @Test
    public static void managedIdentityHappyPath() throws Exception {
        
//        setEnvironmentVariables(managedIdentitySource, endpoint);

        PowerMock.mockStatic(EnvironmentVariables.class);
        EasyMock.expect(
                EnvironmentVariables.getAzurePodIdentityAuthorityHost()).andReturn("AZURE_POD_IDENTITY");
        EasyMock.expect(
                EnvironmentVariables.getIdentityEndpoint()).andReturn("IDENTITY_ENDPOINT");
        EasyMock.expect(
                EnvironmentVariables.getIdentityHeader()).andReturn("IDENTITY_HEADER");
        EasyMock.expect(
                EnvironmentVariables.getIdentityServerThumbprint()).andReturn("THUMBPRINT");
        EasyMock.expect(
                EnvironmentVariables.getImdsEndpoint()).andReturn("IMDS");
        EasyMock.expect(
                EnvironmentVariables.getMsiEndpoint()).andReturn("MSI");

        PowerMock.replay(EnvironmentVariables.class);
        String env = EnvironmentVariables.getMsiEndpoint();
        Assert.assertEquals(env, "MSI");

        OkHttpClientAdapter okHttpClientAdapter = new OkHttpClientAdapter();

        ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication.builder(ManagedIdentityId.SystemAssigned())
                .httpClient(okHttpClientAdapter);

        // Disabling shared cache options to avoid cross test pollution.
        // miBuilder.Config.AccessorOptions = null;

        ManagedIdentityApplication mi = miBuilder.build();

        ManagedIdentityParameters managedIdentityParameters1 = ManagedIdentityParameters.builder("https://management.azure.com").forceRefresh(false).build();

        IAuthenticationResult result = mi.acquireTokenForManagedIdentity(managedIdentityParameters1).get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
//            Assert.assertEquals(TokenSource.IdentityProvider, result.AuthenticationResultMetadata.TokenSource);
            
            result = mi.acquireTokenForManagedIdentity(managedIdentityParameters1)
                .get();

            Assert.assertNotNull(result);
            Assert.assertNotNull(result.accessToken());
//            Assert.assertEquals(TokenSource.Cache, result.AuthenticationResultMetadata.TokenSource);
        }

    @DataProvider(name = "endpointSourceId")
    public static Object[][] endpointSourceId() {
        return new Object[][]
                {{ManagedIdentityTest.AppServiceEndpoint, ManagedIdentitySourceType.AppService, TestConstants.CLIENT_ID, ManagedIdentityIdType.ClientId},
        {ManagedIdentityTest.AppServiceEndpoint, ManagedIdentitySourceType.AppService, TestConstants.MI_RESOURCE_ID, ManagedIdentityIdType.ResourceId},
        {IMDS_ENDPOINT, ManagedIdentitySourceType.Imds, TestConstants.CLIENT_ID, ManagedIdentityIdType.ClientId},
        {IMDS_ENDPOINT, ManagedIdentitySourceType.Imds, TestConstants.MI_RESOURCE_ID, ManagedIdentityIdType.ResourceId},
        {ServiceFabricEndpoint, ManagedIdentitySourceType.ServiceFabric, TestConstants.CLIENT_ID, ManagedIdentityIdType.ClientId},
        {ServiceFabricEndpoint, ManagedIdentitySourceType.ServiceFabric, TestConstants.MI_RESOURCE_ID, ManagedIdentityIdType.ResourceId}};
    }

    @Test(dataProvider = "endpointSourceId")
    public static void managedIdentityUserAssignedHappyPathAsync(
            String endpoint,
            ManagedIdentitySourceType managedIdentitySource,
            String userAssignedId,
            ManagedIdentityIdType userAssignedIdentityId) throws Exception {
        
        {
            setEnvironmentVariables(managedIdentitySource, endpoint);
            
            ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication.builder(
                            userAssignedIdentityId == ManagedIdentityIdType.ClientId ?
                                    ManagedIdentityId.UserAssignedClientId(userAssignedId) :
                                    ManagedIdentityId.UserAssignedResourceId(userAssignedId));
                    // .httpClient();

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
    public static void ManagedIdentityDifferentScopesTestAsync(
            String endpoint,
            String scope,
            String anotherScope,
            ManagedIdentitySourceType managedIdentitySource) throws Exception {
        
        {
            setEnvironmentVariables(managedIdentitySource, endpoint);

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
    public static void ManagedIdentityForceRefreshTestAsync(
            String endpoint,
            String scope,
            ManagedIdentitySourceType managedIdentitySourceType) throws Exception {
        
        {
            setEnvironmentVariables(managedIdentitySourceType, endpoint);

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
    public static void ManagedIdentityTestWrongScopeAsync(String resource, ManagedIdentitySourceType managedIdentitySource, String endpoint) throws Exception {
        
        {
            setEnvironmentVariables(managedIdentitySource, endpoint);

            ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication.builder(ManagedIdentityId.SystemAssigned());
                    // .httpClient();

            // Disabling shared cache options to avoid cross test pollution.
            // miBuilder.Config.AccessorOptions = null;

            ManagedIdentityApplication mi = miBuilder.build();
//
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
   public static void ManagedIdentityErrorResponseNoPayloadTestAsync(ManagedIdentitySourceType managedIdentitySource, String endpoint) throws Exception {
        setEnvironmentVariables(managedIdentitySource, endpoint);

        ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication.builder(ManagedIdentityId.SystemAssigned());
                // .httpClient();

        // Disabling shared cache options to avoid cross test pollution.
        // miBuilder.Config.AccessorOptions = null;

        ManagedIdentityApplication mi = miBuilder.build();

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
    public static void ManagedIdentityNullResponseAsync(ManagedIdentitySourceType managedIdentitySource, String endpoint) throws Exception {
            setEnvironmentVariables(managedIdentitySource, endpoint);

            ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication.builder(ManagedIdentityId.SystemAssigned());
                    // .httpClient();

            // Disabling shared cache options to avoid cross test pollution.
            // miBuilder.Config.AccessorOptions = null;

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
    public static void ManagedIdentityUnreachableNetworkAsync(ManagedIdentitySourceType managedIdentitySource, String endpoint) throws Exception {
        
        {
            setEnvironmentVariables(managedIdentitySource, endpoint);

            ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication.builder(ManagedIdentityId.SystemAssigned());
                    // .httpClient();

            // Disabling shared cache options to avoid cross test pollution.
            // miBuilder.Config.AccessorOptions = null;

            ManagedIdentityApplication mi = miBuilder.build();

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
        return new Object[][]{{ManagedIdentitySourceType.AppService,AppServiceEndpoint,HttpURLConnection.HTTP_CLIENT_TIMEOUT},
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
    public static void ManagedIdentityTestRetry(ManagedIdentitySourceType managedIdentitySource, String endpoint, HttpStatus statusCode) throws Exception {
        
        {
            setEnvironmentVariables(managedIdentitySource, endpoint);

            ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication.builder(ManagedIdentityId.SystemAssigned());
//                    // .httpClient();

            // Disabling shared cache options to avoid cross test pollution.
            // miBuilder.Config.AccessorOptions = null;

            ManagedIdentityApplication mi = miBuilder.build();

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
    public static void systemAssignedManagedIdentityApiIdTest() throws Exception {
        
        {
            setEnvironmentVariables(ManagedIdentitySourceType.AppService, AppServiceEndpoint);

            ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication.builder(ManagedIdentityId.SystemAssigned());
                    // .httpClient();

            // Disabling shared cache options to avoid cross test pollution.
            // miBuilder.Config.AccessorOptions = null;

            ManagedIdentityApplication mi = miBuilder.build();

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
    public static void UserAssignedManagedIdentityApiIdTestAsync() throws Exception {
        
        {
            setEnvironmentVariables(ManagedIdentitySourceType.AppService, AppServiceEndpoint);

            ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication.builder(ManagedIdentityId.UserAssignedClientId(TestConstants.CLIENT_ID));
                    // .httpClient();

            // Disabling shared cache options to avoid cross test pollution.
            // miBuilder.Config.AccessorOptions = null;

            ManagedIdentityApplication mi = miBuilder.build();

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
    }

    @Test
    public static void ManagedIdentityCacheTestAsync() throws Exception {
        
        {
            setEnvironmentVariables(ManagedIdentitySourceType.AppService, AppServiceEndpoint);

            ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication.builder(ManagedIdentityId.SystemAssigned());
                    // .httpClient();

            // Disabling shared cache options to avoid cross test pollution.
            // miBuilder.Config.AccessorOptions = null;

            ManagedIdentityApplication mi = miBuilder.build();
            
//            var appTokenCacheRecoder = mi.AppTokenCacheInternal.RecordAccess((args) =>
//                    {
//                            Assert.assertEquals(Constants.ManagedIdentityDefaultTenant, args.RequestTenantId);
//            Assert.assertEquals(Constants.ManagedIdentityDefaultClientId, args.ClientId);
//            Assert.assertNull(args.Account);
//            Assert.assertTrue(args.IsApplicationCache);
//            Assert.assertEquals(cancellationToken, args.CancellationToken);
//                });

//            httpManager.AddManagedIdentityMockHandler(
//                    AppServiceEndpoint,
//                    Resource,
//                    MockHelpers.GetMsiSuccessfulResponse(),
//                    ManagedIdentitySourceType.AppService);

//            IAuthenticationResult result = mi.acquireTokenForManagedIdentity(managedIdentityParameters).ExecuteAsync(cancellationToken);
//
//            appTokenCacheRecoder.AssertAccessCounts(1, 1);
        }
    }

    @DataProvider(name = "expiresInForceRefresh")
    public static Object[][] expiresInForceRefresh() {
        return new Object[][]{{1, false},
                {2, false},
                {3, true}};
    }    
    
    @Test(dataProvider = "expiresInForceRefresh")
    public static void ManagedIdentityExpiresOnTestAsync(int expiresInHours, boolean refreshIn) throws Exception {
    {
            setEnvironmentVariables(ManagedIdentitySourceType.AppService, AppServiceEndpoint);

            ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication.builder(ManagedIdentityId.SystemAssigned());
                    // .httpClient();

            // Disabling shared cache options to avoid cross test pollution.
            // miBuilder.Config.AccessorOptions = null;

            ManagedIdentityApplication mi = miBuilder.build();

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
    }

    @Test(expectedExceptions = MsalClientException.class)
    public static void ManagedIdentityInvalidRefreshOnThrowsAsync() throws Exception {
        setEnvironmentVariables(ManagedIdentitySourceType.AppService, AppServiceEndpoint);

        ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication.builder(ManagedIdentityId.SystemAssigned());
                // .httpClient();

        // Disabling shared cache options to avoid cross test pollution.
        // miBuilder.Config.AccessorOptions = null;

        ManagedIdentityApplication mi = miBuilder.build();

//            httpManager.AddManagedIdentityMockHandler(
//                    AppServiceEndpoint,
//                    Resource,
//                    MockHelpers.GetMsiSuccessfulResponse(0),
//                    ManagedIdentitySourceType.AppService);

        mi.acquireTokenForManagedIdentity(managedIdentityParameters).get();
    }
    @Test
    public static void ManagedIdentityIsProactivelyRefreshedAsync() throws Exception {
        setEnvironmentVariables(ManagedIdentitySourceType.AppService, AppServiceEndpoint);

        System.out.println("1. Setup an app with a token cache with one AT");

        ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication
                .builder(ManagedIdentityId.SystemAssigned());
                // .httpClient();

        // Disabling shared cache options to avoid cross test pollution.
        // miBuilder.Config.AccessorOptions = null;

        ManagedIdentityApplication mi = miBuilder.build();

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
