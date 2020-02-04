// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class Lab {
    @JsonProperty("labName")
    String labName;

    @JsonProperty("domain")
    String domain;

    @JsonProperty("tenantId")
    String tenantId;

    @JsonProperty("federationProvider")
    String federationProvider;

    @JsonProperty("azureEnvironment")
    String azureEnvironment;

    @JsonProperty("authority")
    String authority;
}
