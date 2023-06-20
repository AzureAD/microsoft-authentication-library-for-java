// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Getter;

public class ManagedIdentityId {

    @Getter
    private String userAssignedId;
    @Getter
    private ManagedIdentityIdType idType;

    private ManagedIdentityId(ManagedIdentityIdType idType)
    {
        this.idType = idType;
    }

    private ManagedIdentityId(ManagedIdentityIdType idType, String id) {
        this.idType = idType;
        this.userAssignedId = id;
    }

    /**
     * Create an instance of a system assigned managed identity.
     * @return Instance of ManagedIdentityId.
     */
    public static ManagedIdentityId SystemAssigned() {
        return new ManagedIdentityId(ManagedIdentityIdType.SystemAssigned);
    }

    /**
     * Create an instance of ManagedIdentityId for a user assigned managed identity from a client id.
     * @param clientId Client id of the user assigned managed identity assigned to azure resource.
     * @return Instance of ManagedIdentityId
     * @exception NullPointerException
     */
    public static ManagedIdentityId UserAssignedClientId(String clientId) {
        if (StringHelper.isNullOrBlank(clientId)) {
            throw new NullPointerException(clientId);
        }

        return new ManagedIdentityId(ManagedIdentityIdType.ClientId, clientId);
    }

    /**
     * Create an instance of ManagedIdentityId for a user assigned managed identity from a resource id.
     * @param resourceId Resource ID of the user assigned managed identity assigned to azure resource.
     * @return Instance of ManagedIdentityId
     * @exception NullPointerException
     */
    public static ManagedIdentityId UserAssignedResourceId(String resourceId)
    {
        if (StringHelper.isNullOrBlank(resourceId))
        {
            throw new NullPointerException(resourceId);
        }

        return new ManagedIdentityId(ManagedIdentityIdType.ResourceId, resourceId);
    }
}
