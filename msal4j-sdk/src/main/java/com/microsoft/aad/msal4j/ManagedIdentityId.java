// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Getter;

@Getter
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

    /**
     * Create an instance of a system assigned managed identity.
     *
     * @return Instance of ManagedIdentityId.
     */
    public static ManagedIdentityId systemAssigned() {
        return new ManagedIdentityId(ManagedIdentityIdType.SYSTEM_ASSIGNED);
    }

    /**
     * Create an instance of ManagedIdentityId for a user assigned managed identity from a client id.
     *
     * @param clientId Client id of the user assigned managed identity assigned to azure resource.
     * @return Instance of ManagedIdentityId
     * @exception NullPointerException Indicates the clientId param is null or blank
     */
    public static ManagedIdentityId userAssignedClientId(String clientId) {
        if (StringHelper.isNullOrBlank(clientId)) {
            throw new NullPointerException(clientId);
        }

        return new ManagedIdentityId(ManagedIdentityIdType.CLIENT_ID, clientId);
    }

    /**
     * Create an instance of ManagedIdentityId for a user assigned managed identity from a resource id.
     *
     * @param resourceId Resource ID of the user assigned managed identity assigned to azure resource.
     * @return Instance of ManagedIdentityId
     * @exception NullPointerException Indicates the resourceId param is null or blank
     */
    public static ManagedIdentityId userAssignedResourceId(String resourceId)
    {
        if (StringHelper.isNullOrBlank(resourceId))
        {
            throw new NullPointerException(resourceId);
        }

        return new ManagedIdentityId(ManagedIdentityIdType.RESOURCE_ID, resourceId);
    }

    /**
     * Create an instance of ManagedIdentityId for a user assigned managed identity from an object id.
     *
     * @param objectId Object ID of the user assigned managed identity assigned to azure resource.
     * @return Instance of ManagedIdentityId
     * @exception NullPointerException Indicates the resourceId param is null or blank
     */
    public static ManagedIdentityId userAssignedObjectId(String objectId)
    {
        if (StringHelper.isNullOrBlank(objectId))
        {
            throw new NullPointerException(objectId);
        }

        return new ManagedIdentityId(ManagedIdentityIdType.OBJECT_ID, objectId);
    }
}
