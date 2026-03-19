// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KnownMetadataProviderTest {

    @Test
    void isKnownEnvironment_allKnownHosts_returnsTrue() {
        String[] knownHosts = {
                "login.microsoftonline.com", "login.windows.net", "login.microsoft.com", "sts.windows.net",
                "login.partner.microsoftonline.cn", "login.chinacloudapi.cn",
                "login.microsoftonline.de",
                "login.microsoftonline.us", "login.usgovcloudapi.net",
                "login-us.microsoftonline.com",
                "login.sovcloud-identity.fr", "login.sovcloud-identity.de", "login.sovcloud-identity.sg"
        };
        for (String host : knownHosts) {
            assertTrue(KnownMetadataProvider.isKnownEnvironment(host),
                    "Expected " + host + " to be a known environment");
        }
    }

    @Test
    void isKnownEnvironment_unknownHost_returnsFalse() {
        assertFalse(KnownMetadataProvider.isKnownEnvironment("custom.authority.example.com"));
    }

    @Test
    void getMetadataEntry_publicCloud_returnsCorrectAliases() {
        // Arrange / Act
        InstanceDiscoveryMetadataEntry entry = KnownMetadataProvider.getMetadataEntry("login.microsoftonline.com");

        // Assert
        assertNotNull(entry);
        assertEquals("login.microsoftonline.com", entry.preferredNetwork());
        assertEquals("login.windows.net", entry.preferredCache());
        assertEquals(4, entry.aliases().size());
        assertTrue(entry.aliases().contains("login.microsoftonline.com"));
        assertTrue(entry.aliases().contains("login.windows.net"));
        assertTrue(entry.aliases().contains("login.microsoft.com"));
        assertTrue(entry.aliases().contains("sts.windows.net"));
    }

    @Test
    void getMetadataEntry_publicCloudAliases_resolvesToSameEntry() {
        // All public cloud aliases should resolve to the same object
        InstanceDiscoveryMetadataEntry entry1 = KnownMetadataProvider.getMetadataEntry("login.microsoftonline.com");
        InstanceDiscoveryMetadataEntry entry2 = KnownMetadataProvider.getMetadataEntry("login.windows.net");
        InstanceDiscoveryMetadataEntry entry3 = KnownMetadataProvider.getMetadataEntry("login.microsoft.com");
        InstanceDiscoveryMetadataEntry entry4 = KnownMetadataProvider.getMetadataEntry("sts.windows.net");

        // Assert
        assertSame(entry1, entry2);
        assertSame(entry2, entry3);
        assertSame(entry3, entry4);
    }

    @Test
    void getMetadataEntry_unknownHost_returnsNull() {
        assertNull(KnownMetadataProvider.getMetadataEntry("custom.authority.example.com"));
    }

    @Test
    void getMetadataEntry_caseInsensitive() {
        // Assert — lookups should be case-insensitive
        assertNotNull(KnownMetadataProvider.getMetadataEntry("LOGIN.MICROSOFTONLINE.COM"));
        assertNotNull(KnownMetadataProvider.getMetadataEntry("Login.Partner.Microsoftonline.Cn"));
        assertNotNull(KnownMetadataProvider.getMetadataEntry("LOGIN.SOVCLOUD-IDENTITY.FR"));
    }

    @Test
    void cacheInstanceDiscoveryMetadata_knownHost_cachesAllAliases() {
        // Arrange
        AadInstanceDiscoveryProvider.cache.clear();

        // Act
        AadInstanceDiscoveryProvider.cacheInstanceDiscoveryMetadata("login.microsoftonline.com");

        // Assert — all public cloud aliases should be cached
        assertNotNull(AadInstanceDiscoveryProvider.cache.get("login.microsoftonline.com"));
        assertNotNull(AadInstanceDiscoveryProvider.cache.get("login.windows.net"));
        assertNotNull(AadInstanceDiscoveryProvider.cache.get("login.microsoft.com"));
        assertNotNull(AadInstanceDiscoveryProvider.cache.get("sts.windows.net"));

        // All should reference the same entry
        InstanceDiscoveryMetadataEntry entry = AadInstanceDiscoveryProvider.cache.get("login.microsoftonline.com");
        assertSame(entry, AadInstanceDiscoveryProvider.cache.get("login.windows.net"));
        assertSame(entry, AadInstanceDiscoveryProvider.cache.get("login.microsoft.com"));
        assertSame(entry, AadInstanceDiscoveryProvider.cache.get("sts.windows.net"));
    }

    @Test
    void cacheInstanceDiscoveryMetadata_unknownHost_cachesSelfEntry() {
        // Arrange
        AadInstanceDiscoveryProvider.cache.clear();

        // Act
        AadInstanceDiscoveryProvider.cacheInstanceDiscoveryMetadata("custom.unknown.example.com");

        // Assert — should get a self-referencing entry
        InstanceDiscoveryMetadataEntry entry = AadInstanceDiscoveryProvider.cache.get("custom.unknown.example.com");
        assertNotNull(entry);
        assertEquals("custom.unknown.example.com", entry.preferredNetwork());
        assertEquals("custom.unknown.example.com", entry.preferredCache());
        assertEquals(1, entry.aliases().size());
        assertTrue(entry.aliases().contains("custom.unknown.example.com"));
    }
}
