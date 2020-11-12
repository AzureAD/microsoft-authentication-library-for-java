// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class ClaimsTest {

    @Test
    public void testClaimsRequest_Format() {

        List<String> values = new ArrayList<>();
        values.add("urn:mace:incommon:iap:silver");
        values.add("urn:mace:incommon:iap:bronze");

        ClaimsRequest cr = new ClaimsRequest();
        cr.requestClaimInUserInfo("given_name", new RequestedClaimAdditionalInfo(true, null, null));
        cr.requestClaimInUserInfo("email", null);
        cr.requestClaimInIdToken("acr", new RequestedClaimAdditionalInfo(false, null, values));
        cr.requestClaimInIdToken("sub", new RequestedClaimAdditionalInfo(true, "248289761001", null));

        Assert.assertEquals(cr.formatAsJSONString(), TestConfiguration.CLAIMS_REQUEST);
    }

    @Test
    public void testClaimsRequest_MergeWithClientCapabilities() {

        List<String> values = new ArrayList<>();
        values.add("abc");
        ClaimsRequest cr = new ClaimsRequest();
        cr.requestClaimInUserInfo("given_name", new RequestedClaimAdditionalInfo(true, null, null));
        cr.requestClaimInUserInfo("email", null);
        cr.requestClaimInAccessToken("xms_cc", new RequestedClaimAdditionalInfo(false, null, values));

        PublicClientApplication pca = PublicClientApplication.builder(
                "client_id").
                clientCapabilities(new HashSet<>(Collections.singletonList("llt"))).
                build();

        String clientCapabilities = pca.clientCapabilities();
        String mergedClaimsAndCapabilities = JsonHelper.mergeJSONString(pca.clientCapabilities(), cr.formatAsJSONString());

        Assert.assertEquals(clientCapabilities, TestConfiguration.CLIENT_CAPABILITIES);
        Assert.assertEquals(mergedClaimsAndCapabilities, TestConfiguration.MERGED_CLAIMS_AND_CAPABILITIES);

    }
}
