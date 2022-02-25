// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public static AadInstanceDiscoveryResponse convertJsonToObject(String json) throws IOException {

        AadInstanceDiscoveryResponse aadInstanceDiscoveryResponse = new AadInstanceDiscoveryResponse();

        if (json != null) {
            JsonFactory jsonFactory = new JsonFactory();
            try (JsonParser jsonParser = jsonFactory.createParser(json)) {


                while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                    String fieldname = jsonParser.getCurrentName();
                    if ("tenant_discovery_endpoint".equals(fieldname)) {
                        jsonParser.nextToken();
                        aadInstanceDiscoveryResponse.tenantDiscoveryEndpoint = jsonParser.getText();
                    } else if ("metadata".equals(fieldname)) {
                        jsonParser.nextToken();
                        List<InstanceDiscoveryMetadataEntry> list = new ArrayList<>();
                        while (jsonParser.currentToken() != JsonToken.END_ARRAY) {
                            InstanceDiscoveryMetadataEntry instanceDiscoveryMetadataEntry = InstanceDiscoveryMetadataEntry.convertJsonToObject(json, jsonParser);
                            list.add(instanceDiscoveryMetadataEntry);
                            jsonParser.nextToken();
                        }
                        InstanceDiscoveryMetadataEntry[] instanceDiscoveryMetadataEntryArray = new InstanceDiscoveryMetadataEntry[list.size()];
                        aadInstanceDiscoveryResponse.metadata = list.toArray(instanceDiscoveryMetadataEntryArray);

                    } else if ("error".equals(fieldname)) {
                        jsonParser.nextToken();
                        aadInstanceDiscoveryResponse.error = jsonParser.getText();
                    } else if ("error_description".equals(fieldname)) {
                        jsonParser.nextToken();
                        aadInstanceDiscoveryResponse.errorDescription = jsonParser.getText();
                    } else if ("error_codes".equals(fieldname)) {
                        jsonParser.nextToken();
                        List<Long> errorCodesList = new ArrayList<>();
                        while (jsonParser.currentToken() != JsonToken.END_ARRAY) {
                            errorCodesList.add(jsonParser.getLongValue());
                            jsonParser.nextToken();
                        }
                        aadInstanceDiscoveryResponse.errorCodes = errorCodesList.stream().mapToLong(l -> l).toArray();
                    } else if ("correlation_id".equals(fieldname)) {
                        jsonParser.nextToken();
                        aadInstanceDiscoveryResponse.correlationId = jsonParser.getText();
                    }

                }
            }
        }
        return aadInstanceDiscoveryResponse;
    }
}
