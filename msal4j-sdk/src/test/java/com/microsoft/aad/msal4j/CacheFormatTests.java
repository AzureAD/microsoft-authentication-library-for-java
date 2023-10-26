// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import net.minidev.json.JSONObject;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.microsoft.aad.msal4j.Constants.POINT_DELIMITER;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheFormatTests {
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

    String APP_METADATA_ENTITY_KEY = "/app_metadata_cache_entity_key.txt";
    String APP_METADATA_CACHE_ENTITY = "/app_metadata_cache_entity.json";

    String ID_TOKEN_PLACEHOLDER = "<removed_id_token>";
    String CACHED_AT_PLACEHOLDER = "<cached_at>";
    String EXPIRES_ON_PLACEHOLDER = "<expires_on>";
    String EXTENDED_EXPIRES_ON_PLACEHOLDER = "<extended_expires_on>";

    @Test
    void cacheDeserializationSerializationTest() throws IOException, URISyntaxException, JSONException {
        ITokenCache tokenCache = new TokenCache(null);

        String previouslyStoredCache = readResource("/cache_data/serialized_cache.json");

        tokenCache.deserialize(previouslyStoredCache);

        String serializedCache = tokenCache.serialize();

        JSONAssert.assertEquals(previouslyStoredCache, serializedCache, JSONCompareMode.STRICT);
    }

    String readResource(String resource) throws IOException, URISyntaxException {
        return new String(
                Files.readAllBytes(
                        Paths.get(getClass().getResource(resource).toURI())));
    }

    boolean doesResourceExist(String resource) throws IOException, URISyntaxException {
        return getClass().getResource(resource) != null;
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
    void AADTokenCacheEntitiesFormatTest() throws JSONException, IOException, ParseException, URISyntaxException {
        tokenCacheEntitiesFormatTest("/AAD_cache_data");
    }

    @Test
    void MSATokenCacheEntitiesFormatTest() throws JSONException, IOException, ParseException, URISyntaxException {
        tokenCacheEntitiesFormatTest("/MSA_cache_data");
    }

    @Test
    void FociTokenCacheEntitiesFormatTest() throws JSONException, IOException, ParseException, URISyntaxException {
        tokenCacheEntitiesFormatTest("/Foci_cache_data");
    }

    public void tokenCacheEntitiesFormatTest(String folder) throws URISyntaxException, IOException, ParseException, JSONException {
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
                null,
                new TelemetryManager(null, false));

        TokenRequestExecutor request = spy(new TokenRequestExecutor(
                new AADAuthority(new URL(AUTHORIZE_REQUEST_URL)), msalRequest, serviceBundle));
        OAuthHttpRequest msalOAuthHttpRequest = mock(OAuthHttpRequest.class);
        HTTPResponse httpResponse = mock(HTTPResponse.class);

        doReturn(msalOAuthHttpRequest).when(request).createOauthHttpRequest();
        doReturn(httpResponse).when(msalOAuthHttpRequest).send();
        doReturn(200).when(httpResponse).getStatusCode();
        doReturn(JSONObjectUtils.parse(tokenResponse)).when(httpResponse).getContentAsJSONObject();

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
            throws IOException, URISyntaxException, ParseException, JSONException {

        assertEquals(tokenCache.accessTokens.size(), 1);

        String keyActual = tokenCache.accessTokens.keySet().stream().findFirst().get();
        String keyExpected = readResource(folder + AT_CACHE_ENTITY_KEY);
        assertEquals(keyActual, keyExpected);

        String valueActual = JsonHelper.mapper.writeValueAsString(tokenCache.accessTokens.get(keyActual));
        String valueExpected = readResource(folder + AT_CACHE_ENTITY);

        JSONObject tokenResponseJsonObj = JSONObjectUtils.parse(tokenResponse);
        long expireIn = TokenResponse.getLongValue(tokenResponseJsonObj, "expires_in");

        long extExpireIn = TokenResponse.getLongValue(tokenResponseJsonObj, "ext_expires_in");

        JSONAssert.assertEquals(valueExpected, valueActual,
                new DynamicTimestampsComparator(JSONCompareMode.STRICT, expireIn, extExpireIn));
    }

    private void validateRefreshTokenCacheEntity(String folder, TokenCache tokenCache)
            throws IOException, URISyntaxException, JSONException {

        assertEquals(tokenCache.refreshTokens.size(), 1);

        String actualKey = tokenCache.refreshTokens.keySet().stream().findFirst().get();
        String keyExpected = readResource(folder + RT_CACHE_ENTITY_KEY);
        assertEquals(actualKey, keyExpected);

        String actualValue = JsonHelper.mapper.writeValueAsString(tokenCache.refreshTokens.get(actualKey));
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

        assertEquals(tokenCache.idTokens.size(), 1);

        String actualKey = tokenCache.idTokens.keySet().stream().findFirst().get();
        String keyExpected = readResource(folder + ID_TOKEN_CACHE_ENTITY_KEY);
        assertEquals(actualKey, keyExpected);

        String actualValue = JsonHelper.mapper.writeValueAsString(tokenCache.idTokens.get(actualKey));
        String valueExpected = readResource(folder + ID_TOKEN_CACHE_ENTITY);
        JSONAssert.assertEquals(valueExpected, actualValue,
                new IdTokenComparator(JSONCompareMode.STRICT, folder));
    }

    private void validateAccountCacheEntity(String folder, TokenCache tokenCache)
            throws IOException, URISyntaxException, JSONException {

        assertEquals(tokenCache.accounts.size(), 1);

        String actualKey = tokenCache.accounts.keySet().stream().findFirst().get();
        String keyExpected = readResource(folder + ACCOUNT_CACHE_ENTITY_KEY);
        assertEquals(actualKey, keyExpected);

        String actualValue = JsonHelper.mapper.writeValueAsString(tokenCache.accounts.get(actualKey));
        String valueExpected = readResource(folder + ACCOUNT_CACHE_ENTITY);

        JSONAssert.assertEquals(valueExpected, actualValue, JSONCompareMode.STRICT);
    }

    private void validateAppMetadataCacheEntity(String folder, TokenCache tokenCache)
            throws IOException, URISyntaxException, JSONException {

        if (!doesResourceExist(folder + APP_METADATA_CACHE_ENTITY)) {
            return;
        }

        assertEquals(tokenCache.appMetadata.size(), 1);

        String actualKey = tokenCache.appMetadata.keySet().stream().findFirst().get();
        String keyExpected = readResource(folder + APP_METADATA_ENTITY_KEY);
        assertEquals(actualKey, keyExpected);

        String actualValue = JsonHelper.mapper.writeValueAsString(tokenCache.appMetadata.get(actualKey));
        String valueExpected = readResource(folder + APP_METADATA_CACHE_ENTITY);

        JSONAssert.assertEquals(valueExpected, actualValue, JSONCompareMode.STRICT);
    }

    String getEmptyBase64EncodedJson() {
        return new String(Base64.getEncoder().encode("{}".getBytes()));
    }

    String getJWTHeaderBase64EncodedJson() {
        return new String(Base64.getEncoder().encode("{\"alg\": \"HS256\", \"typ\": \"JWT\"}".getBytes()));
    }

    private String getTokenResponse(String folder) throws IOException, URISyntaxException {
        String tokenResponse = readResource(folder + TOKEN_RESPONSE);

        return tokenResponse.replace(ID_TOKEN_PLACEHOLDER, getIdToken(folder));
    }

    private String getIdToken(String folder) throws IOException, URISyntaxException {
        String tokenResponseIdToken = readResource(folder + TOKEN_RESPONSE_ID_TOKEN);

        String encodedIdToken = new String(Base64.getEncoder().encode(tokenResponseIdToken.getBytes()), StandardCharsets.UTF_8);

        encodedIdToken = getJWTHeaderBase64EncodedJson() + POINT_DELIMITER +
                encodedIdToken + POINT_DELIMITER +
                getEmptyBase64EncodedJson();

        return encodedIdToken;
    }
}
