// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.Serializable;

/**
 * Contains metadata and additional context for the contents of an AuthenticationResult
 */
public class AuthenticationResultMetadata implements Serializable {

    /**
     * The source of the tokens in the {@link AuthenticationResult}, see {@link TokenSource} for possible values
     */
    private TokenSource tokenSource;

    /**
     * When the token should be proactively refreshed. May be null or 0 if proactive refresh is not used
     */
    private Long refreshOn;

    /**
     * Specifies the reason for refreshing the access token, see {@link CacheRefreshReason} for possible values. Will be {@link CacheRefreshReason#NOT_APPLICABLE} if the token was returned from the cache or if the API used to fetch the token does not attempt to read the cache.
     */
    private CacheRefreshReason cacheRefreshReason = CacheRefreshReason.NOT_APPLICABLE;

    AuthenticationResultMetadata(TokenSource tokenSource, Long refreshOn, CacheRefreshReason cacheRefreshReason) {
        this.tokenSource = tokenSource;
        this.refreshOn = refreshOn;
        this.cacheRefreshReason = cacheRefreshReason == null ? CacheRefreshReason.NOT_APPLICABLE : cacheRefreshReason;
    }

    /**
     * TODO: Add description
     */
    public static AuthenticationResultMetadataBuilder builder() {
        return new AuthenticationResultMetadataBuilder();
    }

    /**
     * TODO: Add description
     */
    public TokenSource tokenSource() {
        return this.tokenSource;
    }

    /**
     * TODO: Add description
     */
    public Long refreshOn() {
        return this.refreshOn;
    }

    /**
     * TODO: Add description
     */
    public CacheRefreshReason cacheRefreshReason() {
        return this.cacheRefreshReason;
    }

    void tokenSource(TokenSource tokenSource) {
        this.tokenSource = tokenSource;
    }

    void refreshOn(Long refreshOn) {
        this.refreshOn = refreshOn;
    }

    void cacheRefreshReason(CacheRefreshReason cacheRefreshReason) {
        this.cacheRefreshReason = cacheRefreshReason;
    }

    public static class AuthenticationResultMetadataBuilder {
        private TokenSource tokenSource;
        private Long refreshOn;
        private CacheRefreshReason cacheRefreshReason;

        AuthenticationResultMetadataBuilder() {
        }

    /**
     * TODO: Add description
     */
        public AuthenticationResultMetadataBuilder tokenSource(TokenSource tokenSource) {
            this.tokenSource = tokenSource;
            return this;
        }

    /**
     * TODO: Add description
     */
        public AuthenticationResultMetadataBuilder refreshOn(Long refreshOn) {
            this.refreshOn = refreshOn;
            return this;
        }

    /**
     * TODO: Add description
     */
        public AuthenticationResultMetadataBuilder cacheRefreshReason(CacheRefreshReason cacheRefreshReason) {
            this.cacheRefreshReason = cacheRefreshReason;
            return this;
        }

    /**
     * Builds and returns the configured object.
     * 
     * @return built object instance
     */
        public AuthenticationResultMetadata build() {
            return new AuthenticationResultMetadata(this.tokenSource, this.refreshOn, cacheRefreshReason);
        }

    /**
     * Returns a string representation of the object.
     * 
     * @return string representation of this object
     */
        public String toString() {
            return "AuthenticationResultMetadata.AuthenticationResultMetadataBuilder(tokenSource=" + this.tokenSource + ", refreshOn=" + this.refreshOn + ", cacheRefreshReason$value=" + this.cacheRefreshReason + ")";
        }
    }
}