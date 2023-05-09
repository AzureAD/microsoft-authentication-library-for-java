// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

public class ManagedIdentityId {

    private String userAssignedId;
    private ManagedIdentityIdType idType;

    private ManagedIdentityId(ManagedIdentityIdType idType)
    {
        this.idType = idType;
    }

    private ManagedIdentityId(ManagedIdentityIdType idType, String id) {
        this.idType = idType;
        this.userAssignedId = id;
    }

    // Create an instance of ManagedIdentityConfiguration for a system assigned managed identity.
    public static ManagedIdentityId SystemAssigned()
    {
        return new ManagedIdentityId(ManagedIdentityIdType.SystemAssigned);
    }
    /** Create an instance of ManagedIdentityConfiguration for a user assigned managed identity from a client id.
     <param name="clientId">Client id of the user assigned managed identity assigned to azure resource.</param>
     <returns>Instance of ManagedIdentityConfiguration.</returns>
     <exception cref="NullPointerException"></exception> */
    public static ManagedIdentityId UserAssignedClientId(String clientId)
    {
        if (StringHelper.isNullOrBlank(clientId))
        {
            throw new NullPointerException(clientId);
        }

        return new ManagedIdentityId(ManagedIdentityIdType.ClientId, clientId);
    }

    /** Create an instance of ManagedIdentityConfiguration for a user assigned managed identity from a resource id.
     <param name="resourceId">Resource id of the user assigned managed identity assigned to azure resource.</param>
     <returns>Instance of ManagedIdentityConfiguration.</returns>
     <exception cref="NullPointerException"></exception> */
    public static ManagedIdentityId UserAssignedResourceId(String resourceId)
    {
        if (StringHelper.isNullOrBlank(resourceId))
        {
            throw new NullPointerException(resourceId);
        }

        return new ManagedIdentityId(ManagedIdentityIdType.ResourceId, resourceId);
    }
}
