package com.microsoft.aad.msal4j;

import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.aad.msal4j.ManagedIdentityTestUtils.setEnvironmentVariables;

public class ManagedIdentityTests extends SeleniumTest{
    private static final String s_msi_scopes = "https://management.azure.com";
    private static final String s_wrong_msi_scopes = "https://managements.azure.com";

    //http proxy base URL 
    private static final String s_baseURL = "https://service.msidlab.com/";

    //Shared User Assigned Client ID
    private static final String UserAssignedClientID = "3b57c42c-3201-4295-ae27-d6baec5b7027";

    //Non Existent User Assigned Client ID 
    private static final String NonExistentUserAssignedClientID = "72f988bf-86f1-41af-91ab-2d7cd011db47";

    //Error Messages
    private static final String UserAssignedIdDoesNotExist = "Managed Identity Error Message: " +
            "No User Assigned or Delegated Managed Identity found for specified ClientId/ResourceId/PrincipalId.";

    //Resource ID of the User Assigned Identity 
    private static final String UamiResourceId = "/subscriptions/c1686c51-b717-4fe0-9af3-24a20a41fb0c/" +
            "resourcegroups/MSAL_MSI/providers/Microsoft.ManagedIdentity/userAssignedIdentities/" +
            "MSAL_MSI_USERID";

    private static final String Non_Existent_UamiResourceId = "/subscriptions/userAssignedIdentities/NO_ID";

    @DataProvider(name = "msiAzureResources")
    public static Object[][] msiAzureResources() throws MalformedURLException {
        return new Object[][]{{TestConstants.MsiAzureResource.WebApp, ""},
                {TestConstants.MsiAzureResource.Function, ""},
                {TestConstants.MsiAzureResource.VM, ""},
                {TestConstants.MsiAzureResource.WebApp, UserAssignedClientID},
                {TestConstants.MsiAzureResource.Function, UserAssignedClientID},
                {TestConstants.MsiAzureResource.VM, UserAssignedClientID},
                {TestConstants.MsiAzureResource.WebApp, UamiResourceId},
                {TestConstants.MsiAzureResource.Function, UamiResourceId},
                {TestConstants.MsiAzureResource.VM, UamiResourceId}};
    }

    //non-existent Resource ID of the User Assigned Identity
    @Test(dataProvider = "msiAzureResources")
    public void acquireMSITokenAsync(TestConstants.MsiAzureResource azureResource, String userIdentity) throws Exception {
        //Arrange
//
//            // Fetch the env variables from the resource and set them locally
//            Map<String, String> envVariables =
//                    getEnvironmentVariables(azureResource);
//
//            //Set the Environment Variables
//            setEnvironmentVariables(envVariables);

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
            //form the http proxy URI
            String uri = s_baseURL + "MSIToken?" +
                "azureresource=" + azureResource + "&uri=";

            //Create CCA with Proxy
            ManagedIdentityApplication managedIdentityApplication = createMIAWithProxy(uri, userIdentity);

            IAuthenticationResult result = managedIdentityApplication
                .acquireTokenForManagedIdentity(ManagedIdentityParameters.builder(s_msi_scopes).forceRefresh(false).build())
                    .get();
            
            //1. Token Type
//            Assert.assertEquals("Bearer", result.TokenType);

            //2. First token response is from the MSI Endpoint
//            Assert.assertEquals(TokenSource.IdentityProvider, result.AuthenticationResultMetadata.TokenSource);

            //3. Validate the ExpiresOn falls within a 24 hour range from now
//            CoreAssert.IsWithinRange(
//                    DateTimeOffset.UtcNow + TimeSpan.FromHours(0),
//                    result.ExpiresOn,
//                    TimeSpan.FromHours(24));

            //4. Validate the scope
            Assert.assertTrue(result.scopes().contains(s_msi_scopes));

            //5. Validate the second call to token endpoint gets returned from the cache
//            Assert.assertEquals(TokenSource.Cache,
//                    result.AuthenticationResultMetadata.TokenSource);
    }

    @DataProvider(name = "msiWrongClientIDs")
    public static Object[][] msiWrongClientIDs() throws MalformedURLException {
        return new Object[][]{{TestConstants.MsiAzureResource.WebApp, NonExistentUserAssignedClientID},
                {TestConstants.MsiAzureResource.WebApp, Non_Existent_UamiResourceId}};

    }

    @Test(dataProvider = "msiWrongClientIDs", expectedExceptions = MsalManagedIdentityException.class,
    expectedExceptionsMessageRegExp = UserAssignedIdDoesNotExist )

    public void acquireTokenUsingWrongClientID(TestConstants.MsiAzureResource azureResource, String userIdentity) throws Exception {
            //Get the Environment Variables
            Map<String, String> envVariables =
                    getEnvironmentVariables(azureResource);

            //Set the Environment Variables
            setEnvironmentVariables(envVariables);

            //form the http proxy URI
            String uri = s_baseURL + "MSIToken?" +
            "azureresource=" + azureResource + "&uri=";


            //Create CCA with Proxy
            ManagedIdentityApplication managedIdentityApplication = createMIAWithProxy(uri, userIdentity);

        managedIdentityApplication.acquireTokenForManagedIdentity(ManagedIdentityParameters.builder(s_msi_scopes).forceRefresh(false).build());

            //Assert
//            Assert.assertTrue(ex.getMessage().contains(UserAssignedIdDoesNotExist));
//            Assert.assertEquals(AbstractManagedIdentity.AppService, ex.ManagedIdentitySource);
        }

