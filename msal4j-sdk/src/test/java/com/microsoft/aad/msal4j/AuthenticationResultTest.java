// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Date;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationResultTest {

    // Shared test constants
    private static final String ACCESS_TOKEN = "test-access-token";
    private static final long EXPIRES_ON = 1735689600L;
    private static final long EXT_EXPIRES_ON = 1735693200L;
    private static final String REFRESH_TOKEN = "test-refresh-token";
    private static final Long REFRESH_ON = 1735686000L;
    private static final String FAMILY_ID = "1";
    private static final String ENVIRONMENT = "login.microsoftonline.com";
    private static final String SCOPES = "user.read openid";

    // Metadata is shared across equals/hashCode tests because AuthenticationResultMetadata
    // does not implement equals(), so two separately-constructed instances will fail
    // reference equality even if all fields match.
    private static final AuthenticationResultMetadata SHARED_METADATA =
            AuthenticationResultMetadata.builder()
                    .tokenSource(TokenSource.IDENTITY_PROVIDER)
                    .refreshOn(REFRESH_ON)
                    .cacheRefreshReason(CacheRefreshReason.NOT_APPLICABLE)
                    .build();

    private static AccountCacheEntity buildAccountCacheEntity() {
        AccountCacheEntity entity = new AccountCacheEntity();
        entity.homeAccountId = "uid.utid";
        entity.environment = ENVIRONMENT;
        entity.realm = "test-tenant";
        entity.localAccountId = "local-oid";
        entity.username = "user@example.com";
        entity.authorityType = AccountCacheEntity.MSSTS_ACCOUNT_TYPE;
        return entity;
    }

    /**
     * Builds a result with no idToken (and thus no idTokenObject or tenantProfile), which
     * is needed for equals/hashCode tests because IdToken and TenantProfile do not implement
     * equals(). This ensures equals() comparisons are based on value equality of all fields.
     */
    private static AuthenticationResult buildBaselineResult() {
        return AuthenticationResult.builder()
                .accessToken(ACCESS_TOKEN)
                .expiresOn(EXPIRES_ON)
                .extExpiresOn(EXT_EXPIRES_ON)
                .refreshToken(REFRESH_TOKEN)
                .refreshOn(REFRESH_ON)
                .familyId(FAMILY_ID)
                .accountCacheEntity(buildAccountCacheEntity())
                .environment(ENVIRONMENT)
                .scopes(SCOPES)
                .metadata(SHARED_METADATA)
                .isPopAuthorization(false)
                .build();
    }

    // ========== Builder and Getter Tests ==========

    @Test
    void build_AllFieldsSet_GettersReturnExpectedValues() {
        AccountCacheEntity accountEntity = buildAccountCacheEntity();
        AuthenticationResultMetadata metadata = AuthenticationResultMetadata.builder()
                .tokenSource(TokenSource.CACHE)
                .refreshOn(REFRESH_ON)
                .cacheRefreshReason(CacheRefreshReason.PROACTIVE_REFRESH)
                .build();

        AuthenticationResult result = AuthenticationResult.builder()
                .accessToken(ACCESS_TOKEN)
                .expiresOn(EXPIRES_ON)
                .extExpiresOn(EXT_EXPIRES_ON)
                .refreshToken(REFRESH_TOKEN)
                .refreshOn(REFRESH_ON)
                .familyId(FAMILY_ID)
                .idToken(TestHelper.ENCODED_JWT)
                .accountCacheEntity(accountEntity)
                .environment(ENVIRONMENT)
                .scopes(SCOPES)
                .metadata(metadata)
                .isPopAuthorization(true)
                .build();

        assertEquals(ACCESS_TOKEN, result.accessToken());
        assertEquals(EXPIRES_ON, result.expiresOn());
        assertEquals(EXT_EXPIRES_ON, result.extExpiresOn());
        assertEquals(REFRESH_TOKEN, result.refreshToken());
        assertEquals(REFRESH_ON, result.refreshOn());
        assertEquals(FAMILY_ID, result.familyId());
        assertEquals(TestHelper.ENCODED_JWT, result.idToken());
        assertEquals(accountEntity, result.accountCacheEntity());
        assertEquals(ENVIRONMENT, result.environment());
        assertEquals(SCOPES, result.scopes());
        assertSame(metadata, result.metadata());
        assertTrue(result.isPopAuthorization());
    }

    @Test
    void build_MinimalFields_NullableFieldsReturnNull() {
        AuthenticationResult result = AuthenticationResult.builder()
                .expiresOn(EXPIRES_ON)
                .build();

        assertNull(result.accessToken());
        assertEquals(EXPIRES_ON, result.expiresOn());
        assertEquals(0, result.extExpiresOn());
        assertNull(result.refreshToken());
        assertNull(result.refreshOn());
        assertNull(result.familyId());
        assertNull(result.idToken());
        assertNull(result.accountCacheEntity());
        assertNull(result.environment());
        assertNull(result.scopes());
        assertNull(result.isPopAuthorization());
        assertNull(result.account());
    }

    @Test
    void build_NullMetadata_DefaultsToEmptyMetadata() {
        AuthenticationResult result = AuthenticationResult.builder()
                .metadata(null)
                .build();

        assertNotNull(result.metadata());
        assertNull(result.metadata().tokenSource());
        assertEquals(CacheRefreshReason.NOT_APPLICABLE, result.metadata().cacheRefreshReason());
    }

    // ========== Derived Field Tests ==========

    @Test
    void build_WithIdToken_IdTokenObjectIsNull_DueToInitOrder() {
        // Documents a known issue: field initializers run before the constructor body,
        // so idTokenObject = getIdTokenObj() executes when this.idToken is still null.
        // This means idTokenObject() always returns null regardless of the idToken value.
        AuthenticationResult result = AuthenticationResult.builder()
                .idToken(TestHelper.ENCODED_JWT)
                .build();

        assertNull(result.idTokenObject(),
                "idTokenObject is always null due to field initialization order");
        // The idToken string itself is stored correctly
        assertEquals(TestHelper.ENCODED_JWT, result.idToken());
    }

    @Test
    void build_WithBlankIdToken_IdTokenObjectIsNull() {
        AuthenticationResult result = AuthenticationResult.builder()
                .idToken("")
                .build();

        assertNull(result.idTokenObject());
    }

    @Test
    void build_WithAccountCacheEntity_PublicGetterRecomputesAccount() {
        // The public account() getter calls getAccount() each time, so it works correctly
        // despite the field initialization order bug (the private 'account' field is always null).
        AccountCacheEntity entity = buildAccountCacheEntity();

        AuthenticationResult result = AuthenticationResult.builder()
                .accountCacheEntity(entity)
                .build();

        IAccount account = result.account();
        assertNotNull(account);
        assertEquals("uid.utid", account.homeAccountId());
        assertEquals(ENVIRONMENT, account.environment());
        assertEquals("user@example.com", account.username());
    }

    @Test
    void build_WithNullAccountCacheEntity_AccountIsNull() {
        AuthenticationResult result = AuthenticationResult.builder()
                .accountCacheEntity(null)
                .build();

        assertNull(result.account());
    }

    @Test
    void build_WithIdTokenAndAccount_TenantProfileIsNull_DueToInitOrder() {
        // Documents same initialization order issue: tenantProfile = getTenantProfile()
        // runs before this.idToken is set, so StringHelper.isBlank(idToken) returns true
        // and tenantProfile is always null.
        AccountCacheEntity entity = buildAccountCacheEntity();

        AuthenticationResult result = AuthenticationResult.builder()
                .idToken(TestHelper.ENCODED_JWT)
                .accountCacheEntity(entity)
                .build();

        assertNull(result.tenantProfile(),
                "tenantProfile is always null due to field initialization order");
    }

    @Test
    void build_ExpiresOn_ExpiresOnDateConvertedCorrectly() {
        AuthenticationResult result = AuthenticationResult.builder()
                .expiresOn(EXPIRES_ON)
                .build();

        Date expectedDate = new Date(EXPIRES_ON * 1000);
        assertEquals(expectedDate, result.expiresOnDate());
    }

    @Test
    void build_WithIdTokenButNullAccount_DoesNotThrowNPE_DueToInitOrder() {
        // If the field initialization order bug were fixed, this scenario would throw NPE:
        // getTenantProfile() calls getAccount().environment(), and getAccount() returns null
        // when accountCacheEntity is null. However, since idToken is null at init time,
        // getTenantProfile() returns null before reaching the getAccount() call.
        AuthenticationResult result = AuthenticationResult.builder()
                .idToken(TestHelper.ENCODED_JWT)
                .accountCacheEntity(null)
                .build();

        assertNull(result.tenantProfile());
        assertEquals(TestHelper.ENCODED_JWT, result.idToken());
    }

    // ========== equals() Tests ==========

    @Test
    void equals_SameInstance_ReturnsTrue() {
        AuthenticationResult result = buildBaselineResult();
        assertEquals(result, result);
    }

    @Test
    void equals_WrongType_ReturnsFalse() {
        AuthenticationResult result = buildBaselineResult();
        assertFalse(result.equals("not an AuthenticationResult"));
    }

    @Test
    void equals_Null_ReturnsFalse() {
        AuthenticationResult result = buildBaselineResult();
        assertFalse(result.equals(null));
    }

    @Test
    void equals_IdenticalFields_NoIdToken_ReturnsTrue() {
        // Two independently-constructed results with identical fields and shared metadata
        // should be equal when idToken is blank (avoiding IdToken/TenantProfile reference
        // equality issues). This is the only scenario where equals() works for separately
        // constructed instances, because AuthenticationResultMetadata also lacks equals().
        AuthenticationResult result1 = buildBaselineResult();
        AuthenticationResult result2 = buildBaselineResult();
        assertEquals(result1, result2);
    }

    /**
     * Parameterized test that verifies each field in equals() by varying one field at a time
     * from the baseline. Uses blank idToken and shared metadata to ensure reference equality
     * issues in IdToken/TenantProfile/Metadata don't interfere.
     *
     * Each argument set contains: field name (for display), and a result with that field changed.
     */
    @ParameterizedTest(name = "equals returns false when {0} differs")
    @MethodSource("differentFieldProvider")
    void equals_SingleFieldDiffers_ReturnsFalse(String fieldName, AuthenticationResult modified) {
        AuthenticationResult baseline = buildBaselineResult();
        assertNotEquals(baseline, modified, "Results should differ when " + fieldName + " differs");
    }

    private static Stream<Arguments> differentFieldProvider() {
        AccountCacheEntity differentAccount = buildAccountCacheEntity();
        differentAccount.homeAccountId = "different.account";

        return Stream.of(
                Arguments.of("accessToken", buildWithOverride().accessToken("different-token").build()),
                Arguments.of("expiresOn", buildWithOverride().expiresOn(999L).build()),
                Arguments.of("extExpiresOn", buildWithOverride().extExpiresOn(999L).build()),
                Arguments.of("refreshToken", buildWithOverride().refreshToken("different-rt").build()),
                Arguments.of("refreshOn", buildWithOverride().refreshOn(999L).build()),
                Arguments.of("familyId", buildWithOverride().familyId("2").build()),
                Arguments.of("environment", buildWithOverride().environment("different.env").build()),
                Arguments.of("scopes", buildWithOverride().scopes("different.scope").build()),
                Arguments.of("isPopAuthorization", buildWithOverride().isPopAuthorization(true).build()),
                Arguments.of("accountCacheEntity", buildWithOverride().accountCacheEntity(differentAccount).build())
        );
    }

    /**
     * Returns a builder pre-populated with baseline values, ready for one field to be overridden.
     */
    private static AuthenticationResult.AuthenticationResultBuilder buildWithOverride() {
        return AuthenticationResult.builder()
                .accessToken(ACCESS_TOKEN)
                .expiresOn(EXPIRES_ON)
                .extExpiresOn(EXT_EXPIRES_ON)
                .refreshToken(REFRESH_TOKEN)
                .refreshOn(REFRESH_ON)
                .familyId(FAMILY_ID)
                .accountCacheEntity(buildAccountCacheEntity())
                .environment(ENVIRONMENT)
                .scopes(SCOPES)
                .metadata(SHARED_METADATA)
                .isPopAuthorization(false);
    }

    @Test
    void equals_IdTokenSetOnBoth_ReturnsTrue_BecauseIdTokenObjectAlwaysNull() {
        // Despite IdToken lacking equals(), this comparison passes because the field
        // initialization order bug means idTokenObject is always null for both results.
        // The idToken string comparison (line 238) passes, and the idTokenObject comparison
        // (line 239) passes because both are null.
        AccountCacheEntity entity = buildAccountCacheEntity();
        AuthenticationResult result1 = AuthenticationResult.builder()
                .idToken(TestHelper.ENCODED_JWT)
                .accountCacheEntity(entity)
                .metadata(SHARED_METADATA)
                .build();
        AuthenticationResult result2 = AuthenticationResult.builder()
                .idToken(TestHelper.ENCODED_JWT)
                .accountCacheEntity(entity)
                .metadata(SHARED_METADATA)
                .build();

        assertEquals(result1, result2,
                "Results with same idToken are equal because idTokenObject is always null");
    }

    @Test
    void equals_DifferentMetadataInstances_ReturnsFalse() {
        // Documents that AuthenticationResultMetadata also lacks equals(), so two results
        // with equivalent but separately-constructed metadata instances will not be equal.
        AuthenticationResultMetadata meta1 = AuthenticationResultMetadata.builder()
                .tokenSource(TokenSource.CACHE).build();
        AuthenticationResultMetadata meta2 = AuthenticationResultMetadata.builder()
                .tokenSource(TokenSource.CACHE).build();

        AuthenticationResult result1 = AuthenticationResult.builder()
                .expiresOn(EXPIRES_ON)
                .metadata(meta1)
                .build();
        AuthenticationResult result2 = AuthenticationResult.builder()
                .expiresOn(EXPIRES_ON)
                .metadata(meta2)
                .build();

        assertNotEquals(result1, result2,
                "Two results with equivalent metadata instances are not equal because AuthenticationResultMetadata lacks equals()");
    }

    @Test
    void equals_NullVsNonNullRefreshOn_ReturnsFalse() {
        AuthenticationResult withRefreshOn = buildBaselineResult();
        AuthenticationResult withoutRefreshOn = buildWithOverride().refreshOn(null).build();

        assertNotEquals(withRefreshOn, withoutRefreshOn);
    }

    // ========== hashCode() Tests ==========

    @Test
    void hashCode_EqualObjects_ReturnSameHash() {
        AuthenticationResult result1 = buildBaselineResult();
        AuthenticationResult result2 = buildBaselineResult();

        // Verify the contract: equal objects must have equal hash codes
        assertEquals(result1, result2, "Precondition: results must be equal");
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void hashCode_AllNullableFieldsNull_DoesNotThrow() {
        AuthenticationResult result = AuthenticationResult.builder().build();

        // Should not throw NPE — all null branches use the constant 43
        int hash = result.hashCode();
        assertNotEquals(0, hash, "Hash should be computed even with all-null fields");
    }

    @Test
    void hashCode_AllFieldsSet_DoesNotThrow() {
        AuthenticationResult result = AuthenticationResult.builder()
                .accessToken(ACCESS_TOKEN)
                .expiresOn(EXPIRES_ON)
                .extExpiresOn(EXT_EXPIRES_ON)
                .refreshToken(REFRESH_TOKEN)
                .refreshOn(REFRESH_ON)
                .familyId(FAMILY_ID)
                .idToken(TestHelper.ENCODED_JWT)
                .accountCacheEntity(buildAccountCacheEntity())
                .environment(ENVIRONMENT)
                .scopes(SCOPES)
                .metadata(SHARED_METADATA)
                .isPopAuthorization(true)
                .build();

        int hash = result.hashCode();
        assertNotEquals(0, hash);
    }

    // ========== AuthenticationResultMetadata Tests ==========

    @Test
    void metadata_BuilderDefaults_CacheRefreshReasonNotApplicable() {
        AuthenticationResultMetadata metadata = AuthenticationResultMetadata.builder().build();

        assertNull(metadata.tokenSource());
        assertNull(metadata.refreshOn());
        // CacheRefreshReason defaults to NOT_APPLICABLE in the constructor
        assertEquals(CacheRefreshReason.NOT_APPLICABLE, metadata.cacheRefreshReason());
    }

    @Test
    void metadata_AllFieldsSet_GettersReturnExpectedValues() {
        AuthenticationResultMetadata metadata = AuthenticationResultMetadata.builder()
                .tokenSource(TokenSource.CACHE)
                .refreshOn(1735686000L)
                .cacheRefreshReason(CacheRefreshReason.EXPIRED)
                .build();

        assertEquals(TokenSource.CACHE, metadata.tokenSource());
        assertEquals(1735686000L, metadata.refreshOn());
        assertEquals(CacheRefreshReason.EXPIRED, metadata.cacheRefreshReason());
    }

    @Test
    void metadata_NullCacheRefreshReason_DefaultsToNotApplicable() {
        AuthenticationResultMetadata metadata = AuthenticationResultMetadata.builder()
                .cacheRefreshReason(null)
                .build();

        assertEquals(CacheRefreshReason.NOT_APPLICABLE, metadata.cacheRefreshReason());
    }

    @Test
    void metadata_Setters_UpdateValues() {
        AuthenticationResultMetadata metadata = AuthenticationResultMetadata.builder().build();

        metadata.tokenSource(TokenSource.IDENTITY_PROVIDER);
        metadata.refreshOn(5000L);
        metadata.cacheRefreshReason(CacheRefreshReason.PROACTIVE_REFRESH);

        assertEquals(TokenSource.IDENTITY_PROVIDER, metadata.tokenSource());
        assertEquals(5000L, metadata.refreshOn());
        assertEquals(CacheRefreshReason.PROACTIVE_REFRESH, metadata.cacheRefreshReason());
    }

    @Test
    void metadata_ToString_ContainsFieldValues() {
        String toString = AuthenticationResultMetadata.builder()
                .tokenSource(TokenSource.CACHE)
                .refreshOn(100L)
                .cacheRefreshReason(CacheRefreshReason.EXPIRED)
                .toString();

        assertTrue(toString.contains("CACHE"));
        assertTrue(toString.contains("100"));
        assertTrue(toString.contains("EXPIRED"));
    }
}
