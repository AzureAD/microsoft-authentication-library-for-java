// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Enum containing HTTP Content-Type header values
 */
enum HTTPContentType {

    ApplicationURLEncoded("application/x-www-form-urlencoded; charset=UTF-8"),
    ApplicationJSON("application/json; charset=UTF-8");

    public final String contentType;

    HTTPContentType(String contentType) {
        this.contentType = contentType;
    }
}
