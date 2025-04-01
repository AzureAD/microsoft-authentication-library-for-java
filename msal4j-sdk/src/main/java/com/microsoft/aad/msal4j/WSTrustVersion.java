// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

enum WSTrustVersion {

    WSTRUST13(
            "//s:Envelope/s:Body/wst:RequestSecurityTokenResponseCollection/wst:RequestSecurityTokenResponse/wst:TokenType",
            "wst:RequestedSecurityToken"), WSTRUST2005(
            "//s:Envelope/s:Body/t:RequestSecurityTokenResponse/t:TokenType",
            "t:RequestedSecurityToken"), UNDEFINED("", "");
    private String responseTokenTypePath = "";
    private String responseSecurityTokenPath = "";

    WSTrustVersion(String tokenType, String responseSecurityToken) {
        this.responseTokenTypePath = tokenType;
        this.responseSecurityTokenPath = responseSecurityToken;
    }

    String responseTokenTypePath() {
        return this.responseTokenTypePath;
    }

    String responseSecurityTokenPath() {
        return this.responseSecurityTokenPath;
    }
}
