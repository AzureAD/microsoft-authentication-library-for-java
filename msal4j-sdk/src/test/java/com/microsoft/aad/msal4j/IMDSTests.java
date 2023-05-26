// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.testng.annotations.Test;

import static com.microsoft.aad.msal4j.ManagedIdentityTestUtils.setEnvironmentVariables;

public class IMDSTests {

    @Test(expectedExceptions = MsalClientException.class,
            expectedExceptionsMessageRegExp = "The requested identity has not been assigned to this resource.")
    public static void badRequest() throws Exception{

        setEnvironmentVariables(ManagedIdentitySourceType.Imds, "http://169.254.169.254");

        ManagedIdentityApplication managedIdentityApplication = ManagedIdentityApplication.builder(ManagedIdentityId.SystemAssigned())
                .build();

        ManagedIdentityApplication managedIdentityApplication1 = ManagedIdentityApplication.builder(ManagedIdentityId.UserAssignedClientId(""))
                .build();

//        httpManager.AddManagedIdentityMockHandler(TestConfiguration.IMDS_ENDPOINT, TestConfiguration.RESOURCE, MockHelpers.GetMsiImdsErrorResponse(),
//                ManagedIdentitySourceType.Imds, statusCode: HttpStatusCode.BadRequest);

        ManagedIdentityParameters managedIdentityParameters = ManagedIdentityParameters.builder(TestConfiguration.RESOURCE)
                .forceRefresh(false).build();

        managedIdentityApplication.acquireTokenForManagedIdentity(managedIdentityParameters);

//        Assert.IsNotNull(ex);
//        Assert.AreEqual(ManagedIdentitySource.Imds, ex.ManagedIdentitySource);
//        Assert.AreEqual(MsalError.ManagedIdentityRequestFailed, ex.ErrorCode);
//        Assert.IsTrue(ex.Message.Contains("The requested identity has not been assigned to this resource."));





    }
}
