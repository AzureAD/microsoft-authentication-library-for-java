// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClaimsTest {

    @Test
    void testClaimsRequest_Format() {

        List<String> values = new ArrayList<>();
        values.add("urn:mace:incommon:iap:silver");
        values.add("urn:mace:incommon:iap:bronze");

        ClaimsRequest cr = new ClaimsRequest();
        cr.requestClaimInAccessToken("given_name", new RequestedClaimAdditionalInfo(true, null, null));
        cr.requestClaimInAccessToken("email", null);
        cr.requestClaimInIdToken("acr", new RequestedClaimAdditionalInfo(false, null, values));
        cr.requestClaimInIdToken("sub", new RequestedClaimAdditionalInfo(true, "248289761001", null));
        cr.requestClaimInIdToken("auth_time", new RequestedClaimAdditionalInfo(false, null, null));

        assertEquals(TestConfiguration.CLAIMS_REQUEST, cr.formatAsJSONString());
    }

    @Test
    void testClaimsRequest_MergeWithClientCapabilitiesAndClaimsChallenge() throws URISyntaxException {

        ClaimsRequest cr = new ClaimsRequest();
        cr.requestClaimInAccessToken("nbf", new RequestedClaimAdditionalInfo(true, "1701477303", null));

        Set<String> capabilities = new HashSet<>();
        capabilities.add("cp1");

        PublicClientApplication pca = PublicClientApplication.builder(
                "client_id").
                clientCapabilities(capabilities).
                build();

        InteractiveRequestParameters parameters = InteractiveRequestParameters.builder(new URI("http://localhost:8080"))
                .claimsChallenge(TestConfiguration.CLAIMS_CHALLENGE)
                .claims(cr)
                .scopes(Collections.singleton(""))
                .build();

        String clientCapabilities = pca.clientCapabilities();
        String claimsChallenge = parameters.claimsChallenge();
        String claimsRequest = parameters.claims().formatAsJSONString();
        String mergedClaimsAndCapabilities = JsonHelper.mergeJSONString(claimsRequest, clientCapabilities);
        String mergedClaimsAndChallenge = JsonHelper.mergeJSONString(claimsChallenge, claimsRequest);
        String mergedAll = JsonHelper.mergeJSONString(claimsChallenge, mergedClaimsAndCapabilities);

        assertEquals(TestConfiguration.CLIENT_CAPABILITIES, clientCapabilities);
        assertEquals(TestConfiguration.CLAIMS_CHALLENGE, claimsChallenge);
        assertEquals(TestConfiguration.MERGED_CLAIMS_AND_CAPABILITIES, mergedClaimsAndCapabilities);
        assertEquals(TestConfiguration.MERGED_CLAIMS_AND_CHALLENGE, mergedClaimsAndChallenge);
        assertEquals(TestConfiguration.MERGED_CLAIMS_CAPABILITIES_AND_CHALLENGE, mergedAll);
    }

    @Test
    void testClaimsRequest_StringToClaimsRequest() {
        ClaimsRequest cr = ClaimsRequest.formatAsClaimsRequest(TestConfiguration.CLAIMS_CHALLENGE);

        assertEquals(TestConfiguration.CLAIMS_CHALLENGE, cr.formatAsJSONString());
    }
}
