// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ImdsV2Client} JSON parsing helpers.
 *
 * <p>The IMDS HTTP calls themselves cannot be exercised in unit tests without a live Azure VM,
 * so these tests focus on the package-private parsing methods that contain all the logic.
 * Mirrors msal-go's JSON parsing validation in {@code imdsv2_test.go}.</p>
 */
class ImdsV2ClientTest {

    // ─── extractString ────────────────────────────────────────────────────────

    @Test
    void extractString_returnsStringValue() {
        String json = "{\"clientId\":\"my-client-id\",\"tenantId\":\"my-tenant\"}";
        assertEquals("my-client-id", ImdsV2Client.extractString(json, "clientId"));
        assertEquals("my-tenant",    ImdsV2Client.extractString(json, "tenantId"));
    }

    @Test
    void extractString_missingKey_returnsNull() {
        String json = "{\"foo\":\"bar\"}";
        assertNull(ImdsV2Client.extractString(json, "missing"));
    }

    @Test
    void extractString_emptyString_returnsEmpty() {
        String json = "{\"key\":\"\"}";
        assertEquals("", ImdsV2Client.extractString(json, "key"));
    }

    @Test
    void extractString_handlesQuoteEscape() {
        String json = "{\"key\":\"val\\\"ue\"}";
        assertEquals("val\"ue", ImdsV2Client.extractString(json, "key"));
    }

    @Test
    void extractString_handlesNewlineEscape() {
        String json = "{\"key\":\"line1\\nline2\"}";
        assertEquals("line1\nline2", ImdsV2Client.extractString(json, "key"));
    }

    @Test
    void extractString_handlesBackslashEscape() {
        String json = "{\"key\":\"path\\\\to\\\\file\"}";
        assertEquals("path\\to\\file", ImdsV2Client.extractString(json, "key"));
    }

    @Test
    void extractString_nonStringValue_returnsNull() {
        // Numeric and boolean values have no leading quote → returns null
        String json = "{\"count\":42,\"flag\":true}";
        assertNull(ImdsV2Client.extractString(json, "count"));
        assertNull(ImdsV2Client.extractString(json, "flag"));
    }

    @Test
    void extractString_nestedObject_findsTopLevelKey() {
        // extractString finds the first matching key by name (IMDS JSON is flat at top level)
        String json = "{\"outer\":{\"inner\":\"nested\"},\"clientId\":\"top-level\"}";
        assertEquals("top-level", ImdsV2Client.extractString(json, "clientId"));
    }

    @Test
    void extractString_urlValue_preservesSlashes() {
        String json = "{\"attestationEndpoint\":\"https://sharedeus.eus.attest.azure.net\"}";
        assertEquals("https://sharedeus.eus.attest.azure.net",
                ImdsV2Client.extractString(json, "attestationEndpoint"));
    }

    // ─── PlatformMetadata.cuIdString ──────────────────────────────────────────

    @Test
    void cuIdString_vmIdPresent_returnsVmId() {
        ImdsV2Client.PlatformMetadata m = new ImdsV2Client.PlatformMetadata(
                "client-id", "tenant-id", "vm-123", "vmss-456", "https://attest.example.com");
        assertEquals("vm-123", m.cuIdString(),
                "cuIdString must return vmId when present (matches msal-go logic)");
    }

    @Test
    void cuIdString_vmIdNull_returnsClientId() {
        ImdsV2Client.PlatformMetadata m = new ImdsV2Client.PlatformMetadata(
                "client-id", "tenant-id", null, null, null);
        assertEquals("client-id", m.cuIdString(),
                "cuIdString must fall back to clientId when vmId is null");
    }

    @Test
    void cuIdString_vmIdEmpty_returnsClientId() {
        ImdsV2Client.PlatformMetadata m = new ImdsV2Client.PlatformMetadata(
                "client-id", "tenant-id", "", null, null);
        assertEquals("client-id", m.cuIdString(),
                "cuIdString must fall back to clientId when vmId is empty");
    }

    @Test
    void platformMetadata_fieldsStoredCorrectly() {
        ImdsV2Client.PlatformMetadata m = new ImdsV2Client.PlatformMetadata(
                "c1", "t1", "v1", "vs1", "https://attest");
        assertEquals("c1",            m.clientId);
        assertEquals("t1",            m.tenantId);
        assertEquals("v1",            m.vmId);
        assertEquals("vs1",           m.vmssId);
        assertEquals("https://attest", m.attestationEndpoint);
    }

    // ─── CredentialResponse ────────────────────────────────────────────────────

    @Test
    void credentialResponse_fieldsStoredCorrectly() {
        ImdsV2Client.CredentialResponse resp = new ImdsV2Client.CredentialResponse(
                "base64cert",
                "https://eastus.mtlsauth.microsoft.com",
                "client-id",
                "tenant-id",
                "https://eastus.mtlsauth.microsoft.com/tenant-id/oauth2/v2.0/token");
        assertEquals("base64cert",                  resp.certificate);
        assertEquals("https://eastus.mtlsauth.microsoft.com", resp.mtlsAuthenticationEndpoint);
        assertEquals("client-id",                   resp.clientId);
        assertEquals("tenant-id",                   resp.tenantId);
        assertEquals("https://eastus.mtlsauth.microsoft.com/tenant-id/oauth2/v2.0/token",
                resp.regionalTokenUrl);
    }

    @Test
    void credentialResponse_nullableFields_accepted() {
        // mtlsAuthenticationEndpoint and regionalTokenUrl may be null on some IMDS configs
        ImdsV2Client.CredentialResponse resp = new ImdsV2Client.CredentialResponse(
                "cert", null, "c", "t", null);
        assertEquals("cert", resp.certificate);
        assertNull(resp.mtlsAuthenticationEndpoint);
        assertNull(resp.regionalTokenUrl);
    }
}
