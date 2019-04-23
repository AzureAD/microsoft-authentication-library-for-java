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

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import net.minidev.json.JSONObject;
import org.easymock.EasyMock;
import org.json.JSONException;
import org.powermock.api.easymock.PowerMock;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class CacheTests extends AbstractMsalTests {
    String APP_DATA = "/app_data.json";

    String TOKEN_RESPONSE = "/token_response.json";
    String TOKEN_RESPONSE_ID_TOKEN = "/token_response_id_token.json";

    String AT_CACHE_ENTITY_KEY = "/at_cache_entity_key.txt";
    String AT_CACHE_ENTITY = "/at_cache_entity.json";

    String RT_CACHE_ENTITY_KEY = "/rt_cache_entity_key.txt";
    String RT_CACHE_ENTITY = "/rt_cache_entity.json";

    String ID_TOKEN_CACHE_ENTITY_KEY = "/id_token_cache_entity_key.txt";
    String ID_TOKEN_CACHE_ENTITY = "/id_token_cache_entity.json";

    String ACCOUNT_CACHE_ENTITY_KEY = "/account_cache_entity_key.txt";
    String ACCOUNT_CACHE_ENTITY = "/account_cache_entity.json";

    String ID_TOKEN_PLACEHOLDER = "<removed_id_token>";
    String CACHED_AT_PLACEHOLDER = "<cached_at>";
    String EXPIRES_ON_PLACEHOLDER = "<expires_on>";
    String EXTENDED_EXPIRES_ON_PLACEHOLDER = "<extended_expires_on>";

    class AppData{
        @SerializedName("client_id")
        String clientId;

        @SerializedName("authorize_request_url")
        String authorizeRequestUrl;
    }

    @Test
    public void cacheDeserializationSerializationTest() throws IOException, URISyntaxException, JSONException {
        TokenCache tokenCache = new TokenCache(null);

        String previouslyStoredCache = readResource("/cache_data/serialized_cache.json");

        tokenCache.deserializeAndLoadToCache(previouslyStoredCache);

        String serializedCache = tokenCache.serialize();

        JSONAssert.assertEquals(previouslyStoredCache, serializedCache, JSONCompareMode.STRICT);
    }

    String readResource(String resource) throws IOException, URISyntaxException {
        return new String(
                Files.readAllBytes(
                        Paths.get(getClass().getResource(resource).toURI())));
    }

    public class DynamicTimestampsComparator extends DefaultComparator {
        private Map<String, Long> expectations = new HashMap<>();

        List<String> timestampPlaceholders = Arrays.asList(CACHED_AT_PLACEHOLDER, EXPIRES_ON_PLACEHOLDER,
                EXTENDED_EXPIRES_ON_PLACEHOLDER);

        public DynamicTimestampsComparator(JSONCompareMode mode, long expiresIn, long extExpiresIn) {
            super(mode);

            long timestampSec = new Date().getTime() / 1000;

            expectations.put(CACHED_AT_PLACEHOLDER, timestampSec);
            expectations.put(EXPIRES_ON_PLACEHOLDER, timestampSec + expiresIn);
            expectations.put(EXTENDED_EXPIRES_ON_PLACEHOLDER, timestampSec + extExpiresIn);
        }

        @Override
        public void compareValues(String s, Object o, Object o1, JSONCompareResult jsonCompareResult) throws JSONException {
            if (timestampPlaceholders.contains(o.toString())) {
                long timestamp = Long.parseLong(o1.toString());

                if (Math.abs(timestamp - expectations.get(o.toString())) > 1) {
                    jsonCompareResult.fail("timestamp for " + s + " differ more than 1 sec : " + timestamp + " : "
                            + expectations.get(o.toString()) );
                    return;
                }
                return;
            }
            super.compareValues(s, o, o1, jsonCompareResult);
        }
    }

    @Test
    public void AADTokenCacheEntitiesFormatTest() throws JSONException, IOException, ParseException, URISyntaxException {
        tokenCacheEntitiesFormatTest("/AAD_cache_data");
    }

    @Test
    public void MSATokenCacheEntitiesFormatTest() throws JSONException, IOException, ParseException, URISyntaxException {
        tokenCacheEntitiesFormatTest("/MSA_cache_data");
    }

    public void tokenCacheEntitiesFormatTest(String folder) throws URISyntaxException, IOException, ParseException, JSONException {
        AppData appData = new GsonBuilder().create().fromJson(readResource(folder + APP_DATA), AppData.class);

        String tokenResponse = getTokenResponse(folder);

        PublicClientApplication app = new PublicClientApplication.Builder(appData.clientId)
                .authority(appData.authorizeRequestUrl)
                .build();

        AuthorizationCodeParameters parameters =
                AuthorizationCodeParameters.builder
                        ("code", new URI("http://my.redirect.com"))
                        .build();

        MsalRequest msalRequest = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(appData.clientId, "correlation_id",
                        AcquireTokenPublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        ServiceBundle serviceBundle = new ServiceBundle(
                null,
                null,
                null,
                new TelemetryManager(null, false));

        TokenRequest request = PowerMock.createPartialMock(
                TokenRequest.class, new String[] { "toOauthHttpRequest" },
                new URL(appData.authorizeRequestUrl), msalRequest, serviceBundle);

        OAuthHttpRequest msalOAuthHttpRequest = PowerMock.createMock(OAuthHttpRequest.class);

        HTTPResponse httpResponse = PowerMock.createMock(HTTPResponse.class);

        EasyMock.expect(request.toOauthHttpRequest()).andReturn(msalOAuthHttpRequest).times(1);
        EasyMock.expect(msalOAuthHttpRequest.send()).andReturn(httpResponse).times(1);
        EasyMock.expect(httpResponse.getHeaderValue(EasyMock.isA(String.class))).andReturn(null).times(3);
        EasyMock.expect(httpResponse.getStatusCode()).andReturn(200).times(2);
        EasyMock.expect(httpResponse.getContentAsJSONObject())
                .andReturn(
                        JSONObjectUtils.parse(tokenResponse))
                .times(1);
        httpResponse.ensureStatusCode(200);
        EasyMock.expectLastCall();

        PowerMock.replay(request, msalOAuthHttpRequest, httpResponse);

        final AuthenticationResult result = request.executeOauthRequestAndProcessResponse();

        PowerMock.verifyAll();

        TokenCache tokenCache = new TokenCache();

        tokenCache.saveTokens(request, result, "login.microsoftonline.com");

        validateAccessTokenCacheEntity(folder, tokenResponse, tokenCache);
        validateRefreshTokenCacheEntity(folder, tokenCache);
        validateIdTokenCacheEntity(folder, tokenCache);
        validateAccountCacheEntity(folder, tokenCache);
    }

    private void validateAccessTokenCacheEntity(String folder, String tokenResponse, TokenCache tokenCache)
            throws IOException, URISyntaxException, ParseException, JSONException {

        Assert.assertEquals(tokenCache.accessTokens.size(),1 );

        String keyActual = tokenCache.accessTokens.keySet().stream().findFirst().get();
        String keyExpected = readResource(folder + AT_CACHE_ENTITY_KEY);
        Assert.assertEquals(keyActual, keyExpected);

        String valueActual = new GsonBuilder().create().toJson(tokenCache.accessTokens.get(keyActual));
        String valueExpected = readResource(folder + AT_CACHE_ENTITY);

        JSONObject tokenResponseJsonObj = JSONObjectUtils.parse(tokenResponse);
        long expireIn = Long.parseLong(tokenResponseJsonObj.getAsString("expires_in"));
        long extExpireIn = Long.parseLong(tokenResponseJsonObj.getAsString("ext_expires_in"));

        JSONAssert.assertEquals(valueExpected, valueActual,
                new DynamicTimestampsComparator(JSONCompareMode.STRICT, expireIn, extExpireIn));
    }

    private void validateRefreshTokenCacheEntity(String folder, TokenCache tokenCache)
            throws IOException, URISyntaxException, JSONException {

        Assert.assertEquals(tokenCache.refreshTokens.size(),1 );

        String actualKey = tokenCache.refreshTokens.keySet().stream().findFirst().get();
        String keyExpected = readResource(folder + RT_CACHE_ENTITY_KEY);
        Assert.assertEquals(actualKey, keyExpected);

        String actualValue = new GsonBuilder().create().toJson(tokenCache.refreshTokens.get(actualKey));
        String valueExpected = readResource(folder + RT_CACHE_ENTITY);
        JSONAssert.assertEquals(valueExpected, actualValue, JSONCompareMode.STRICT);
    }

    public class IdTokenComparator extends DefaultComparator {
        private String idToken;

        public IdTokenComparator(JSONCompareMode mode, String folder) throws IOException, URISyntaxException {
            super(mode);
            idToken = getIdToken(folder);
        }

        @Override
        public void compareValues(String s, Object o, Object o1, JSONCompareResult jsonCompareResult) throws JSONException {
            if (ID_TOKEN_PLACEHOLDER.equals(o.toString())) {

                if (!idToken.equals(o1.toString())) {
                    jsonCompareResult.fail("idTokens don not match ");
                    return;
                }
                return;
            }
            super.compareValues(s, o, o1, jsonCompareResult);
        }
    }

    private void validateIdTokenCacheEntity(String folder, TokenCache tokenCache)
            throws IOException, URISyntaxException, JSONException {

        Assert.assertEquals(tokenCache.idTokens.size(),1 );

        String actualKey = tokenCache.idTokens.keySet().stream().findFirst().get();
        String keyExpected = readResource(folder + ID_TOKEN_CACHE_ENTITY_KEY);
        Assert.assertEquals(actualKey, keyExpected);

        String actualValue = new GsonBuilder().create().toJson(tokenCache.idTokens.get(actualKey));
        String valueExpected = readResource(folder + ID_TOKEN_CACHE_ENTITY);
        JSONAssert.assertEquals(valueExpected, actualValue,
                new IdTokenComparator(JSONCompareMode.STRICT, folder));
    }

    private void validateAccountCacheEntity(String folder, TokenCache tokenCache)
            throws IOException, URISyntaxException, JSONException {

        Assert.assertEquals(tokenCache.accounts.size(),1 );

        String actualKey = tokenCache.accounts.keySet().stream().findFirst().get();
        String keyExpected = readResource(folder + ACCOUNT_CACHE_ENTITY_KEY);
        Assert.assertEquals(actualKey, keyExpected);

        String actualValue = new GsonBuilder().create().toJson(tokenCache.accounts.get(actualKey));
        String valueExpected = readResource(folder + ACCOUNT_CACHE_ENTITY);

        JSONAssert.assertEquals(valueExpected, actualValue, JSONCompareMode.STRICT);
    }

    String getEmptyBase64EncodedJson(){
        return new String(Base64.getEncoder().encode("{}".getBytes()));
    }

    String getJWTHeaderBase64EncodedJson(){
        return new String(Base64.getEncoder().encode("{\"alg\": \"HS256\", \"typ\": \"JWT\"}".getBytes()));
    }

    private String getTokenResponse(String folder) throws IOException, URISyntaxException {
        String tokenResponse = readResource(folder + TOKEN_RESPONSE);

        return tokenResponse.replace(ID_TOKEN_PLACEHOLDER, getIdToken(folder));
    }

    private String getIdToken(String folder) throws IOException, URISyntaxException {
        String tokenResponseIdToken = readResource(folder + TOKEN_RESPONSE_ID_TOKEN);

        String encodedIdToken = new String(Base64.getEncoder().encode(tokenResponseIdToken.getBytes()), "UTF-8");

        encodedIdToken = getJWTHeaderBase64EncodedJson() + "." + encodedIdToken + "." + getEmptyBase64EncodedJson();

        return encodedIdToken;
    }
}
