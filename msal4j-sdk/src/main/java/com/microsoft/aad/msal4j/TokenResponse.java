// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;

class TokenResponse {

    private String scope;
    private String clientInfo;
    private long expiresIn;
    private long extExpiresIn;
    private String foci;
    private long refreshIn;
    private String accessToken;
    private String idToken;
    private String refreshToken;
    private String tokenType;

    TokenResponse(Map<String, String> jsonMap) {
        this.accessToken = jsonMap.get("access_token");
        this.idToken = jsonMap.get("id_token");
        this.refreshToken = jsonMap.get("refresh_token");
        this.tokenType = jsonMap.get("token_type");
        this.scope = jsonMap.get("scope");
        this.clientInfo = jsonMap.get("client_info");
        this.expiresIn = StringHelper.isNullOrBlank(jsonMap.get("expires_in")) ? 0 : Long.parseLong(jsonMap.get("expires_in"));
        this.extExpiresIn = StringHelper.isNullOrBlank(jsonMap.get("ext_expires_in")) ? 0 : Long.parseLong(jsonMap.get("ext_expires_in"));
        this.refreshIn = StringHelper.isNullOrBlank(jsonMap.get("refresh_in")) ? 0: Long.parseLong(jsonMap.get("refresh_in"));
        this.foci = jsonMap.get("foci");
    }

    static TokenResponse parseHttpResponse(final HttpResponse httpResponse) {

        if (httpResponse.statusCode() != HttpStatus.HTTP_OK) {
            throw MsalServiceExceptionFactory.fromHttpResponse(httpResponse);
        }

        return new TokenResponse(httpResponse.getBodyAsMap());
    }

    String getScope() {
        return this.scope;
    }

    String getClientInfo() {
        return this.clientInfo;
    }

    long getExpiresIn() {
        return this.expiresIn;
    }

    long getExtExpiresIn() {
        return this.extExpiresIn;
    }

    String getFoci() {
        return this.foci;
    }

    long getRefreshIn() {
        return this.refreshIn;
    }

    public String accessToken() {
        return accessToken;
    }

    public String idToken() {
        return idToken;
    }

    public String refreshToken() {
        return refreshToken;
    }

    String tokenType() {
        return tokenType;
    }
}
