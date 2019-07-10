// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.SAML2BearerGrant;

class SAML11BearerGrant extends SAML2BearerGrant {

    /**
     * The grant type.
     */
    public static GrantType grantType = new GrantType(
            "urn:ietf:params:oauth:grant-type:saml1_1-bearer");

    public SAML11BearerGrant(Base64URL assertion) {
        super(assertion);
    }

    @Override
    public Map<String, List<String>> toParameters() {

        Map<String, List<String>> params = super.toParameters();
        params.put("grant_type",Collections.singletonList(grantType.getValue()));
        return params;
    }
}
