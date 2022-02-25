// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.util.*;

@Accessors(fluent = true)
@Getter(AccessLevel.PACKAGE)
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

    public static InstanceDiscoveryMetadataEntry convertJsonToObject(String json, JsonParser jsonParser) throws IOException {

        InstanceDiscoveryMetadataEntry instanceDiscoveryMetadataEntry = new InstanceDiscoveryMetadataEntry();
        if(json!=null){

            while(jsonParser.nextToken()!= JsonToken.END_OBJECT){
                String fieldname = jsonParser.getCurrentName();
                if ("preferred_network".equals(fieldname)) {
                    jsonParser.nextToken();
                    instanceDiscoveryMetadataEntry.preferredNetwork = jsonParser.getText();
                }

                else if ("preferred_cache".equals(fieldname)) {
                    jsonParser.nextToken();
                    instanceDiscoveryMetadataEntry.preferredCache = jsonParser.getText();
                }

                else if ("aliases".equals(fieldname)) {
                    jsonParser.nextToken();
                    jsonParser.nextToken();
                    Set<String> aliasesSet = new HashSet<>();
                    while(jsonParser.currentToken()!=JsonToken.END_ARRAY){
                         aliasesSet.add(jsonParser.getValueAsString());
                         jsonParser.nextToken();
                    }
                    instanceDiscoveryMetadataEntry.aliases = aliasesSet;
                }

            }

        }

        return instanceDiscoveryMetadataEntry;

    }
}
