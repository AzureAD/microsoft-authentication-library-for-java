// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

class AcquireTokenByManagedIdentitySupplier extends AuthenticationResultSupplier {

    private static final Logger LOG = LoggerFactory.getLogger(AcquireTokenByManagedIdentitySupplier.class);

    private static final int TWO_HOURS = 2 * 3600;

    private ManagedIdentityParameters managedIdentityParameters;

    AcquireTokenByManagedIdentitySupplier(ManagedIdentityApplication managedIdentityApplication, MsalRequest msalRequest) {
        super(managedIdentityApplication, msalRequest);
        this.managedIdentityParameters = (ManagedIdentityParameters) msalRequest.requestContext().apiParameters();
    }

    @Override
    AuthenticationResult execute() throws Exception {

        if (StringHelper.isNullOrBlank(managedIdentityParameters.resource)) {
            throw new MsalClientException(
                    MsalError.RESOURCE_REQUIRED_MANAGED_IDENTITY,
                    MsalErrorMessage.SCOPES_REQUIRED);
        }

        TokenRequestExecutor tokenRequestExecutor = new TokenRequestExecutor(
                clientApplication.authenticationAuthority,
                msalRequest,
                clientApplication.serviceBundle()
        );

        CacheRefreshReason cacheRefreshReason = CacheRefreshReason.NOT_APPLICABLE;

        if (managedIdentityParameters.forceRefresh) {
            LOG.debug("ForceRefresh set to true. Skipping cache lookup and attempting to acquire new token");
            return fetchNewAccessTokenAndSaveToCache(tokenRequestExecutor, CacheRefreshReason.FORCE_REFRESH);
        }


        LOG.debug("ForceRefresh set to false. Attempting cache lookup");
        try {
            Set<String> scopes = new HashSet<>();
            scopes.add(this.managedIdentityParameters.resource);
            SilentParameters parameters = SilentParameters
                    .builder(scopes)
                    .tenant(managedIdentityParameters.tenant())
                    .claims(managedIdentityParameters.claims())
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

            AuthenticationResult result = supplier.execute();
            cacheRefreshReason = SilentRequestHelper.getCacheRefreshReasonIfApplicable(
                    parameters,
                    result,
                    LOG);

            // If the token does not need a refresh, return the cached token
            // Else refresh the token if it is either expired, proactively refreshable, or if the claims are passed.
            if (cacheRefreshReason == CacheRefreshReason.NOT_APPLICABLE) {
                LOG.debug("Returning token from cache");
                result.metadata().tokenSource(TokenSource.CACHE);
                return result;
            } else {
                LOG.debug(String.format("Refreshing access token. Cache refresh reason: %s", cacheRefreshReason));
                return fetchNewAccessTokenAndSaveToCache(tokenRequestExecutor, cacheRefreshReason);
            }
        } catch (MsalClientException ex) {
            if (ex.errorCode().equals(AuthenticationErrorCode.CACHE_MISS)) {
                LOG.debug(String.format("Cache lookup failed: %s", ex.getMessage()));
                return fetchNewAccessTokenAndSaveToCache(tokenRequestExecutor, cacheRefreshReason);
            } else {
                LOG.error(String.format("Error occurred while cache lookup: %s", ex.getMessage()));
                throw ex;
            }
        }
    }

    private AuthenticationResult fetchNewAccessTokenAndSaveToCache(TokenRequestExecutor tokenRequestExecutor, CacheRefreshReason cacheRefreshReason) throws Exception {

        ManagedIdentityClient managedIdentityClient = new ManagedIdentityClient(msalRequest, tokenRequestExecutor.getServiceBundle());

        LOG.debug(String.format("[Managed Identity] Managed Identity source and ID type identified and set successfully, request will use Managed Identity for %s",
                managedIdentityClient.managedIdentitySource.managedIdentitySourceType.name()));

        ManagedIdentityResponse managedIdentityResponse = managedIdentityClient
                .getManagedIdentityResponse(managedIdentityParameters);

        AuthenticationResult authenticationResult = createFromManagedIdentityResponse(managedIdentityResponse);
        clientApplication.tokenCache.saveTokens(tokenRequestExecutor, authenticationResult, clientApplication.authenticationAuthority.host);
        AuthenticationResult result = authenticationResult;
        result.metadata().tokenSource(TokenSource.IDENTITY_PROVIDER);
        result.metadata().cacheRefreshReason(cacheRefreshReason);
        return result;
    }

    private AuthenticationResult createFromManagedIdentityResponse(ManagedIdentityResponse managedIdentityResponse) {
        long expiresOn = Long.parseLong(managedIdentityResponse.expiresOn);
        long refreshOn = calculateRefreshOn(expiresOn);
        AuthenticationResultMetadata metadata = AuthenticationResultMetadata.builder()
                .tokenSource(TokenSource.IDENTITY_PROVIDER)
                .refreshOn(refreshOn)
                .build();

        return AuthenticationResult.builder()
                .accessToken(managedIdentityResponse.getAccessToken())
                .scopes(managedIdentityParameters.resource())
                .expiresOn(expiresOn)
                .extExpiresOn(0)
                .refreshOn(refreshOn)
                .metadata(metadata)
                .build();
    }

    private long calculateRefreshOn(long expiresOn) {
        long timestampSeconds = System.currentTimeMillis() / 1000;
        long expiresIn = expiresOn - timestampSeconds;

        //The refreshOn value should be half the value of the token lifetime, if the lifetime is greater than two hours
        return expiresIn > TWO_HOURS ? (expiresIn / 2) + timestampSeconds : 0;
    }
}
