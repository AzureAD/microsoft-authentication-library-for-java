// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.*;

@Accessors(fluent = true)
@Getter(AccessLevel.PACKAGE)
@Builder
class InstanceDiscoveryMetadataEntry {

    @SerializedName("preferred_network")
    String preferredNetwork;

    @SerializedName("preferred_cache")
    String preferredCache;

    @SerializedName("aliases")
    Set<String> aliases;

    @SerializedName("expires_on")
    long expiresOn;

    public static class InstanceDiscoveryMetadataEntryBuilder{
        public InstanceDiscoveryMetadataEntryBuilder aliases(String[] aliasesArray) {
            Set<String> set =  new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

            if(aliasesArray != null){
                set.addAll(Arrays.asList(aliasesArray));
            }
            aliases = Collections.unmodifiableSet(set);

            return this;
        }
    }
}
