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

    public static UserDiscoveryResponse convertJsonToObject(String json) throws IOException {

        JsonFactory jsonFactory = new JsonFactory();
        UserDiscoveryResponse userDiscoveryResponse;
        try (JsonParser jsonParser = jsonFactory.createParser(json)) {

            userDiscoveryResponse = new UserDiscoveryResponse();
            if (json != null) {

                while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                    String fieldname = jsonParser.getCurrentName();
                    if ("ver".equals(fieldname)) {
                        jsonParser.nextToken();
                        userDiscoveryResponse.version = Float.parseFloat(jsonParser.getValueAsString());
                    }

                    if ("account_type".equals(fieldname)) {
                        jsonParser.nextToken();
                        userDiscoveryResponse.accountType = jsonParser.getText();
                    }

                    if ("federation_metadata_url".equals(fieldname)) {
                        jsonParser.nextToken();
                        userDiscoveryResponse.federationMetadataUrl = jsonParser.getText();
                    }

                    if ("federation_protocol".equals(fieldname)) {
                        jsonParser.nextToken();
                        userDiscoveryResponse.federationProtocol = jsonParser.getText();
                    }

                    if ("federation_active_auth_url".equals(fieldname)) {
                        jsonParser.nextToken();
                        userDiscoveryResponse.federationActiveAuthUrl = jsonParser.getText();
                    }

                    if ("cloud_audience_urn".equals(fieldname)) {
                        jsonParser.nextToken();
                        userDiscoveryResponse.cloudAudienceUrn = jsonParser.getText();
                    }

                }
            }
        }
        return userDiscoveryResponse;

    }
}
