// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter(AccessLevel.PACKAGE)
class AadInstanceDiscoveryResponse {

    @SerializedName("tenant_discovery_endpoint")
    private String tenantDiscoveryEndpoint;

    @SerializedName("metadata")
    private InstanceDiscoveryMetadataEntry[] metadata;

    @SerializedName("error_description")
    private String errorDescription;

    @SerializedName("error_codes")
    private long[] errorCodes;

    @SerializedName("error")
    private String error;

    @SerializedName("correlation_id")
    private String correlationId;
}
