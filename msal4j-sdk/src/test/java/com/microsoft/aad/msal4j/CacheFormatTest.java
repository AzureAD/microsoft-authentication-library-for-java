// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.microsoft.aad.msal4j.Constants.POINT_DELIMITER;

@ExtendWith(MockitoExtension.class)
class CacheFormatTest {
    private static final String TOKEN_RESPONSE = "/token_response.json";
    private static final String TOKEN_RESPONSE_ID_TOKEN = "/token_response_id_token.json";

    private static final String AT_CACHE_ENTITY_KEY = "/at_cache_entity_key.txt";
    private static final String AT_CACHE_ENTITY = "/at_cache_entity.json";

    private static final String RT_CACHE_ENTITY_KEY = "/rt_cache_entity_key.txt";
    private static final String RT_CACHE_ENTITY = "/rt_cache_entity.json";

    private static final String ID_TOKEN_CACHE_ENTITY_KEY = "/id_token_cache_entity_key.txt";
    private static final String ID_TOKEN_CACHE_ENTITY = "/id_token_cache_entity.json";

    private static final String ACCOUNT_CACHE_ENTITY_KEY = "/account_cache_entity_key.txt";
    private static final String ACCOUNT_CACHE_ENTITY = "/account_cache_entity.json";

    private static final String APP_METADATA_ENTITY_KEY = "/app_metadata_cache_entity_key.txt";
    private static final String APP_METADATA_CACHE_ENTITY = "/app_metadata_cache_entity.json";

    private static final String ID_TOKEN_PLACEHOLDER = "<removed_id_token>";
    private static final String CACHED_AT_PLACEHOLDER = "<cached_at>";
    private static final String EXPIRES_ON_PLACEHOLDER = "<expires_on>";
    private static final String EXTENDED_EXPIRES_ON_PLACEHOLDER = "<extended_expires_on>";

