// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class AcquireTokenByManagedIdentitySupplier extends AuthenticationResultSupplier{

    private static final Logger LOG = LoggerFactory.getLogger(AcquireTokenByManagedIdentitySupplier.class);

    private ManagedIdentityParameters managedIdentityParameters;

    AcquireTokenByManagedIdentitySupplier(ManagedIdentityApplication managedIdentityApplication, MsalRequest msalRequest){
        super(managedIdentityApplication, msalRequest);
        this.managedIdentityParameters = (ManagedIdentityParameters) msalRequest.requestContext().apiParameters();
    }

    @Override
    AuthenticationResult execute() throws Exception {

        if (StringHelper.isNullOrBlank(managedIdentityParameters.resource))
        {
            throw new MsalClientException(
                    MsalError.SCOPES_REQUIRED,
                    MsalErrorMessage.SCOPES_REQUIRED);
        }

        TokenRequestExecutor tokenRequestExecutor = new TokenRequestExecutor(
                clientApplication.authenticationAuthority,
                msalRequest,
                clientApplication.getServiceBundle()
        );

        if (!managedIdentityParameters.forceRefresh)
        {
            LOG.debug("ForceRefresh set to false. Attempting cache lookup");

//            AuthenticationResult authenticationResult = clientApplication.tokenCache.getCachedAuthenticationResult(
//                    clientApplication.authenticationAuthority.authority,
//                    managedIdentityParameters.resource,
//                    clientApplication.clientId(),
//                    silentRequest.assertion());

            AuthenticationResult authenticationResult = clientApplication.tokenCache.getCacheAuthenticationResult();

            if (authenticationResult == null) {
                return fetchNewAccessTokenAndSaveToCache(tokenRequestExecutor, clientApplication.authenticationAuthority.host);
            }
            if (!StringHelper.isBlank(authenticationResult.accessToken())) {
                clientApplication.getServiceBundle().getServerSideTelemetry().incrementSilentSuccessfulCount();
            }
            return authenticationResult;
        }
            LOG.info("Skipped looking for an Access Token in the cache because forceRefresh or Claims were set. ");

            // No AT in the cache
            return fetchNewAccessTokenAndSaveToCache(tokenRequestExecutor, clientApplication.authenticationAuthority.host);
        }

    private AuthenticationResult fetchNewAccessTokenAndSaveToCache(TokenRequestExecutor tokenRequestExecutor, String host) {

        ManagedIdentityClient managedIdentityClient = new ManagedIdentityClient(msalRequest, tokenRequestExecutor.getServiceBundle());

        ManagedIdentityResponse managedIdentityResponse = managedIdentityClient
                .getManagedIdentityResponse(managedIdentityParameters);

        AuthenticationResult authenticationResult =  createFromManagedIdentityResponse(managedIdentityResponse);
        clientApplication.tokenCache.saveTokens(tokenRequestExecutor,authenticationResult,clientApplication.authenticationAuthority.host);
        return authenticationResult;
    }

    private AuthenticationResult createFromManagedIdentityResponse(ManagedIdentityResponse managedIdentityResponse){
        long currTimestampSec = new Date().getTime() / 1000;
        long expiresOn = Long.valueOf(currTimestampSec + managedIdentityResponse.expiresOn);
        long refreshOn = expiresOn > 2*3600 ? (expiresOn/2) : 0L;

        return AuthenticationResult.builder()
                .accessToken(managedIdentityResponse.getAccessToken())
                .scopes(managedIdentityParameters.scopes().toString())
                .expiresOn(expiresOn)
                .extExpiresOn(0)
                .refreshOn(refreshOn)
                .scopes(managedIdentityParameters.scopes().toString())
                .build();
    }
}
