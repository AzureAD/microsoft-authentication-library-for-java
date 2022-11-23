// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.*;

@Accessors(fluent = true)
@Getter(AccessLevel.PACKAGE)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class InstanceDiscoveryMetadataEntry {

    @JsonProperty("preferred_network")
    String preferredNetwork;

    @JsonProperty("preferred_cache")
    String preferredCache;

    @JsonProperty("aliases")
    Set<String> aliases;
}
