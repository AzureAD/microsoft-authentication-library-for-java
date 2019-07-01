// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
@AllArgsConstructor
enum CredentialTypeEnum {

    ACCESS_TOKEN("AccessToken"),
    REFRESH_TOKEN("RefreshToken"),
    ID_TOKEN("IdToken");

    private final String value;
}