    @Test
    void cacheDeserializationSerializationTest() throws JSONException {
        ITokenCache tokenCache = new TokenCache(null);

        String previouslyStoredCache = TestHelper.readResource(this.getClass(), "/cache_data/serialized_cache.json");

        tokenCache.deserialize(previouslyStoredCache);

        String serializedCache = tokenCache.serialize();

        JSONAssert.assertEquals(previouslyStoredCache, serializedCache, JSONCompareMode.STRICT);
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
                            + expectations.get(o.toString()));
                    return;
                }
                return;
            }
            super.compareValues(s, o, o1, jsonCompareResult);
        }
    }

    @Test
    void AADTokenCacheEntitiesFormatTest() throws Exception {
        tokenCacheEntitiesFormatTest("/AAD_cache_data");
    }

    @Test
    void MSATokenCacheEntitiesFormatTest() throws Exception {
        tokenCacheEntitiesFormatTest("/MSA_cache_data");
    }

    @Test
    void FociTokenCacheEntitiesFormatTest() throws Exception {
        tokenCacheEntitiesFormatTest("/Foci_cache_data");
    }

    private void tokenCacheEntitiesFormatTest(String folder) throws Exception {
        String CLIENT_ID = "b6c69a37-df96-4db0-9088-2ab96e1d8215";
        String AUTHORIZE_REQUEST_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";

        String tokenResponse = getTokenResponse(folder);

        PublicClientApplication app = PublicClientApplication.builder(CLIENT_ID).correlationId("correlation_id").build();

        AuthorizationCodeParameters parameters =
                AuthorizationCodeParameters.builder
                        ("code", new URI("http://my.redirect.com"))
                        .build();

        MsalRequest msalRequest = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        ServiceBundle serviceBundle = new ServiceBundle(
                null,
                new TelemetryManager(null, false),
                null);

        TokenRequestExecutor request = spy(new TokenRequestExecutor(
                new AADAuthority(new URL(AUTHORIZE_REQUEST_URL)), msalRequest, serviceBundle));
        OAuthHttpRequest msalOAuthHttpRequest = mock(OAuthHttpRequest.class);
        HttpResponse httpResponse = mock(HttpResponse.class);

        doReturn(msalOAuthHttpRequest).when(request).createOauthHttpRequest();
        doReturn(httpResponse).when(msalOAuthHttpRequest).send();
        doReturn(HttpStatus.HTTP_OK).when(httpResponse).statusCode();
        doReturn(JsonHelper.convertJsonToMap(tokenResponse)).when(httpResponse).getBodyAsMap();

        final AuthenticationResult result = request.executeTokenRequest();

        TokenCache tokenCache = new TokenCache();

        tokenCache.saveTokens(request, result, "login.microsoftonline.com");

        validateAccessTokenCacheEntity(folder, tokenResponse, tokenCache);
        validateRefreshTokenCacheEntity(folder, tokenCache);
        validateIdTokenCacheEntity(folder, tokenCache);
        validateAccountCacheEntity(folder, tokenCache);
        validateAppMetadataCacheEntity(folder, tokenCache);
    }

    private void validateAccessTokenCacheEntity(String folder, String tokenResponse, TokenCache tokenCache)
            throws JSONException {

        assertEquals(1, tokenCache.accessTokens.size());

        String keyActual = tokenCache.accessTokens.keySet().stream().findFirst().get();
        String keyExpected = TestHelper.readResource(this.getClass(), folder + AT_CACHE_ENTITY_KEY);
        assertEquals(keyExpected, keyActual);

        String valueActual = JsonHelper.convertJsonSerializableObjectToString(tokenCache.accessTokens.get(keyActual));
        String valueExpected = TestHelper.readResource(this.getClass(), folder + AT_CACHE_ENTITY);

        Map<String, String> tokenResponseMap = JsonHelper.convertJsonToMap(tokenResponse);

        JSONAssert.assertEquals(valueExpected, valueActual,
                new DynamicTimestampsComparator(JSONCompareMode.STRICT, Long.parseLong(tokenResponseMap.get("expires_in")), Long.parseLong(tokenResponseMap.get("ext_expires_in"))));
    }

    private void validateRefreshTokenCacheEntity(String folder, TokenCache tokenCache)
            throws JSONException {

        assertEquals(1, tokenCache.refreshTokens.size());

        String actualKey = tokenCache.refreshTokens.keySet().stream().findFirst().get();
        String keyExpected = TestHelper.readResource(this.getClass(), folder + RT_CACHE_ENTITY_KEY);
        assertEquals(keyExpected, actualKey);

        String actualValue = JsonHelper.convertJsonSerializableObjectToString(tokenCache.refreshTokens.get(actualKey));
        String valueExpected = TestHelper.readResource(this.getClass(), folder + RT_CACHE_ENTITY);
        JSONAssert.assertEquals(valueExpected, actualValue, JSONCompareMode.STRICT);
    }

    public class IdTokenComparator extends DefaultComparator {
        private String idToken;

        public IdTokenComparator(JSONCompareMode mode, String folder) {
            super(mode);
            idToken = getIdToken(folder);
        }

        @Override
        public void compareValues(String s, Object o, Object o1, JSONCompareResult jsonCompareResult) throws JSONException {
            if (ID_TOKEN_PLACEHOLDER.equals(o.toString())) {

                if (!idToken.equals(o1.toString())) {
                    jsonCompareResult.fail("idTokens do not match");
                    return;
                }
                return;
            }
            super.compareValues(s, o, o1, jsonCompareResult);
        }
    }

    private void validateIdTokenCacheEntity(String folder, TokenCache tokenCache)
            throws JSONException {

        assertEquals(1, tokenCache.idTokens.size());

        String actualKey = tokenCache.idTokens.keySet().stream().findFirst().get();
        String keyExpected = TestHelper.readResource(this.getClass(), folder + ID_TOKEN_CACHE_ENTITY_KEY);
        assertEquals(keyExpected, actualKey);

        String actualValue = JsonHelper.convertJsonSerializableObjectToString(tokenCache.idTokens.get(actualKey));
        String valueExpected = TestHelper.readResource(this.getClass(), folder + ID_TOKEN_CACHE_ENTITY);
        JSONAssert.assertEquals(valueExpected, actualValue,
                new IdTokenComparator(JSONCompareMode.STRICT, folder));
    }

    private void validateAccountCacheEntity(String folder, TokenCache tokenCache)
            throws JSONException {

        assertEquals(1, tokenCache.accounts.size());

        String actualKey = tokenCache.accounts.keySet().stream().findFirst().get();
        String keyExpected = TestHelper.readResource(this.getClass(), folder + ACCOUNT_CACHE_ENTITY_KEY);
        assertEquals(keyExpected, actualKey);

        String actualValue = JsonHelper.convertJsonSerializableObjectToString(tokenCache.accounts.get(actualKey));
        String valueExpected = TestHelper.readResource(this.getClass(), folder + ACCOUNT_CACHE_ENTITY);

        JSONAssert.assertEquals(valueExpected, actualValue, JSONCompareMode.STRICT);
    }

    private void validateAppMetadataCacheEntity(String folder, TokenCache tokenCache)
            throws JSONException {

        if (this.getClass().getResource(folder + APP_METADATA_CACHE_ENTITY) == null) {
            return;
        }

        assertEquals(1, tokenCache.appMetadata.size());

        String actualKey = tokenCache.appMetadata.keySet().stream().findFirst().get();
        String keyExpected = TestHelper.readResource(this.getClass(), folder + APP_METADATA_ENTITY_KEY);
        assertEquals(keyExpected, actualKey);

        String actualValue = JsonHelper.convertJsonSerializableObjectToString(tokenCache.appMetadata.get(actualKey));
        String valueExpected = TestHelper.readResource(this.getClass(), folder + APP_METADATA_CACHE_ENTITY);

        JSONAssert.assertEquals(valueExpected, actualValue, JSONCompareMode.STRICT);
    }

    private String getTokenResponse(String folder) {
        String tokenResponse = TestHelper.readResource(this.getClass(), folder + TOKEN_RESPONSE);

        return tokenResponse.replace(ID_TOKEN_PLACEHOLDER, getIdToken(folder));
    }

    private String getIdToken(String folder) {
        String tokenResponseIdToken = TestHelper.readResource(this.getClass(), folder + TOKEN_RESPONSE_ID_TOKEN);

        String encodedIdToken = new String(Base64.getEncoder().encode(tokenResponseIdToken.getBytes()), StandardCharsets.UTF_8);

        encodedIdToken = TestHelper.getJWTHeaderBase64EncodedJson() + POINT_DELIMITER +
                encodedIdToken + POINT_DELIMITER +
                TestHelper.getEmptyBase64EncodedJson();

        return encodedIdToken;
    }
}