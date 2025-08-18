// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CacheTests {

    private TokenCache tokenCache;

    @BeforeEach
    void setUp() {
        tokenCache = new TokenCache();
    }

    @Test
    void cacheLookup_MixAccountBasedAndAssertionBasedSilentFlows() throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromSecret("password"))
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        HashMap<String, String> responseParameters = new HashMap<>();
        //Acquire a token with no ID token/account associated with it
        responseParameters.put("access_token", "accessTokenNoAccount");

        ClientCredentialParameters clientCredentialParameters = ClientCredentialParameters.builder(Collections.singleton("someScopes")).build();
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(TestHelper.expectedResponse(HttpStatus.HTTP_OK, TestHelper.getSuccessfulTokenResponse(responseParameters)));
        IAuthenticationResult resultNoAccount = cca.acquireToken(clientCredentialParameters).get();

        //Ensure there is one token in the cache, and the result had no account
        assertEquals(1, cca.tokenCache.accessTokens.size());
        assertNull(resultNoAccount.account());
        verify(httpClientMock, times(1)).send(any());

        //Acquire a second token, this time with an ID token/account
        responseParameters.put("access_token", "accessTokenWithAccount");
        responseParameters.put("id_token", TestHelper.createIdToken(new HashMap<>()));

        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(TestHelper.expectedResponse(HttpStatus.HTTP_OK, TestHelper.getSuccessfulTokenResponse(responseParameters)));
        OnBehalfOfParameters onBehalfOfParametersarameters = OnBehalfOfParameters.builder(Collections.singleton("someOtherScopes"), new UserAssertion(TestHelper.signedAssertion)).build();
        IAuthenticationResult resultWithAccount = cca.acquireToken(onBehalfOfParametersarameters).get();

        //Ensure there are now two tokens in the cache, and the result has an account
        assertEquals(2, cca.tokenCache.accessTokens.size());
        assertNull(resultNoAccount.account());
        verify(httpClientMock, times(2)).send(any());

        //Make two silent calls, one with the account and one without
        SilentParameters silentParametersNoAccount = SilentParameters.builder(Collections.singleton("someScopes")).build();
        SilentParameters silentParametersWithAccount = SilentParameters.builder(Collections.singleton("someOtherScopes"), resultWithAccount.account()).build();

        resultNoAccount = cca.acquireTokenSilently(silentParametersNoAccount).get();
        resultWithAccount = cca.acquireTokenSilently(silentParametersWithAccount).get();

        //Ensure the correct access tokens were returned from each silent call
        assertEquals("accessTokenNoAccount", resultNoAccount.accessToken());
        assertEquals("accessTokenWithAccount", resultWithAccount.accessToken());
    }

    @Test
    void serializeDeserialize_EmptyCache_Success() {
        // Serialize empty cache
        String serializedCache = tokenCache.serialize();

        // Should have all required sections but be empty
        assertTrue(serializedCache.contains("\"Account\":{}"));
        assertTrue(serializedCache.contains("\"AccessToken\":{}"));
        assertTrue(serializedCache.contains("\"RefreshToken\":{}"));
        assertTrue(serializedCache.contains("\"IdToken\":{}"));
        assertTrue(serializedCache.contains("\"AppMetadata\":{}"));

        // Create new cache and deserialize
        TokenCache newCache = new TokenCache();
        newCache.deserialize(serializedCache);

        // Verify all collections are empty
        assertTrue(newCache.accessTokens.isEmpty());
        assertTrue(newCache.refreshTokens.isEmpty());
        assertTrue(newCache.idTokens.isEmpty());
        assertTrue(newCache.accounts.isEmpty());
        assertTrue(newCache.appMetadata.isEmpty());
    }

    @Test
    void serializeDeserialize_WithAccountData_PreservesData() {
        // Add an account to the cache
        AccountCacheEntity account = new AccountCacheEntity();
        account.homeAccountId("home-account-id");
        account.environment("login.microsoftonline.com");
        account.realm("tenant-id");
        account.localAccountId("local-id");
        account.username("user@example.com");
        account.name("Test User");

        tokenCache.accounts.put(account.getKey(), account);

        // Serialize the cache
        String serializedCache = tokenCache.serialize();

        // Create new cache and deserialize
        TokenCache newCache = new TokenCache();
        newCache.deserialize(serializedCache);

        // Verify account was preserved
        assertEquals(1, newCache.accounts.size());
        AccountCacheEntity deserializedAccount = newCache.accounts.get(account.getKey());
        assertNotNull(deserializedAccount);
        assertEquals("home-account-id", deserializedAccount.homeAccountId());
        assertEquals("login.microsoftonline.com", deserializedAccount.environment());
        assertEquals("tenant-id", deserializedAccount.realm());
        assertEquals("user@example.com", deserializedAccount.username());
        assertEquals("Test User", deserializedAccount.name());
    }

    @Test
    void removeAccount_RemovesAllRelatedEntities() {
        // Setup test data
        String homeAccountId = "home-account-id";
        String environment = "login.microsoftonline.com";
        String clientId = "client-id";
        String realm = "tenant-id";
        String username = "user@example.com";

        // Create account
        AccountCacheEntity account = new AccountCacheEntity();
        account.homeAccountId(homeAccountId);
        account.environment(environment);
        account.realm(realm);
        account.username(username);
        tokenCache.accounts.put(account.getKey(), account);

        // Create access token
        AccessTokenCacheEntity accessToken = new AccessTokenCacheEntity();
        accessToken.homeAccountId(homeAccountId);
        accessToken.environment(environment);
        accessToken.clientId(clientId);
        accessToken.realm(realm);
        accessToken.target("scope1 scope2");
        accessToken.secret("access-token-secret");
        accessToken.cachedAt("1600000000");
        accessToken.expiresOn("1600100000");
        tokenCache.accessTokens.put(accessToken.getKey(), accessToken);

        // Create refresh token
        RefreshTokenCacheEntity refreshToken = new RefreshTokenCacheEntity();
        refreshToken.homeAccountId(homeAccountId);
        refreshToken.environment(environment);
        refreshToken.clientId(clientId);
        refreshToken.secret("refresh-token-secret");
        tokenCache.refreshTokens.put(refreshToken.getKey(), refreshToken);

        // Create ID token
        IdTokenCacheEntity idToken = new IdTokenCacheEntity();
        idToken.homeAccountId(homeAccountId);
        idToken.environment(environment);
        idToken.clientId(clientId);
        idToken.realm(realm);
        idToken.secret("id-token-secret");
        tokenCache.idTokens.put(idToken.getKey(), idToken);

        // Remove the account
        tokenCache.removeAccount(clientId, new Account(homeAccountId, environment, username, null));

        // Verify all related entities are removed
        assertTrue(tokenCache.accounts.isEmpty());
        assertTrue(tokenCache.accessTokens.isEmpty());
        assertTrue(tokenCache.refreshTokens.isEmpty());
        assertTrue(tokenCache.idTokens.isEmpty());
    }

    @Test
    void removeAccount_WithMultipleAccounts_OnlyRemovesSpecificAccount() {
        // Create two accounts with different homeAccountIds
        String homeAccountId1 = "home-account-id-1";
        String homeAccountId2 = "home-account-id-2";
        String environment = "login.microsoftonline.com";
        String clientId = "client-id";
        String username = "user@example.com";

        // Account 1
        AccountCacheEntity account1 = new AccountCacheEntity();
        account1.homeAccountId(homeAccountId1);
        account1.environment(environment);
        tokenCache.accounts.put(account1.getKey(), account1);

        // Account 2
        AccountCacheEntity account2 = new AccountCacheEntity();
        account2.homeAccountId(homeAccountId2);
        account2.environment(environment);
        tokenCache.accounts.put(account2.getKey(), account2);

        // Add tokens for both accounts
        AccessTokenCacheEntity accessToken1 = new AccessTokenCacheEntity();
        accessToken1.homeAccountId(homeAccountId1);
        accessToken1.environment(environment);
        accessToken1.clientId(clientId);
        accessToken1.expiresOn("1600100000");
        tokenCache.accessTokens.put(accessToken1.getKey(), accessToken1);

        AccessTokenCacheEntity accessToken2 = new AccessTokenCacheEntity();
        accessToken2.homeAccountId(homeAccountId2);
        accessToken2.environment(environment);
        accessToken2.clientId(clientId);
        accessToken2.expiresOn("1600100000");
        tokenCache.accessTokens.put(accessToken2.getKey(), accessToken2);

        // Remove account1
        tokenCache.removeAccount(clientId, new Account(homeAccountId1, environment, username, null));

        // Verify only account1 and its tokens are removed
        assertEquals(1, tokenCache.accounts.size());
        assertEquals(1, tokenCache.accessTokens.size());
        assertTrue(tokenCache.accounts.containsKey(account2.getKey()));
        assertFalse(tokenCache.accounts.containsKey(account1.getKey()));
    }

    @Test
    void mergeCache_PreservesRemovals() {
        // Setup initial cache with two accounts
        String homeAccountId1 = "home-account-id-1";
        String homeAccountId2 = "home-account-id-2";
        String environment = "login.microsoftonline.com";
        String clientId = "client-id";
        String username = "user@example.com";

        // Account 1
        AccountCacheEntity account1 = new AccountCacheEntity();
        account1.homeAccountId(homeAccountId1);
        account1.environment(environment);
        tokenCache.accounts.put(account1.getKey(), account1);

        // Account 2
        AccountCacheEntity account2 = new AccountCacheEntity();
        account2.homeAccountId(homeAccountId2);
        account2.environment(environment);
        tokenCache.accounts.put(account2.getKey(), account2);

        // Serialize the cache with both accounts
        String serializedWithTwoAccounts = tokenCache.serialize();

        // Create a new cache with this serialized data
        TokenCache deserializedCache = new TokenCache();
        deserializedCache.deserialize(serializedWithTwoAccounts);
        assertEquals(2, deserializedCache.accounts.size());

        // Now remove account1 from the original cache
        tokenCache.removeAccount(clientId, new Account(homeAccountId1, environment, username, null));

        // Serialize the cache with just one account
        String serializedWithOneAccount = tokenCache.serialize();

        // Deserialize into a new cache
        TokenCache finalCache = new TokenCache();
        finalCache.deserialize(serializedWithOneAccount);

        // Verify only one account exists
        assertEquals(1, finalCache.accounts.size());
        assertTrue(finalCache.accounts.containsKey(account2.getKey()));
        assertFalse(finalCache.accounts.containsKey(account1.getKey()));
    }

    @Test
    void getAccounts_ReturnsCorrectAccounts() {
        // Setup multiple accounts in the cache
        String homeAccountId1 = "home-account-id-1";
        String localAccountId1 = "local-id-1";
        String homeAccountId2 = "home-account-id-2";
        String environment = "login.microsoftonline.com";
        String clientId = "client-id";
        String realm = "tenant-id";

        // Account 1
        AccountCacheEntity account1 = new AccountCacheEntity();
        account1.homeAccountId(homeAccountId1);
        account1.localAccountId(localAccountId1);
        account1.environment(environment);
        account1.realm(realm);
        account1.username("user1@example.com");
        tokenCache.accounts.put(account1.getKey(), account1);

        // Account 2
        AccountCacheEntity account2 = new AccountCacheEntity();
        account2.homeAccountId(homeAccountId2);
        account2.environment(environment);
        account2.realm(realm);
        account2.username("user2@example.com");
        tokenCache.accounts.put(account2.getKey(), account2);

        // Get accounts
        Set<IAccount> accounts = tokenCache.getAccounts(clientId);

        // Verify correct accounts are returned
        assertEquals(2, accounts.size());

        // Verify account details
        for (IAccount account : accounts) {
            if (account.homeAccountId().equals(homeAccountId1)) {
                assertEquals("user1@example.com", account.username());
            } else if (account.homeAccountId().equals(homeAccountId2)) {
                assertEquals("user2@example.com", account.username());
            } else {
                fail("Unexpected account returned");
            }
        }
    }
}
