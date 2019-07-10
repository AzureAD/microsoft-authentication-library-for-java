// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter(AccessLevel.PACKAGE)
@AllArgsConstructor
abstract class MsalRequest {
    private final ClientApplicationBase application;

    AbstractMsalAuthorizationGrant msalAuthorizationGrant;

    private final RequestContext requestContext;

    @Getter(value = AccessLevel.PACKAGE, lazy = true)
    private final ClientDataHttpHeaders headers = new ClientDataHttpHeaders(requestContext.getCorrelationId());
}


