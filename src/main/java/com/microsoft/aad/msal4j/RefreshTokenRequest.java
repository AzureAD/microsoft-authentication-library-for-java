// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Set;
import java.util.TreeSet;

import static com.microsoft.aad.msal4j.Constants.POINT_DELIMITER;

@Accessors(fluent = true)
@Getter
class RefreshTokenRequest extends MsalRequest {

    private SilentRequest parentSilentRequest;
    private RefreshTokenParameters parameters;

    RefreshTokenRequest(RefreshTokenParameters parameters,
                        AbstractClientApplicationBase application,
                        RequestContext requestContext) {
        super(application, createAuthenticationGrant(parameters), requestContext);
        this.parameters = parameters;
    }

    RefreshTokenRequest(RefreshTokenParameters parameters,
                        AbstractClientApplicationBase application,
                        RequestContext requestContext,
                        SilentRequest silentRequest) {
        this(parameters, application, requestContext);
        this.parentSilentRequest = silentRequest;
    }

    private static AbstractMsalAuthorizationGrant createAuthenticationGrant(
            RefreshTokenParameters parameters) {

        RefreshTokenGrant refreshTokenGrant = new RefreshTokenGrant(new RefreshToken(parameters.refreshToken()));
        return new OAuthAuthorizationGrant(refreshTokenGrant, parameters.scopes(), parameters.claims());
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

        return StringHelper.createSha256Hash(sb.toString());
    }
}
