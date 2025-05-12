// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Date;
import java.util.Objects;

final class AuthenticationResult implements IAuthenticationResult {
    private static final long serialVersionUID = 1L;

    private final String accessToken;
    private final long expiresOn;
    private final long extExpiresOn;
    private final String refreshToken;
    private final Long refreshOn;
    private final String familyId;
    private final String idToken;
    private final IdToken idTokenObject = getIdTokenObj();
    private final AccountCacheEntity accountCacheEntity;
    private final IAccount account = getAccount();
    private final ITenantProfile tenantProfile = getTenantProfile();
    private String environment;
    private final Date expiresOnDate;
    private final String scopes;
    private final AuthenticationResultMetadata metadata;
    private final Boolean isPopAuthorization;

    AuthenticationResult(String accessToken, long expiresOn, long extExpiresOn, String refreshToken, Long refreshOn, String familyId, String idToken, AccountCacheEntity accountCacheEntity, String environment, String scopes, AuthenticationResultMetadata metadata, Boolean isPopAuthorization) {
        this.accessToken = accessToken;
        this.expiresOn = expiresOn;
        this.extExpiresOn = extExpiresOn;
        this.refreshToken = refreshToken;
        this.refreshOn = refreshOn;
        this.familyId = familyId;
        this.idToken = idToken;
        this.accountCacheEntity = accountCacheEntity;
        this.environment = environment;
        this.scopes = scopes;
        this.metadata = metadata == null ? AuthenticationResultMetadata.builder().build() : metadata;
        this.isPopAuthorization = isPopAuthorization;
        this.expiresOnDate = new Date(expiresOn * 1000);
    }

    private IdToken getIdTokenObj() {
        if (StringHelper.isBlank(idToken)) {
            return null;
        }

        return JsonHelper.createIdTokenFromEncodedTokenString(idToken);
    }

    private IAccount getAccount() {
        if (accountCacheEntity == null) {
            return null;
        }
        return accountCacheEntity.toAccount();
    }

    private ITenantProfile getTenantProfile() {
        if (StringHelper.isBlank(idToken)) {
            return null;
        }

        return new TenantProfile(JsonHelper.parseJsonToMap(JsonHelper.getTokenPayloadClaims(idToken)), getAccount().environment());
    }

    public String accessToken() {
        return this.accessToken;
    }

    String refreshToken() {
        return this.refreshToken;
    }

    Long refreshOn() {
        return this.refreshOn;
    }

    public String idToken() {
        return this.idToken;
    }

    public String environment() {
        return this.environment;
    }

    public String scopes() {
        return this.scopes;
    }

    public AuthenticationResultMetadata metadata() {
        return this.metadata;
    }

    long expiresOn() {
        return this.expiresOn;
    }

    long extExpiresOn() {
        return this.extExpiresOn;
    }

    String familyId() {
        return this.familyId;
    }

    IdToken idTokenObject() {
        return this.idTokenObject;
    }

    AccountCacheEntity accountCacheEntity() {
        return this.accountCacheEntity;
    }

    public IAccount account() {
        return getAccount();
    }

    public ITenantProfile tenantProfile() {
        return this.tenantProfile;
    }

    public Date expiresOnDate() {
        return this.expiresOnDate;
    }

    Boolean isPopAuthorization() {
        return this.isPopAuthorization;
    }

    static AuthenticationResultBuilder builder() {
        return new AuthenticationResultBuilder();
    }

    static class AuthenticationResultBuilder {
        private String accessToken;
        private long expiresOn;
        private long extExpiresOn;
        private String refreshToken;
        private Long refreshOn;
        private String familyId;
        private String idToken;
        private AccountCacheEntity accountCacheEntity;
        private String environment;
        private String scopes;
        private AuthenticationResultMetadata metadata;
        private Boolean isPopAuthorization;

        AuthenticationResultBuilder() {
        }

        public AuthenticationResultBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public AuthenticationResultBuilder expiresOn(long expiresOn) {
            this.expiresOn = expiresOn;
            return this;
        }

        public AuthenticationResultBuilder extExpiresOn(long extExpiresOn) {
            this.extExpiresOn = extExpiresOn;
            return this;
        }

        public AuthenticationResultBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public AuthenticationResultBuilder refreshOn(Long refreshOn) {
            this.refreshOn = refreshOn;
            return this;
        }

        public AuthenticationResultBuilder familyId(String familyId) {
            this.familyId = familyId;
            return this;
        }

        public AuthenticationResultBuilder idToken(String idToken) {
            this.idToken = idToken;
            return this;
        }

