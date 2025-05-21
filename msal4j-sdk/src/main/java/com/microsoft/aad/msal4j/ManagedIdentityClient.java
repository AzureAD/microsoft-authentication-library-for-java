// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Class to initialize a managed identity and identify the service.
 */
class ManagedIdentityClient {
    private static final Logger LOG = LoggerFactory.getLogger(ManagedIdentityClient.class);
    private static String WINDOWS_HIMDS_FILEPATH  = "%Programfiles%\\AzureConnectedMachineAgent\\himds.exe";
    private static String LINUX_HIMDS_FILEPATH = "/opt/azcmagent/bin/himds";

    static ManagedIdentitySourceType getManagedIdentitySource() {
        IEnvironmentVariables environmentVariables = AbstractManagedIdentitySource.getEnvironmentVariables();

        if (!StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable(Constants.IDENTITY_ENDPOINT)) &&
                !StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable(Constants.IDENTITY_HEADER))) {
            if (!StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable(Constants.IDENTITY_SERVER_THUMBPRINT))) {
                return ManagedIdentitySourceType.SERVICE_FABRIC;
            } else {
                return ManagedIdentitySourceType.APP_SERVICE;
            }
        } else if (!StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable(Constants.MSI_ENDPOINT))) {
            return ManagedIdentitySourceType.CLOUD_SHELL;
        } else if (validateAzureArcEnvironment(environmentVariables)) {
            return ManagedIdentitySourceType.AZURE_ARC;
        } else {
            return ManagedIdentitySourceType.DEFAULT_TO_IMDS;
        }
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

    ManagedIdentityClient(){
        // Default constructor for testing
    }

    ManagedIdentityResponse getManagedIdentityResponse(ManagedIdentityParameters parameters) {
        return managedIdentitySource.getManagedIdentityResponse(parameters);
    }

    // This method tries to create managed identity source for different sources, if none is created then defaults to IMDS.
    private static AbstractManagedIdentitySource createManagedIdentitySource(MsalRequest msalRequest,
            ServiceBundle serviceBundle) {

        switch (getManagedIdentitySource()) {
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

    static boolean validateAzureArcEnvironment(IEnvironmentVariables environmentVariables) {
        if (!StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable(Constants.IDENTITY_ENDPOINT)) &&
                !StringHelper.isNullOrBlank(environmentVariables.getEnvironmentVariable(Constants.IMDS_ENDPOINT))) {
            LOG.info("[Managed Identity] Azure Arc managed identity is available through environment variables.");
            return true;
        }

        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("windows")) {
            File windowsFile = new File(WINDOWS_HIMDS_FILEPATH);
            if (windowsFile.exists()) {
                LOG.info("[Managed Identity] Azure Arc managed identity is available through file detection.");
                return true;
            }
        } else if (osName.contains("linux")) {
            File linuxFile = new File(LINUX_HIMDS_FILEPATH);
            if (linuxFile.exists()) {
                LOG.info("[Managed Identity] Azure Arc managed identity is available through file detection.");
                return true;
            }
        } else {
            LOG.warn("[Managed Identity] Azure Arc managed identity cannot be configured on a platform other than Windows and Linux.");
        }

        LOG.info("[Managed Identity] Azure Arc managed identity is not available.");
        return false;
    }

    //These set methods should be used solely for automated testing.
    //  -The file paths are not normally customizable in this flow, as they should exist in an Azure Arc environment.
    //  -However, unit tests need some way to adjust them as part of mocking the environment and creating temporary files.
    void setWindowsFilePath(String filePath) {
        WINDOWS_HIMDS_FILEPATH = filePath;
    }

    void setLinuxFilePath(String filePath) {
        LINUX_HIMDS_FILEPATH = filePath;
    }
}