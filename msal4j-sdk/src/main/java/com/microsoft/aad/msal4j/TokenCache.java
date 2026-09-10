// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Cache used for storing tokens. For more details, see https://aka.ms/msal4j-token-cache
 * <p>
 * Conditionally thread-safe
 */
public class TokenCache implements ITokenCache {

    protected static final int MIN_ACCESS_TOKEN_EXPIRE_IN_SEC = 5 * 60;

    private ReadWriteLock lock = new ReentrantReadWriteLock();

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

    Map<String, AccessTokenCacheEntity> accessTokens = new LinkedHashMap<>();
    Map<String, RefreshTokenCacheEntity> refreshTokens = new LinkedHashMap<>();
    Map<String, IdTokenCacheEntity> idTokens = new LinkedHashMap<>();
    Map<String, AccountCacheEntity> accounts = new LinkedHashMap<>();
    Map<String, AppMetadataCacheEntity> appMetadata = new LinkedHashMap<>();

    ITokenCacheAccessAspect tokenCacheAccessAspect;

    private String serializedCachedSnapshot;

    @Override
    public void deserialize(String data) {
        if (StringHelper.isBlank(data)) {
            return;
        }
        serializedCachedSnapshot = data;

        try {
            JsonReader jsonReader = JsonProviders.createReader(data);
            deserializeFromJson(jsonReader);
        } catch (IOException e) {
            throw new MsalClientException(e);
        }
    }