        public AuthenticationResultBuilder accountCacheEntity(AccountCacheEntity accountCacheEntity) {
            this.accountCacheEntity = accountCacheEntity;
            return this;
        }

        public AuthenticationResultBuilder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public AuthenticationResultBuilder scopes(String scopes) {
            this.scopes = scopes;
            return this;
        }

        public AuthenticationResultBuilder metadata(AuthenticationResultMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public AuthenticationResultBuilder isPopAuthorization(Boolean isPopAuthorization) {
            this.isPopAuthorization = isPopAuthorization;
            return this;
        }

        public AuthenticationResult build() {
            return new AuthenticationResult(this.accessToken, this.expiresOn, this.extExpiresOn, this.refreshToken, this.refreshOn, this.familyId, this.idToken, this.accountCacheEntity, this.environment, this.scopes, this.metadata, this.isPopAuthorization);
        }

        public String toString() {
            return "AuthenticationResult.AuthenticationResultBuilder(accessToken=" + this.accessToken + ", expiresOn=" + this.expiresOn + ", extExpiresOn=" + this.extExpiresOn + ", refreshToken=" + this.refreshToken + ", refreshOn=" + this.refreshOn + ", familyId=" + this.familyId + ", idToken=" + this.idToken + ", accountCacheEntity=" + this.accountCacheEntity + ", environment=" + this.environment + ", scopes=" + this.scopes + ", metadata=" + this.metadata + ", isPopAuthorization=" + this.isPopAuthorization + ")";
        }
    }

    //These methods are based on those generated by Lombok's @EqualsAndHashCode annotation.
    //They have the same functionality as the generated methods, but were refactored for readability.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthenticationResult)) return false;

        AuthenticationResult other = (AuthenticationResult) o;

        if (this.expiresOn() != other.expiresOn()) return false;
        if (this.extExpiresOn() != other.extExpiresOn()) return false;
        if (!Objects.equals(refreshOn, other.refreshOn)) return false;
        if (!Objects.equals(isPopAuthorization, other.isPopAuthorization)) return false;
        if (!Objects.equals(accessToken, other.accessToken)) return false;
        if (!Objects.equals(refreshToken, other.refreshToken)) return false;
        if (!Objects.equals(familyId, other.familyId)) return false;
        if (!Objects.equals(idToken, other.idToken)) return false;
        if (!Objects.equals(idTokenObject, other.idTokenObject)) return false;
        if (!Objects.equals(accountCacheEntity, other.accountCacheEntity)) return false;
        if (!Objects.equals(account, other.account)) return false;
        if (!Objects.equals(tenantProfile, other.tenantProfile)) return false;
        if (!Objects.equals(environment, other.environment)) return false;
        if (!Objects.equals(expiresOnDate, other.expiresOnDate)) return false;
        if (!Objects.equals(scopes, other.scopes)) return false;
        return Objects.equals(metadata, other.metadata);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = result * 59 + (int) (this.expiresOn >>> 32 ^ this.expiresOn);
        result = result * 59 + (int) (this.extExpiresOn >>> 32 ^ this.extExpiresOn);
        result = result * 59 + (this.refreshOn == null ? 43 : this.refreshOn.hashCode());
        result = result * 59 + (this.isPopAuthorization == null ? 43 : this.isPopAuthorization.hashCode());
        result = result * 59 + (this.accessToken == null ? 43 : this.accessToken.hashCode());
        result = result * 59 + (this.refreshToken == null ? 43 : this.refreshToken.hashCode());
        result = result * 59 + (this.familyId == null ? 43 : this.familyId.hashCode());
        result = result * 59 + (this.idToken == null ? 43 : this.idToken.hashCode());
        result = result * 59 + (this.idTokenObject == null ? 43 : this.idTokenObject.hashCode());
        result = result * 59 + (this.accountCacheEntity == null ? 43 : this.accountCacheEntity.hashCode());
        result = result * 59 + (this.account == null ? 43 : this.account.hashCode());
        result = result * 59 + (this.tenantProfile == null ? 43 : this.tenantProfile.hashCode());
        result = result * 59 + (this.environment == null ? 43 : this.environment.hashCode());
        result = result * 59 + (this.expiresOnDate == null ? 43 : this.expiresOnDate.hashCode());
        result = result * 59 + (this.scopes == null ? 43 : this.scopes.hashCode());
        result = result * 59 + (this.metadata == null ? 43 : this.metadata.hashCode());
        return result;
    }
}
