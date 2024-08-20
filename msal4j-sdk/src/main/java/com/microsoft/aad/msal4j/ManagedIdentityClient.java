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

    private static ManagedIdentitySourceType managedIdentitySourceType;

    protected static void resetManagedIdentitySourceType() {
        managedIdentitySourceType = ManagedIdentitySourceType.NONE;
    }

    static ManagedIdentitySourceType getManagedIdentitySource() {
        if (managedIdentitySourceType != null && managedIdentitySourceType != ManagedIdentitySourceType.NONE) {
            return managedIdentitySourceType;
        }

        IEnvironmentVariables environmentVariables = AbstractManagedIdentitySource.getEnvironmentVariables();

        if (!StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable(Constants.IDENTITY_ENDPOINT)) &&
                !StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable(Constants.IDENTITY_HEADER))) {
            if (!StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable(Constants.IDENTITY_SERVER_THUMBPRINT))) {
                managedIdentitySourceType = ManagedIdentitySourceType.SERVICE_FABRIC;
            } else {
                managedIdentitySourceType = ManagedIdentitySourceType.APP_SERVICE;
            }
        } else if (!StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable(Constants.MSI_ENDPOINT))) {
            managedIdentitySourceType = ManagedIdentitySourceType.CLOUD_SHELL;
        } else if (!StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable(Constants.IDENTITY_ENDPOINT)) &&
                !StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable(Constants.IMDS_ENDPOINT))) {
            managedIdentitySourceType = ManagedIdentitySourceType.AZURE_ARC;
        } else {
            managedIdentitySourceType = ManagedIdentitySourceType.DEFAULT_TO_IMDS;
        }

        return managedIdentitySourceType;
    }

    AbstractManagedIdentitySource managedIdentitySource;

    ManagedIdentityClient(MsalRequest msalRequest, ServiceBundle serviceBundle) {
        managedIdentitySource = createManagedIdentitySource(msalRequest, serviceBundle);

        ManagedIdentityApplication managedIdentityApplication = (ManagedIdentityApplication) msalRequest.application();
        ManagedIdentityIdType identityIdType = managedIdentityApplication.getManagedIdentityId().getIdType();
        if (!identityIdType.equals(ManagedIdentityIdType.SYSTEM_ASSIGNED)) {
            managedIdentitySource.setUserAssignedManagedIdentity(true);
        }
    }

    ManagedIdentityResponse getManagedIdentityResponse(ManagedIdentityParameters parameters) {
        return managedIdentitySource.getManagedIdentityResponse(parameters);
    }

    // This method tries to create managed identity source for different sources, if none is created then defaults to IMDS.
    private static AbstractManagedIdentitySource createManagedIdentitySource(MsalRequest msalRequest,
            ServiceBundle serviceBundle) {

        if (managedIdentitySourceType == null || managedIdentitySourceType == ManagedIdentitySourceType.NONE) {
            managedIdentitySourceType = getManagedIdentitySource();
        }

        switch (managedIdentitySourceType) {
            case SERVICE_FABRIC:
                return ServiceFabricManagedIdentitySource.create(msalRequest, serviceBundle);
            case APP_SERVICE:
                return AppServiceManagedIdentitySource.create(msalRequest, serviceBundle);
            case CLOUD_SHELL:
                return CloudShellManagedIdentitySource.create(msalRequest, serviceBundle);
            case AZURE_ARC:
                return AzureArcManagedIdentitySource.create(msalRequest, serviceBundle);
            default:
                return new IMDSManagedIdentitySource(msalRequest, serviceBundle);
        }
    }
}