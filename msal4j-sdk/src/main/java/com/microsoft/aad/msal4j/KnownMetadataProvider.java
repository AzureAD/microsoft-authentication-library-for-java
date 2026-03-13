// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.*;

/**
 * Provides hardcoded instance discovery metadata for well-known cloud environments.
 * This allows correct alias resolution and cache behavior even when the network
 * instance discovery endpoint is unreachable.
 *
 * Mirrors the KnownMetadataProvider in MSAL .NET.
 */
class KnownMetadataProvider {

    private static final Map<String, InstanceDiscoveryMetadataEntry> KNOWN_ENTRIES;

    static {
        Map<String, InstanceDiscoveryMetadataEntry> entries = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        addEntry(entries,
                "login.microsoftonline.com", "login.windows.net",
                "login.microsoftonline.com", "login.windows.net", "login.microsoft.com", "sts.windows.net");

        addEntry(entries,
                "login.partner.microsoftonline.cn", "login.partner.microsoftonline.cn",
                "login.partner.microsoftonline.cn", "login.chinacloudapi.cn");

        addEntry(entries,
                "login.microsoftonline.de", "login.microsoftonline.de",
                "login.microsoftonline.de");

        addEntry(entries,
                "login.microsoftonline.us", "login.microsoftonline.us",
                "login.microsoftonline.us", "login.usgovcloudapi.net");

        addEntry(entries,
                "login-us.microsoftonline.com", "login-us.microsoftonline.com",
                "login-us.microsoftonline.com");

        addEntry(entries,
                "login.sovcloud-identity.fr", "login.sovcloud-identity.fr",
                "login.sovcloud-identity.fr");

        addEntry(entries,
                "login.sovcloud-identity.de", "login.sovcloud-identity.de",
                "login.sovcloud-identity.de");

        addEntry(entries,
                "login.sovcloud-identity.sg", "login.sovcloud-identity.sg",
                "login.sovcloud-identity.sg");

        KNOWN_ENTRIES = Collections.unmodifiableMap(entries);
    }

    private static void addEntry(Map<String, InstanceDiscoveryMetadataEntry> entries,
                                 String preferredNetwork,
                                 String preferredCache,
                                 String... aliases) {
        Set<String> aliasSet = new LinkedHashSet<>(Arrays.asList(aliases));
        InstanceDiscoveryMetadataEntry entry = new InstanceDiscoveryMetadataEntry(preferredNetwork, preferredCache, aliasSet);
        for (String alias : aliases) {
            entries.put(alias, entry);
        }
    }

    /**
     * Returns the known metadata entry for the given host, or null if unknown.
     */
    static InstanceDiscoveryMetadataEntry getMetadataEntry(String host) {
        return KNOWN_ENTRIES.get(host);
    }

    /**
     * Returns true if the host is a well-known cloud environment.
     */
    static boolean isKnownEnvironment(String host) {
        return KNOWN_ENTRIES.containsKey(host);
    }
}
