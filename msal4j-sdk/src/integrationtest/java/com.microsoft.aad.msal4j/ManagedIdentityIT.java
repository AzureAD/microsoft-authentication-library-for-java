// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.aad.msal4j.ManagedIdentityTestUtils.setEnvironmentVariables;

public class ManagedIdentityIT extends SeleniumTest{
    private static final String S_MSI_SCOPES = "https://management.azure.com";
    private static final String S_WRONG_MSI_SCOPES = "https://managements.azure.com";

    //http proxy base URL 
    private static final String S_BASE_URL = "https://service.msidlab.com/";

    //Shared User Assigned Client ID
    private static final String USER_ASSIGNED_CLIENT_ID = "3b57c42c-3201-4295-ae27-d6baec5b7027";

    //Non Existent User Assigned Client ID 
    private static final String NON_EXISTENT_USER_ASSIGNED_CLIENT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";

    //Error Messages
    private static final String USER_ASSIGNED_ID_DOES_NOT_EXIST = "Managed Identity Error Message: " +
            "No User Assigned or Delegated Managed Identity found for specified ClientId/ResourceId/PrincipalId.";

    //Resource ID of the User Assigned Identity 
    private static final String UAMI_RESOURCE_ID = "/subscriptions/c1686c51-b717-4fe0-9af3-24a20a41fb0c/" +
            "resourcegroups/MSAL_MSI/providers/Microsoft.ManagedIdentity/userAssignedIdentities/" +
            "MSAL_MSI_USERID";

    private static final String NON_EXISTENT_UAMI_RESOURCE_ID = "/subscriptions/userAssignedIdentities/NO_ID";

    @DataProvider(name = "msiAzureResources")
    public static Object[][] msiAzureResources(){
        return new Object[][]{{TestConstants.MsiAzureResource.WebApp, "", ManagedIdentityIdType.SystemAssigned},
                {TestConstants.MsiAzureResource.Function, "", ManagedIdentityIdType.SystemAssigned},
                {TestConstants.MsiAzureResource.VM, "", ManagedIdentityIdType.SystemAssigned},
                {TestConstants.MsiAzureResource.WebApp, USER_ASSIGNED_CLIENT_ID, ManagedIdentityIdType.ClientId},
                {TestConstants.MsiAzureResource.Function, USER_ASSIGNED_CLIENT_ID, ManagedIdentityIdType.ClientId},
                {TestConstants.MsiAzureResource.VM, USER_ASSIGNED_CLIENT_ID, ManagedIdentityIdType.ClientId},
                {TestConstants.MsiAzureResource.WebApp, UAMI_RESOURCE_ID, ManagedIdentityIdType.ResourceId},
                {TestConstants.MsiAzureResource.Function, UAMI_RESOURCE_ID, ManagedIdentityIdType.ResourceId},
                {TestConstants.MsiAzureResource.VM, UAMI_RESOURCE_ID, ManagedIdentityIdType.ResourceId}};
    }

    //non-existent Resource ID of the User Assigned Identity
    @Test(dataProvider = "msiAzureResources")
    public void acquireMSIToken(TestConstants.MsiAzureResource azureResource, String userIdentity, ManagedIdentityIdType idType)
            throws Exception {
            // Fetch the env variables from the resource and set them locally
            Map<String, String> envVariables =
                    getEnvironmentVariables(azureResource);
//
//            //Set the Environment Variables
//            setEnvironmentVariables(envVariables);

//        PowerMock.mockStatic(EnvironmentVariables.class);
//        EasyMock.expect(
//                EnvironmentVariables.getAzurePodIdentityAuthorityHost()).andReturn("AZURE_POD_IDENTITY");
//        EasyMock.expect(
//                EnvironmentVariables.getIdentityEndpoint()).andReturn("IDENTITY_ENDPOINT");
//        EasyMock.expect(
//                EnvironmentVariables.getIdentityHeader()).andReturn("IDENTITY_HEADER");
//        EasyMock.expect(
//                EnvironmentVariables.getIdentityServerThumbprint()).andReturn("THUMBPRINT");
//        EasyMock.expect(
//                EnvironmentVariables.getImdsEndpoint()).andReturn("IMDS");
//        EasyMock.expect(
//                EnvironmentVariables.getMsiEndpoint()).andReturn("MSI");
            //form the http proxy URI
            String uri = S_BASE_URL + "MSIToken?" +
                "azureresource=" + azureResource + "&uri=";

            //Create CCA with Proxy
            ManagedIdentityApplication managedIdentityApplication = createManagedIdentityApplicationWithProxy(uri, userIdentity, idType);

            IAuthenticationResult result = managedIdentityApplication
                .acquireTokenForManagedIdentity(ManagedIdentityParameters.builder(S_MSI_SCOPES)
                        .forceRefresh(false)
                        .build())
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
            Assert.assertTrue(result.scopes().contains(S_MSI_SCOPES));

            //5. Validate the second call to token endpoint gets returned from the cache
//            Assert.assertEquals(TokenSource.Cache,
//                    result.AuthenticationResultMetadata.TokenSource);
    }

