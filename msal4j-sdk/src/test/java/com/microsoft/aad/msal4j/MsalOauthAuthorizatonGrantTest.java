// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MsalOauthAuthorizatonGrantTest {

    @Test
    void testToParameters() {
        Map<String, List<String>> params = new LinkedHashMap<>();
        params.put(GrantConstants.GRANT_TYPE_PARAMETER, Collections.singletonList("SomeGrantType"));

        final OAuthAuthorizationGrant grant = new OAuthAuthorizationGrant(params, null, null);

        assertNotNull(grant);
        assertNotNull(grant.toParameters());
        assertEquals("SomeGrantType", grant.toParameters().get(GrantConstants.GRANT_TYPE_PARAMETER).get(0));
    }
}
