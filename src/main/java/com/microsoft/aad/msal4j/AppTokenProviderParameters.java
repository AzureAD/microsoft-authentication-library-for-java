// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
/// The authentication parameters provided to the app token provider callback.
public class AppTokenProviderParameters {

    /// Specifies which scopes to request.
    public Set<String> scopes;
    /// Correlation id of the authentication request.
    public String correlationId;
    /// A string with one or multiple claims.
    public String claims;
    /// tenant id
    public String tenantId;
}
