// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AcquireTokenByClientCredentialSupplier extends AuthenticationResultSupplier {

    private static final Logger LOG = LoggerFactory.getLogger(AcquireTokenByClientCredentialSupplier.class);
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
            LOG.debug("SkipCache set to false. Attempting cache lookup");
            try {
                SilentParameters parameters = SilentParameters
                        .builder(this.clientCredentialRequest.parameters.scopes())
                        .claims(this.clientCredentialRequest.parameters.claims())
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
                LOG.debug(String.format("Cache lookup failed: %s", ex.getMessage()));
                return acquireTokenByClientCredential();
            }
        }

        LOG.debug("SkipCache set to true. Skipping cache lookup and attempting client credentials request");
        return acquireTokenByClientCredential();
    }

    private AuthenticationResult acquireTokenByClientCredential() throws Exception {

        if (this.clientCredentialRequest.appTokenProvider != null) {

            String claims = "";
            if (null != clientCredentialRequest.parameters.claims()) {
                claims = clientCredentialRequest.parameters.claims().toString();
            }

            AppTokenProviderParameters appTokenProviderParameters = new AppTokenProviderParameters(
                    clientCredentialRequest.parameters.scopes(),
                    clientCredentialRequest.requestContext().correlationId(),
                    claims,
                    clientCredentialRequest.parameters.tenant()
            );

            AcquireTokenByAppProviderSupplier supplier =
                    new AcquireTokenByAppProviderSupplier((AbstractClientApplicationBase) this.clientApplication,
                            clientCredentialRequest,
                            appTokenProviderParameters);

            return supplier.execute();
        }

        AcquireTokenByAuthorizationGrantSupplier supplier = new AcquireTokenByAuthorizationGrantSupplier(
                this.clientApplication,
                clientCredentialRequest,
                null);

        return supplier.execute();
    }


}
