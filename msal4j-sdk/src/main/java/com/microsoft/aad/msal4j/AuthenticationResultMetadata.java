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

    private TokenSource tokenSource;
    private Long refreshOn;
}