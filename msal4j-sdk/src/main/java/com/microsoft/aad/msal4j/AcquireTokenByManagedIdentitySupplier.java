// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
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
            String message = MsalErrorMessage.SCOPES_REQUIRED;
            LOG.error(LogHelper.createMessage(
                    "[Managed Identity] " + message,
                    msalRequest.requestContext().correlationId()));
            throw new MsalClientException(
                    message,
                    MsalError.RESOURCE_REQUIRED_MANAGED_IDENTITY,
                    msalRequest.requestContext().correlationId());
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
                if (cacheRefreshReason == CacheRefreshReason.CLAIMS) {
                    LOG.debug("Claims are passed, creating token hash and refreshing the token");
                    managedIdentityParameters.revokedTokenHash = StringHelper.createSha256HashHexString(result.accessToken());
                    return fetchNewAccessTokenAndSaveToCache(tokenRequestExecutor, CacheRefreshReason.CLAIMS);
                }

                LOG.debug("Refreshing access token. Cache refresh reason: {}", cacheRefreshReason);
                return fetchNewAccessTokenAndSaveToCache(tokenRequestExecutor, cacheRefreshReason);
            }
        } catch (MsalClientException ex) {
            if (ex.errorCode().equals(AuthenticationErrorCode.CACHE_MISS)) {
                LOG.debug("Cache lookup failed: {}", ex.getMessage());
                return fetchNewAccessTokenAndSaveToCache(tokenRequestExecutor, cacheRefreshReason);
            } else {
                LOG.error("Error occurred while cache lookup: {}", ex.getMessage());
                throw ex;
            }
        }
    }

    private AuthenticationResult fetchNewAccessTokenAndSaveToCache(TokenRequestExecutor tokenRequestExecutor, CacheRefreshReason cacheRefreshReason) {

        ManagedIdentityClient managedIdentityClient = new ManagedIdentityClient(msalRequest, tokenRequestExecutor.getServiceBundle());

        LOG.debug("[Managed Identity] Managed Identity source and ID type identified and set successfully, request will use Managed Identity for {}",
                managedIdentityClient.managedIdentitySource.managedIdentitySourceType.name());

        ManagedIdentityResponse managedIdentityResponse = managedIdentityClient
                .getManagedIdentityResponse(managedIdentityParameters);

        AuthenticationResult authenticationResult = createFromManagedIdentityResponse(managedIdentityResponse);
        clientApplication.tokenCache.saveTokens(tokenRequestExecutor, authenticationResult, clientApplication.authenticationAuthority.host);
        authenticationResult.metadata().tokenSource(TokenSource.IDENTITY_PROVIDER);
        authenticationResult.metadata().cacheRefreshReason(cacheRefreshReason);
        return authenticationResult;
    }

    private AuthenticationResult createFromManagedIdentityResponse(ManagedIdentityResponse managedIdentityResponse) {
        long expiresOn = getExpiresOnFromManagedIdentityTimestamp(managedIdentityResponse.expiresOn);
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

    static long getExpiresOnFromManagedIdentityTimestamp(String dateTimeStamp) {
        if (dateTimeStamp == null || dateTimeStamp.isEmpty()) {
            return 0;
        }

        // Try parsing as Unix timestamp (seconds since epoch)
        try {
            return Long.parseLong(dateTimeStamp);
        } catch (NumberFormatException e) {
            // Not a number
        }

        // Try parsing as ISO 8601
        try {
            return Instant.parse(dateTimeStamp).getEpochSecond();
        } catch (Exception e) {
            // Not ISO 8601
        }

        throw new MsalClientException(
                String.format("Failed to parse timestamp '%s'. Expected Unix epoch seconds or ISO 8601 format.",
                        dateTimeStamp),
                AuthenticationErrorCode.INVALID_TIMESTAMP_FORMAT);
    }

    private long calculateRefreshOn(long expiresOn) {
        long timestampSeconds = System.currentTimeMillis() / 1000;
        long expiresIn = expiresOn - timestampSeconds;

        //The refreshOn value should be half the value of the token lifetime, if the lifetime is greater than two hours
        return expiresIn > TWO_HOURS ? (expiresIn / 2) + timestampSeconds : 0;
    }
}
