// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Context in which the token cache is accessed
 * <p>
 * For more details, see https://aka.ms/msal4j-token-cache
 */
public class TokenCacheAccessContext implements ITokenCacheAccessContext {

    private ITokenCache tokenCache;
    private String clientId;
    private IAccount account;
    private boolean hasCacheChanged;

    TokenCacheAccessContext(ITokenCache tokenCache, String clientId, IAccount account, boolean hasCacheChanged) {
        this.tokenCache = tokenCache;
        this.clientId = clientId;
        this.account = account;
        this.hasCacheChanged = hasCacheChanged;
    }

    /**
     * TODO: Add description
     */
    public static TokenCacheAccessContextBuilder builder() {
        return new TokenCacheAccessContextBuilder();
    }

    /**
     * TODO: Add description
     */
    public ITokenCache tokenCache() {
        return this.tokenCache;
    }

    /**
     * TODO: Add description
     */
    public String clientId() {
        return this.clientId;
    }

    /**
     * TODO: Add description
     */
    public IAccount account() {
        return this.account;
    }

    /**
     * Checks if has cache changed.
     * 
     * @return true if has cache changed, false otherwise
     */
    public boolean hasCacheChanged() {
        return this.hasCacheChanged;
    }

    public static class TokenCacheAccessContextBuilder {
        private ITokenCache tokenCache;
        private String clientId;
        private IAccount account;
        private boolean hasCacheChanged;

        TokenCacheAccessContextBuilder() {
        }

    /**
     * TODO: Add description
     */
        public TokenCacheAccessContextBuilder tokenCache(ITokenCache tokenCache) {
            this.tokenCache = tokenCache;
            return this;
        }

    /**
     * TODO: Add description
     */
        public TokenCacheAccessContextBuilder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

    /**
     * TODO: Add description
     */
        public TokenCacheAccessContextBuilder account(IAccount account) {
            this.account = account;
            return this;
        }

    /**
     * TODO: Add description
     */
        public TokenCacheAccessContextBuilder hasCacheChanged(boolean hasCacheChanged) {
            this.hasCacheChanged = hasCacheChanged;
            return this;
        }

    /**
     * Builds and returns the configured object.
     * 
     * @return built object instance
     */
        public TokenCacheAccessContext build() {
            return new TokenCacheAccessContext(this.tokenCache, this.clientId, this.account, this.hasCacheChanged);
        }

    /**
     * Returns a string representation of the object.
     * 
     * @return string representation of this object
     */
        public String toString() {
            return "TokenCacheAccessContext.TokenCacheAccessContextBuilder(tokenCache=" + this.tokenCache + ", clientId=" + this.clientId + ", account=" + this.account + ", hasCacheChanged=" + this.hasCacheChanged + ")";
        }
    }
}
