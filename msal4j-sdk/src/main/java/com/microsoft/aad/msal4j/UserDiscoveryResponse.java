// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

class UserDiscoveryResponse implements JsonSerializable<UserDiscoveryResponse> {

    private float version;
    private String accountType;
    private String federationMetadataUrl;
    private String federationProtocol;
    private String federationActiveAuthUrl;
    private String cloudAudienceUrn;

    boolean isAccountFederated() {
        return !StringHelper.isBlank(this.accountType)
                && this.accountType.equalsIgnoreCase("Federated");
    }

    boolean isAccountManaged() {
        return !StringHelper.isBlank(this.accountType)
                && this.accountType.equalsIgnoreCase("Managed");
    }

    /**
     * TODO: Add description
     */
    public static UserDiscoveryResponse fromJson(JsonReader jsonReader) throws IOException {
        UserDiscoveryResponse response = new UserDiscoveryResponse();
        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();
                switch (fieldName) {
                    case "ver":
                        response.version = Float.parseFloat(reader.getString());
                        break;
                    case "account_type":
                        response.accountType = reader.getString();
                        break;
                    case "federation_metadata_url":
                        response.federationMetadataUrl = reader.getString();
                        break;
                    case "federation_protocol":
                        response.federationProtocol = reader.getString();
                        break;
                    case "federation_active_auth_url":
                        response.federationActiveAuthUrl = reader.getString();
                        break;
                    case "cloud_audience_urn":
                        response.cloudAudienceUrn = reader.getString();
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return response;
        });
    }

    /**
     * TODO: Add description
     */
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();
        jsonWriter.writeFloatField("ver", version);
        jsonWriter.writeStringField("account_type", accountType);
        jsonWriter.writeStringField("federation_metadata_url", federationMetadataUrl);
        jsonWriter.writeStringField("federation_protocol", federationProtocol);
        jsonWriter.writeStringField("federation_active_auth_url", federationActiveAuthUrl);
        jsonWriter.writeStringField("cloud_audience_urn", cloudAudienceUrn);
        jsonWriter.writeEndObject();
        return jsonWriter;
    }

    float version() {
        return this.version;
    }

    String accountType() {
        return this.accountType;
    }

    String federationMetadataUrl() {
        return this.federationMetadataUrl;
    }

    String federationProtocol() {
        return this.federationProtocol;
    }

    String federationActiveAuthUrl() {
        return this.federationActiveAuthUrl;
    }

    String cloudAudienceUrn() {
        return this.cloudAudienceUrn;
    }
}
