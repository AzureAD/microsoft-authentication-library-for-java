// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.nimbusds.jose.util.StandardCharset;
import lombok.AccessLevel;
import lombok.Getter;

import java.io.IOException;
import java.util.Base64;

import static com.microsoft.aad.msal4j.Constants.POINT_DELIMITER;

@Getter(AccessLevel.PACKAGE)
class ClientInfo {

    @JsonProperty("uid")
    private String uniqueIdentifier;

    @JsonProperty("utid")
    private String uniqueTenantIdentifier;

    public static ClientInfo createFromJson(String clientInfoJsonBase64Encoded) {
        if (StringHelper.isBlank(clientInfoJsonBase64Encoded)) {
            return null;
        }

        byte[] decodedInput = Base64.getUrlDecoder().decode(clientInfoJsonBase64Encoded.getBytes(StandardCharset.UTF_8));

        String jsonString = new String(decodedInput, StandardCharset.UTF_8);

        try {
            return convertJsonToObject(jsonString);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    String toAccountIdentifier() {
        return uniqueIdentifier + POINT_DELIMITER + uniqueTenantIdentifier;
    }

    public static ClientInfo convertJsonToObject(String json) throws IOException{

        JsonFactory jsonFactory = new JsonFactory();
        ClientInfo clientInfo;
        try (JsonParser jsonParser = jsonFactory.createParser(json)) {
            clientInfo = new ClientInfo();
            if (json != null) {

                if (jsonParser.nextToken().equals(JsonToken.START_ARRAY)) {
                    jsonParser.nextToken();
                }

                while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                    String fieldname = jsonParser.getCurrentName();
                    if ("uid".equals(fieldname)) {
                        jsonParser.nextToken();
                        clientInfo.uniqueIdentifier = jsonParser.getText();
                    }

                    if ("utid".equals(fieldname)) {
                        jsonParser.nextToken();
                        clientInfo.uniqueTenantIdentifier = jsonParser.getText();
                    }
                }

            }
        }

        return clientInfo;
    }
}
