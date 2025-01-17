// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * Contains metadata and additional context for the contents of an AuthenticationResult
 */
@Accessors(fluent = true)
@Getter
@Setter(AccessLevel.PACKAGE)
@Builder
public class AuthenticationResultMetadata implements Serializable {

    /**
     * The source of the tokens in the {@link AuthenticationResult}, see {@link TokenSource} for possible values
     */
    private TokenSource tokenSource;

    /**
     * When the token should be proactively refreshed. May be null or 0 if proactive refresh is not used
     */
    private Long refreshOn;

    /**
     * Specifies the reason for refreshing a token, see {@link CacheRefreshReason} for possible values.Will be {@link CacheRefreshReason#NOT_APPLICABLE} if the token was not refreshed
     */
    @Builder.Default
    private CacheRefreshReason cacheRefreshReason = CacheRefreshReason.NOT_APPLICABLE;
}