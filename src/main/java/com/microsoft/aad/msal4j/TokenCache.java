// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Cache used for storing tokens.
 */
public class TokenCache implements ITokenCache {

    protected static final int MIN_ACCESS_TOKEN_EXPIRE_IN_SEC = 5 * 60;

    transient private ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Constructor for token cache
     *
     * @param tokenCacheAccessAspect {@link ITokenCacheAccessAspect}
     */
    public TokenCache(ITokenCacheAccessAspect tokenCacheAccessAspect) {
        this();
        this.tokenCacheAccessAspect = tokenCacheAccessAspect;
    }

    /**
     * Constructor for token cache
     */
    public TokenCache() {
    }

    @JsonProperty("AccessToken")
    Map<String, AccessTokenCacheEntity> accessTokens = new LinkedHashMap<>();

    @JsonProperty("RefreshToken")
    Map<String, RefreshTokenCacheEntity> refreshTokens = new LinkedHashMap<>();

    @JsonProperty("IdToken")
    Map<String, IdTokenCacheEntity> idTokens = new LinkedHashMap<>();

    @JsonProperty("Account")
    Map<String, AccountCacheEntity> accounts = new LinkedHashMap<>();

    @JsonProperty("AppMetadata")
    Map<String, AppMetadataCacheEntity> appMetadata = new LinkedHashMap<>();

    transient ITokenCacheAccessAspect tokenCacheAccessAspect;

    private transient String serializedCachedSnapshot;

