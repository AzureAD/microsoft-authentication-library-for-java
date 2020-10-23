// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import lombok.AccessLevel;
import lombok.Getter;
import net.minidev.json.JSONObject;

@Getter(AccessLevel.PACKAGE)
class TokenResponse extends OIDCTokenResponse {

    private String scope;
    private String clientInfo;
    private long expiresIn;
    private long extExpiresIn;
    private String foci;
    private long refreshIn;

    TokenResponse(final AccessToken accessToken, final RefreshToken refreshToken, final String idToken,
                  final String scope, String clientInfo, long expiresIn, long extExpiresIn, String foci,
                  long refreshIn) {
        super(new OIDCTokens(idToken, accessToken, refreshToken));
        this.scope = scope;
        this.clientInfo = clientInfo;
        this.expiresIn = expiresIn;
        this.extExpiresIn = extExpiresIn;
        this.refreshIn = refreshIn;
        this.foci = foci;
    }

    static TokenResponse parseHttpResponse(final HTTPResponse httpResponse) throws ParseException {

        httpResponse.ensureStatusCode(HTTPResponse.SC_OK);

        final JSONObject jsonObject = httpResponse.getContentAsJSONObject();

        return parseJsonObject(jsonObject);
    }

    static Long getLongValue(JSONObject jsonObject, String key) throws ParseException {
        Object value = jsonObject.get(key);

        if(value instanceof Long){
            return JSONObjectUtils.getLong(jsonObject, key);
        }
        else
        {
            return Long.parseLong(JSONObjectUtils.getString(jsonObject, key));
        }
    }

    static TokenResponse parseJsonObject(final JSONObject jsonObject)
            throws ParseException {

        // In same cases such as client credentials there isn't an id token. Instead of a null value
        // use an empty string in order to avoid an IllegalArgumentException from OIDCTokens.
        String idTokenValue = "";
        if (jsonObject.containsKey("id_token")) {
            idTokenValue = JSONObjectUtils.getString(jsonObject, "id_token");
        }

        // Parse value
        String scopeValue = null;
        if (jsonObject.containsKey("scope")) {
            scopeValue = JSONObjectUtils.getString(jsonObject, "scope");
        }

        String clientInfo = null;
        if (jsonObject.containsKey("client_info")) {
            clientInfo = JSONObjectUtils.getString(jsonObject, "client_info");
        }

        long expiresIn = 0;
        if (jsonObject.containsKey("expires_in")) {
            expiresIn = getLongValue(jsonObject, "expires_in");
        }

        long ext_expires_in = 0;
        if (jsonObject.containsKey("ext_expires_in")) {
            ext_expires_in = getLongValue(jsonObject, "ext_expires_in");
        }

        String foci = null;
        if (jsonObject.containsKey("foci")) {
            foci = JSONObjectUtils.getString(jsonObject, "foci");
        }

        long refreshIn = 0;
        if (jsonObject.containsKey("refresh_in")) {
            refreshIn = getLongValue(jsonObject, "refresh_in");
        }

        try {
            final AccessToken accessToken = AccessToken.parse(jsonObject);
            final RefreshToken refreshToken = RefreshToken.parse(jsonObject);
            return new TokenResponse(accessToken, refreshToken, idTokenValue, scopeValue, clientInfo,
                    expiresIn, ext_expires_in, foci, refreshIn);
        } catch (ParseException e) {
            throw new MsalClientException("Invalid or missing token, could not parse. If using B2C, information on a potential B2C issue and workaround can be found here: https://aka.ms/msal4j-b2c-known-issues",
                    AuthenticationErrorCode.INVALID_JSON);
        } catch (Exception e) {
            throw new MsalClientException(e);
        }
    }
}
