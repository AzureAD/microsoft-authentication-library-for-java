// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.concurrent.ExecutionException;

class AcquireTokenByAppProviderSupplier extends AuthenticationResultSupplier {

    private AppTokenProviderParameters appTokenProviderParameters;

    private ClientCredentialRequest clientCredentialRequest;

    AcquireTokenByAppProviderSupplier(AbstractClientApplicationBase clientApplication,
                                      ClientCredentialRequest clientCredentialRequest,
                                      AppTokenProviderParameters appTokenProviderParameters) {
        super(clientApplication, clientCredentialRequest);
        this.clientCredentialRequest = clientCredentialRequest;
        this.appTokenProviderParameters = appTokenProviderParameters;
    }

    private static void validateTokenProviderResult(TokenProviderResult tokenProviderResult) {
        if (null == tokenProviderResult.getAccessToken() || tokenProviderResult.getAccessToken().isEmpty()) {
            handleInvalidExternalValueError(tokenProviderResult.getAccessToken());
        }

        if (tokenProviderResult.getExpiresInSeconds() == 0 || tokenProviderResult.getExpiresInSeconds() < 0) {
            handleInvalidExternalValueError(Long.valueOf(tokenProviderResult.getExpiresInSeconds()).toString());
        }

        if (null == tokenProviderResult.getTenantId() || tokenProviderResult.getTenantId().isEmpty()) {
            handleInvalidExternalValueError(tokenProviderResult.getTenantId());
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

    public AuthenticationResult fetchTokenUsingAppTokenProvider(AppTokenProviderParameters appTokenProviderParameters) {

//        CompletableFuture<TokenProviderResult> completableFuture =
//                CompletableFuture.supplyAsync(() -> this.clientCredentialRequest.appTokenProvider.apply(appTokenProviderParameters));

        TokenProviderResult tokenProviderResult = this.clientCredentialRequest.appTokenProvider.apply(appTokenProviderParameters);

        validateTokenProviderResult(tokenProviderResult);

        return AuthenticationResult.builder()
                .accessToken(tokenProviderResult.getAccessToken())
                .refreshToken(null)
                .idToken(null)
                .expiresOn(tokenProviderResult.getExpiresInSeconds())
                .refreshOn(tokenProviderResult.getRefreshInSeconds())
                .build();

    }
}
