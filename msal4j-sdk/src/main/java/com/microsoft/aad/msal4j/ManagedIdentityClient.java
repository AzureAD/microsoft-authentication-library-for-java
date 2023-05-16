// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/** Class to initialize a managed identity and identify the service.
 Original source of code: https://github.com/Azure/azure-sdk-for-net/blob/main/sdk/identity/Azure.Identity/src/ManagedIdentityClient.cs */
public class ManagedIdentityClient {

    private AbstractManagedIdentitySource managedIdentitySource;

    public ManagedIdentityClient(RequestContext requestContext, ServiceBundle serviceBundle)
    {
        managedIdentitySource = createManagedIdentitySource(requestContext, serviceBundle);
    }

    public ManagedIdentityResponse sendTokenRequest(ManagedIdentityParameters parameters)
    {
        return managedIdentitySource.getManagedIdentityResponse(parameters);
    }

    // This method tries to create managed identity source for different sources, if none is created then defaults to IMDS.
    private static AbstractManagedIdentitySource createManagedIdentitySource(RequestContext requestContext,
                                                                             ServiceBundle serviceBundle)
    {
        AbstractManagedIdentitySource managedIdentitySource;
        if((managedIdentitySource = ServiceFabricManagedIdentity.create(requestContext, serviceBundle)) != null){
            return managedIdentitySource;
        }else if((managedIdentitySource = AppServiceManagedIdentity.create(requestContext, serviceBundle)) != null ){
            return managedIdentitySource;
        }else if((managedIdentitySource = CloudShellManagedIdentity.create(requestContext, serviceBundle)) != null ){
            return managedIdentitySource;
        }else if((managedIdentitySource = AzureArcManagedIdentity.create(requestContext, serviceBundle)) != null ){
            return managedIdentitySource;
        }else{
            return new IMDSManagedIdentity(requestContext, serviceBundle);
        }
    }
}
