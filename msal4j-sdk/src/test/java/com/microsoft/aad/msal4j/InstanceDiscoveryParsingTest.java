// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class InstanceDiscoveryParsingTest {

    // ========== AadInstanceDiscoveryResponse ==========

    @Test
    void aadInstanceDiscoveryResponse_fromJson_allFields() throws IOException {
        String json = "{"
                + "\"tenant_discovery_endpoint\":\"https://login.microsoftonline.com/tenant/.well-known/openid-configuration\","
                + "\"metadata\":[{"
                + "\"preferred_network\":\"login.microsoftonline.com\","
                + "\"preferred_cache\":\"login.windows.net\","
                + "\"aliases\":[\"login.microsoftonline.com\",\"login.windows.net\"]"
                + "}],"
                + "\"error_description\":null,"
                + "\"error_codes\":null,"
                + "\"error\":null,"
                + "\"correlation_id\":\"corr-123\""
                + "}";

        AadInstanceDiscoveryResponse response = TestHelper.parseJson(json, AadInstanceDiscoveryResponse::fromJson);

        assertEquals("https://login.microsoftonline.com/tenant/.well-known/openid-configuration",
                response.tenantDiscoveryEndpoint());
        assertNotNull(response.metadata());
        assertEquals(1, response.metadata().size());
        assertEquals("login.microsoftonline.com", response.metadata().get(0).preferredNetwork());
        assertEquals("login.windows.net", response.metadata().get(0).preferredCache());
        assertEquals("corr-123", response.correlationId());
    }

    @Test
    void aadInstanceDiscoveryResponse_fromJson_errorResponse() throws IOException {
        String json = "{"
                + "\"error\":\"invalid_instance\","
                + "\"error_description\":\"AADSTS50049: Unknown or invalid instance.\","
                + "\"error_codes\":[50049],"
                + "\"correlation_id\":\"corr-err\""
                + "}";

        AadInstanceDiscoveryResponse response = TestHelper.parseJson(json, AadInstanceDiscoveryResponse::fromJson);

        assertEquals("invalid_instance", response.error());
        assertEquals("AADSTS50049: Unknown or invalid instance.", response.errorDescription());
        assertNotNull(response.errorCodes());
        assertEquals(1, response.errorCodes().size());
        assertEquals(50049L, response.errorCodes().get(0).longValue());
        assertEquals("corr-err", response.correlationId());
        assertNull(response.tenantDiscoveryEndpoint());
        assertNull(response.metadata());
    }

    @Test
    void aadInstanceDiscoveryResponse_fromJson_multipleMetadataEntries() throws IOException {
        String json = "{"
                + "\"tenant_discovery_endpoint\":\"https://endpoint\","
                + "\"metadata\":["
                + "{\"preferred_network\":\"login.microsoftonline.com\",\"preferred_cache\":\"login.windows.net\",\"aliases\":[\"login.microsoftonline.com\"]},"
                + "{\"preferred_network\":\"login.chinacloudapi.cn\",\"preferred_cache\":\"login.chinacloudapi.cn\",\"aliases\":[\"login.chinacloudapi.cn\"]}"
                + "]"
                + "}";

        AadInstanceDiscoveryResponse response = TestHelper.parseJson(json, AadInstanceDiscoveryResponse::fromJson);

        assertEquals(2, response.metadata().size());
        assertEquals("login.microsoftonline.com", response.metadata().get(0).preferredNetwork());
        assertEquals("login.chinacloudapi.cn", response.metadata().get(1).preferredNetwork());
    }

    @Test
    void aadInstanceDiscoveryResponse_fromJson_unknownFieldsSkipped() throws IOException {
        String json = "{\"error\":\"test\",\"api-version\":\"1.1\",\"extra\":true}";

        AadInstanceDiscoveryResponse response = TestHelper.parseJson(json, AadInstanceDiscoveryResponse::fromJson);

        assertEquals("test", response.error());
    }

    @Test
    void aadInstanceDiscoveryResponse_toJson_roundTrip() throws IOException {
        String json = "{"
                + "\"tenant_discovery_endpoint\":\"https://endpoint\","
                + "\"metadata\":[{"
                + "\"preferred_network\":\"login.microsoftonline.com\","
                + "\"preferred_cache\":\"login.windows.net\","
                + "\"aliases\":[\"login.microsoftonline.com\"]"
                + "}],"
                + "\"error\":null,"
                + "\"correlation_id\":\"c-1\""
                + "}";

        AadInstanceDiscoveryResponse original = TestHelper.parseJson(json, AadInstanceDiscoveryResponse::fromJson);
        String serialized = TestHelper.writeToJson(original);

        assertTrue(serialized.contains("tenant_discovery_endpoint"));
        assertTrue(serialized.contains("login.microsoftonline.com"));
        assertTrue(serialized.contains("correlation_id"));
    }

    @Test
    void aadInstanceDiscoveryResponse_getters_defaultNull() throws IOException {
        String json = "{}";

        AadInstanceDiscoveryResponse response = TestHelper.parseJson(json, AadInstanceDiscoveryResponse::fromJson);

        assertNull(response.tenantDiscoveryEndpoint());
        assertNull(response.metadata());
        assertNull(response.errorDescription());
        assertNull(response.errorCodes());
        assertNull(response.error());
        assertNull(response.correlationId());
    }

    // ========== InstanceDiscoveryMetadataEntry ==========

    @Test
    void instanceDiscoveryMetadataEntry_fromJson_allFields() throws IOException {
        String json = "{"
                + "\"preferred_network\":\"login.microsoftonline.com\","
                + "\"preferred_cache\":\"login.windows.net\","
                + "\"aliases\":[\"login.microsoftonline.com\",\"login.windows.net\",\"login.microsoft.com\"]"
                + "}";

        InstanceDiscoveryMetadataEntry entry = TestHelper.parseJson(json, InstanceDiscoveryMetadataEntry::fromJson);

        assertEquals("login.microsoftonline.com", entry.preferredNetwork());
        assertEquals("login.windows.net", entry.preferredCache());
        assertEquals(3, entry.aliases().size());
        assertTrue(entry.aliases().contains("login.microsoftonline.com"));
        assertTrue(entry.aliases().contains("login.windows.net"));
        assertTrue(entry.aliases().contains("login.microsoft.com"));
    }

    @Test
    void instanceDiscoveryMetadataEntry_constructorAndGetters() {
        HashSet<String> aliases = new HashSet<>(Arrays.asList("host1", "host2"));
        InstanceDiscoveryMetadataEntry entry = new InstanceDiscoveryMetadataEntry(
                "preferred-net", "preferred-cache", aliases);

        assertEquals("preferred-net", entry.preferredNetwork());
        assertEquals("preferred-cache", entry.preferredCache());
        assertEquals(aliases, entry.aliases());
    }

    @Test
    void instanceDiscoveryMetadataEntry_defaultConstructor() {
        InstanceDiscoveryMetadataEntry entry = new InstanceDiscoveryMetadataEntry();

        assertNull(entry.preferredNetwork());
        assertNull(entry.preferredCache());
        assertNull(entry.aliases());
    }

    @Test
    void instanceDiscoveryMetadataEntry_toJson_roundTrip() throws IOException {
        String json = "{"
                + "\"preferred_network\":\"login.microsoftonline.com\","
                + "\"preferred_cache\":\"login.windows.net\","
                + "\"aliases\":[\"login.microsoftonline.com\"]"
                + "}";

        InstanceDiscoveryMetadataEntry original = TestHelper.parseJson(json, InstanceDiscoveryMetadataEntry::fromJson);
        String serialized = TestHelper.writeToJson(original);

        InstanceDiscoveryMetadataEntry roundTripped = TestHelper.parseJson(serialized, InstanceDiscoveryMetadataEntry::fromJson);

        assertEquals(original.preferredNetwork(), roundTripped.preferredNetwork());
        assertEquals(original.preferredCache(), roundTripped.preferredCache());
        assertEquals(original.aliases(), roundTripped.aliases());
    }

    @Test
    void instanceDiscoveryMetadataEntry_fromJson_unknownFieldsSkipped() throws IOException {
        String json = "{\"preferred_network\":\"host\",\"extra_field\":123}";

        InstanceDiscoveryMetadataEntry entry = TestHelper.parseJson(json, InstanceDiscoveryMetadataEntry::fromJson);

        assertEquals("host", entry.preferredNetwork());
    }
}
