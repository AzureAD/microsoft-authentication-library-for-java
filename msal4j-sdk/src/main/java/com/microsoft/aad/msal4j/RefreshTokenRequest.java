// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.microsoft.aad.msal4j.Constants.POINT_DELIMITER;

class RefreshTokenRequest extends MsalRequest {

    private SilentRequest parentSilentRequest;
    private RefreshTokenParameters parameters;
    private AuthenticationResult cachedResult;

    RefreshTokenRequest(RefreshTokenParameters parameters,
                        AbstractApplicationBase application,
                        RequestContext requestContext) {
        super(application, createAuthenticationGrant(parameters), requestContext);
        this.parameters = parameters;
    }

    RefreshTokenRequest(RefreshTokenParameters parameters,
                        AbstractApplicationBase application,
                        RequestContext requestContext,
                        SilentRequest silentRequest,
                        AuthenticationResult cachedResult) {
        this(parameters, application, requestContext);
        this.parentSilentRequest = silentRequest;
        this.cachedResult = cachedResult;
    }

    private static AbstractMsalAuthorizationGrant createAuthenticationGrant(RefreshTokenParameters parameters) {
        Map<String, String> params = new LinkedHashMap<>();

        params.put(GrantConstants.GRANT_TYPE_PARAMETER, GrantConstants.REFRESH_TOKEN);
        params.put("refresh_token", parameters.refreshToken());

        return new OAuthAuthorizationGrant(params, parameters.scopes(), parameters.claims());
    }

    String extCacheKeyHash() {
        return parentSilentRequest != null ? parentSilentRequest.extCacheKeyHash() : null;
    }

    AccountCacheEntity accountCacheEntity() {
        return cachedResult != null ? cachedResult.accountCacheEntity() : null;
    }

    String userAssertionHash() {
        return cachedResult != null ? cachedResult.userAssertionHash() : null;
    }

    String getFullThumbprint() {
        StringBuilder sb = new StringBuilder();

        sb.append(application().clientId() + POINT_DELIMITER);

        String authority = (parentSilentRequest != null && parentSilentRequest.requestAuthority() != null)
                ? parentSilentRequest.requestAuthority().authority() : application().authority();
        sb.append(authority + POINT_DELIMITER);

        if (parentSilentRequest != null && parentSilentRequest.parameters().account() != null) {
            sb.append(parentSilentRequest.parameters().account().homeAccountId() + POINT_DELIMITER);
        }

        sb.append(parameters.refreshToken() + POINT_DELIMITER);

        Set<String> sortedScopes = new TreeSet<>(parameters.scopes());
        sb.append(String.join(" ", sortedScopes) + POINT_DELIMITER);

        if (parameters.claims() != null) {
            sb.append(parameters.claims().formatAsJSONString() + POINT_DELIMITER);
        }

        return StringHelper.createSha256Hash(sb.toString());
    }
}
