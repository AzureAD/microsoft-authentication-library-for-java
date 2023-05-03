package com.microsoft.aad.msal4j;

public interface IManagedIdentityApplication extends IClientApplicationBase {

    /**
     * Acquires token for a managed identity configured on Azure resource. See https://aka.ms/msal-net-managed-identity.
     *
     * @param resource resource requested to access the protected API. For this flow (managed identity), the resource
     *                 should be of the form "{ResourceIdUri}" or {ResourceIdUri/.default} for instance <c>https://management.azure.net</c> or, for Microsoft
     *                 Graph, <c>https://graph.microsoft.com/.default</c>.
     * @return A builder enabling you to add optional parameters before executing the token request
     * You can also chain the following optional parameters:
     * "AcquireTokenForManagedIdentityParameterBuilder.WithForceRefresh(bool)"
     */
    AuthenticationResult acquireTokenForManagedIdentity(String resource);
}
