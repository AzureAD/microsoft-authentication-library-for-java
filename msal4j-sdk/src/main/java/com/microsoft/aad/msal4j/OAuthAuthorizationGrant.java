// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class OAuthAuthorizationGrant extends AbstractMsalAuthorizationGrant {

    private final Map<String, List<String>> params = new LinkedHashMap<>();

    OAuthAuthorizationGrant(Map<String, List<String>> params, Set<String> scopesSet, ClaimsRequest claims) {
        this(params, scopesSet != null ? String.join(" ", scopesSet) : null, claims);
    }

    OAuthAuthorizationGrant(Map<String, List<String>> params, String scopes, ClaimsRequest claims) {
        this(params);

        this.scopes = addCommonScopes(scopes);
        this.params.put(SCOPE_PARAM_NAME, Collections.singletonList(this.scopes));

        if (claims != null) {
            this.claims = claims;
            this.params.put("claims", Collections.singletonList(claims.formatAsJSONString()));
        }
    }

    OAuthAuthorizationGrant(Map<String, List<String>> params) {
        if (params != null) {
            this.params.putAll(params);
        }
    }

    @Override
    public Map<String, List<String>> toParameters() {
        final Map<String, List<String>> outParams = new LinkedHashMap<>(params);

        outParams.put("client_info", Collections.singletonList("1"));

        if (claims != null) {
            outParams.put("claims", Collections.singletonList(claims.formatAsJSONString()));
        }

        return Collections.unmodifiableMap(outParams);
    }

    Map<String, List<String>> getParameters() {
        return params;
    }

    String addCommonScopes(String scopes) {
        Set<String> allScopes = new HashSet<>(
                Arrays.asList(COMMON_SCOPES_PARAM.split(SCOPES_DELIMITER)));

        if (!StringHelper.isBlank(scopes)) {
            allScopes.addAll(Arrays.asList(scopes.split(SCOPES_DELIMITER)));
        }
        return String.join(SCOPES_DELIMITER, allScopes);
    }
}
