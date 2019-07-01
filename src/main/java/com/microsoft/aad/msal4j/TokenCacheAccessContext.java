// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * State of token cache
 */
@Builder
@Accessors(fluent = true)
@Getter
public class TokenCacheAccessContext implements ITokenCacheAccessContext {

    private ITokenCache tokenCache;

    private String clientId;

    private IAccount account;

    private boolean hasCacheChanged;
}
