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
     */
    OAuthAuthorizationGrant(Map<String, List<String>> params, Set<String> scopes) {
        this.scopes = new HashSet<>(AbstractMsalAuthorizationGrant.COMMON_SCOPES);

        if (scopes != null) {
            this.scopes.addAll(scopes);
        }

        // Default scopes that apply to most flows
        this.params.put(SCOPE_PARAM_NAME, Collections.singletonList(String.join(" ", this.scopes)));
        // Parameter to request client info from the endpoint
        this.params.put("client_info", Collections.singletonList("1"));

        if (params != null) {
            this.params.putAll(params);
        }
    }

        /**
         * Constructor to create an OAuthAuthorizationGrant
         *
         * @param params parameters relevant for the specific authorization grant type
         * @param scopes additional scopes which will be added to a default set of common scopes
         * @param claims optional claims
         */
        OAuthAuthorizationGrant(Map<String, List<String>> params, Set<String> scopes, ClaimsRequest claims) {
            this(params, scopes);

            if (claims != null) {
                this.claims = claims;
                this.params.put("claims", Collections.singletonList(claims.formatAsJSONString()));
            }
        }

    /**
     * Returns an unmodifiable version of the parameters map
     */
    @Override
    public Map<String, List<String>> toParameters() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }
}