    @DataProvider(name = "msiWrongIDs")
    public static Object[][] msiWrongIDs() {
        return new Object[][]{{TestConstants.MsiAzureResource.WebApp, NON_EXISTENT_USER_ASSIGNED_CLIENT_ID, ManagedIdentityIdType.ClientId},
                {TestConstants.MsiAzureResource.WebApp, NON_EXISTENT_UAMI_RESOURCE_ID, ManagedIdentityIdType.ResourceId}};

    }

    @Test(dataProvider = "msiWrongIDs", expectedExceptions = MsalManagedIdentityException.class,
    expectedExceptionsMessageRegExp = USER_ASSIGNED_ID_DOES_NOT_EXIST )
    public void acquireTokenUsingWrongClientID(TestConstants.MsiAzureResource azureResource, String userIdentity, ManagedIdentityIdType idType) throws Exception {
            //Get the Environment Variables
            Map<String, String> envVariables =
                    getEnvironmentVariables(azureResource);

            //Set the Environment Variables
            setEnvironmentVariables(envVariables);

            //form the http proxy URI
            String uri = S_BASE_URL + "MSIToken?" +
            "azureresource=" + azureResource + "&uri=";


            //Create CCA with Proxy
            ManagedIdentityApplication managedIdentityApplication = createManagedIdentityApplicationWithProxy(uri, userIdentity, idType);

        managedIdentityApplication.acquireTokenForManagedIdentity(ManagedIdentityParameters.builder(S_MSI_SCOPES).forceRefresh(false).build());

            //Assert
//            Assert.assertTrue(ex.getMessage().contains(UserAssignedIdDoesNotExist));
//            Assert.assertEquals(AbstractManagedIdentity.AppService, ex.ManagedIdentitySource);
        }

    @DataProvider(name = "msiWrongIDsForFunctions")
    public static Object[][] msiWrongIDsForFunctions(){
        return new Object[][]{{TestConstants.MsiAzureResource.Function, NON_EXISTENT_USER_ASSIGNED_CLIENT_ID, ManagedIdentityIdType.ClientId},
                {TestConstants.MsiAzureResource.Function, NON_EXISTENT_UAMI_RESOURCE_ID, ManagedIdentityIdType.ResourceId}};

    }

  @Test(dataProvider = "msiWrongIDsForFunctions", expectedExceptions = MsalManagedIdentityException.class, expectedExceptionsMessageRegExp = "")
    public void functionAppErrorNotInExpectedFormatAsync(TestConstants.MsiAzureResource azureResource, String userIdentity, ManagedIdentityIdType idType) throws Exception {
        //Arrange
            //Get the Environment Variables
            Map<String, String> envVariables =
                    getEnvironmentVariables(azureResource);

            //Set the Environment Variables
            setEnvironmentVariables(envVariables);

            //form the http proxy URI
            String uri = S_BASE_URL + "MSIToken?" +
                "azureresource=" + azureResource + "&uri=";

            //Create CCA with Proxy
            ManagedIdentityApplication managedIdentityApplication = createManagedIdentityApplicationWithProxy(uri, userIdentity, idType);

            managedIdentityApplication
                        .acquireTokenForManagedIdentity(ManagedIdentityParameters.builder(S_MSI_SCOPES).forceRefresh(false).build());

            //Assert
//            Assert.assertEquals(AbstractManagedIdentity.AppService, ex.ManagedIdentitySource);
    }