    private void deserializeFromJson(JsonReader jsonReader) throws IOException {
        lock.writeLock().lock();
        try {
            jsonReader.readObject(reader -> {
                while (reader.nextToken() != JsonToken.END_OBJECT) {
                    String fieldName = reader.getFieldName();
                    reader.nextToken();

                    switch (fieldName) {
                        case "AccessToken":
                            deserializeCollection(reader, accessTokens, AccessTokenCacheEntity::fromJson);
                            break;
                        case "RefreshToken":
                            deserializeCollection(reader, refreshTokens, RefreshTokenCacheEntity::fromJson);
                            break;
                        case "IdToken":
                            deserializeCollection(reader, idTokens, IdTokenCacheEntity::fromJson);
                            break;
                        case "Account":
                            deserializeCollection(reader, accounts, AccountCacheEntity::fromJson);
                            break;
                        case "AppMetadata":
                            deserializeCollection(reader, appMetadata, AppMetadataCacheEntity::fromJson);
                            break;
                        default:
                            reader.skipChildren();
                            break;
                    }
                }
                return null;
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    private <T> void deserializeCollection(
            JsonReader reader,
            Map<String, T> targetCollection,
            ReadValueCallback<JsonReader, T> deserializer) throws IOException {

        reader.readObject(entityReader -> {
            while (entityReader.nextToken() != JsonToken.END_OBJECT) {
                String key = entityReader.getFieldName();
                entityReader.nextToken();
                T entity = deserializer.read(entityReader);
                targetCollection.put(key, entity);
            }
            return null;
        });
    }

    @Override
    public String serialize() {
        lock.readLock().lock();
        try {
            if (!StringHelper.isBlank(serializedCachedSnapshot)) {
                String updatedCache = mergeWithExistingCache();
                if (updatedCache != null) {
                    return updatedCache;
                }
            }
            return serializeToJson();
        } finally {
            lock.readLock().unlock();
        }
    }

    private String serializeToJson() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             JsonWriter jsonWriter = JsonProviders.createWriter(outputStream)) {

            jsonWriter.writeStartObject();

            // Write all collections
            writeCollection(jsonWriter, "AccessToken", accessTokens);
            writeCollection(jsonWriter, "RefreshToken", refreshTokens);
            writeCollection(jsonWriter, "IdToken", idTokens);
            writeCollection(jsonWriter, "Account", accounts);
            writeCollection(jsonWriter, "AppMetadata", appMetadata);

            jsonWriter.writeEndObject();
            jsonWriter.flush();

            return outputStream.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new MsalClientException(e);
        }
    }

    private <T> void writeCollection(
            JsonWriter jsonWriter,
            String collectionName,
            Map<String, T> collection) throws IOException {

        jsonWriter.writeFieldName(collectionName);

        jsonWriter.writeStartObject();

        for (Map.Entry<String, T> entry : collection.entrySet()) {
            jsonWriter.writeFieldName(entry.getKey());
            if (entry.getValue() instanceof JsonSerializable) {
                ((JsonSerializable<?>) entry.getValue()).toJson(jsonWriter);
            }
        }

        jsonWriter.writeEndObject();
    }

    private String mergeWithExistingCache() {
        try {
            // Parse existing cache snapshot
            TokenCache updatedCache = new TokenCache();
            updatedCache.deserialize(serializedCachedSnapshot);

            // Merge current in-memory cache with the snapshot
            mergeCache(updatedCache);

            // Serialize merged cache
            return updatedCache.serializeToJson();
        } catch (Exception e) {
            return null;
        }
    }

    private void mergeCache(TokenCache targetCache) {
        targetCache.accessTokens.putAll(accessTokens);
        targetCache.refreshTokens.putAll(refreshTokens);
        targetCache.idTokens.putAll(idTokens);
        targetCache.accounts.putAll(accounts);
        targetCache.appMetadata.putAll(appMetadata);

        // Handle removals by removing keys that are not in the current cache
        targetCache.accessTokens.keySet().retainAll(accessTokens.keySet());
        targetCache.refreshTokens.keySet().retainAll(refreshTokens.keySet());
        targetCache.idTokens.keySet().retainAll(idTokens.keySet());
        targetCache.accounts.keySet().retainAll(accounts.keySet());
        targetCache.appMetadata.keySet().retainAll(appMetadata.keySet());
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
                    if(accountCacheEntity!=null) {
                        accountCacheEntity.environment(environment);
                        accounts.put(accountCacheEntity.getKey(), accountCacheEntity);
                    }
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private static RefreshTokenCacheEntity createRefreshTokenCacheEntity(TokenRequestExecutor tokenRequestExecutor,
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

        if (tokenRequestExecutor.getMsalRequest() instanceof OnBehalfOfRequest) {
            OnBehalfOfRequest onBehalfOfRequest = (OnBehalfOfRequest) tokenRequestExecutor.getMsalRequest();
            rt.userAssertionHash(onBehalfOfRequest.parameters.userAssertion().getAssertionHash());
        }

        return rt;
    }

    private static AccessTokenCacheEntity createAccessTokenCacheEntity(TokenRequestExecutor tokenRequestExecutor,
                                                                       AuthenticationResult authenticationResult,
                                                                       String environmentAlias) {
        AccessTokenCacheEntity at = new AccessTokenCacheEntity();

        // Determine if extended cache key is needed (e.g., for fmi_path in agent identity scenarios)
        String extCacheKeyHash = computeExtCacheKeyHashForRequest(tokenRequestExecutor.getMsalRequest());

        if (!StringHelper.isBlank(extCacheKeyHash)) {
            at.credentialType(CredentialTypeEnum.ACCESS_TOKEN_EXTENDED.value());
            at.extCacheKeyHash(extCacheKeyHash);
        } else {
            at.credentialType(CredentialTypeEnum.ACCESS_TOKEN.value());
        }

        if (authenticationResult.account() != null) {
            at.homeAccountId(authenticationResult.account().homeAccountId());
        }
        at.environment(environmentAlias);
        at.clientId(tokenRequestExecutor.getMsalRequest().application().clientId());
        at.secret(authenticationResult.accessToken());
        at.realm(tokenRequestExecutor.tenant);

        String scopes = !StringHelper.isBlank(authenticationResult.scopes()) ? authenticationResult.scopes() :
                String.join(" ", tokenRequestExecutor.getMsalRequest().msalAuthorizationGrant().getScopes());

        at.target(scopes);

        if (tokenRequestExecutor.getMsalRequest() instanceof OnBehalfOfRequest) {
            OnBehalfOfRequest onBehalfOfRequest = (OnBehalfOfRequest) tokenRequestExecutor.getMsalRequest();
            at.userAssertionHash(onBehalfOfRequest.parameters.userAssertion().getAssertionHash());
        }

        long currTimestampSec = System.currentTimeMillis() / 1000;
        at.cachedAt(Long.toString(currTimestampSec));
        at.expiresOn(Long.toString(authenticationResult.expiresOn()));
        if (authenticationResult.refreshOn() > 0) {
            at.refreshOn(Long.toString(authenticationResult.refreshOn()));
        }
        if (authenticationResult.extExpiresOn() > 0) {
            at.extExpiresOn(Long.toString(authenticationResult.extExpiresOn()));
        }

        return at;
    }

    /**
     * Computes the extended cache key hash for a request, if applicable.
     * Delegates to the generic cache key components exposed by the request's parameters object
     * ({@link IAcquireTokenParameters#computeExtCacheKeyHash()}), which covers client credentials,
     * managed identity, on-behalf-of, user-FIC and authorization-code redemption. Parameter types
     * without extended cache-key components return an empty string via the interface default.
     * The algorithm uses sorted key-value concatenation → SHA-256 → Base64URL (cross-SDK compatible).
     */
    private static String computeExtCacheKeyHashForRequest(MsalRequest msalRequest) {
        if (!StringHelper.isBlank(msalRequest.extCacheKeyHash())) {
            return msalRequest.extCacheKeyHash();
        }

        // A RefreshTokenRequest inherits the parent silent request's RequestContext, whose
        // apiParameters (SilentParameters) carries no client-originated claims and would therefore
        // return an empty hash. Prefer the hash threaded onto the parent silent request so a refreshed
        // token is written back to the same cache partition as the original (e.g. client_claims /
        // user-FIC) instead of leaking into the default partition.
        if (msalRequest instanceof RefreshTokenRequest) {
            String parentHash = ((RefreshTokenRequest) msalRequest).extCacheKeyHash();
            if (!StringHelper.isBlank(parentHash)) {
                return parentHash;
            }
        }

        IAcquireTokenParameters apiParameters = msalRequest.requestContext().apiParameters();
        if (apiParameters != null) {
            return apiParameters.computeExtCacheKeyHash();
        }
        return "";
    }

    private static IdTokenCacheEntity createIdTokenCacheEntity(TokenRequestExecutor tokenRequestExecutor,
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
        idToken.realm(tokenRequestExecutor.tenant);

        if (tokenRequestExecutor.getMsalRequest() instanceof OnBehalfOfRequest) {
            OnBehalfOfRequest onBehalfOfRequest = (OnBehalfOfRequest) tokenRequestExecutor.getMsalRequest();
            idToken.userAssertionHash(onBehalfOfRequest.parameters.userAssertion().getAssertionHash());
        }

        return idToken;
    }

    private static AppMetadataCacheEntity createAppMetadataCacheEntity(TokenRequestExecutor tokenRequestExecutor,
                                                                       AuthenticationResult authenticationResult,
                                                                       String environmentAlias) {
        AppMetadataCacheEntity appMetadataCacheEntity = new AppMetadataCacheEntity();

        appMetadataCacheEntity.clientId(tokenRequestExecutor.getMsalRequest().application().clientId());
        appMetadataCacheEntity.environment(environmentAlias);
        appMetadataCacheEntity.familyId(authenticationResult.familyId());

        return appMetadataCacheEntity;
    }

    Set<IAccount> getAccounts(String clientId) {
        try (CacheAspect cacheAspect = new CacheAspect(
                TokenCacheAccessContext.builder().
                        clientId(clientId).
                        tokenCache(this).
                        build())) {
            try {
                lock.readLock().lock();
                Map<String, IAccount> rootAccounts = new HashMap<>();

                for (AccountCacheEntity accCached : accounts.values()) {

                    IdTokenCacheEntity idToken = idTokens.get(getIdTokenKey(
                            accCached.homeAccountId(),
                            accCached.environment(),
                            clientId,
                            accCached.realm()));

                    ITenantProfile profile = null;
                    if (idToken != null) {
                        Map<String, ?> idTokenClaims = JsonHelper.parseJsonToMap(JsonHelper.getTokenPayloadClaims(idToken.secret));
                        profile = new TenantProfile(idTokenClaims, accCached.environment());
                    }

                    if (rootAccounts.get(accCached.homeAccountId()) == null) {
                        IAccount acc = accCached.toAccount();
                        ((Account) acc).tenantProfiles = new HashMap<>();

                        rootAccounts.put(accCached.homeAccountId(), acc);
                    }

                    if (profile != null) {
                        ((Account) rootAccounts.get(accCached.homeAccountId())).tenantProfiles.put(accCached.realm(), profile);
                    }

                    if (accCached.localAccountId() != null && accCached.homeAccountId().contains(accCached.localAccountId())) {
                        ((Account) rootAccounts.get(accCached.homeAccountId())).username(accCached.username());
                    }
                }

                return new HashSet<>(rootAccounts.values());
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    /**
     * Returns a String representing a key of a cached ID token, formatted in the same way as {@link IdTokenCacheEntity#getKey}
     *
     * @return String representing a possible key of a cached ID token
     */
    private String getIdTokenKey(String homeAccountId, String environment, String clientId, String realm) {
        return String.join(Constants.CACHE_KEY_SEPARATOR,
                Arrays.asList(homeAccountId,
                        environment,
                        "idtoken", clientId,
                        realm, "")).toLowerCase();
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
     * @param clientId client id
     * @param account  account
     */
    void removeAccount(String clientId, IAccount account) {
        try (CacheAspect cacheAspect = new CacheAspect(
                TokenCacheAccessContext.builder().
                        clientId(clientId).
                        tokenCache(this).
                        hasCacheChanged(true).
                        build())) {
            try {
                lock.writeLock().lock();

                removeAccount(account);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private void removeAccount(IAccount account) {

        Predicate<Map.Entry<String, ? extends Credential>> credentialToRemovePredicate =
                e -> !StringHelper.isBlank(e.getValue().homeAccountId()) &&
                        !StringHelper.isBlank(e.getValue().environment()) &&
                        e.getValue().homeAccountId().equals(account.homeAccountId());

        accessTokens.entrySet().removeIf(credentialToRemovePredicate);

        refreshTokens.entrySet().removeIf(credentialToRemovePredicate);

        idTokens.entrySet().removeIf(credentialToRemovePredicate);

        accounts.entrySet().removeIf(
                e -> !StringHelper.isBlank(e.getValue().homeAccountId()) &&
                        !StringHelper.isBlank(e.getValue().environment()) &&
                        e.getValue().homeAccountId().equals(account.homeAccountId()));
    }

    private boolean isMatchingScopes(AccessTokenCacheEntity accessTokenCacheEntity, Set<String> scopes) {

        Set<String> accessTokenCacheEntityScopes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        accessTokenCacheEntityScopes.addAll
                (Arrays.asList(accessTokenCacheEntity.target().split(Constants.SCOPES_SEPARATOR)));

        return accessTokenCacheEntityScopes.containsAll(scopes);
    }

    private boolean userAssertionHashMatches(Credential credential, String userAssertionHash) {
        if (userAssertionHash == null) {
            return true;
        }

        return credential.userAssertionHash() != null &&
                credential.userAssertionHash().equalsIgnoreCase(userAssertionHash);
    }

    private boolean userAssertionHashMatches(AccountCacheEntity accountCacheEntity, String userAssertionHash) {
        if (userAssertionHash == null) {
            return true;
        }

        return accountCacheEntity.userAssertionHash() != null &&
                accountCacheEntity.userAssertionHash().equalsIgnoreCase(userAssertionHash);
    }

    private Optional<AccessTokenCacheEntity> getAccessTokenCacheEntity(
            IAccount account,
            Authority authority,
            Set<String> scopes,
            String clientId,
            Set<String> environmentAliases) {
        return getAccessTokenCacheEntity(account, authority, scopes, clientId, environmentAliases, null);
    }

    private Optional<AccessTokenCacheEntity> getAccessTokenCacheEntity(
            IAccount account,
            Authority authority,
            Set<String> scopes,
            String clientId,
            Set<String> environmentAliases,
            String extCacheKeyHash) {

        return accessTokens.values().stream().filter(
                accessToken ->
                        accessToken.homeAccountId != null &&
                                accessToken.homeAccountId.equals(account.homeAccountId()) &&
                                extCacheKeyHashMatches(accessToken, extCacheKeyHash) &&
                                environmentAliases.contains(accessToken.environment) &&
                                accessToken.realm.equals(authority.tenant()) &&
                                accessToken.clientId.equals(clientId) &&
                                isMatchingScopes(accessToken, scopes)
        ).findAny();
    }

    private Optional<AccessTokenCacheEntity> getApplicationAccessTokenCacheEntity(
            Authority authority,
            Set<String> scopes,
            String clientId,
            Set<String> environmentAliases,
            String userAssertionHash) {
        return getApplicationAccessTokenCacheEntity(authority, scopes, clientId, environmentAliases, userAssertionHash, null);
    }

    private Optional<AccessTokenCacheEntity> getApplicationAccessTokenCacheEntity(
            Authority authority,
            Set<String> scopes,
            String clientId,
            Set<String> environmentAliases,
            String userAssertionHash,
            String extCacheKeyHash) {
        long currTimeStampSec = new Date().getTime() / 1000;

        return accessTokens.values().stream().filter(
                accessToken ->
                        userAssertionHashMatches(accessToken, userAssertionHash) &&
                                extCacheKeyHashMatches(accessToken, extCacheKeyHash) &&
                                environmentAliases.contains(accessToken.environment) &&
                                Long.parseLong(accessToken.expiresOn()) > currTimeStampSec + MIN_ACCESS_TOKEN_EXPIRE_IN_SEC &&
                                accessToken.realm.equals(authority.tenant()) &&
                                accessToken.clientId.equals(clientId) &&
                                isMatchingScopes(accessToken, scopes))
                .findAny();
    }

    private boolean extCacheKeyHashMatches(AccessTokenCacheEntity accessToken, String expectedHash) {
        String cachedHash = accessToken.extCacheKeyHash();
        if (StringHelper.isBlank(expectedHash)) {
            // When no fmi_path/ext key is expected, only match tokens that also have no ext key
            return StringHelper.isBlank(cachedHash);
        }
        return expectedHash.equals(cachedHash);
    }


    private Optional<IdTokenCacheEntity> getIdTokenCacheEntity(
            IAccount account,
            Authority authority,
            String clientId,
            Set<String> environmentAliases) {
        return idTokens.values().stream().filter(
                idToken ->
                        idToken.homeAccountId.equals(account.homeAccountId()) &&
                                environmentAliases.contains(idToken.environment) &&
                                idToken.realm.equals(authority.tenant()) &&
                                idToken.clientId.equals(clientId)
        ).findAny();
    }

    private Optional<IdTokenCacheEntity> getIdTokenCacheEntity(
            Authority authority,
            String clientId,
            Set<String> environmentAliases,
            String userAssertionHash) {
        return idTokens.values().stream().filter(
                idToken ->
                        userAssertionHashMatches(idToken, userAssertionHash) &&
                                environmentAliases.contains(idToken.environment) &&
                                idToken.realm.equals(authority.tenant()) &&
                                idToken.clientId.equals(clientId)
        ).findAny();
    }

    private Optional<RefreshTokenCacheEntity> getRefreshTokenCacheEntity(
            String clientId,
            Set<String> environmentAliases,
            String userAssertionHash) {
        return refreshTokens.values().stream().filter(
                refreshToken ->
                        userAssertionHashMatches(refreshToken, userAssertionHash) &&
                                environmentAliases.contains(refreshToken.environment) &&
                                refreshToken.clientId.equals(clientId)
        ).findAny();
    }

    private Optional<RefreshTokenCacheEntity> getRefreshTokenCacheEntity(
            IAccount account,
            String clientId,
            Set<String> environmentAliases) {

        return refreshTokens.values().stream().filter(
                refreshToken ->
                        refreshToken.homeAccountId != null &&
                        refreshToken.homeAccountId.equals(account.homeAccountId()) &&
                                environmentAliases.contains(refreshToken.environment) &&
                                refreshToken.clientId.equals(clientId)
        ).findAny();
    }

    private Optional<AccountCacheEntity> getAccountCacheEntity(
            IAccount account,
            Set<String> environmentAliases) {

        return accounts.values().stream().filter(
                acc ->
                        acc.homeAccountId.equals(account.homeAccountId()) &&
                                environmentAliases.contains(acc.environment)
        ).findAny();
    }

    private Optional<AccountCacheEntity> getAccountCacheEntity(
            Set<String> environmentAliases,
            String userAssertionHash) {

        return accounts.values().stream().filter(
                acc -> userAssertionHashMatches(acc, userAssertionHash) &&
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

    AuthenticationResult getCachedAuthenticationResult(
            IAccount account,
            Authority authority,
            Set<String> scopes,
            String clientId) {
        return getCachedAuthenticationResult(account, authority, scopes, clientId, null);
    }

    AuthenticationResult getCachedAuthenticationResult(
            IAccount account,
            Authority authority,
            Set<String> scopes,
            String clientId,
            String extCacheKeyHash) {

        AuthenticationResult.AuthenticationResultBuilder builder = AuthenticationResult.builder();

        Set<String> environmentAliases = AadInstanceDiscoveryProvider.getAliases(account.environment());

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
                        getAccessTokenCacheEntity(account, authority, scopes, clientId, environmentAliases, extCacheKeyHash);

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
                            environment(atCacheEntity.get().environment).
                            accessToken(atCacheEntity.get().secret).
                            expiresOn(Long.parseLong(atCacheEntity.get().expiresOn()));
                    if (atCacheEntity.get().refreshOn() != null) {
                        builder.refreshOn(Long.parseLong(atCacheEntity.get().refreshOn()));
                    }
                } else {
                    builder.environment(authority.host());
                }
                idTokenCacheEntity.ifPresent(tokenCacheEntity -> builder.idToken(tokenCacheEntity.secret));
                rtCacheEntity.ifPresent(refreshTokenCacheEntity ->
                        builder.refreshToken(refreshTokenCacheEntity.secret));
                accountCacheEntity.ifPresent(builder::accountCacheEntity);
            } finally {
                lock.readLock().unlock();
            }
        }
        return builder.build();
    }

    AuthenticationResult getCachedAuthenticationResult(
            Authority authority,
            Set<String> scopes,
            String clientId,
            IUserAssertion assertion) {
        return getCachedAuthenticationResult(authority, scopes, clientId, assertion, null);
    }

    AuthenticationResult getCachedAuthenticationResult(
            Authority authority,
            Set<String> scopes,
            String clientId,
            IUserAssertion assertion,
            String extCacheKeyHash) {

        AuthenticationResult.AuthenticationResultBuilder builder = AuthenticationResult.builder();

        Set<String> environmentAliases = AadInstanceDiscoveryProvider.getAliases(authority.host);
        builder.environment(authority.host());

        try (CacheAspect cacheAspect = new CacheAspect(
                TokenCacheAccessContext.builder().
                        clientId(clientId).
                        tokenCache(this).
                        build())) {
            try {
                lock.readLock().lock();

                String userAssertionHash = assertion == null ? null : assertion.getAssertionHash();

                Optional<AccountCacheEntity> accountCacheEntity =
                        getAccountCacheEntity(environmentAliases, userAssertionHash);

                accountCacheEntity.ifPresent(builder::accountCacheEntity);

                Optional<AccessTokenCacheEntity> atCacheEntity =
                        getApplicationAccessTokenCacheEntity(authority, scopes, clientId, environmentAliases, userAssertionHash, extCacheKeyHash);

                if (atCacheEntity.isPresent()) {
                    builder.
                            accessToken(atCacheEntity.get().secret).
                            expiresOn(Long.parseLong(atCacheEntity.get().expiresOn()));
                    if (atCacheEntity.get().refreshOn() != null) {
                        builder.refreshOn(Long.parseLong(atCacheEntity.get().refreshOn()));
                    }
                }

                Optional<IdTokenCacheEntity> idTokenCacheEntity =
                        getIdTokenCacheEntity(authority, clientId, environmentAliases, userAssertionHash);

                idTokenCacheEntity.ifPresent(tokenCacheEntity -> builder.idToken(tokenCacheEntity.secret));

                Optional<RefreshTokenCacheEntity> rtCacheEntity = getRefreshTokenCacheEntity(clientId, environmentAliases, userAssertionHash);

                rtCacheEntity.ifPresent(refreshTokenCacheEntity ->
                        builder.refreshToken(refreshTokenCacheEntity.secret));
            } finally {
                lock.readLock().unlock();
            }
            return builder.build();
        }
    }
}
