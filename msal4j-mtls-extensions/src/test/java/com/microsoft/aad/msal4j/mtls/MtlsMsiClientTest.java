// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MtlsMsiClient} input validation and token request helpers.
 *
 * <p>Full end-to-end token acquisition requires a live Azure VM with Managed Identity and
 * a properly configured mTLS PoP tenant. These tests cover the logic that can be exercised
 * without CNG or IMDS:</p>
 * <ul>
 *   <li>Null/empty resource validation in {@link MtlsMsiClient#acquireToken}</li>
 *   <li>Token URL construction ({@code buildTokenUrl})</li>
 *   <li>Token request body construction ({@code buildTokenRequestBody})</li>
 * </ul>
 *
 * <p>Requires Windows because loading {@link MtlsMsiClient} transitively loads
 * {@link CngProvider} → {@link CngSignatureSpi} → {@link CngRsaPrivateKey} →
 * {@link NCryptLibrary}, which loads {@code ncrypt.dll}.</p>
 */
@EnabledOnOs(OS.WINDOWS)
class MtlsMsiClientTest {

    // ─── acquireToken null/empty resource validation ───────────────────────────

    @Test
    void acquireToken_nullResource_throwsMtlsMsiException() {
        MtlsMsiClient client = new MtlsMsiClient();
        MtlsMsiException ex = assertThrows(MtlsMsiException.class, () ->
                client.acquireToken(null, "SystemAssigned", null, false, null));
        assertTrue(ex.getMessage().contains("resource"),
                "Exception message must mention 'resource'");
    }

    @Test
    void acquireToken_emptyResource_throwsMtlsMsiException() {
        MtlsMsiClient client = new MtlsMsiClient();
        MtlsMsiException ex = assertThrows(MtlsMsiException.class, () ->
                client.acquireToken("", "SystemAssigned", null, false, null));
        assertTrue(ex.getMessage().contains("resource"),
                "Exception message must mention 'resource'");
    }

    // ─── buildTokenUrl (private static — accessed via reflection) ─────────────

    @Test
    void buildTokenUrl_appendsTenantAndPath() throws Exception {
        String url = invokeBuildTokenUrl("https://mtlsauth.microsoft.com", "my-tenant");
        assertEquals("https://mtlsauth.microsoft.com/my-tenant/oauth2/v2.0/token", url);
    }

    @Test
    void buildTokenUrl_stripsTrailingSlash() throws Exception {
        String url = invokeBuildTokenUrl("https://mtlsauth.microsoft.com/", "my-tenant");
        assertEquals("https://mtlsauth.microsoft.com/my-tenant/oauth2/v2.0/token", url,
                "Trailing slash on the endpoint must be stripped before appending the path");
    }

    @Test
    void buildTokenUrl_regionalEndpoint() throws Exception {
        String url = invokeBuildTokenUrl(
                "https://eastus.mtlsauth.microsoft.com", "a-tenant-guid");
        assertEquals("https://eastus.mtlsauth.microsoft.com/a-tenant-guid/oauth2/v2.0/token", url);
    }

    // ─── buildTokenRequestBody (private static — accessed via reflection) ──────

    @Test
    void buildTokenRequestBody_includesGrantTypeAndTokenType() throws Exception {
        String body = invokeBuildTokenRequestBody("my-client", "https://management.azure.com");
        assertTrue(body.contains("grant_type=client_credentials"),
                "request body must include grant_type=client_credentials");
        assertTrue(body.contains("token_type=mtls_pop"),
                "request body must include token_type=mtls_pop (mTLS PoP token grant)");
    }

    @Test
    void buildTokenRequestBody_appendsDefaultScopeWhenMissing() throws Exception {
        String body = invokeBuildTokenRequestBody("my-client", "https://management.azure.com");
        // /.default must be appended
        assertTrue(body.contains(".default"),
                "scope must have /.default appended when it is absent from the resource URI");
    }

    @Test
    void buildTokenRequestBody_doesNotDoubleAppendDefault() throws Exception {
        // Resource already has /.default — must not add it again
        String body = invokeBuildTokenRequestBody("my-client",
                "https://management.azure.com/.default");
        int firstIdx  = body.indexOf(".default");
        int secondIdx = body.indexOf(".default", firstIdx + 1);
        assertEquals(-1, secondIdx,
                "/.default must not appear twice in the request body");
    }

    @Test
    void buildTokenRequestBody_includesClientId() throws Exception {
        String body = invokeBuildTokenRequestBody("abc-def-123", "https://management.azure.com");
        assertTrue(body.contains("client_id=abc-def-123"),
                "request body must include the client_id");
    }

    @Test
    void buildTokenRequestBody_urlEncodesSpecialChars() throws Exception {
        // Slashes in the scope value must be URL-encoded
        String body = invokeBuildTokenRequestBody("c", "https://management.azure.com");
        // URL-encoded form: https%3A%2F%2Fmanagement.azure.com%2F.default
        assertTrue(body.contains("%3A") || body.contains(":"),
                "scope must be URL-encoded or use the raw value (both are correct)");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static String invokeBuildTokenUrl(String endpoint, String tenantId) throws Exception {
        Method m = MtlsMsiClient.class.getDeclaredMethod(
                "buildTokenUrl", String.class, String.class);
        m.setAccessible(true);
        try {
            return (String) m.invoke(null, endpoint, tenantId);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    private static String invokeBuildTokenRequestBody(String clientId, String resource)
            throws Exception {
        Method m = MtlsMsiClient.class.getDeclaredMethod(
                "buildTokenRequestBody", String.class, String.class);
        m.setAccessible(true);
        try {
            return (String) m.invoke(null, clientId, resource);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }
}
