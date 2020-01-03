// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import lombok.AccessLevel;
import lombok.Getter;
import net.minidev.json.JSONObject;

@Getter(AccessLevel.PACKAGE)
class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private String idToken;
    private String scope;
    private String clientInfo;
    private long expiresIn;
    private long extExpiresIn;
    private String foci;

    TokenResponse(final String accessToken, final String refreshToken, final String idToken,
                  final String scope, String clientInfo, long expiresIn, long extExpiresIn, String foci) {

        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.idToken = idToken; //TODO: Do we assume that IdToken won't be null or empty
        this.scope = scope;
        this.clientInfo = clientInfo;
        this.expiresIn = expiresIn;
        this.extExpiresIn = extExpiresIn;
        this.foci = foci;
    }

    static TokenResponse parseHttpResponse(final HTTPResponse httpResponse) throws ParseException {

        httpResponse.ensureStatusCode(HTTPResponse.SC_OK);

        final JSONObject jsonObject = httpResponse.getContentAsJSONObject();

        return parseJsonObject(jsonObject);
    }

    static TokenResponse parseJsonObject(final JSONObject jsonObject)
            throws ParseException {

        // In some B2C scenarios, no access or refresh tokens will be returned.
        String accessToken = null;
        if (jsonObject.containsKey("access_token")) {
            accessToken = jsonObject.getAsString("access_token");
        }

        String refreshToken = null;
        if (jsonObject.containsKey("refresh_token")) {
            refreshToken = jsonObject.getAsString("refresh_token");
        }

        // In some cases such as client credentials there isn't an id token.
        String idTokenValue = null;
        if (jsonObject.containsKey("id_token")) {
            idTokenValue = jsonObject.getAsString("id_token");
        }

        String scopeValue = null;
        if (jsonObject.containsKey("scope")) {
            scopeValue = jsonObject.getAsString("scope");
        }

        String clientInfo = null;
        if (jsonObject.containsKey("client_info")) {
            clientInfo = jsonObject.getAsString("client_info");
        }

        long expiresIn = 0;
        if (jsonObject.containsKey("expires_in")) {
            expiresIn = jsonObject.getAsNumber("expires_in").longValue();
        }

        long ext_expires_in = 0;
        if (jsonObject.containsKey("ext_expires_in")) {
            ext_expires_in = jsonObject.getAsNumber("ext_expires_in").longValue();
        }

        String foci = null;
        if (jsonObject.containsKey("foci")) {
            foci = JSONObjectUtils.getString(jsonObject, "foci");
        }

        return new TokenResponse(accessToken, refreshToken, idTokenValue, scopeValue, clientInfo,
                expiresIn, ext_expires_in, foci);
    }
}
