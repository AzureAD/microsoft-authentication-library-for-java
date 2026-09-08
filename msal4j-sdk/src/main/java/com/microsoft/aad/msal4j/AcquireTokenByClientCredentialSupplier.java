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
        // For mTLS Proof-of-Possession, isolate the access token in the cache by the binding certificate's
        // KeyId (x5t#S256) in addition to the token_type dimension, so PoP tokens bound to different
        // certificates never alias. Stamped before the cache lookup so reads and writes hash identically.
        IClientCertificate resolvedBindingCertificate = null;
        if (clientCredentialRequest.parameters.mtlsProofOfPossession()) {
            resolvedBindingCertificate = MtlsClientCertificateHelper.resolveBindingCertificate(
                    (ConfidentialClientApplication) this.clientApplication, clientCredentialRequest.parameters);
            clientCredentialRequest.parameters.bindingCertificateKeyId(
                    MtlsClientCertificateHelper.computeCertificateKeyId(resolvedBindingCertificate));
        }

        if (clientCredentialRequest.parameters.skipCache() != null &&
                !clientCredentialRequest.parameters.skipCache()) {
            LOG.debug("SkipCache set to false. Attempting cache lookup");
            try {
                SilentParameters parameters = SilentParameters
                        .builder(this.clientCredentialRequest.parameters.scopes())
                        .claims(this.clientCredentialRequest.parameters.claims())
                        .tenant(this.clientCredentialRequest.parameters.tenant())
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

                // Propagate ext_cache_key_hash for cache isolation (e.g., fmi_path, credential_fmi_path)
                String extCacheKeyHash = this.clientCredentialRequest.parameters.computeExtCacheKeyHash();
                if (!StringHelper.isBlank(extCacheKeyHash)) {
                    silentRequest.extCacheKeyHash(extCacheKeyHash);
                }

                AcquireTokenSilentSupplier supplier = new AcquireTokenSilentSupplier(
                        this.clientApplication,
                        silentRequest);

                AuthenticationResult cachedResult = supplier.execute();
                restoreMtlsProofOfPossessionMetadata(cachedResult, resolvedBindingCertificate);
                return cachedResult;
            } catch (MsalClientException ex) {
                LOG.debug("Cache lookup failed: {}", ex.getMessage());
                return acquireTokenByClientCredential();
            }
        }

        LOG.debug("SkipCache set to true. Skipping cache lookup and attempting client credentials request");
        return acquireTokenByClientCredential();
    }

    /**
     * Restores the mTLS Proof-of-Possession metadata (token type and binding certificate) on a result
     * served from the cache. The network path stamps these in {@link TokenRequestExecutor}, but the
     * silent/cache read path does not, so without this a cached PoP token would report {@code BEARER}
     * and a null binding certificate.
     *
     * <p>The cache is isolated by {@code token_type} and the certificate KeyId (x5t#S256), so a cache
     * hit for an mTLS PoP request is guaranteed to be an {@code mtls_pop} token bound to this exact
     * certificate; re-stamping it makes a cached PoP token report the same {@code tokenType()} and
     * {@code bindingCertificate()} as a freshly issued one. Mirrors MSAL.NET, which persists/restores
     * the token type from the cache item and re-runs the mTLS operation (re-stamping the binding
     * certificate) on cache hits.
     */
    private void restoreMtlsProofOfPossessionMetadata(AuthenticationResult result,
                                                      IClientCertificate bindingCertificate) {
        if (result == null || bindingCertificate == null) {
            return;
        }
        result.metadata().tokenType(TokenType.MTLS_POP);
        result.metadata().bindingCertificate(
                MtlsClientCertificateHelper.buildBindingCertificate(bindingCertificate));
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
