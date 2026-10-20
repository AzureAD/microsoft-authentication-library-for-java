// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthenticationResultTest {

    private static final String TENANT_ID = "6c3d51dd-f0e5-4959-b4ea-a80c4e36fe5e";

    // Unsigned ID token with a parsable payload, enough for the claim parsing under test
    private static final String ID_TOKEN = encode("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "."
            + encode("{\"tid\":\"" + TENANT_ID + "\",\"given_name\":\"John\",\"oid\":\"oid\"}") + ".signature";

    private static String encode(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static AccountCacheEntity account(String environment) {
        AccountCacheEntity account = new AccountCacheEntity();
        account.homeAccountId("uid.utid");
        account.environment(environment);
        account.username("user@contoso.com");
        return account;
    }

    @Test
    void tenantProfileAndIdTokenObjectAreReadFromTheIdToken() {
        AuthenticationResult result = AuthenticationResult.builder()
                .accessToken("access")
                .idToken(ID_TOKEN)
                .accountCacheEntity(account("login.microsoftonline.com"))
                .environment("login.microsoftonline.com")
                .build();

        ITenantProfile tenantProfile = result.tenantProfile();
        assertNotNull(tenantProfile);
        assertEquals(TENANT_ID, tenantProfile.getClaims().get("tid"));
        assertEquals("John", tenantProfile.getClaims().get("given_name"));
        assertEquals("login.microsoftonline.com", tenantProfile.environment());

        assertNotNull(result.idTokenObject());
        assertEquals(TENANT_ID, result.idTokenObject().tenantIdentifier);
        assertEquals("user@contoso.com", result.account().username());
    }

    @Test
    void tenantProfileUsesTheResultEnvironmentWithoutAnAccount() {
        AuthenticationResult result = AuthenticationResult.builder()
                .accessToken("access")
                .idToken(ID_TOKEN)
                .environment("login.microsoftonline.us")
                .build();

        assertNull(result.account());
        assertNotNull(result.tenantProfile());
        assertEquals("login.microsoftonline.us", result.tenantProfile().environment());
    }

    @Test
    void noIdTokenMeansNoTenantProfile() {
        AuthenticationResult result = AuthenticationResult.builder()
                .accessToken("access")
                .accountCacheEntity(account("login.microsoftonline.com"))
                .build();

        assertNull(result.tenantProfile());
        assertNull(result.idTokenObject());
    }
}
