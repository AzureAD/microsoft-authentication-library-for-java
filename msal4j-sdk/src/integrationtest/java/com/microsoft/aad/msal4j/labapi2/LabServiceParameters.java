// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

class LabServiceParameters {

    enum FederationProvider {
        CIAMCUD     // CIAM CUD
    }

    enum B2CIdentityProvider {
        LOCAL       // Local B2C account
    }

    enum UserType {
        B2C,
        CLOUD,
        FEDERATED,
        MSA
    }

    enum MFA {
        NONE
    }

    enum ProtectionPolicy {
        NONE
    }

    enum SignInAudience {
        AzureAdMyOrg
    }
}