// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

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
        prepareMtlsProofOfPossession();

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
                String extCacheKeyHash = this.clientCredentialRequest.acquisitionCacheKeyHash();
                if (StringHelper.isBlank(extCacheKeyHash)) {
                    extCacheKeyHash = this.clientCredentialRequest.parameters.computeExtCacheKeyHash();
                }
                if (!StringHelper.isBlank(extCacheKeyHash)) {
                    silentRequest.extCacheKeyHash(extCacheKeyHash);
                }

                AcquireTokenSilentSupplier supplier = new AcquireTokenSilentSupplier(
                        this.clientApplication,
                        silentRequest);

                AuthenticationResult result = supplier.execute();
                return reattachMtlsBinding(result);
            } catch (MsalClientException ex) {
                LOG.debug("Cache lookup failed: {}", ex.getMessage());
                return acquireTokenByClientCredential();
            }
        }

        LOG.debug("SkipCache set to true. Skipping cache lookup and attempting client credentials request");
        return acquireTokenByClientCredential();
    }

    private void prepareMtlsProofOfPossession() throws MalformedURLException {
                ClientCredentialParameters parameters = clientCredentialRequest.parameters;
                if (!parameters.mtlsProofOfPossession()) {
                    return;
                }
                if (clientCredentialRequest.appTokenProvider != null) {
                    throw new MsalClientException(
                            "mTLS Proof-of-Possession cannot be used with appTokenProvider.",
                            AuthenticationErrorCode.MTLS_POP_ERROR);
                }

                Set<String> protocolOwned = new HashSet<>(Arrays.asList(
                        "token_type", "client_assertion", "client_assertion_type", "req_cnf"));
                if (parameters.extraQueryParameters() != null) {
                    for (String name : parameters.extraQueryParameters().keySet()) {
                        if (protocolOwned.contains(name.toLowerCase(Locale.ROOT))) {
                            throw new MsalClientException(
                                    "extraQueryParameters cannot override protocol-owned parameter '" + name + "'.",
                                    AuthenticationErrorCode.MTLS_POP_ERROR);
                        }
                    }
                }

                ConfidentialClientApplication application = (ConfidentialClientApplication) clientApplication;
                Authority authority = application.authenticationAuthority;
                if (parameters.tenant() != null) {
                    authority = Authority.replaceTenant(authority, parameters.tenant());
                }
                if (authority.authorityType() != AuthorityType.AAD) {
                    throw new MsalClientException(
                            "mTLS Proof-of-Possession requires a Microsoft Entra AAD authority.",
                            AuthenticationErrorCode.MTLS_POP_ERROR);
                }
                MtlsEndpointHelper.deriveMtlsTokenEndpoint(authority.tokenEndpointUrl());

                IClientCredential credential = parameters.clientCredential() != null
                        ? parameters.clientCredential() : application.clientCredential;
                if (!(credential instanceof IClientCertificate)) {
                    throw new MsalClientException(
                            "mTLS Proof-of-Possession requires a certificate credential.",
                            AuthenticationErrorCode.MTLS_POP_ERROR);
                }
                if (!(application.httpClient() instanceof IMtlsCapableHttpClient)) {
                    throw new MsalClientException(
                            "The configured custom HTTP client does not support request-specific mTLS. "
                                    + "Implement IMtlsCapableHttpClient and honor the request TLS and redirect settings.",
                            AuthenticationErrorCode.MTLS_POP_ERROR);
                }
                if (application.httpClient() instanceof DefaultHttpClient
                        && application.sslSocketFactory() != null) {
                    throw new MsalClientException(
                            "mTLS Proof-of-Possession cannot safely compose the configured application "
                                    + "SSLSocketFactory with the binding certificate. Use an "
                                    + "IMtlsCapableHttpClient that combines HttpRequest.sslContext() with "
                                    + "the application's trust policy.",
                            AuthenticationErrorCode.MTLS_POP_ERROR);
                }

                MtlsBindingContext bindingContext = MtlsBindingContext.create((IClientCertificate) credential);
                clientCredentialRequest.mtlsBindingContext(bindingContext);
                SortedMap<String, String> components = new TreeMap<>();
                components.put("token_type", TokenType.MTLS_POP.value());
                components.put("cert_der_hash", bindingContext.keyId());
                String baseHash = parameters.computeExtCacheKeyHash();
                if (!StringHelper.isBlank(baseHash)) {
                    components.put("parameters", baseHash);
                }
                clientCredentialRequest.acquisitionCacheKeyHash(
                        StringHelper.computeExtCacheKeyHash(components));
            }

    private AuthenticationResult reattachMtlsBinding(AuthenticationResult result) {
                MtlsBindingContext context = clientCredentialRequest.mtlsBindingContext();
                return context == null ? result : result.withMtlsBindingContext(context);
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