    @DataProvider(name = "msiWebApps")
    public static Object[][] msiWebApps(){
        return new Object[][]{{TestConstants.MsiAzureResource.WebApp, "", ManagedIdentityIdType.SystemAssigned},
                {TestConstants.MsiAzureResource.WebApp, USER_ASSIGNED_CLIENT_ID, ManagedIdentityIdType.ClientId},
                {TestConstants.MsiAzureResource.WebApp, UAMI_RESOURCE_ID, ManagedIdentityIdType.ResourceId}};

    }

    @Test(dataProvider = "msiWebApps", expectedExceptions = MsalManagedIdentityException.class, expectedExceptionsMessageRegExp = MsalError.MANAGED_IDENTITY_REQUEST_FAILED )
    public void mSIWrongScopesAsync(TestConstants.MsiAzureResource azureResource, String userIdentity, ManagedIdentityIdType idType) throws Exception {
        //Arrange
            //Get the Environment Variables
            Map<String, String> envVariables =
                    getEnvironmentVariables(azureResource);

            //Set the Environment Variables
            setEnvironmentVariables(envVariables);

            //form the http proxy URI
            String uri = S_BASE_URL + "MSIToken?" +
                "azureresource=" + azureResource + "&uri=";

            //Create CCA with Proxy
            ManagedIdentityApplication managedIdentityApplication = createManagedIdentityApplicationWithProxy(uri, userIdentity, idType);

            managedIdentityApplication
                        .acquireTokenForManagedIdentity(ManagedIdentityParameters.builder(S_WRONG_MSI_SCOPES)
                                .forceRefresh(false).build());

            //Assert
//            Assert.assertTrue(ex.ErrorCode == MsalError.ManagedIdentityRequestFailed);
//            Assert.assertEquals(AbstractManagedIdentity.AppService, ex.ManagedIdentitySource);
    }

   /// Gets the environment variable
    private Map<String, String> getEnvironmentVariables(
            TestConstants.MsiAzureResource resource)
    {
        Map<String, String> environmentVariables = new HashMap<>();
        //Get the Environment Variables from the MSI Helper Service
        String uri = S_BASE_URL + "EnvironmentVariables?resource=" + resource;

        String environmentVariableResponse = labUserProvider
            .getMSIEnvironmentVariables(uri);

        //process the response
        if (!StringHelper.isNullOrBlank(environmentVariableResponse))
        {
            environmentVariables = JsonHelper.convertJsonToObject(environmentVariableResponse, Map.class);
        }

        return environmentVariables;
    }

    /** Create the ManagedIdentityApplication with the http proxy
    /// <param name="url"></param>
    /// <param name="userAssignedId"></param>
     <returns></returns> */
    private ManagedIdentityApplication createManagedIdentityApplicationWithProxy(String url, String userAssignedId, ManagedIdentityIdType idType) {

        ManagedIdentityApplication.Builder managedIdentityApplicationbuilder;

        if (!StringHelper.isNullOrBlank(userAssignedId)) {
            if (ManagedIdentityIdType.ClientId.equals(idType)) {
                managedIdentityApplicationbuilder = ManagedIdentityApplication
                        .builder(ManagedIdentityId.UserAssignedClientId(userAssignedId))
                        .httpClient(new MSIHttpClient(url)); //proxy the MSI token request
            } else {
                managedIdentityApplicationbuilder = ManagedIdentityApplication
                        .builder(ManagedIdentityId.UserAssignedResourceId(userAssignedId))
                        .httpClient(new MSIHttpClient(url));
            }
        } else {
            managedIdentityApplicationbuilder = ManagedIdentityApplication
                    .builder(ManagedIdentityId.SystemAssigned())
                    .httpClient(new MSIHttpClient(url));
        }

        return managedIdentityApplicationbuilder.build();
    }
}
