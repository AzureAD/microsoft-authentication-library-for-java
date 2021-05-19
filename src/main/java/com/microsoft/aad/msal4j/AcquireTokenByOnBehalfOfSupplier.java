// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AcquireTokenByOnBehalfOfSupplier extends AuthenticationResultSupplier {

    private final static Logger LOG = LoggerFactory.getLogger(AcquireTokenByOnBehalfOfSupplier.class);
    private OnBehalfOfRequest onBehalfOfRequest;

    AcquireTokenByOnBehalfOfSupplier(ConfidentialClientApplication clientApplication,
                                     OnBehalfOfRequest onBehalfOfRequest) {
        super(clientApplication, onBehalfOfRequest);
        this.onBehalfOfRequest = onBehalfOfRequest;
    }

    @Override
    AuthenticationResult execute() throws Exception {
        if (onBehalfOfRequest.parameters.skipCache() != null &&
                !onBehalfOfRequest.parameters.skipCache()) {
            LOG.debug("SkipCache set to false. Attempting cache lookup");
            try {
                SilentParameters parameters = SilentParameters
                        .builder(this.onBehalfOfRequest.parameters.scopes())
                        .claims(this.onBehalfOfRequest.parameters.claims())
                        .build();

                SilentRequest silentRequest = new SilentRequest(
                        parameters,
                        this.clientApplication,
                        this.clientApplication.createRequestContext(PublicApi.ACQUIRE_TOKEN_SILENTLY, parameters),
                        onBehalfOfRequest.parameters.userAssertion());

                AcquireTokenSilentSupplier supplier = new AcquireTokenSilentSupplier(
                        this.clientApplication,
                        silentRequest);

                return supplier.execute();
            } catch (MsalClientException ex) {
                LOG.debug(String.format("Cache lookup failed: %s", ex.getMessage()));
                return acquireTokenOnBehalfOf();
            }
        }

        LOG.debug("SkipCache set to true. Skipping cache lookup and attempting on-behalf-of request");
        return acquireTokenOnBehalfOf();
    }

    private AuthenticationResult acquireTokenOnBehalfOf() throws Exception {
        AcquireTokenByAuthorizationGrantSupplier supplier = new AcquireTokenByAuthorizationGrantSupplier(
                this.clientApplication,
                onBehalfOfRequest,
                null);

        return supplier.execute();
    }
}
