// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

class OAuthAuthorizationGrant extends AbstractMsalAuthorizationGrant {

    private final Map<String, String> params = new LinkedHashMap<>();

    /**
     * Constructor to create an OAuthAuthorizationGrant
     *
     * @param params parameters relevant for the specific authorization grant type
     * @param scopes additional scopes which will be added to a default set of common scopes
     */
    OAuthAuthorizationGrant(Map<String, String> params, Set<String> scopes) {
        this.scopes = new HashSet<>(AbstractMsalAuthorizationGrant.COMMON_SCOPES);

        if (scopes != null) {
            this.scopes.addAll(scopes);
        }

        // Default scopes that apply to most flows
        this.params.put(SCOPE_PARAM_NAME, String.join(" ", this.scopes));
        // Parameter to request client info from the endpoint
        this.params.put("client_info", "1");

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
    OAuthAuthorizationGrant(Map<String, String> params, Set<String> scopes, ClaimsRequest claims) {
        this(params, scopes);

        if (claims != null) {
            this.claims = claims;
            this.params.put("claims", claims.formatAsJSONString());
        }
    }

    void addAndReplaceParams(Map<String, String> params) {
        if (params != null) {
            //putAll() will overwrite existing values if the key already exists in the map
            this.params.putAll(params);
        }
    }

    String getParamValue(String paramKey) {
        return this.params.get(paramKey);
    }

    /**
     * Returns an unmodifiable version of the parameters map
     */
    @Override
    public Map<String, String> toParameters() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }
}