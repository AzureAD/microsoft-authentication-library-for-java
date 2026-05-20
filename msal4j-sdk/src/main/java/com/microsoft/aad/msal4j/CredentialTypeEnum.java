// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

enum CredentialTypeEnum {

    ACCESS_TOKEN("AccessToken"),
    ACCESS_TOKEN_EXTENDED("AText"),
    REFRESH_TOKEN("RefreshToken"),
    ID_TOKEN("IdToken");

    private final String value;

    CredentialTypeEnum(String value) {
        this.value = value;
    }

    String value() {
        return this.value;
    }
}
