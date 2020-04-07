// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;

import static com.microsoft.aad.msal4j.Constants.POINT_DELIMITER;

@Accessors(fluent = true)
@Getter
class SilentRequest extends MsalRequest {

    private SilentParameters parameters;

    private Authority requestAuthority;

    SilentRequest(SilentParameters parameters,
                  AbstractClientApplicationBase application,
                  RequestContext requestContext) throws MalformedURLException {

        super(application, null, requestContext);

        this.parameters = parameters;
        this.requestAuthority = StringHelper.isBlank(parameters.authorityUrl()) ?
                application.authenticationAuthority :
                Authority.createAuthority(new URL(parameters.authorityUrl()));

        application.getServiceBundle().getServerSideTelemetry().getCurrentRequest().forceRefresh(
                parameters.forceRefresh());
    }

    String getFullThumbprint(){
        StringBuilder sb = new StringBuilder();
        sb.append(application().clientId() + POINT_DELIMITER);
        sb.append(application().authority() + POINT_DELIMITER);

        Set<String> sortedScopes = new TreeSet<>(parameters.scopes());
        sb.append(String.join(" ", sortedScopes) + POINT_DELIMITER);

        sb.append(parameters.account().homeAccountId() + POINT_DELIMITER);
        sb.append(parameters.authorityUrl());

        return StringHelper.createSha256Hash(sb.toString());
    }
}
