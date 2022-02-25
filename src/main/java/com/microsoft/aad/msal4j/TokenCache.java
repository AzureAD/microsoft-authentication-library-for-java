// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.nimbusds.jwt.JWTParser;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
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

        TokenCache deserializedCache = new TokenCache();
        try {
             deserializedCache.convertJsonToObject(data);
        } catch (IOException e) {
            throw new MsalClientException(e);
        }

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

    private void convertJsonToObject(String json) throws IOException {
        if(json!=null){
            JsonFactory jsonFactory = new JsonFactory();
            JsonParser jsonParser = jsonFactory.createParser(json);

            String key;
            jsonParser.nextToken();
            while (jsonParser.currentToken() != JsonToken.END_OBJECT) {
                String fieldname = jsonParser.getCurrentName();
                if(fieldname!=null){
                    jsonParser.nextToken();
                    jsonParser.nextToken();
                    if ("AccessToken".equals(fieldname)) {
                        while(jsonParser.getCurrentToken() != JsonToken.END_OBJECT){
                            key = jsonParser.getCurrentName();
                            jsonParser.nextToken();
                            AccessTokenCacheEntity accessTokenCacheEntity = AccessTokenCacheEntity.convertJsonToObject(json, jsonParser);
                            accessTokens.put(key, accessTokenCacheEntity);
                            jsonParser.nextToken();
                        }

                    } else if ("RefreshToken".equals(fieldname)) {
                        while(jsonParser.getCurrentToken() != JsonToken.END_OBJECT){
                            key = jsonParser.getCurrentName();
                            jsonParser.nextToken();
                            RefreshTokenCacheEntity refreshTokenCacheEntity = RefreshTokenCacheEntity.convertJsonToObject(json, jsonParser);
                            refreshTokens.put(key, refreshTokenCacheEntity);
                            jsonParser.nextToken();
                        }
                    } else if ("IdToken".equals(fieldname)) {
                        while(jsonParser.getCurrentToken() != JsonToken.END_OBJECT){
                            key = jsonParser.getCurrentName();
                            jsonParser.nextToken();
                            IdTokenCacheEntity idTokenCacheEntity = IdTokenCacheEntity.convertJsonToObject(json, jsonParser);
                            idTokens.put(key, idTokenCacheEntity);
                            jsonParser.nextToken();
                        }
                    } else if ("Account".equals(fieldname)) {
                        while(jsonParser.getCurrentToken() != JsonToken.END_OBJECT){
                            key = jsonParser.getCurrentName();
                            jsonParser.nextToken();
                            AccountCacheEntity accountCacheEntity = AccountCacheEntity.convertJsonToObject(json, jsonParser);
                            accounts.put(key, accountCacheEntity);
                            jsonParser.nextToken();
                        }

                    } else if ("AppMetadata".equals(fieldname)) {
                        while(jsonParser.getCurrentToken() != JsonToken.END_OBJECT){
                            key = jsonParser.getCurrentName();
                            jsonParser.nextToken();
                            AppMetadataCacheEntity appMetadataCacheEntity = AppMetadataCacheEntity.
                                    convertJsonToObject(json, jsonParser);
                            appMetadata.put(key, appMetadataCacheEntity);
                            jsonParser.nextToken();
                        }
                    } else {
                        while(jsonParser.getCurrentToken() != JsonToken.END_OBJECT){
                            jsonParser.nextToken();
                        }
                    }
                }
                jsonParser.nextToken();
            }

        }
    }

    private String convertToJsonString(){
        JSONObject jsonObject = new JSONObject();
        JSONObject jsonObject1 = new JSONObject();
        for(String key: accounts.keySet()){
                jsonObject1.put(key, accounts.get(key).convertToJSONObject());

        }
        jsonObject.put("Account", jsonObject1);
        jsonObject1 = new JSONObject();
        for(String key: accessTokens.keySet()){
                jsonObject1.put(key, accessTokens.get(key).convertToJSONObject());
        }
        jsonObject.put("AccessToken", jsonObject1);
        jsonObject1 = new JSONObject();
        for(String key: refreshTokens.keySet()){
                jsonObject1.put(key, refreshTokens.get(key).convertToJSONObject());
        }
        jsonObject.put("RefreshToken", jsonObject1);
        jsonObject1 = new JSONObject();
        for(String key: idTokens.keySet()){
                jsonObject1.put(key, idTokens.get(key).convertToJSONObject());
        }
        jsonObject.put("IdToken", jsonObject1);
        jsonObject1 = new JSONObject();
        for(String key: appMetadata.keySet()){
                jsonObject1.put(key, appMetadata.get(key).convertToJSONObject());
        }
        jsonObject.put("AppMetadata", jsonObject1);
        return jsonObject.toString();
    }

    @Override
    public String serialize(){
        lock.readLock().lock();
        try {
            if (!StringHelper.isBlank(serializedCachedSnapshot)){
                serializedCachedSnapshot = JsonHelper.mergeJSONString(serializedCachedSnapshot, convertToJsonString());
                return serializedCachedSnapshot;
            }
            return convertToJsonString();


        } catch (Exception e) {
            throw new MsalClientException(e);
        } finally {
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
        idToken.realm(tokenRequestExecutor.requestAuthority.tenant());

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
                        Map<String, ?> idTokenClaims = JWTParser.parse(idToken.secret()).getJWTClaimsSet().getClaims();
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

                    if (accCached.homeAccountId().contains(accCached.localAccountId())) {
                        ((Account) rootAccounts.get(accCached.homeAccountId())).username(accCached.username());
                    }
                }

                return new HashSet<>(rootAccounts.values());
            } catch (ParseException e) {
                throw new MsalClientException("Cached JWT could not be parsed: " + e.getMessage(), AuthenticationErrorCode.INVALID_JWT);
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
        long currTimeStampSec = new Date().getTime() / 1000;

        return accessTokens.values().stream().filter(
                accessToken ->
                        accessToken.homeAccountId.equals(account.homeAccountId()) &&
                                environmentAliases.contains(accessToken.environment) &&
                                Long.parseLong(accessToken.expiresOn()) > currTimeStampSec + MIN_ACCESS_TOKEN_EXPIRE_IN_SEC &&
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
        long currTimeStampSec = new Date().getTime() / 1000;

        return accessTokens.values().stream().filter(
                accessToken ->
                        userAssertionHashMatches(accessToken, userAssertionHash) &&
                                environmentAliases.contains(accessToken.environment) &&
                                Long.parseLong(accessToken.expiresOn()) > currTimeStampSec + MIN_ACCESS_TOKEN_EXPIRE_IN_SEC &&
                                accessToken.realm.equals(authority.tenant()) &&
                                accessToken.clientId.equals(clientId) &&
                                isMatchingScopes(accessToken, scopes))
                .findAny();
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
            IAccount account,
            String clientId,
            Set<String> environmentAliases) {

        return refreshTokens.values().stream().filter(
                refreshToken ->
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

        AuthenticationResult.AuthenticationResultBuilder builder = AuthenticationResult.builder();
        builder.environment(authority.host());

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
                    if (atCacheEntity.get().refreshOn() != null) {
                        builder.refreshOn(Long.parseLong(atCacheEntity.get().refreshOn()));
                    }
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
                        getApplicationAccessTokenCacheEntity(authority, scopes, clientId, environmentAliases, userAssertionHash);

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
            } finally {
                lock.readLock().unlock();
            }
            return builder.build();
        }
    }
}