    @DataProvider(name = "msiWrongClientIDsForFunctions")
    public static Object[][] msiWrongClientIDsForFunctions() throws MalformedURLException {
        return new Object[][]{{TestConstants.MsiAzureResource.Function, NonExistentUserAssignedClientID},
                {TestConstants.MsiAzureResource.Function, Non_Existent_UamiResourceId}};

    }

  @Test(dataProvider = "msiWrongClientIDsForFunctions", expectedExceptions = MsalManagedIdentityException.class, expectedExceptionsMessageRegExp = "")
    public void functionAppErrorNotInExpectedFormatAsync(TestConstants.MsiAzureResource azureResource, String userIdentity) throws Exception {
        //Arrange
            //Get the Environment Variables
            Map<String, String> envVariables =
                    getEnvironmentVariables(azureResource);

            //Set the Environment Variables
            setEnvironmentVariables(envVariables);

            //form the http proxy URI
            String uri = s_baseURL + "MSIToken?" +
                "azureresource=" + azureResource + "&uri=";

            //Create CCA with Proxy
            ManagedIdentityApplication managedIdentityApplication = createMIAWithProxy(uri, userIdentity);

            managedIdentityApplication
                        .acquireTokenForManagedIdentity(ManagedIdentityParameters.builder(s_msi_scopes).forceRefresh(false).build());

            //Assert
//            Assert.assertEquals(AbstractManagedIdentity.AppService, ex.ManagedIdentitySource);
    }

    @DataProvider(name = "msiWebApps")
    public static Object[][] msiWebApps() throws MalformedURLException {
        return new Object[][]{{TestConstants.MsiAzureResource.WebApp, ""},
                {TestConstants.MsiAzureResource.WebApp, UserAssignedClientID},
                {TestConstants.MsiAzureResource.WebApp, UamiResourceId}};

    }

    @Test(dataProvider = "msiWebApps", expectedExceptions = MsalManagedIdentityException.class, expectedExceptionsMessageRegExp = MsalError.ManagedIdentityRequestFailed )
    public void mSIWrongScopesAsync(TestConstants.MsiAzureResource azureResource, String userIdentity) throws Exception {
        //Arrange
            //Get the Environment Variables
            Map<String, String> envVariables =
                    getEnvironmentVariables(azureResource);

            //Set the Environment Variables
            setEnvironmentVariables(envVariables);

            //form the http proxy URI
            String uri = s_baseURL + "MSIToken?" +
                "azureresource=" + azureResource + "&uri=";

            //Create CCA with Proxy
            ManagedIdentityApplication managedIdentityApplication = createMIAWithProxy(uri, userIdentity);

            managedIdentityApplication
                        .acquireTokenForManagedIdentity(ManagedIdentityParameters.builder(s_wrong_msi_scopes)
                                .forceRefresh(false).build());

            //Assert
//            Assert.assertTrue(ex.ErrorCode == MsalError.ManagedIdentityRequestFailed);
//            Assert.assertEquals(AbstractManagedIdentity.AppService, ex.ManagedIdentitySource);
    }

//    /// Gets the environment variable
//    /// <param name="resource"></param>
    private Map<String, String> getEnvironmentVariables(
            TestConstants.MsiAzureResource resource)
    {
        Map<String, String> environmentVariables = new HashMap<String, String>();

        //Get the Environment Variables from the MSI Helper Service
        String uri = s_baseURL + "EnvironmentVariables?resource=" + resource;

        String environmentVariableResponse = labUserProvider
            .getMSIEnvironmentVariables(uri);

        //process the response
        if (!StringHelper.isNullOrBlank(environmentVariableResponse))
        {
            environmentVariables = JsonHelper.convertJsonToObject(environmentVariableResponse, Map.class);
        }

        return environmentVariables;
    }




    /// Create the ManagedIdentityApplication with the http proxy
    /// <param name="url"></param>
    /// <param name="userAssignedId"></param>
    /// <returns></returns>
    private ManagedIdentityApplication createMIAWithProxy(String url, String userAssignedId)
    {
        //Proxy the MSI token request 
//        MsiProxyHttpManager proxyHttpManager = new MsiProxyHttpManager(url);

        ManagedIdentityApplication.Builder builder = ManagedIdentityApplication.builder();
//                .withHttpManager(proxyHttpManager);

        if (!StringHelper.isNullOrBlank(userAssignedId))
        {
            builder = ManagedIdentityApplication.builder(ManagedIdentityId.UserAssignedClientId(userAssignedId));
//                    .withHttpManager(proxyHttpManager);
        }

        ManagedIdentityApplication managedIdentityApplication = builder.build();

        return managedIdentityApplication;
    }

    private static class MockEnvironment{
        public static String getIdentityEndpoint() {
            return "IDENTITY_ENDPOINT";
        }

        public static String getIdentityHeader() {
            return "IDENTITY_HEADER";
        }

        public static String getAzurePodIdentityAuthorityHost() {
            return "AZURE_POD_IDENTITY_AUTHORITY_HOST";
        }

        public static String getImdsEndpoint() {
            return "IMDS_ENDPOINT";
        }

        public static String getMsiEndpoint() {
            return "MSI_ENDPOINT";
        }

        public static String getIdentityServerThumbprint() {
            return "IDENTITY_SERVER_THUMBPRINT";
        }
    }
}
