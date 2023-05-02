// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/** Disclaimer - This  class is meant to be used by the Azure SDK team only.
 *  Any other teams are discouraged from using this class to prevent any side effects.
 */
class AcquireTokenByAppProviderSupplier extends AuthenticationResultSupplier {

    private static final int TWO_HOURS = 2*3600;

    private AppTokenProviderParameters appTokenProviderParameters;

    private ClientCredentialRequest clientCredentialRequest;

    AcquireTokenByAppProviderSupplier(AbstractClientApplicationBase clientApplication,
                                      ClientCredentialRequest clientCredentialRequest,
                                      AppTokenProviderParameters appTokenProviderParameters) {
        super(clientApplication, clientCredentialRequest);
        this.clientCredentialRequest = clientCredentialRequest;
        this.appTokenProviderParameters = appTokenProviderParameters;
    }

    private static void validateAndUpdateTokenProviderResult(TokenProviderResult tokenProviderResult) {
        if (null == tokenProviderResult.getAccessToken() || tokenProviderResult.getAccessToken().isEmpty()) {
            handleInvalidExternalValueError(tokenProviderResult.getAccessToken());
        }

        if (tokenProviderResult.getExpiresInSeconds() == 0 || tokenProviderResult.getExpiresInSeconds() < 0) {
            handleInvalidExternalValueError(Long.valueOf(tokenProviderResult.getExpiresInSeconds()).toString());
        }

        if (null == tokenProviderResult.getTenantId() || tokenProviderResult.getTenantId().isEmpty()) {
            handleInvalidExternalValueError(tokenProviderResult.getTenantId());
        }

        if (0 == tokenProviderResult.getRefreshInSeconds()){
            long expireInSeconds = tokenProviderResult.getExpiresInSeconds();
            if(expireInSeconds >= TWO_HOURS){
                tokenProviderResult.setRefreshInSeconds(expireInSeconds/2);
            }
        }
    }

    private static void handleInvalidExternalValueError(String nameOfValue) {
        throw new MsalClientException("The following token provider result value is invalid" + nameOfValue, "Invalid_TokenProviderResult_Input");
    }

    @Override
    AuthenticationResult execute() throws Exception {

        AuthenticationResult authenticationResult = fetchTokenUsingAppTokenProvider(appTokenProviderParameters);

        TokenRequestExecutor tokenRequestExecutor = new TokenRequestExecutor(
                clientCredentialRequest.application().authenticationAuthority,
                msalRequest,
                clientApplication.getServiceBundle()
        );

        clientApplication.tokenCache.saveTokens(tokenRequestExecutor, authenticationResult, clientCredentialRequest.application().authenticationAuthority.host);

        return authenticationResult;
    }

    public AuthenticationResult fetchTokenUsingAppTokenProvider(AppTokenProviderParameters appTokenProviderParameters) throws ExecutionException, InterruptedException {

        TokenProviderResult tokenProviderResult;
        try{

            CompletableFuture<TokenProviderResult> completableFuture = this.clientCredentialRequest.appTokenProvider.apply(appTokenProviderParameters);
            tokenProviderResult = completableFuture.get();

        } catch (Exception ex){
            throw new MsalAzureSDKException(ex);
        }

        validateAndUpdateTokenProviderResult(tokenProviderResult);

        return AuthenticationResult.builder()
                .accessToken(tokenProviderResult.getAccessToken())
                .refreshToken(null)
                .idToken(null)
                .expiresOn(tokenProviderResult.getExpiresInSeconds())
                .refreshOn(tokenProviderResult.getRefreshInSeconds())
                .build();
    }
}
