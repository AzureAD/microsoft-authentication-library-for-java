// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;

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
        String DELIMITER = ".";

        StringBuilder sb = new StringBuilder();
        sb.append(application().clientId() + DELIMITER);
        sb.append(application().authority() + DELIMITER);

        Set<String> sortedScopes = new TreeSet<>(parameters.scopes());
        sb.append(String.join(" ", sortedScopes) + DELIMITER);

        sb.append(parameters.account().homeAccountId() + DELIMITER);
        sb.append(parameters.authorityUrl());

        return StringHelper.createSha256Hash(sb.toString());
    }
}
