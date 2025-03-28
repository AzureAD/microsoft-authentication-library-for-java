// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class OAuthAuthorizationGrant extends AbstractMsalAuthorizationGrant {

    private final Map<String, List<String>> params = new LinkedHashMap<>();

    /**
     * Constructor to create an OAuthAuthorizationGrant
     *
     * @param params parameters relevant for the specific authorization grant type
     * @param scopes additional scopes which will be added to a default set of common scopes
     * @param claims optional claims
     */
    OAuthAuthorizationGrant(Map<String, List<String>> params, Set<String> scopes, ClaimsRequest claims) {
        this.scopes = new HashSet<>(AbstractMsalAuthorizationGrant.COMMON_SCOPES);

        if (scopes != null) {
            this.scopes.addAll(scopes);
        }

        this.params.put(SCOPE_PARAM_NAME, Collections.singletonList(String.join(" ", this.scopes)));

        if (params != null) {
            this.params.putAll(params);
        }

        if (claims != null) {
            this.claims = claims;
            this.params.put("claims", Collections.singletonList(claims.formatAsJSONString()));
        }
    }

    /**
     * Returns an unmodifiable version of the parameters map, and adds the client_info parameter
     */
    @Override
    public Map<String, List<String>> toParameters() {
        final Map<String, List<String>> outParams = new LinkedHashMap<>(params);

        outParams.put("client_info", Collections.singletonList("1"));

        return Collections.unmodifiableMap(outParams);
    }
}
