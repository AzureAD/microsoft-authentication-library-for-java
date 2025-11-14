// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

public class LabServiceParameters {

    public enum FederationProvider {
        ADFS_V4,
        @Deprecated // ADFSv3 is out of support, do not use. The Arlington lab is federated to ADFSv3, so this value is needed
        ADFS_V3,
        ADFS_V2019,
    }

    public enum B2CIdentityProvider {
        LOCAL,       // Local B2C account
    }

    public enum UserType {
        B2C,
        CLOUD,
        FEDERATED,
    }

    public enum MFA {
        NONE,
    }

    public enum ProtectionPolicy {
        NONE,
    }
}