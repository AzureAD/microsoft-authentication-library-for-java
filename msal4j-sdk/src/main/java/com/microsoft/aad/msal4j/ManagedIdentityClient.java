// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to initialize a managed identity and identify the service.
 */
class ManagedIdentityClient {
    private static final Logger LOG = LoggerFactory.getLogger(ManagedIdentityClient.class);

    AbstractManagedIdentitySource managedIdentitySource;

    ManagedIdentityClient(MsalRequest msalRequest, ServiceBundle serviceBundle) {
        managedIdentitySource = createManagedIdentitySource(msalRequest, serviceBundle);

        ManagedIdentityApplication managedIdentityApplication = (ManagedIdentityApplication) msalRequest.application();
        ManagedIdentityIdType identityIdType = managedIdentityApplication.getManagedIdentityId().getIdType();
        if (!identityIdType.equals(ManagedIdentityIdType.SYSTEM_ASSIGNED)) {
            managedIdentitySource.setUserAssignedManagedIdentity(true);
            String userAssignedId = managedIdentityApplication.getManagedIdentityId().getUserAssignedId();
            if (identityIdType.equals(ManagedIdentityIdType.CLIENT_ID)) {
                managedIdentitySource.setManagedIdentityUserAssignedClientId(userAssignedId);
            } else if (identityIdType.equals(ManagedIdentityIdType.RESOURCE_ID)) {
                managedIdentitySource.setManagedIdentityUserAssignedResourceId(userAssignedId);
            }
        }
    }

    ManagedIdentityResponse getManagedIdentityResponse(ManagedIdentityParameters parameters) {
        return managedIdentitySource.getManagedIdentityResponse(parameters);
    }

    // This method tries to create managed identity source for different sources, if none is created then defaults to IMDS.
    private static AbstractManagedIdentitySource createManagedIdentitySource(MsalRequest msalRequest,
            ServiceBundle serviceBundle) {
        AbstractManagedIdentitySource managedIdentitySource;
        if ((managedIdentitySource = ServiceFabricManagedIdentitySource.create(msalRequest, serviceBundle)) != null) {
            return managedIdentitySource;
        } else if ((managedIdentitySource = AppServiceManagedIdentitySource.create(msalRequest, serviceBundle)) != null) {
            return managedIdentitySource;
        } else if ((managedIdentitySource = CloudShellManagedIdentitySource.create(msalRequest, serviceBundle)) != null) {
            return managedIdentitySource;
        } else if ((managedIdentitySource = AzureArcManagedIdentitySource.create(msalRequest, serviceBundle)) != null) {
            return managedIdentitySource;
        } else {
            return new IMDSManagedIdentitySource(msalRequest, serviceBundle);
        }
    }
}