package com.microsoft.aad.msal4j;

import java.util.concurrent.CompletableFuture;

/** Class to initialize a managed identity and identify the service.
 Original source of code: https://github.com/Azure/azure-sdk-for-net/blob/main/sdk/identity/Azure.Identity/src/ManagedIdentityClient.cs */
public class ManagedIdentityClient {

    private AbstractManagedIdentity managedIdentitySource;

    public ManagedIdentityClient(RequestContext requestContext)
    {
        managedIdentitySource = selectManagedIdentitySource(requestContext);
    }

    public ManagedIdentityResponse sendTokenRequest(ManagedIdentityParameters parameters)
    {
        return managedIdentitySource.authenticate(parameters);
    }

    // This method tries to create managed identity source for different sources, if none is created then defaults to IMDS.
    private static AbstractManagedIdentity selectManagedIdentitySource(RequestContext requestContext)
    {
        AbstractManagedIdentity managedIdentitySource;
        if((managedIdentitySource = ServiceFabricManagedIdentity.tryCreate(requestContext)) != null){
            return managedIdentitySource;
        }else if((managedIdentitySource = AppServiceManagedIdentity.tryCreate(requestContext)) != null ){
            return managedIdentitySource;
        }else if((managedIdentitySource = CloudShellManagedIdentity.tryCreate(requestContext)) != null ){
            return managedIdentitySource;
        }else if((managedIdentitySource = AzureArcManagedIdentity.tryCreate(requestContext)) != null ){
            return managedIdentitySource;
        }else{
            return new IMDSManagedIdentity(requestContext);
        }
    }
}
