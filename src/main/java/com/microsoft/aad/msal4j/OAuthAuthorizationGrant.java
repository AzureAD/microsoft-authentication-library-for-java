// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.AuthorizationGrant;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class OAuthAuthorizationGrant extends AbstractMsalAuthorizationGrant {

    private AuthorizationGrant grant;
    private final Map<String, List<String>> params = new LinkedHashMap<>();

    OAuthAuthorizationGrant(final AuthorizationGrant grant, Set<String> scopesSet, ClaimsRequest claims) {
        this(grant, scopesSet != null ? String.join(" ", scopesSet) : null, claims);
    }

    String addCommonScopes(String scopes){
        String allScopes = COMMON_SCOPES_PARAM;

        if (!StringHelper.isBlank(scopes)) {
            allScopes = allScopes + SCOPES_DELIMITER + scopes;
        }
        return allScopes;
    }

    OAuthAuthorizationGrant(final AuthorizationGrant grant, String scopes, ClaimsRequest claims) {
        this.grant = grant;

        String allScopes = addCommonScopes(scopes);
        this.scopes = allScopes;
        params.put(SCOPE_PARAM_NAME, Collections.singletonList(allScopes));

        if (claims != null) {
            this.claims = claims;
            params.put("claims", Collections.singletonList(claims.formatAsJSONString()));
        }
    }

    OAuthAuthorizationGrant(AuthorizationGrant grant, String scopes, Map<String, List<String>> extraParams) {
        this.grant = grant;

        String allScopes = addCommonScopes(scopes);
        this.scopes = allScopes;
        this.params.put(SCOPE_PARAM_NAME, Collections.singletonList(allScopes));

        if(extraParams != null){
            this.params.putAll(extraParams);
        }
    }

    OAuthAuthorizationGrant(AuthorizationGrant grant, Map<String, List<String>> params) {
        this.grant = grant;

        if(params != null){
            this.params.putAll(params);
        }
    }

    @Override
    public Map<String, List<String>> toParameters() {
        final Map<String, List<String>> outParams = new LinkedHashMap<>();
        outParams.putAll(params);
        outParams.put("client_info", Collections.singletonList("1"));
        outParams.putAll(grant.toParameters());
        if (claims != null) {
            outParams.put("claims", Collections.singletonList(claims.formatAsJSONString()));
        }

        return Collections.unmodifiableMap(outParams);
    }
    
    AuthorizationGrant getAuthorizationGrant() {
        return this.grant;
    }

    Map<String, List<String>> getParameters() {
        return params;
    }
}
