// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AcquireTokenByClientCredentialSupplier extends AuthenticationResultSupplier {

    private final static Logger LOG = LoggerFactory.getLogger(AcquireTokenByClientCredentialSupplier.class);
    private ClientCredentialRequest clientCredentialRequest;

    AcquireTokenByClientCredentialSupplier(ConfidentialClientApplication clientApplication,
                                           ClientCredentialRequest clientCredentialRequest) {
        super(clientApplication, clientCredentialRequest);
        this.clientCredentialRequest = clientCredentialRequest;
    }

    @Override
    AuthenticationResult execute() throws Exception {
        if (clientCredentialRequest.parameters.skipCache() != null &&
                !clientCredentialRequest.parameters.skipCache()) {
            LOG.info("SkipCache set to false. Attempting cache lookup");
            try {
                SilentParameters parameters = SilentParameters
                        .builder(this.clientCredentialRequest.parameters.scopes())
                        .claims(this.clientCredentialRequest.parameters.claims())
                        .build();

                SilentRequest silentRequest = new SilentRequest(
                        parameters,
                        this.clientApplication,
                        this.clientApplication.createRequestContext(PublicApi.ACQUIRE_TOKEN_SILENTLY, parameters));

                AcquireTokenSilentSupplier supplier = new AcquireTokenSilentSupplier(
                        this.clientApplication,
                        silentRequest);

                return supplier.execute();
            } catch (MsalClientException ex) {
                LOG.debug(String.format("Cache lookup failed: %s", ex.getMessage()));
                return acquireTokenByClientCredential();
            }
        }

        LOG.info("SkipCache set to true. Skipping cache lookup and attempting client credentials request");
        return acquireTokenByClientCredential();
    }

    private AuthenticationResult acquireTokenByClientCredential() throws Exception {
        AcquireTokenByAuthorizationGrantSupplier supplier = new AcquireTokenByAuthorizationGrantSupplier(
                this.clientApplication,
                clientCredentialRequest,
                null);

        return supplier.execute();
    }
}
