// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.util.StandardCharset;
import lombok.AccessLevel;
import lombok.Getter;

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

        return JsonHelper.convertJsonToObject(new String(decodedInput, StandardCharset.UTF_8), ClientInfo.class);
    }

    String toAccountIdentifier() {
        return uniqueIdentifier + POINT_DELIMITER + uniqueTenantIdentifier;
    }
}
