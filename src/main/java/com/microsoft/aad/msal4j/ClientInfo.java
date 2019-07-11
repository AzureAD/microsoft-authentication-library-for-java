// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.google.gson.annotations.SerializedName;
import com.nimbusds.jose.util.StandardCharset;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.Base64;

@Getter(AccessLevel.PACKAGE)
class ClientInfo {

    @SerializedName("uid")
    private String uniqueIdentifier;

    @SerializedName("utid")
    private String unqiueTenantIdentifier;

    public static ClientInfo createFromJson(String clientInfoJsonBase64Encoded){
        if(StringHelper.isBlank(clientInfoJsonBase64Encoded)){
           return null;
        }
        byte[] decodedInput =  Base64.getDecoder().decode(clientInfoJsonBase64Encoded.getBytes(StandardCharset.UTF_8));

        return JsonHelper.convertJsonToObject(new String(decodedInput, StandardCharset.UTF_8), ClientInfo.class);
    }

    String toAccountIdentifier(){
        return uniqueIdentifier + "." + unqiueTenantIdentifier;
    }
}
