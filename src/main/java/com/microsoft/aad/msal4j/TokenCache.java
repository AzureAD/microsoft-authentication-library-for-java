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

public class TokenCache {

    public static final int MIN_ACCESS_TOKEN_EXPIRE_IN_SEC = 5*60;

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

    @SerializedName("Account")
    Map<String, Account> accounts = new LinkedTreeMap<>();

    private ITokenCacheAccessAspect tokenCacheAccessAspect;

    private String serializedCachedData;

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
    }

    public String serialize() {
        if(!StringHelper.isBlank(serializedCachedData)){
            Object o = new Gson().fromJson(serializedCachedData, Object.class);
            Map<String, Object> map = (Map<String, Object>)o;

            map.put("AccessToken", accessTokens);
            map.put("RefreshToken", refreshTokens);

            map.put("IdToken", idTokens);
            map.put("Account", accounts);

            return new GsonBuilder().create().toJson(map);
        }

        return new GsonBuilder().create().toJson(this);
    }

    protected void saveTokens
            (AdalTokenRequest tokenRequest, AuthenticationResult authenticationResult, String environment){

        if(tokenCacheAccessAspect != null){
            TokenCacheAccessContext context = TokenCacheAccessContext.builder().
                    clientId(tokenRequest.getClientAuth().getClientID().getValue()).
                    tokenCache(this).
                    build();
            tokenCacheAccessAspect.beforeCacheAccess(context);
        }

        if(!StringHelper.isBlank(authenticationResult.accessToken())){
            AccessTokenCacheEntity atEntity = createAccessTokenCacheEntity
                    (tokenRequest, authenticationResult, environment);
            accessTokens.put(atEntity.getKey(), atEntity);
        }
        if(!StringHelper.isBlank(authenticationResult.refreshToken())){
            RefreshTokenCacheEntity rtEntity = createRefreshTokenCacheEntity
                    (tokenRequest, authenticationResult, environment);
            refreshTokens.put(rtEntity.getKey(), rtEntity);
        }
        if(!StringHelper.isBlank(authenticationResult.idToken())){
            IdTokenCacheEntity idTokenEntity = createIdTokenCacheEntity
                    (tokenRequest, authenticationResult, environment);
            idTokens.put(idTokenEntity.getKey(), idTokenEntity);

            Account account = authenticationResult.account();
            account.environment(environment);
            accounts.put(account.getKey(), account);
        }

        if(tokenCacheAccessAspect != null){
            TokenCacheAccessContext context = TokenCacheAccessContext.builder().
                    clientId(tokenRequest.getClientAuth().getClientID().getValue()).
                    tokenCache(this).
                    isCacheChanged(true).
                    build();
            tokenCacheAccessAspect.afterCacheAccess(context);
        }
    }

    static RefreshTokenCacheEntity createRefreshTokenCacheEntity(AdalTokenRequest tokenRequest,
                                                                 AuthenticationResult authenticationResult,
                                                                 String environmentAlias) {
        RefreshTokenCacheEntity rt = new RefreshTokenCacheEntity();

        if(authenticationResult.account() != null){
            rt.homeAccountId(authenticationResult.account().homeAccountId);
        }

        rt.environment(environmentAlias);

        rt.clientId(tokenRequest.getClientAuth().getClientID().toString());

        rt.secret(authenticationResult.refreshToken());

        return rt;
    }

    static AccessTokenCacheEntity createAccessTokenCacheEntity(AdalTokenRequest tokenRequest,
                                                               AuthenticationResult authenticationResult,
                                                               String environmentAlias) {
        AccessTokenCacheEntity at = new AccessTokenCacheEntity();

        if(authenticationResult.account() != null){
            at.homeAccountId(authenticationResult.account().homeAccountId);
        }
        at.environment(environmentAlias);
        at.clientId(tokenRequest.getClientAuth().getClientID().toString());
        at.secret(authenticationResult.accessToken());

        IdToken idTokenObj = authenticationResult.idTokenObject();
        if (idTokenObj != null) {
            at.realm(idTokenObj.tenantIdentifier);
        }

        String scopes = !StringHelper.isBlank(authenticationResult.scopes()) ? authenticationResult.scopes() :
                tokenRequest.getAuthorizationGrant().getScopes();

        at.target(scopes);

        long currTimestampSec = System.currentTimeMillis()/1000;
        at.cachedAt(Long.toString(currTimestampSec));
        at.expiresOn(Long.toString(authenticationResult.expiresOn()));
        if(authenticationResult.extExpiresOn() > 0){
            at.extExpiresOn(Long.toString(authenticationResult.extExpiresOn()));
        }

        return at;
    }

    static IdTokenCacheEntity createIdTokenCacheEntity(AdalTokenRequest tokenRequest,
                                                       AuthenticationResult authenticationResult,
                                                       String environmentAlias) {
        IdTokenCacheEntity idToken = new IdTokenCacheEntity();

        if(authenticationResult.account() != null){
            idToken.homeAccountId(authenticationResult.account().homeAccountId);
        }
        idToken.environment(environmentAlias);
        idToken.clientId(tokenRequest.getClientAuth().getClientID().toString());
        idToken.secret(authenticationResult.idToken());

        IdToken idTokenObj = authenticationResult.idTokenObject();
        if (idTokenObj != null) {
            idToken.setRealm(idTokenObj.tenantIdentifier);
        }

        return idToken;
    }

    /**
     * @return Collection of accounts from cache_data which can be used for silent acquire token call
     */
    protected Collection<Account> getAccounts(String clientId, Set<String> environmentAliases) {
        TokenCacheAccessContext context = null;
        if(tokenCacheAccessAspect != null){
            context = TokenCacheAccessContext.builder().
                    clientId(clientId).
                    tokenCache(this).
                    build();
            tokenCacheAccessAspect.beforeCacheAccess(context);
        }

        Collection<Account> result = accounts.values().stream().filter
                (account -> environmentAliases.contains(account.environment) &&
                        refreshTokens.values().stream().anyMatch
                        (refreshToken -> refreshToken.homeAccountId.equals(account.homeAccountId) &&
                                refreshToken.environment.equals(account.environment) &&
                                refreshToken.clientId.equals(clientId)
                        )
                ).collect(Collectors.toList());

        if(tokenCacheAccessAspect != null){
            tokenCacheAccessAspect.afterCacheAccess(context);
        }

        return result;
    }

    /**
     * Remove all credentials from cache related to account
     */
    protected void removeAccount(String clientId, Account account, Set<String> environmentAliases) {
        TokenCacheAccessContext context = null;
        if(tokenCacheAccessAspect != null){
            context = TokenCacheAccessContext.builder().
                    clientId(clientId).
                    tokenCache(this).
                    build();
            tokenCacheAccessAspect.beforeCacheAccess(context);
        }

        Predicate<Map.Entry<String, ? extends Credential>> credentialToRemovePredicate = e ->
                e.getValue().homeAccountId().equals(account.homeAccountId) &&
                        environmentAliases.contains(e.getValue().environment);

        accessTokens.entrySet().removeIf(credentialToRemovePredicate);

        refreshTokens.entrySet().removeIf(credentialToRemovePredicate);

        idTokens.entrySet().removeIf(credentialToRemovePredicate);

        if(tokenCacheAccessAspect != null){
            tokenCacheAccessAspect.afterCacheAccess(context);
        }
    }

    boolean isMatchingScopes(AccessTokenCacheEntity accessTokenCacheEntity, Set<String> scopes){

        Set<String> accessTokenCacheEntityScopes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        accessTokenCacheEntityScopes.addAll
                (Arrays.asList(accessTokenCacheEntity.target().split(Constants.SCOPES_SEPARATOR)));

        return accessTokenCacheEntityScopes.containsAll(scopes);
    }

    Optional<AccessTokenCacheEntity> getAccessTokenCacheEntity
            (Account account, AuthenticationAuthority authority, Set<String> scopes, String clientId,
             Set<String> environmentAliases){
        long currTimeStampSec = new Date().getTime()/1000;

        return accessTokens.values().stream().filter
                (accessToken -> accessToken.homeAccountId.equals(account.homeAccountId) &&
                        environmentAliases.contains(accessToken.environment) &&
                        Long.parseLong(accessToken.expiresOn()) > currTimeStampSec + MIN_ACCESS_TOKEN_EXPIRE_IN_SEC &&
                        accessToken.realm.equals(authority.getTenant()) &&
                        accessToken.clientId.equals(clientId) &&
                        isMatchingScopes(accessToken, scopes)
                ).findAny();
    }

    Optional<IdTokenCacheEntity> getIdTokenCacheEntity
            (Account account, AuthenticationAuthority authority, String clientId, Set<String> environmentAliases){
        return idTokens.values().stream().filter
                (idToken -> idToken.homeAccountId.equals(account.homeAccountId) &&
                        environmentAliases.contains(idToken.environment) &&
                        idToken.realm.equals(authority.getTenant()) &&
                        idToken.clientId.equals(clientId)
                ).findAny();
    }

    Optional<RefreshTokenCacheEntity> getRefreshTokenCacheEntity
            (Account account, String clientId, Set<String> environmentAliases) {

        return refreshTokens.values().stream().filter
                (refreshToken -> refreshToken.homeAccountId.equals(account.homeAccountId) &&
                        environmentAliases.contains(refreshToken.environment) &&
                        refreshToken.clientId.equals(clientId)
                ).findAny();
    }

    AuthenticationResult getAuthenticationResult
            (Account account, AuthenticationAuthority authority, Set<String> scopes, String clientId) {

        TokenCacheAccessContext context = null;
        if(tokenCacheAccessAspect != null){
            context = TokenCacheAccessContext.builder().
                    clientId(clientId).
                    tokenCache(this).
                    account(account).
                    build();
            tokenCacheAccessAspect.beforeCacheAccess(context);
        }

        Set<String> environmentAliases = AadInstanceDiscovery.cache.get(account.environment).getAliasesSet();

        Optional<AccessTokenCacheEntity> atCacheEntity =
                getAccessTokenCacheEntity(account, authority, scopes, clientId, environmentAliases);

        Optional<IdTokenCacheEntity> idTokenCacheEntity =
                getIdTokenCacheEntity(account, authority, clientId, environmentAliases);

        Optional<RefreshTokenCacheEntity> rtCacheEntity =
                getRefreshTokenCacheEntity(account, clientId, environmentAliases);

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
        builder.account(account);
        builder.environment(authority.getHost());

        return builder.build();
    }
}
