// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter(AccessLevel.PACKAGE)
class UserDiscoveryResponse {

    @SerializedName("ver")
    private float version;

    @SerializedName("account_type")
    private String accountType;

    @SerializedName("federation_metadata_url")
    private String federationMetadataUrl;

    @SerializedName("federation_protocol")
    private String federationProtocol;

    @SerializedName("federation_active_auth_url")
    private String federationActiveAuthUrl;

    @SerializedName("cloud_audience_urn")
    private String cloudAudienceUrn;

    boolean isAccountFederated() {
        return !StringHelper.isBlank(this.accountType)
                && this.accountType.equalsIgnoreCase("Federated");
    }

    boolean isAccountManaged() {
        return !StringHelper.isBlank(this.accountType)
                && this.accountType.equalsIgnoreCase("Managed");
    }
}
