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
    private final Map<String, List<String>> params;

    /**
     * init standard scopes
     */
    private OAuthAuthorizationGrant() {
        params = new LinkedHashMap<>();

        params.put(SCOPE_PARAM_NAME, Collections.singletonList(COMMON_SCOPES_PARAM));
    }

    OAuthAuthorizationGrant(final AuthorizationGrant grant, Set<String> scopesSet, ClaimsRequest claims) {
        this(grant, scopesSet != null ? String.join(" ", scopesSet) : null, claims);
    }

    OAuthAuthorizationGrant(final AuthorizationGrant grant, String scopes, ClaimsRequest claims) {
        this();
        this.grant = grant;


        if (!StringHelper.isBlank(scopes)) {
            this.scopes = scopes;
            params.put(SCOPE_PARAM_NAME,
                    Collections.singletonList(String.join(" ",params.get(SCOPE_PARAM_NAME)) + SCOPES_DELIMITER + scopes));
        }

        if (claims != null) {
            this.claims = claims;
            params.put("claims", Collections.singletonList(claims.formatAsJSONString()));
        }
    }

    OAuthAuthorizationGrant(final AuthorizationGrant grant,
                                final Map<String, List<String>> params) {
        this();
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

    Map<String, List<String>> getCustomParameters() {
        return params;
    }
}
