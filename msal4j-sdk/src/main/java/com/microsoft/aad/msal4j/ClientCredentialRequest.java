// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

class ClientCredentialRequest extends MsalRequest {

    ClientCredentialParameters parameters;
    /** AppTokenProvider creates a Credential from a function that provides access tokens. The function
       must be concurrency safe. This is intended only to allow the Azure SDK to cache MSI tokens. It isn't
     useful to applications in general because the token provider must implement all authentication logic. */
    Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> appTokenProvider;

    ClientCredentialRequest(ClientCredentialParameters parameters,
                            ConfidentialClientApplication application,
                            RequestContext requestContext,
                            Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> appTokenProvider) {
        super(application, createMsalGrant(parameters), requestContext);
        this.parameters = parameters;
        this.appTokenProvider = appTokenProvider;
    }

    private static OAuthAuthorizationGrant createMsalGrant(ClientCredentialParameters parameters) {
        Map<String, String> params = new LinkedHashMap<>();

        params.put(GrantConstants.GRANT_TYPE_PARAMETER, GrantConstants.CLIENT_CREDENTIALS);

        if (!StringHelper.isBlank(parameters.fmiPath())) {
            params.put("fmi_path", parameters.fmiPath());
        }

        if (parameters.mtlsProofOfPossession()) {
            params.put("token_type", TokenType.MTLS_POP.value());
        }

        return new OAuthAuthorizationGrant(params, parameters.scopes(), parameters.claims());
    }
}
