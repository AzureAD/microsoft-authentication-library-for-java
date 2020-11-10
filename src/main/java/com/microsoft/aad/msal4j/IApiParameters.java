// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Set;

interface IApiParameters {
    Set<String> scopes();

    ClaimsRequest withClaims();
}
