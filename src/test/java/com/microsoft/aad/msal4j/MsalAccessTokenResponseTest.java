// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.msal4j;

import java.text.ParseException;

import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = { "checkin" })
public class MsalAccessTokenResponseTest extends AbstractMsalTests {

    private final String idToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6Ik5HVEZ2ZEstZnl0aEV1THdqcHdBSk9NOW4tQSJ9."
            + "eyJhdWQiOiIyMTZlZjgxZC1mM2IyLTQ3ZDQtYWQyMS1hNGRmNDliNTZkZWUiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5l"
            + "dC9kM2VhYjEzMi1iM2Y3LTRkNzktOTM5Yy0zMDIyN2FlYjhjMjYvIiwiaWF0IjoxMzkzNDk2MDM3LCJuYmYiOjEzOTM0OTYwMzcsI"
            + "mV4cCI6MTM5MzQ5OTkzNywidmVyIjoiMS4wIiwidGlkIjoiZDNlYWIxMzItYjNmNy00ZDc5LTkzOWMtMzAyMjdhZWI4YzI2Iiwib2l"
            + "kIjoiMzZiNjE4MTMtM2EyYi00NTA4LWFlOGQtZmM3NTQyMDE3NTlhIiwidXBuIjoibWVAa2FuaXNoa3BhbndhcmhvdG1haWwub25ta"
            + "WNyb3NvZnQuY29tIiwidW5pcXVlX25hbWUiOiJtZUBrYW5pc2hrcGFud2FyaG90bWFpbC5vbm1pY3Jvc29mdC5jb20iLCJzdWIiOiJ"
            + "mZU40RU4wTW1vQ3ZubFZoRk1KeWozMzRSd0NaTGxrdTFfMVQ1VlNSN0xrIiwiZmFtaWx5X25hbWUiOiJQYW53YXIiLCJnaXZlbl9uYW"
            + "1lIjoiS2FuaXNoayIsIm5vbmNlIjoiYTM1OWY0MGItNDJhOC00YTRjLTk2YWMtMTE0MjRhZDk2N2U5IiwiY19oYXNoIjoib05kOXE1e"
            + "m1fWTZQaUNpYTg1MDZUQSJ9.iyGfoL0aKai-rZVGFwaCYm73h2Dk93M80CRAOoIwlxAKfGrQ2YDbvAPIvlQUrNQacqzenmkJvVEMqXT"
            + "OYO5teyweUkxruod_iMgmhC6RZZZ603vMoqItUVu8c-4Y3KIEweRi17BYjdR2_tEowPlcEteRY52nwCmiNJRQnkqnQ2aZP89Jzhb9qw"
            + "_G3CeYsOmV4f7jUp7anDT9hae7eGuvdUAf4LTDD6hFTBJP8MsyuMD6DkgBytlSxaXXJBKBJ5r5XPHdtStCTNF7edktlSufA2owTWVGw"
            + "gWpKmnue_2Mgl3jBozTSJJ34r-R6lnWWeN6lqZ2Svw7saI5pmPtC8OZbw";

    @Test
    public void testConstructor() throws ParseException {
        final MsalAccessTokenResponse response = new MsalAccessTokenResponse(
                new BearerAccessToken("access_token"), new RefreshToken(
                        "refresh_token"), idToken);
        Assert.assertNotNull(response);
        OIDCTokens tokens = response.getOIDCTokens();
        Assert.assertNotNull(tokens);
        final JWT jwt = tokens.getIDToken();
        Assert.assertTrue(jwt.getJWTClaimsSet().getClaims().size() >= 0);
    }

    @Test
    public void testParseJsonObject()
            throws com.nimbusds.oauth2.sdk.ParseException {
        final MsalAccessTokenResponse response = MsalAccessTokenResponse
                .parseJsonObject(JSONObjectUtils
                        .parseJSONObject(TestConfiguration.HTTP_RESPONSE_FROM_AUTH_CODE));
        Assert.assertNotNull(response);
        OIDCTokens tokens = response.getOIDCTokens();
        Assert.assertNotNull(tokens);
        Assert.assertNotNull(tokens.getIDToken());
        Assert.assertFalse(StringHelper.isBlank(tokens.getIDTokenString()));
        Assert.assertFalse(StringHelper.isBlank(response.getScope()));
    }

    @Test
    public void testEmptyIdToken() throws ParseException {
        final MsalAccessTokenResponse response = new MsalAccessTokenResponse(
                new BearerAccessToken(idToken), new RefreshToken(
                        "refresh_token"), "");

        Assert.assertNotNull(response);
        OIDCTokens tokens = response.getOIDCTokens();
        Assert.assertNotNull(tokens);
        final AccessToken accessToken = tokens.getAccessToken();
        Assert.assertNotNull(accessToken);
    }
}
