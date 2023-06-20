// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class AcquireTokenByManagedIdentitySupplier extends AuthenticationResultSupplier {

    private static final Logger LOG = LoggerFactory.getLogger(AcquireTokenByManagedIdentitySupplier.class);

    private ManagedIdentityParameters managedIdentityParameters;

    AcquireTokenByManagedIdentitySupplier(ManagedIdentityApplication managedIdentityApplication, MsalRequest msalRequest) {
        super(managedIdentityApplication, msalRequest);
        this.managedIdentityParameters = (ManagedIdentityParameters) msalRequest.requestContext().apiParameters();
    }

    @Override
    AuthenticationResult execute() throws Exception {

        if (StringHelper.isNullOrBlank(managedIdentityParameters.resource)) {
            throw new MsalClientException(
                    MsalError.SCOPES_REQUIRED,
                    MsalErrorMessage.SCOPES_REQUIRED);
        }

        TokenRequestExecutor tokenRequestExecutor = new TokenRequestExecutor(
                clientApplication.authenticationAuthority,
                msalRequest,
                clientApplication.getServiceBundle()
        );

        if (!managedIdentityParameters.forceRefresh) {
            LOG.debug("ForceRefresh set to false. Attempting cache lookup");

            try {
                Set<String> scopes = new HashSet<>();
                scopes.add(this.managedIdentityParameters.resource);
                SilentParameters parameters = SilentParameters
                        .builder(scopes)
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
                return fetchNewAccessTokenAndSaveToCache(tokenRequestExecutor, clientApplication.authenticationAuthority.host);
            }
        }

        LOG.info("Skipped looking for an Access Token in the cache because forceRefresh or Claims were set. ");
        return fetchNewAccessTokenAndSaveToCache(tokenRequestExecutor, clientApplication.authenticationAuthority.host);
    }

    private AuthenticationResult fetchNewAccessTokenAndSaveToCache(TokenRequestExecutor tokenRequestExecutor, String host) throws Exception {

        ManagedIdentityClient managedIdentityClient = new ManagedIdentityClient(msalRequest, tokenRequestExecutor.getServiceBundle());

        ManagedIdentityResponse managedIdentityResponse = managedIdentityClient
                .getManagedIdentityResponse(managedIdentityParameters);

        AuthenticationResult authenticationResult = createFromManagedIdentityResponse(managedIdentityResponse);
        clientApplication.tokenCache.saveTokens(tokenRequestExecutor, authenticationResult, clientApplication.authenticationAuthority.host);
        return authenticationResult;
    }

    private AuthenticationResult createFromManagedIdentityResponse(ManagedIdentityResponse managedIdentityResponse) {
        long expiresOn = Long.valueOf(managedIdentityResponse.expiresOn);
        long refreshOn = expiresOn > 2 * 3600 ? (expiresOn / 2) : 0L;

        return AuthenticationResult.builder()
                .accessToken(managedIdentityResponse.getAccessToken())
                .scopes(managedIdentityParameters.getResource())
                .expiresOn(expiresOn)
                .extExpiresOn(0)
                .refreshOn(refreshOn)
                .build();
    }
}
