// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi;

/**
 * Helper class to provide configuration needed by integration tests, such as Azure user and app info.
 *
 * The returned LabResponse objects merge user, app, and lab configuration that represent a specific test scenario.
 */
public class LabConfigHelper {

    /**
     * Most common configuration used by integration tests, meant for public cloud and managed user scenarios.
     */
    public static LabResponse getDefaultConfig() {
        return KeyVaultRegistry.getMsalTeamProvider().mergeLabResponses("MSAL-User-Default-JSON", "ID4SLAB1", "MSAL-App-Default-JSON");
    }

    /**
     * Configuration for multi-tenant public client app scenarios.
     *
     * Avoids AADSTS7000218 credential errors in certain public client scenarios.
     */
    public static LabResponse getMultiTenantAppPublicClientConfig() {
        return KeyVaultRegistry.getMsalTeamProvider().mergeLabResponses("MSAL-User-Default-JSON", "ID4SLAB1", "MSAL-APP-AzureADMultipleOrgsPC-JSON");
    }

    /**
     * Configuration for ADFS federated user scenarios.
     */
    public static LabResponse getAdfsConfig() {
        return KeyVaultRegistry.getMsalTeamProvider().mergeLabResponses("MSAL-USER-FedDefault-JSON", "ID4SLAB1", "MSAL-App-Default-JSON");
    }

    /**
     * Configuration for B2C user scenarios.
     */
    public static LabResponse getB2CConfig() {
        return KeyVaultRegistry.getMsalTeamProvider().mergeLabResponses("MSAL-USER-B2C-JSON", "ID4SLAB1", "MSAL-App-B2C-JSON");
    }

    /**
     * Configuration for Arlington/US government cloud scenarios.
     */
    public static LabResponse getArlingtonConfig() {
        return KeyVaultRegistry.getMsalTeamProvider().mergeLabResponses("MSAL-USER-Arlington-JSON", "ARLMSIDLAB1", "MSAL-App-Arlington-JSON");
    }

    /**
     * Configuration for CIAM scenarios.
     */
    public static LabResponse getCiamConfig() {
        return KeyVaultRegistry.getMsalTeamProvider().mergeLabResponses("MSAL-USER-CIAM-JSON", "ARLMSIDLAB1", "MSAL-App-CIAM-JSON");
    }
}