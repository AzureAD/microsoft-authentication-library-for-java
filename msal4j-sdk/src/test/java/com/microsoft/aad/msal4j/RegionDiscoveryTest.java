// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegionDiscoveryTest {

    @Test
    void parseRegionFromImdsResponse_validLocation_returnsRegion() {
        String body = "{\"location\":\"centralus\",\"vmId\":\"11111111-1111-1111-1111-111111111111\"}";

        assertEquals("centralus", AadInstanceDiscoveryProvider.parseRegionFromImdsResponse(body));
    }

    @Test
    void parseRegionFromImdsResponse_locationWithExtraNestedFields_returnsRegion() {
        //The real /compute payload contains many extra fields, including nested objects and arrays
        String body = "{\"azEnvironment\":\"AzurePublicCloud\",\"location\":\"westus2\"," +
                "\"tagsList\":[{\"name\":\"t\",\"value\":\"v\"}],\"storageProfile\":{\"osDisk\":{\"diskSizeGB\":\"30\"}}}";

        assertEquals("westus2", AadInstanceDiscoveryProvider.parseRegionFromImdsResponse(body));
    }

    @Test
    void parseRegionFromImdsResponse_missingLocation_returnsNull() {
        String body = "{\"vmId\":\"11111111-1111-1111-1111-111111111111\"}";

        assertNull(AadInstanceDiscoveryProvider.parseRegionFromImdsResponse(body));
    }

    @Test
    void parseRegionFromImdsResponse_nullLocation_returnsNull() {
        String body = "{\"location\":null}";

        assertNull(AadInstanceDiscoveryProvider.parseRegionFromImdsResponse(body));
    }

    @Test
    void parseRegionFromImdsResponse_malformedJson_returnsNull() {
        String body = "{ this is not valid json";

        assertNull(AadInstanceDiscoveryProvider.parseRegionFromImdsResponse(body));
    }

    @Test
    void parseRegionFromImdsResponse_emptyBody_returnsNull() {
        assertNull(AadInstanceDiscoveryProvider.parseRegionFromImdsResponse(""));
        assertNull(AadInstanceDiscoveryProvider.parseRegionFromImdsResponse(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "eastus", "westus2", "east-us-2", "centralus", "a", "a1", "a-1",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void isValidRegion_validRegionNames_returnsTrue(String region) {
        assertTrue(AadInstanceDiscoveryProvider.isValidRegion(region));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "east.us",      //dot would add a subdomain to the authority host
            "east/us",      //slash would add a path segment to the authority URL
            "../evil",      //path traversal attempt
            "EastUS",       //uppercase characters
            "east us",      //whitespace
            "1eastus",      //does not start with a letter
            "-eastus",      //does not start with a letter
            "eastus-",      //DNS labels cannot end with a hyphen
            "east_us",      //underscore
            "east$us",      //other special characters
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}) //longer than a DNS label
    void isValidRegion_invalidRegionNames_returnsFalse(String region) {
        assertFalse(AadInstanceDiscoveryProvider.isValidRegion(region));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void isValidRegion_nullOrEmpty_returnsFalse(String region) {
        assertFalse(AadInstanceDiscoveryProvider.isValidRegion(region));
    }

    @ParameterizedTest
    @ValueSource(strings = {"West US", "West US 2"})
    void isValidRegion_displayName_returnsFalse(String region) {
        //Regions must be given as their short name ("westus"), not their display name ("West US")
        assertFalse(AadInstanceDiscoveryProvider.isValidRegion(region));
    }
}
