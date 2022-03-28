// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter(AccessLevel.PACKAGE)
class UserDiscoveryResponse {

    @JsonProperty("ver")
    private float version;

    @JsonProperty("account_type")
    private String accountType;

    @JsonProperty("federation_metadata_url")
    private String federationMetadataUrl;

    @JsonProperty("federation_protocol")
    private String federationProtocol;

    @JsonProperty("federation_active_auth_url")
    private String federationActiveAuthUrl;

    @JsonProperty("cloud_audience_urn")
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
