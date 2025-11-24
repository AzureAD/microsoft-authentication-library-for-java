// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi;

public class LabUserHelper {

    private static final LabApiService labService = new LabApiService();

    public static LabResponse getDefaultUser() {
        return KeyVaultRegistry.getMsalTeamProvider().mergeLabResponses("MSAL-User-Default-JSON", "ID4SLAB1", "MSAL-App-Default-JSON");
    }

    public static LabResponse getDefaultUserMultiTenantApp() {
        return KeyVaultRegistry.getMsalTeamProvider().mergeLabResponses("MSAL-User-Default-JSON", "ID4SLAB1", "MSAL-APP-AzureADMultipleOrgs-JSON");
    }

    //Avoids AADSTS7000218 credential errors in certain public client scenarios
    public static LabResponse getDefaultUserMultiTenantAppPublicClient() {
        return KeyVaultRegistry.getMsalTeamProvider().mergeLabResponses("MSAL-User-Default-JSON", "ID4SLAB1", "MSAL-APP-AzureADMultipleOrgsPC-JSON");
    }

    public static LabResponse getDefaultAdfsUser() {
        return KeyVaultRegistry.getMsalTeamProvider().mergeLabResponses("MSAL-USER-FedDefault-JSON", "ID4SLAB1", "MSAL-App-Default-JSON");
    }

    public static LabResponse getB2CLocalAccount() {
        return labService.getLabUserData(UserQueryHelper.b2cLocalAccountQuery());
    }

    public static LabResponse getArlingtonUser() {
        return labService.getLabUserData(UserQueryHelper.arlingtonUserQuery());
    }

    public static LabResponse getCiamCudUser() {
        return labService.getLabUserData(UserQueryHelper.ciamCudUserQuery());
    }

    public static LabResponse getMSAUser() {
        return labService.getLabUserData(UserQueryHelper.msaUserQuery());
    }
}