// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.MalformedURLException;
import java.net.URL;

@Accessors(fluent = true)
@Getter
class SilentRequest extends MsalRequest {

    private SilentParameters parameters;
    private IUserAssertion assertion;
    private Authority requestAuthority;

    SilentRequest(SilentParameters parameters,
                  AbstractApplicationBase application,
                  RequestContext requestContext,
                  IUserAssertion assertion) throws MalformedURLException {

        super(application, null, requestContext);

        this.parameters = parameters;
        this.assertion = assertion;
        this.requestAuthority = StringHelper.isBlank(parameters.authorityUrl()) ?
                application.authenticationAuthority :
                Authority.createAuthority(new URL(Authority.enforceTrailingSlash(parameters.authorityUrl())));

        if (parameters.forceRefresh()) {
            application.serviceBundle().getServerSideTelemetry().getCurrentRequest().cacheInfo(
                    CacheTelemetry.REFRESH_FORCE_REFRESH.telemetryValue);
        }
    }
}
