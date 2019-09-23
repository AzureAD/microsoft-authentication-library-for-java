// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
class LastRequest {

    private final PublicApi publicApi;
    private final String correlationId;

    @Setter
    private String errorCode;

    LastRequest(PublicApi publicApi, String correlationId){
        this.publicApi = publicApi;
        this.correlationId = correlationId;
    }
}
