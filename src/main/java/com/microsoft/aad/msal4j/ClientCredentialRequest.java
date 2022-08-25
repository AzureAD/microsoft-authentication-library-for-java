// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;

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
                            RequestContext requestContext) {
        super(application, createMsalGrant(parameters), requestContext);
        this.parameters = parameters;
        appTokenProvider = null;
    }

    ClientCredentialRequest(ClientCredentialParameters parameters,
                            ConfidentialClientApplication application,
                            RequestContext requestContext,
                            Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> appTokenProvider) {
        super(application, createMsalGrant(parameters), requestContext);
        this.parameters = parameters;
        this.appTokenProvider = appTokenProvider;
    }

    private static OAuthAuthorizationGrant createMsalGrant(ClientCredentialParameters parameters) {
        return new OAuthAuthorizationGrant(new ClientCredentialsGrant(), parameters.scopes(), parameters.claims());
    }
}
