// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
