// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
class CurrentRequest {

    private final PublicApi publicApi;

    @Setter
    private int cacheInfo = -1;

    @Setter
    private String regionUsed = StringHelper.EMPTY_STRING;

    @Setter
    private int regionSource = 0;

    @Setter
    private int regionOutcome = 0;

    CurrentRequest(PublicApi publicApi){
        this.publicApi = publicApi;
    }
}