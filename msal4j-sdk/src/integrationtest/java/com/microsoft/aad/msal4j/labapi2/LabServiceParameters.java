// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

public class LabServiceParameters {

    public enum FederationProvider {
        NONE,        // No federation
        CIAM,     // CIAM
        CIAMCUD,     // CIAM CUD
    }

    public enum B2CIdentityProvider {
        LOCAL,       // Local B2C account
        MSA
    }

    public enum UserType {
        B2C,
        CLOUD,
        FEDERATED,
        ONPREM,
        MSA
    }

    public enum MFA {
        NONE,
    }

    public enum ProtectionPolicy {
        NONE,
    }

    public enum SignInAudience
    {
        AzureAdMyOrg
    }
}