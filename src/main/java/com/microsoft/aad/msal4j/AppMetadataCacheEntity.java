// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Accessors(fluent = true)
@Getter
@Setter
class AppMetadataCacheEntity {

    public static final String APP_METADATA_CACHE_ENTITY_ID = "appmetadata";

    @SerializedName("client_id")
    private String clientId;

    @SerializedName("environment")
    private String environment;

    @SerializedName("family_id")
    private String familyId;

    String getKey(){
        List<String> keyParts = new ArrayList<>();

        keyParts.add(APP_METADATA_CACHE_ENTITY_ID);
        keyParts.add(environment);
        keyParts.add(clientId);

        return String.join(Constants.CACHE_KEY_SEPARATOR, keyParts).toLowerCase();
    }
}
