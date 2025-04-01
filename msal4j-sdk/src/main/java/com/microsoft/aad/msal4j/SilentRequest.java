// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;

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
                    CacheRefreshReason.FORCE_REFRESH);
        }
    }

    SilentParameters parameters() {
        return this.parameters;
    }

    IUserAssertion assertion() {
        return this.assertion;
    }

    Authority requestAuthority() {
        return this.requestAuthority;
    }
}
