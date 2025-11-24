// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

public class LabUserHelper {

    private static final LabServiceApi labService = new LabServiceApi();

    /**
     * Fetch user password from Key Vault.
     * Uses the MSID Lab vault (different from configuration vault).
     *
     * @param userLabName The lab name of the user (used as secret name)
     * @return The user's password
     */
    static String fetchUserPassword(String userLabName) {
        return KeyVaultRegistry.getMsidLabProvider().getUserPassword(userLabName);
    }

    public static LabResponse getDefaultUser() {
        return KeyVaultRegistry.getMsalTeamProvider().mergeLabResponses("MSAL-User-Default-JSON", "ID4SLAB1", "MSAL-App-Default-JSON");
    }

    public static LabResponse getDefaultUserMultiTenantApp() {
        return KeyVaultRegistry.getMsalTeamProvider().mergeLabResponses("MSAL-User-Default-JSON", "ID4SLAB1", "MSAL-APP-AzureADMultipleOrgs-JSON");
    }

    //Used to avoid AADSTS7000218 credential errors in certain public client scenarios
    public static LabResponse getDefaultUserMultiTenantAppPublicClient() {
        return KeyVaultRegistry.getMsalTeamProvider().mergeLabResponses("MSAL-User-Default-JSON", "ID4SLAB1", "MSAL-APP-AzureADMultipleOrgsPC-JSON");
    }

    public static LabResponse getDefaultAdfsUser() {
        return KeyVaultRegistry.getMsalTeamProvider().mergeLabResponses("MSAL-USER-FedDefault-JSON", "ID4SLAB1", "MSAL-App-Default-JSON");
    }

    public static LabResponse getB2CLocalAccount() {
        return labService.getLabUserData(UserQuery.b2cLocalAccountQuery());
    }

    public static LabResponse getArlingtonUser() {
        // Query the Lab API with Arlington-specific parameters
        return labService.getLabUserData(UserQuery.arlingtonUserQuery());
    }

    public static LabResponse getCiamCudUser() {
        return labService.getLabUserData(UserQuery.ciamCudUserQuery());
    }

    public static LabResponse getMSAUser() {
        return labService.getLabUserData(UserQuery.msaUserQuery());
    }
}