    @Override
    public void deserialize(String data) {
        if (StringHelper.isBlank(data)) {
            return;
        }
        serializedCachedSnapshot = data;

        TokenCache deserializedCache = JsonHelper.convertJsonToObject(data, TokenCache.class);

        lock.writeLock().lock();
        try {
            this.accounts = deserializedCache.accounts;
            this.accessTokens = deserializedCache.accessTokens;
            this.refreshTokens = deserializedCache.refreshTokens;
            this.idTokens = deserializedCache.idTokens;
            this.appMetadata = deserializedCache.appMetadata;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static void mergeJsonObjects(JsonNode old, JsonNode update) {
        mergeRemovals(old, update);
        mergeUpdates(old, update);
    }

    private static void mergeUpdates(JsonNode old, JsonNode update) {
        Iterator<String> fieldNames = update.fieldNames();
        while (fieldNames.hasNext()) {
            String uKey = fieldNames.next();
            JsonNode uValue = update.get(uKey);

            // add new property
            if (!old.has(uKey)) {
                if (!uValue.isNull() &&
                        !(uValue.isObject() && uValue.size() == 0)) {
                    ((ObjectNode)old).set(uKey, uValue);
                }
            }
            // merge old and new property
            else {
                JsonNode oValue = old.get(uKey);
                if (uValue.isObject()) {
                    mergeUpdates(oValue, uValue);
                } else {
                    ((ObjectNode)old).set(uKey, uValue);
                }
            }
        }
    }

    private static void mergeRemovals(JsonNode old, JsonNode update) {
        Set<String> msalEntities =
                new HashSet<>(Arrays.asList("Account", "AccessToken", "RefreshToken", "IdToken", "AppMetadata"));

        for (String msalEntity : msalEntities) {
            JsonNode oldEntries = old.get(msalEntity);
            JsonNode newEntries = update.get(msalEntity);
            if (oldEntries != null) {
                Iterator<Map.Entry<String, JsonNode>> iterator = oldEntries.fields();

                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> oEntry = iterator.next();

                    String key = oEntry.getKey();
                    if (newEntries == null || !newEntries.has(key)) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    @Override
    public String serialize() {
        lock.readLock().lock();
        try {
            if (!StringHelper.isBlank(serializedCachedSnapshot)) {
                JsonNode cache = JsonHelper.mapper.readTree(serializedCachedSnapshot);
                JsonNode update = JsonHelper.mapper.valueToTree(this);

                mergeJsonObjects(cache, update);

                return JsonHelper.mapper.writeValueAsString(cache);
            }
            return JsonHelper.mapper.writeValueAsString(this);
        }
        catch (JsonProcessingException e) {
            throw new MsalClientException(e);
        }finally {
            lock.readLock().unlock();
        }
    }

    private class CacheAspect implements AutoCloseable {
        ITokenCacheAccessContext context;

        CacheAspect(ITokenCacheAccessContext context) {
            if (tokenCacheAccessAspect != null) {
                this.context = context;
                tokenCacheAccessAspect.beforeCacheAccess(context);
            }
        }

        @Override
        public void close() {
            if (tokenCacheAccessAspect != null) {
                tokenCacheAccessAspect.afterCacheAccess(context);
            }
        }
    }

    void saveTokens(TokenRequestExecutor tokenRequestExecutor, AuthenticationResult authenticationResult, String environment) {
        try (CacheAspect cacheAspect = new CacheAspect(
                TokenCacheAccessContext.builder().
                        clientId(tokenRequestExecutor.getMsalRequest().application().clientId()).
                        tokenCache(this).
                        hasCacheChanged(true).build())) {
            try {
                lock.writeLock().lock();

                if (!StringHelper.isBlank(authenticationResult.accessToken())) {
                    AccessTokenCacheEntity atEntity = createAccessTokenCacheEntity
                            (tokenRequestExecutor, authenticationResult, environment);
                    accessTokens.put(atEntity.getKey(), atEntity);
                }
                if (!StringHelper.isBlank(authenticationResult.familyId())) {
                    AppMetadataCacheEntity appMetadataCacheEntity =
                            createAppMetadataCacheEntity(tokenRequestExecutor, authenticationResult, environment);

                    appMetadata.put(appMetadataCacheEntity.getKey(), appMetadataCacheEntity);
                }
                if (!StringHelper.isBlank(authenticationResult.refreshToken())) {
                    RefreshTokenCacheEntity rtEntity = createRefreshTokenCacheEntity
                            (tokenRequestExecutor, authenticationResult, environment);

                    rtEntity.family_id(authenticationResult.familyId());

                    refreshTokens.put(rtEntity.getKey(), rtEntity);
                }
                if (!StringHelper.isBlank(authenticationResult.idToken())) {
                    IdTokenCacheEntity idTokenEntity = createIdTokenCacheEntity
                            (tokenRequestExecutor, authenticationResult, environment);
                    idTokens.put(idTokenEntity.getKey(), idTokenEntity);

                    AccountCacheEntity accountCacheEntity = authenticationResult.accountCacheEntity();
                    accountCacheEntity.environment(environment);
                    accounts.put(accountCacheEntity.getKey(), accountCacheEntity);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    static private RefreshTokenCacheEntity createRefreshTokenCacheEntity(TokenRequestExecutor tokenRequestExecutor,
                                                                         AuthenticationResult authenticationResult,
                                                                         String environmentAlias) {
        RefreshTokenCacheEntity rt = new RefreshTokenCacheEntity();
        rt.credentialType(CredentialTypeEnum.REFRESH_TOKEN.value());

        if (authenticationResult.account() != null) {
            rt.homeAccountId(authenticationResult.account().homeAccountId());
        }

        rt.environment(environmentAlias);

        rt.clientId(tokenRequestExecutor.getMsalRequest().application().clientId());

        rt.secret(authenticationResult.refreshToken());

        return rt;
    }

    static private AccessTokenCacheEntity createAccessTokenCacheEntity(TokenRequestExecutor tokenRequestExecutor,
                                                                       AuthenticationResult authenticationResult,
                                                                       String environmentAlias) {
        AccessTokenCacheEntity at = new AccessTokenCacheEntity();
        at.credentialType(CredentialTypeEnum.ACCESS_TOKEN.value());

        if (authenticationResult.account() != null) {
            at.homeAccountId(authenticationResult.account().homeAccountId());
        }
        at.environment(environmentAlias);
        at.clientId(tokenRequestExecutor.getMsalRequest().application().clientId());
        at.secret(authenticationResult.accessToken());
        at.realm(tokenRequestExecutor.requestAuthority.tenant());

        String scopes = !StringHelper.isBlank(authenticationResult.scopes()) ? authenticationResult.scopes() :
                tokenRequestExecutor.getMsalRequest().msalAuthorizationGrant().getScopes();

        at.target(scopes);

        long currTimestampSec = System.currentTimeMillis() / 1000;
        at.cachedAt(Long.toString(currTimestampSec));
        at.expiresOn(Long.toString(authenticationResult.expiresOn()));
        if (authenticationResult.extExpiresOn() > 0) {
            at.extExpiresOn(Long.toString(authenticationResult.extExpiresOn()));
        }

        return at;
    }

    static private IdTokenCacheEntity createIdTokenCacheEntity(TokenRequestExecutor tokenRequestExecutor,
                                                               AuthenticationResult authenticationResult,
                                                               String environmentAlias) {
        IdTokenCacheEntity idToken = new IdTokenCacheEntity();
        idToken.credentialType(CredentialTypeEnum.ID_TOKEN.value());

        if (authenticationResult.account() != null) {
            idToken.homeAccountId(authenticationResult.account().homeAccountId());
        }
        idToken.environment(environmentAlias);
        idToken.clientId(tokenRequestExecutor.getMsalRequest().application().clientId());
        idToken.secret(authenticationResult.idToken());
        idToken.realm(tokenRequestExecutor.requestAuthority.tenant());

        return idToken;
    }

    static private AppMetadataCacheEntity createAppMetadataCacheEntity(TokenRequestExecutor tokenRequestExecutor,
                                                                       AuthenticationResult authenticationResult,
                                                                       String environmentAlias) {
        AppMetadataCacheEntity appMetadataCacheEntity = new AppMetadataCacheEntity();

        appMetadataCacheEntity.clientId(tokenRequestExecutor.getMsalRequest().application().clientId());
        appMetadataCacheEntity.environment(environmentAlias);
        appMetadataCacheEntity.familyId(authenticationResult.familyId());

        return appMetadataCacheEntity;
    }

    Set<IAccount> getAccounts(String clientId, Set<String> environmentAliases) {
        try (CacheAspect cacheAspect = new CacheAspect(
                TokenCacheAccessContext.builder().
                        clientId(clientId).
                        tokenCache(this).
                        build())) {
            try {
                lock.readLock().lock();

                return accounts.values().stream().
                        filter(acc -> environmentAliases.contains(acc.environment())).
                        collect(Collectors.mapping(AccountCacheEntity::toAccount, Collectors.toSet()));
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    /**
     * @return familyId status of application
     */
    private String getApplicationFamilyId(String clientId, Set<String> environmentAliases) {
        for (AppMetadataCacheEntity data : appMetadata.values()) {
            if (data.clientId().equals(clientId) &&
                    environmentAliases.contains(data.environment()) &&
                    !StringHelper.isBlank(data.familyId())) {
                return data.familyId();
            }
        }
        return null;
    }

    /**
     * @return set of client ids which belong to the family
     */
    private Set<String> getFamilyClientIds(String familyId, Set<String> environmentAliases) {

        return appMetadata.values().stream().filter
                (appMetadata -> environmentAliases.contains(appMetadata.environment()) &&
                        familyId.equals(appMetadata.familyId())

                ).map(AppMetadataCacheEntity::clientId).collect(Collectors.toSet());
    }

    /**
     * Remove all cache entities related to account, including account cache entity
     *
     * @param clientId           client id
     * @param account            account
     * @param environmentAliases environment aliases
     */
    void removeAccount(String clientId, IAccount account, Set<String> environmentAliases) {
        try (CacheAspect cacheAspect = new CacheAspect(
                TokenCacheAccessContext.builder().
                        clientId(clientId).
                        tokenCache(this).
                        hasCacheChanged(true).
                        build())) {
            try {
                lock.writeLock().lock();

                removeAccount(account, environmentAliases);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private void removeAccount(IAccount account, Set<String> environmentAliases) {

        Predicate<Map.Entry<String, ? extends Credential>> credentialToRemovePredicate =
                e -> !StringHelper.isBlank(e.getValue().homeAccountId()) &&
                        !StringHelper.isBlank(e.getValue().environment()) &&
                        e.getValue().homeAccountId().equals(account.homeAccountId()) &&
                        environmentAliases.contains(e.getValue().environment());

        accessTokens.entrySet().removeIf(credentialToRemovePredicate);

        refreshTokens.entrySet().removeIf(credentialToRemovePredicate);

        idTokens.entrySet().removeIf(credentialToRemovePredicate);

        accounts.entrySet().removeIf(
                e -> !StringHelper.isBlank(e.getValue().homeAccountId()) &&
                        !StringHelper.isBlank(e.getValue().environment()) &&
                        e.getValue().homeAccountId().equals(account.homeAccountId()) &&
                        environmentAliases.contains(e.getValue().environment));
    }

    private boolean isMatchingScopes(AccessTokenCacheEntity accessTokenCacheEntity, Set<String> scopes) {

        Set<String> accessTokenCacheEntityScopes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        accessTokenCacheEntityScopes.addAll
                (Arrays.asList(accessTokenCacheEntity.target().split(Constants.SCOPES_SEPARATOR)));

        return accessTokenCacheEntityScopes.containsAll(scopes);
    }

    private Optional<AccessTokenCacheEntity> getAccessTokenCacheEntity
            (IAccount account, Authority authority, Set<String> scopes, String clientId,
             Set<String> environmentAliases) {
        long currTimeStampSec = new Date().getTime() / 1000;

        return accessTokens.values().
                stream().filter
                (accessToken -> accessToken.homeAccountId.equals(account.homeAccountId()) &&
                        environmentAliases.contains(accessToken.environment) &&
                        Long.parseLong(accessToken.expiresOn()) > currTimeStampSec + MIN_ACCESS_TOKEN_EXPIRE_IN_SEC &&
                        accessToken.realm.equals(authority.tenant()) &&
                        accessToken.clientId.equals(clientId) &&
                        isMatchingScopes(accessToken, scopes)
                ).findAny();
    }

    private Optional<AccessTokenCacheEntity> getApplicationAccessTokenCacheEntity
            (Authority authority, Set<String> scopes, String clientId,
             Set<String> environmentAliases) {
        long currTimeStampSec = new Date().getTime() / 1000;

        return accessTokens.values().
                stream().filter
                (accessToken -> environmentAliases.contains(accessToken.environment) &&
                        Long.parseLong(accessToken.expiresOn()) > currTimeStampSec + MIN_ACCESS_TOKEN_EXPIRE_IN_SEC &&
                        accessToken.realm.equals(authority.tenant()) &&
                        accessToken.clientId.equals(clientId) &&
                        isMatchingScopes(accessToken, scopes)
                ).findAny();
    }

    private Optional<IdTokenCacheEntity> getIdTokenCacheEntity
            (IAccount account, Authority authority, String clientId, Set<String> environmentAliases) {
        return idTokens.values().stream().filter
                (idToken -> idToken.homeAccountId.equals(account.homeAccountId()) &&
                        environmentAliases.contains(idToken.environment) &&
                        idToken.realm.equals(authority.tenant()) &&
                        idToken.clientId.equals(clientId)
                ).findAny();
    }

    private Optional<RefreshTokenCacheEntity> getRefreshTokenCacheEntity
            (IAccount account, String clientId, Set<String> environmentAliases) {

        return refreshTokens.values().stream().filter
                (refreshToken -> refreshToken.homeAccountId.equals(account.homeAccountId()) &&
                        environmentAliases.contains(refreshToken.environment) &&
                        refreshToken.clientId.equals(clientId)
                ).findAny();
    }

    private Optional<AccountCacheEntity> getAccountCacheEntity
            (IAccount account, Set<String> environmentAliases) {

        return accounts.values().stream().filter
                (acc -> acc.homeAccountId.equals(account.homeAccountId()) &&
                        environmentAliases.contains(acc.environment)
                ).findAny();
    }

    private Optional<RefreshTokenCacheEntity> getAnyFamilyRefreshTokenCacheEntity
            (IAccount account, Set<String> environmentAliases) {

        return refreshTokens.values().stream().filter
                (refreshToken -> refreshToken.homeAccountId.equals(account.homeAccountId()) &&
                        environmentAliases.contains(refreshToken.environment) &&
                        refreshToken.isFamilyRT()
                ).findAny();
    }

    AuthenticationResult getCachedAuthenticationResult
            (IAccount account, Authority authority, Set<String> scopes, String clientId) {
        AuthenticationResult.AuthenticationResultBuilder builder = AuthenticationResult.builder();
        builder.environment(authority.host());

        Set<String> environmentAliases = AadInstanceDiscovery.getAliases(account.environment());

        try (CacheAspect cacheAspect = new CacheAspect(
                TokenCacheAccessContext.builder().
                        clientId(clientId).
                        tokenCache(this).
                        account(account).
                        build())) {
            try {
                lock.readLock().lock();

                Optional<AccountCacheEntity> accountCacheEntity =
                        getAccountCacheEntity(account, environmentAliases);

                Optional<AccessTokenCacheEntity> atCacheEntity =
                        getAccessTokenCacheEntity(account, authority, scopes, clientId, environmentAliases);

                Optional<IdTokenCacheEntity> idTokenCacheEntity =
                        getIdTokenCacheEntity(account, authority, clientId, environmentAliases);

                Optional<RefreshTokenCacheEntity> rtCacheEntity;

                if (!StringHelper.isBlank(getApplicationFamilyId(clientId, environmentAliases))) {
                    rtCacheEntity = getAnyFamilyRefreshTokenCacheEntity(account, environmentAliases);
                    if (!rtCacheEntity.isPresent()) {
                        rtCacheEntity = getRefreshTokenCacheEntity(account, clientId, environmentAliases);
                    }
                } else {
                    rtCacheEntity = getRefreshTokenCacheEntity(account, clientId, environmentAliases);
                    if (!rtCacheEntity.isPresent()) {
                        rtCacheEntity = getAnyFamilyRefreshTokenCacheEntity(account, environmentAliases);
                    }
                }

                if (atCacheEntity.isPresent()) {
                    builder.
                            accessToken(atCacheEntity.get().secret).
                            expiresOn(Long.parseLong(atCacheEntity.get().expiresOn()));
                }
                if (idTokenCacheEntity.isPresent()) {
                    builder.
                            idToken(idTokenCacheEntity.get().secret);
                }
                if (rtCacheEntity.isPresent()) {
                    builder.
                            refreshToken(rtCacheEntity.get().secret);
                }
                if (accountCacheEntity.isPresent()) {
                    builder.
                            accountCacheEntity(accountCacheEntity.get());
                }
            } finally {
                lock.readLock().unlock();
            }
        }
        return builder.build();
    }

    AuthenticationResult getCachedAuthenticationResult
            (Authority authority, Set<String> scopes, String clientId) {
        AuthenticationResult.AuthenticationResultBuilder builder = AuthenticationResult.builder();

        Set<String> environmentAliases = AadInstanceDiscovery.getAliases(authority.host);
        builder.environment(authority.host());

        try (CacheAspect cacheAspect = new CacheAspect(
                TokenCacheAccessContext.builder().
                        clientId(clientId).
                        tokenCache(this).
                        build())) {
            try {
                lock.readLock().lock();

                Optional<AccessTokenCacheEntity> atCacheEntity =
                        getApplicationAccessTokenCacheEntity(authority, scopes, clientId, environmentAliases);

                if (atCacheEntity.isPresent()) {
                    builder.
                            accessToken(atCacheEntity.get().secret).
                            expiresOn(Long.parseLong(atCacheEntity.get().expiresOn()));
                }
            } finally {
                lock.readLock().unlock();
            }
            return builder.build();
        }
    }
}
