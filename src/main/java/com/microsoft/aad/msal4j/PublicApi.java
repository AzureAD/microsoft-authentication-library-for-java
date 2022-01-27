// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

enum PublicApi {

    ACQUIRE_TOKEN_BY_REFRESH_TOKEN(82),
    ACQUIRE_TOKEN_INTERACTIVE(168),
    ACQUIRE_TOKEN_BY_USERNAME_PASSWORD(300),
    ACQUIRE_TOKEN_BY_INTEGRATED_WINDOWS_AUTH(400),
    ACQUIRE_TOKEN_ON_BEHALF_OF(522),
    ACQUIRE_TOKEN_BY_DEVICE_CODE_FLOW(620),
    ACQUIRE_TOKEN_FOR_CLIENT(729),
    ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE(831),
    ACQUIRE_TOKEN_SILENTLY(800),
    GET_ACCOUNTS(801),
    REMOVE_ACCOUNTS(802);

    private final int apiId;

    PublicApi(int apiId) {
        this.apiId = apiId;
    }

    int getApiId() {
        return apiId;
    }
}
