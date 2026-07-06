// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.Serializable;
import java.util.Objects;

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

    public static AuthenticationResultMetadataBuilder builder() {
        return new AuthenticationResultMetadataBuilder();
    }

    public TokenSource tokenSource() {
        return this.tokenSource;
    }

    public Long refreshOn() {
        return this.refreshOn;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthenticationResultMetadata)) return false;

        AuthenticationResultMetadata other = (AuthenticationResultMetadata) o;

        return Objects.equals(tokenSource, other.tokenSource)
                && Objects.equals(refreshOn, other.refreshOn)
                && Objects.equals(cacheRefreshReason, other.cacheRefreshReason);
    }

    @Override
    public int hashCode() {
        int result = tokenSource == null ? 0 : tokenSource.hashCode();
        result = 31 * result + (refreshOn == null ? 0 : refreshOn.hashCode());
        result = 31 * result + (cacheRefreshReason == null ? 0 : cacheRefreshReason.hashCode());
        return result;
    }

    public static class AuthenticationResultMetadataBuilder {
        private TokenSource tokenSource;
        private Long refreshOn;
        private CacheRefreshReason cacheRefreshReason;

        AuthenticationResultMetadataBuilder() {
        }

        public AuthenticationResultMetadataBuilder tokenSource(TokenSource tokenSource) {
            this.tokenSource = tokenSource;
            return this;
        }

        public AuthenticationResultMetadataBuilder refreshOn(Long refreshOn) {
            this.refreshOn = refreshOn;
            return this;
        }

        public AuthenticationResultMetadataBuilder cacheRefreshReason(CacheRefreshReason cacheRefreshReason) {
            this.cacheRefreshReason = cacheRefreshReason;
            return this;
        }

        public AuthenticationResultMetadata build() {
            return new AuthenticationResultMetadata(this.tokenSource, this.refreshOn, cacheRefreshReason);
        }

        public String toString() {
            return "AuthenticationResultMetadata.AuthenticationResultMetadataBuilder(tokenSource=" + this.tokenSource + ", refreshOn=" + this.refreshOn + ", cacheRefreshReason$value=" + this.cacheRefreshReason + ")";
        }
    }
}