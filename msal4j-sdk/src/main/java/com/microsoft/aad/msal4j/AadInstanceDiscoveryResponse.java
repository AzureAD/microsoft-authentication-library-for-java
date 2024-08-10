// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter(AccessLevel.PACKAGE)
class AadInstanceDiscoveryResponse {

    @JsonProperty("tenant_discovery_endpoint")
    private String tenantDiscoveryEndpoint;

    @JsonProperty("metadata")
    private InstanceDiscoveryMetadataEntry[] metadata;

    @JsonProperty("error_description")
    private String errorDescription;

    @JsonProperty("error_codes")
    private long[] errorCodes;

    @JsonProperty("error")
    private String error;

    @JsonProperty("correlation_id")
    private String correlationId;

    @JsonProperty("authorization_endpoint")
    private String authorizationEndpoint;

    @JsonProperty("token_endpoint")
    private String tokenEndpoint;

    @JsonProperty("device_authorization_endpoint")
    private String deviceCodeEndpoint;
}
