// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

class AppMetadataCacheEntity {

    public static final String APP_METADATA_CACHE_ENTITY_ID = "appmetadata";

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("environment")
    private String environment;

    @JsonProperty("family_id")
    private String familyId;

    String getKey() {
        List<String> keyParts = new ArrayList<>();

        keyParts.add(APP_METADATA_CACHE_ENTITY_ID);
        keyParts.add(environment);
        keyParts.add(clientId);

        return String.join(Constants.CACHE_KEY_SEPARATOR, keyParts).toLowerCase();
    }

    String clientId() {
        return this.clientId;
    }

    String environment() {
        return this.environment;
    }

    String familyId() {
        return this.familyId;
    }

    void clientId(String clientId) {
        this.clientId = clientId;
    }

    void environment(String environment) {
        this.environment = environment;
    }

    void familyId(String familyId) {
        this.familyId = familyId;
    }
}
