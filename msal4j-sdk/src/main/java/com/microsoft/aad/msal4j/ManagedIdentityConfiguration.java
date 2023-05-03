package com.microsoft.aad.msal4j;

public class ManagedIdentityConfiguration {

    private String userAssignedId;
    private ManagedIdentityIdType idType;

    private ManagedIdentityConfiguration(ManagedIdentityIdType idType)
    {
        this.idType = idType;
    }

    private ManagedIdentityConfiguration(ManagedIdentityIdType idType, String id) {
        this.idType = idType;
        this.userAssignedId = id;
    }

    // Create an instance of ManagedIdentityConfiguration for a system assigned managed identity.
    public static ManagedIdentityConfiguration SystemAssigned()
    {
        return new ManagedIdentityConfiguration(ManagedIdentityIdType.SystemAssigned);
    }
    /** Create an instance of ManagedIdentityConfiguration for a user assigned managed identity from a client id.
     <param name="clientId">Client id of the user assigned managed identity assigned to azure resource.</param>
     <returns>Instance of ManagedIdentityConfiguration.</returns>
     <exception cref="NullPointerException"></exception> */
    public static ManagedIdentityConfiguration UserAssignedClientId(String clientId)
    {
        if (StringHelper.isNullOrBlank(clientId))
        {
            throw new NullPointerException(clientId);
        }

        return new ManagedIdentityConfiguration(ManagedIdentityIdType.ClientId, clientId);
    }

    /** Create an instance of ManagedIdentityConfiguration for a user assigned managed identity from a resource id.
     <param name="resourceId">Resource id of the user assigned managed identity assigned to azure resource.</param>
     <returns>Instance of ManagedIdentityConfiguration.</returns>
     <exception cref="NullPointerException"></exception> */
    public static ManagedIdentityConfiguration UserAssignedResourceId(String resourceId)
    {
        if (StringHelper.isNullOrBlank(resourceId))
        {
            throw new NullPointerException(resourceId);
        }

        return new ManagedIdentityConfiguration(ManagedIdentityIdType.ResourceId, resourceId);
    }
}
