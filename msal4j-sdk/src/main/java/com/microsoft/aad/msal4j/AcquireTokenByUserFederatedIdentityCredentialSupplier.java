// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AcquireTokenByUserFederatedIdentityCredentialSupplier extends AuthenticationResultSupplier {

    private static final Logger LOG = LoggerFactory.getLogger(AcquireTokenByUserFederatedIdentityCredentialSupplier.class);
    private UserFederatedIdentityCredentialRequest userFicRequest;

    AcquireTokenByUserFederatedIdentityCredentialSupplier(ConfidentialClientApplication clientApplication,
                                                          UserFederatedIdentityCredentialRequest userFicRequest) {
        super(clientApplication, userFicRequest);
        this.userFicRequest = userFicRequest;
    }

    @Override
    AuthenticationResult execute() throws Exception {
        if (!userFicRequest.parameters.forceRefresh()) {
            LOG.debug("ForceRefresh is false. Attempting cache lookup");
            try {
                SilentParameters parameters = SilentParameters
                        .builder(this.userFicRequest.parameters.scopes())
                        .claims(this.userFicRequest.parameters.claims())
                        .tenant(this.userFicRequest.parameters.tenant())
                        .build();

                RequestContext context = new RequestContext(
                        this.clientApplication,
                        PublicApi.ACQUIRE_TOKEN_SILENTLY,
                        parameters);

                SilentRequest silentRequest = new SilentRequest(
                        parameters,
                        this.clientApplication,
                        context,
                        null);

                AcquireTokenSilentSupplier supplier = new AcquireTokenSilentSupplier(
                        this.clientApplication,
                        silentRequest);

                return supplier.execute();
            } catch (MsalClientException ex) {
                LOG.debug("Cache lookup failed: {}", ex.getMessage());
                return acquireTokenByUserFic();
            }
        }

        LOG.debug("ForceRefresh is true. Skipping cache lookup");
        return acquireTokenByUserFic();
    }

    private AuthenticationResult acquireTokenByUserFic() throws Exception {
        AcquireTokenByAuthorizationGrantSupplier supplier = new AcquireTokenByAuthorizationGrantSupplier(
                this.clientApplication,
                userFicRequest,
                null);

        return supplier.execute();
    }
}
