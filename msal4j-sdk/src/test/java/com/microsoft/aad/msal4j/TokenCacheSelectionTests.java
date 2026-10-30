// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TokenCacheSelectionTests {
    private static final String TENANT = "00000000-0000-0000-0000-000000000001";
    private static final String CLIENT = "00000000-0000-0000-0000-000000000002";
    private static final String SCOPE = "api://local-test/.default";
    private static final UserAssertion ASSERTION_A = new UserAssertion("local-assertion-a");
    private static final UserAssertion ASSERTION_B = new UserAssertion("local-assertion-b");

    @Test
    void accountlessSilentLookupRejectsUserEntryAfterSerialization() throws Exception {
        AtomicReference<String> cache = seedCache();
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication restored = application(cache, rejectingTransport(calls), CLIENT, TENANT);

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> restored.acquireTokenSilently(
                        SilentParameters.builder(Collections.singleton(SCOPE)).tenant(TENANT).build())
                        .get(10, TimeUnit.SECONDS));
        assertInstanceOf(MsalClientException.class, failure.getCause());
        assertEquals(AuthenticationErrorCode.CACHE_MISS, ((MsalClientException) failure.getCause()).errorCode());
        assertEquals(0, calls.get());
        assertNotEquals(ASSERTION_A.getAssertionHash(), ASSERTION_B.getAssertionHash());
        assertTrue(cache.get().contains(ASSERTION_A.getAssertionHash()));
        assertFalse(cache.get().contains(ASSERTION_B.getAssertionHash()));
    }

    @Test
    void matchingAssertionUsesSerializedCache() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication restored = application(seedCache(), rejectingTransport(calls), CLIENT, TENANT);

        assertEquals("local-token-a", acquire(restored, ASSERTION_A).accessToken());
        assertEquals(0, calls.get());
    }

    @Test
    void differentAssertionExchangesAndThenUsesItsOwnEntry() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        IHttpClient transport = request -> {
            calls.incrementAndGet();
            assertTrue(request.body().contains("assertion=local-assertion-b"));
            return response("b", 3600, false);
        };
        ConfidentialClientApplication restored = application(seedCache(), transport, CLIENT, TENANT);

        assertEquals("local-token-b", acquire(restored, ASSERTION_B).accessToken());
        assertEquals("local-token-b", acquire(restored, ASSERTION_B).accessToken());
        assertEquals("local-token-a", acquire(restored, ASSERTION_A).accessToken());
        assertEquals(1, calls.get());
    }

    @Test
    void differentAssertionDoesNotUseOtherUsersRefreshToken() throws Exception {
        AtomicReference<String> cache = new AtomicReference<>();
        ConfidentialClientApplication first = application(cache, request -> response("a", 0, true), CLIENT, TENANT);
        acquire(first, ASSERTION_A);
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication restored = application(cache, request -> {
            calls.incrementAndGet();
            assertTrue(request.body().contains("assertion=local-assertion-b"));
            assertFalse(request.body().contains("grant_type=refresh_token"));
            return response("b", 3600, false);
        }, CLIENT, TENANT);

        assertEquals("local-token-b", acquire(restored, ASSERTION_B).accessToken());
        assertEquals(1, calls.get());
    }

    @Test
    void accountlessSilentLookupDoesNotRefreshSerializedEntry() throws Exception {
        AtomicReference<String> cache = new AtomicReference<>();
        acquire(application(cache, request -> response("a", 0, true), CLIENT, TENANT), ASSERTION_A);
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication restored = application(cache, rejectingTransport(calls), CLIENT, TENANT);

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> restored.acquireTokenSilently(SilentParameters.builder(Collections.singleton(SCOPE)).build())
                        .get(10, TimeUnit.SECONDS));

        assertInstanceOf(MsalClientException.class, failure.getCause());
        assertEquals(0, calls.get());
    }

    @Test
    void clientCredentialsLookupWithSerializedUserEntry() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication restored = application(seedCache(), request -> {
            calls.incrementAndGet();
            assertEquals("client_credentials", StringHelper.parseQueryParameters(request.body()).get("grant_type"));
            return TestHelper.expectedResponse(200,
                    "{\"token_type\":\"Bearer\",\"expires_in\":3600,\"access_token\":\"local-app-token\"}");
        }, CLIENT, TENANT);

        IAuthenticationResult result = restored.acquireToken(
                ClientCredentialParameters.builder(Collections.singleton(SCOPE)).build())
                .get(10, TimeUnit.SECONDS);

        assertEquals("local-app-token", result.accessToken());
        assertNull(result.account());
        assertNull(result.idToken());
        assertEquals(1, calls.get());
    }

    @Test
    void accountBoundLookupUsesMatchingAccount() throws Exception {
        AtomicReference<String> cache = new AtomicReference<>();
        IAuthenticationResult first = acquire(
                application(cache, request -> response("a", 3600, false), CLIENT, TENANT), ASSERTION_A);
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication restored = application(cache, rejectingTransport(calls), CLIENT, TENANT);

        IAuthenticationResult result = restored.acquireTokenSilently(
                SilentParameters.builder(Collections.singleton(SCOPE), first.account()).build())
                .get(10, TimeUnit.SECONDS);

        assertEquals("local-token-a", result.accessToken());
        assertEquals(0, calls.get());
    }

    @Test
    void expiredEntryWithoutIdTokenDoesNotRefreshSilently() throws Exception {
        AtomicReference<String> cache = new AtomicReference<>();
        acquire(application(cache, request -> response("a", 0, true, false), CLIENT, TENANT), ASSERTION_A);
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication restored = application(cache, rejectingTransport(calls), CLIENT, TENANT);

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> restored.acquireTokenSilently(SilentParameters.builder(Collections.singleton(SCOPE)).build())
                        .get(10, TimeUnit.SECONDS));

        assertInstanceOf(MsalClientException.class, failure.getCause());
        assertEquals(0, calls.get());
    }

    @Test
    void entryWithoutIdTokenDoesNotMatchAccountlessLookup() throws Exception {
        AtomicReference<String> cache = new AtomicReference<>();
        acquire(application(cache, request -> response("a", 3600, false, false), CLIENT, TENANT), ASSERTION_A);
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication restored = application(cache, rejectingTransport(calls), CLIENT, TENANT);

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> restored.acquireTokenSilently(
                        SilentParameters.builder(Collections.singleton(SCOPE)).build()).get(10, TimeUnit.SECONDS));
        assertInstanceOf(MsalClientException.class, failure.getCause());
        assertEquals(AuthenticationErrorCode.CACHE_MISS, ((MsalClientException) failure.getCause()).errorCode());
        assertEquals(0, calls.get());
    }

    @ParameterizedTest
    @CsvSource({
            "other-client, 00000000-0000-0000-0000-000000000001, api://local-test/.default",
            "00000000-0000-0000-0000-000000000002, other-tenant, api://local-test/.default",
            "00000000-0000-0000-0000-000000000002, 00000000-0000-0000-0000-000000000001, api://local-test/other"
    })
    void otherLookupDimensionsMiss(String client, String tenant, String scope) throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication restored = application(seedCache(), rejectingTransport(calls), client, tenant);

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> restored.acquireTokenSilently(SilentParameters.builder(Collections.singleton(scope)).build())
                        .get(10, TimeUnit.SECONDS));

        assertInstanceOf(MsalClientException.class, failure.getCause());
        assertEquals(0, calls.get());
    }

    @Test
    void separateCacheDoesNotReturnPriorEntry() throws Exception {
        seedCache();
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication restored = application(
                new AtomicReference<>(), rejectingTransport(calls), CLIENT, TENANT);

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> restored.acquireTokenSilently(SilentParameters.builder(Collections.singleton(SCOPE)).build())
                        .get(10, TimeUnit.SECONDS));

        assertInstanceOf(MsalClientException.class, failure.getCause());
        assertEquals(0, calls.get());
    }

    private static AtomicReference<String> seedCache() throws Exception {
        AtomicReference<String> cache = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication first = application(cache, request -> {
            calls.incrementAndGet();
            assertTrue(request.body().contains("assertion=local-assertion-a"));
            return response("a", 3600, false);
        }, CLIENT, TENANT);
        assertEquals("local-token-a", acquire(first, ASSERTION_A).accessToken());
        assertEquals(1, calls.get());
        assertNotNull(cache.get());
        return cache;
    }

    private static IAuthenticationResult acquire(ConfidentialClientApplication application, UserAssertion assertion)
            throws Exception {
        return application.acquireToken(OnBehalfOfParameters.builder(Collections.singleton(SCOPE), assertion).build())
                .get(10, TimeUnit.SECONDS);
    }

    private static IHttpClient rejectingTransport(AtomicInteger calls) {
        return request -> {
            calls.incrementAndGet();
            throw new AssertionError("Unexpected local HTTP request: " + request.url());
        };
    }

    private static HttpResponse response(String user, int expiresIn, boolean refreshToken) {
        return response(user, expiresIn, refreshToken, true);
    }

    private static HttpResponse response(String user, int expiresIn, boolean refreshToken, boolean idToken) {
        String info = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"uid\":\"" + user + "\",\"utid\":\"" + TENANT + "\"}").getBytes(StandardCharsets.UTF_8));
        HashMap<String, String> claims = new HashMap<>();
        claims.put("oid", user);
        claims.put("sub", user);
        claims.put("preferred_username", user);
        return TestHelper.expectedResponse(200, "{\"token_type\":\"Bearer\",\"scope\":\"" + SCOPE
                + "\",\"expires_in\":" + expiresIn + ",\"access_token\":\"local-token-" + user
                + "\",\"client_info\":\"" + info + "\""
                + (idToken ? ",\"id_token\":\"" + TestHelper.createIdToken(claims) + "\"" : "")
                + (refreshToken ? ",\"refresh_token\":\"local-refresh-" + user + "\"" : "") + "}");
    }

    private static ConfidentialClientApplication application(AtomicReference<String> cache, IHttpClient transport,
                                                             String client, String tenant) throws Exception {
        return ConfidentialClientApplication.builder(client, ClientCredentialFactory.createFromSecret("local-secret"))
                .authority("https://login.microsoftonline.com/" + tenant)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .httpClient(transport)
                .setTokenCacheAccessAspect(new ITokenCacheAccessAspect() {
                    @Override
                    public void beforeCacheAccess(ITokenCacheAccessContext context) {
                        if (cache.get() != null) {
                            context.tokenCache().deserialize(cache.get());
                        }
                    }

                    @Override
                    public void afterCacheAccess(ITokenCacheAccessContext context) {
                        if (context.hasCacheChanged()) {
                            cache.set(context.tokenCache().serialize());
                        }
                    }
                })
                .build();
    }
}
