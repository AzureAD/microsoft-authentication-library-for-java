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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TokenCache implements ITokenCache {

    protected static final int MIN_ACCESS_TOKEN_EXPIRE_IN_SEC = 5*60;

    public TokenCache(ITokenCacheAccessAspect tokenCacheAccessAspect) {
        this();
        this.tokenCacheAccessAspect = tokenCacheAccessAspect;
    }

    public TokenCache() {
    }

    @SerializedName("AccessToken")
    Map<String, AccessTokenCacheEntity> accessTokens = new LinkedTreeMap<>();

    @SerializedName("RefreshToken")
    Map<String, RefreshTokenCacheEntity> refreshTokens = new LinkedTreeMap<>();

    @SerializedName("IdToken")
    Map<String, IdTokenCacheEntity> idTokens = new LinkedTreeMap<>();

    @SerializedName("AccountCacheEntity")
    Map<String, AccountCacheEntity> accounts = new LinkedTreeMap<>();

    @SerializedName("AppMetadata")
    Map<String, AppMetadataCacheEntity> appMetadata = new LinkedTreeMap<>();

    transient ITokenCacheAccessAspect tokenCacheAccessAspect;

    private transient String serializedCachedData;

    @Override
    public void deserializeAndLoadToCache(String data) {
        if(StringHelper.isBlank(data)){
            return;
        }
        serializedCachedData = data;
        Gson gson = new GsonBuilder().create();

        TokenCache deserializedCache = gson.fromJson(data, TokenCache.class);

        this.accounts = deserializedCache.accounts;
        this.accessTokens = deserializedCache.accessTokens;
        this.refreshTokens = deserializedCache.refreshTokens;
        this.idTokens = deserializedCache.idTokens;
        this.appMetadata = deserializedCache.appMetadata;
    }

    @Override
    public String serialize() {
        if(!StringHelper.isBlank(serializedCachedData)){
            Object o = new Gson().fromJson(serializedCachedData, Object.class);
            Map<String, Object> map = (Map<String, Object>)o;

            map.put("AccessToken", accessTokens);
            map.put("RefreshToken", refreshTokens);

            map.put("IdToken", idTokens);
            map.put("AccountCacheEntity", accounts);

            map.put("AppMetadata", appMetadata);

            return new GsonBuilder().create().toJson(map);
        }

        return new GsonBuilder().create().toJson(this);
    }

    protected void saveTokens
            (TokenRequest tokenRequest, AuthenticationResult authenticationResult, String environment){

        if(tokenCacheAccessAspect != null){
            ITokenCacheAccessContext context = TokenCacheAccessContext.builder().
                    clientId(tokenRequest.getMsalRequest().application().clientId()).
                    tokenCache(this).
                    build();
            tokenCacheAccessAspect.beforeCacheAccess(context);
        }

        if(!StringHelper.isBlank(authenticationResult.accessToken())){
            AccessTokenCacheEntity atEntity = createAccessTokenCacheEntity
                    (tokenRequest, authenticationResult, environment);
            accessTokens.put(atEntity.getKey(), atEntity);
        }
        if(!StringHelper.isBlank(authenticationResult.familyId())){
            AppMetadataCacheEntity appMetadataCacheEntity =
                    createAppMetadataCacheEntity(tokenRequest, authenticationResult, environment);

            appMetadata.put(appMetadataCacheEntity.getKey(), appMetadataCacheEntity);
        }
        if(!StringHelper.isBlank(authenticationResult.refreshToken())){
            RefreshTokenCacheEntity rtEntity = createRefreshTokenCacheEntity
                    (tokenRequest, authenticationResult, environment);

            rtEntity.family_id(authenticationResult.familyId());

            refreshTokens.put(rtEntity.getKey(), rtEntity);
        }
        if(!StringHelper.isBlank(authenticationResult.idToken())){
            IdTokenCacheEntity idTokenEntity = createIdTokenCacheEntity
                    (tokenRequest, authenticationResult, environment);
            idTokens.put(idTokenEntity.getKey(), idTokenEntity);

            AccountCacheEntity accountCacheEntity = authenticationResult.accountCacheEntity();
            accountCacheEntity.environment(environment);
            accounts.put(accountCacheEntity.getKey(), accountCacheEntity);
        }

        if(tokenCacheAccessAspect != null){
            ITokenCacheAccessContext context = TokenCacheAccessContext.builder().
                    clientId(tokenRequest.getMsalRequest().application().clientId()).
                    tokenCache(this).
                    isCacheChanged(true).
                    build();
            tokenCacheAccessAspect.afterCacheAccess(context);
        }
    }

    static RefreshTokenCacheEntity createRefreshTokenCacheEntity(TokenRequest tokenRequest,
                                                                 AuthenticationResult authenticationResult,
                                                                 String environmentAlias) {
        RefreshTokenCacheEntity rt = new RefreshTokenCacheEntity();

        if(authenticationResult.account() != null){
            rt.homeAccountId(authenticationResult.account().homeAccountId());
        }

        rt.environment(environmentAlias);

        rt.clientId(tokenRequest.getMsalRequest().application().clientId());

        rt.secret(authenticationResult.refreshToken());

        return rt;
    }

    static AccessTokenCacheEntity createAccessTokenCacheEntity(TokenRequest tokenRequest,
                                                               AuthenticationResult authenticationResult,
                                                               String environmentAlias) {
        AccessTokenCacheEntity at = new AccessTokenCacheEntity();

        if(authenticationResult.account() != null){
            at.homeAccountId(authenticationResult.account().homeAccountId());
        }
        at.environment(environmentAlias);
        at.clientId(tokenRequest.getMsalRequest().application().clientId());
        at.secret(authenticationResult.accessToken());

        IdToken idTokenObj = authenticationResult.idTokenObject();
        if (idTokenObj != null) {
            at.realm(idTokenObj.tenantIdentifier);
        }

        String scopes = !StringHelper.isBlank(authenticationResult.scopes()) ? authenticationResult.scopes() :
                tokenRequest.getMsalRequest().msalAuthorizationGrant().getScopes();

        at.target(scopes);

        long currTimestampSec = System.currentTimeMillis()/1000;
        at.cachedAt(Long.toString(currTimestampSec));
        at.expiresOn(Long.toString(authenticationResult.expiresOn()));
        if(authenticationResult.extExpiresOn() > 0){
            at.extExpiresOn(Long.toString(authenticationResult.extExpiresOn()));
        }

        return at;
    }

    static IdTokenCacheEntity createIdTokenCacheEntity(TokenRequest tokenRequest,
                                                       AuthenticationResult authenticationResult,
                                                       String environmentAlias) {
        IdTokenCacheEntity idToken = new IdTokenCacheEntity();

        if(authenticationResult.account() != null){
            idToken.homeAccountId(authenticationResult.account().homeAccountId());
        }
        idToken.environment(environmentAlias);
        idToken.clientId(tokenRequest.getMsalRequest().application().clientId());
        idToken.secret(authenticationResult.idToken());

        IdToken idTokenObj = authenticationResult.idTokenObject();
        if (idTokenObj != null) {
            idToken.setRealm(idTokenObj.tenantIdentifier);
        }

        return idToken;
    }

    static AppMetadataCacheEntity createAppMetadataCacheEntity(TokenRequest tokenRequest,
                                                       AuthenticationResult authenticationResult,
                                                       String environmentAlias) {
        AppMetadataCacheEntity appMetadataCacheEntity = new AppMetadataCacheEntity();

        appMetadataCacheEntity.clientId(tokenRequest.getMsalRequest().application().clientId());
        appMetadataCacheEntity.environment(environmentAlias);
        appMetadataCacheEntity.familyId(authenticationResult.familyId());

        return appMetadataCacheEntity;
    }

    protected Set<IAccount> getAccounts(String clientId, Set<String> environmentAliases) {

        ITokenCacheAccessContext context = null;
        if(tokenCacheAccessAspect != null){
            context = TokenCacheAccessContext.builder().
                    clientId(clientId).
                    tokenCache(this).
                    build();
            tokenCacheAccessAspect.beforeCacheAccess(context);
        }

        Set<IAccount> result = accounts.values().stream().
                filter(acc -> environmentAliases.contains(acc.environment())).
                collect(Collectors.mapping(AccountCacheEntity::toAccount, Collectors.toSet()));

        if(tokenCacheAccessAspect != null){
            tokenCacheAccessAspect.afterCacheAccess(context);
        }

        return result;
    }

    /**
     * @return familyId status of application
     */
    String getApplicationFamilyId(String clientId, Set<String> environmentAliases) {
        for(AppMetadataCacheEntity data : appMetadata.values()){
            if(data.clientId().equals(clientId) &&
                    environmentAliases.contains(data.environment()) &&
                    !StringHelper.isBlank(data.familyId()))
            {
                return data.familyId();
            }
        }
        return null;
    }

    /**
     * @return set of client ids which belong to the family
     */
    Set<String> getFamilyClientIds(String familyId, Set<String> environmentAliases) {

        return appMetadata.values().stream().filter
                (appMetadata -> environmentAliases.contains(appMetadata.environment()) &&
                        familyId.equals(appMetadata.familyId())

                ).map(AppMetadataCacheEntity::clientId).collect(Collectors.toSet());
    }

    /**
     * Remove all cache entities related to account, including account cache entity
     *
     * @param clientId client id
     * @param account account
     * @param environmentAliases environment aliases
     */
    protected void removeAccount(String clientId, IAccount account, Set<String> environmentAliases) {
        ITokenCacheAccessContext context = null;
        if(tokenCacheAccessAspect != null){
            context = TokenCacheAccessContext.builder().
                    clientId(clientId).
                    tokenCache(this).
                    build();
            tokenCacheAccessAspect.beforeCacheAccess(context);
        }

        removeAccount(account, environmentAliases);

        if(tokenCacheAccessAspect != null){
            tokenCacheAccessAspect.afterCacheAccess(context);
        }
    }

    private void removeAccount(IAccount account, Set<String> environmentAliases) {

        Predicate<Map.Entry<String, ? extends Credential>> credentialToRemovePredicate = e ->
                e.getValue().homeAccountId().equals(account.homeAccountId()) &&
                        environmentAliases.contains(e.getValue().environment);

        accessTokens.entrySet().removeIf(credentialToRemovePredicate);

        refreshTokens.entrySet().removeIf(credentialToRemovePredicate);

        idTokens.entrySet().removeIf(credentialToRemovePredicate);

        accounts.entrySet().removeIf(e -> e.getValue().homeAccountId().equals(account.homeAccountId()) &&
                        environmentAliases.contains(e.getValue().environment));
    }

    boolean isMatchingScopes(AccessTokenCacheEntity accessTokenCacheEntity, Set<String> scopes){

        Set<String> accessTokenCacheEntityScopes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        accessTokenCacheEntityScopes.addAll
                (Arrays.asList(accessTokenCacheEntity.target().split(Constants.SCOPES_SEPARATOR)));

        return accessTokenCacheEntityScopes.containsAll(scopes);
    }

    Optional<AccessTokenCacheEntity> getAccessTokenCacheEntity
            (IAccount account, Authority authority, Set<String> scopes, String clientId,
             Set<String> environmentAliases){
        long currTimeStampSec = new Date().getTime()/1000;

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

    Optional<IdTokenCacheEntity> getIdTokenCacheEntity
            (IAccount account, Authority authority, String clientId, Set<String> environmentAliases){
        return idTokens.values().stream().filter
                (idToken -> idToken.homeAccountId.equals(account.homeAccountId()) &&
                        environmentAliases.contains(idToken.environment) &&
                        idToken.realm.equals(authority.tenant()) &&
                        idToken.clientId.equals(clientId)
                ).findAny();
    }

    Optional<RefreshTokenCacheEntity> getRefreshTokenCacheEntity
            (IAccount account, String clientId, Set<String> environmentAliases) {

        return refreshTokens.values().stream().filter
                (refreshToken -> refreshToken.homeAccountId.equals(account.homeAccountId()) &&
                        environmentAliases.contains(refreshToken.environment) &&
                        refreshToken.clientId.equals(clientId)
                ).findAny();
    }

    Optional<AccountCacheEntity> getAccountCacheEntity
            (IAccount account, Set<String> environmentAliases) {

        return accounts.values().stream().filter
                (acc -> acc.homeAccountId.equals(account.homeAccountId()) &&
                        environmentAliases.contains(acc.environment)
                ).findAny();
    }

    Optional<RefreshTokenCacheEntity> getAnyFamilyRefreshTokenCacheEntity
            (IAccount account, Set<String> environmentAliases) {

        return refreshTokens.values().stream().filter
                (refreshToken -> refreshToken.homeAccountId.equals(account.homeAccountId()) &&
                        environmentAliases.contains(refreshToken.environment) &&
                        refreshToken.isFamilyRT()
                ).findAny();
    }

    AuthenticationResult getAuthenticationResult
            (IAccount account, Authority authority, Set<String> scopes, String clientId) {

        ITokenCacheAccessContext context = null;
        if(tokenCacheAccessAspect != null){
            context = TokenCacheAccessContext.builder().
                    clientId(clientId).
                    tokenCache(this).
                    account(account).
                    build();
            tokenCacheAccessAspect.beforeCacheAccess(context);
        }

        Set<String> environmentAliases = AadInstanceDiscovery.cache.get(account.environment()).getAliasesSet();

        Optional<AccountCacheEntity> accountCacheEntity =
                getAccountCacheEntity(account, environmentAliases);

        Optional<AccessTokenCacheEntity> atCacheEntity =
                getAccessTokenCacheEntity(account, authority, scopes, clientId, environmentAliases);

        Optional<IdTokenCacheEntity> idTokenCacheEntity =
                getIdTokenCacheEntity(account, authority, clientId, environmentAliases);

        Optional<RefreshTokenCacheEntity> rtCacheEntity;

        if (!StringHelper.isBlank(getApplicationFamilyId(clientId, environmentAliases))) {
            rtCacheEntity = getAnyFamilyRefreshTokenCacheEntity(account, environmentAliases);
            if(!rtCacheEntity.isPresent()){
                rtCacheEntity = getRefreshTokenCacheEntity(account, clientId, environmentAliases);
            }
        }
        else{
            rtCacheEntity = getRefreshTokenCacheEntity(account, clientId, environmentAliases);
            if(!rtCacheEntity.isPresent()){
                rtCacheEntity = getAnyFamilyRefreshTokenCacheEntity(account, environmentAliases);
            }
        }

        if(tokenCacheAccessAspect != null){
            tokenCacheAccessAspect.afterCacheAccess(context);
        }

        AuthenticationResult.AuthenticationResultBuilder builder = AuthenticationResult.builder();
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
        builder.environment(authority.host());

        return builder.build();
    }
}
