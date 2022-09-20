// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Values for possible methods in which AAD can send the authorization result back to the calling
 * application
 */
public enum ResponseMode {

    /**
     * Authorization result is encoded as HTML form values that are transmitted via a HTTP POST
     * to the redirect URL
     */
    FORM_POST("form_post"),

    /**
     * Authorization result returned as query string in the redirect URL when redirecting back to the
     * client application.
     */
    QUERY("query"),

    /**
     * Authorization result is returned in the fragment added to the redirect URL when redirecting
     * back to the client application
     */
    FRAGMENT("fragment");

    private String responseMode;

    ResponseMode(String responseMode) {
        this.responseMode = responseMode;
    }

    @Override
    public String toString() {
        return this.responseMode;
    }
}
