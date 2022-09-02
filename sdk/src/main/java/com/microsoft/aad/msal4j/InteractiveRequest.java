// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Accessors(fluent = true)
class InteractiveRequest extends MsalRequest {

    @Getter(AccessLevel.PACKAGE)
    private AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference;

    @Getter(AccessLevel.PACKAGE)
    private InteractiveRequestParameters interactiveRequestParameters;

    @Getter(AccessLevel.PACKAGE)
    private String verifier;

    @Getter(AccessLevel.PACKAGE)
    private String state;

    private PublicClientApplication publicClientApplication;
    private URL authorizationUrl;

    InteractiveRequest(InteractiveRequestParameters parameters,
                       AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference,
                       PublicClientApplication publicClientApplication,
                       RequestContext requestContext) {

        super(publicClientApplication, null, requestContext);

        this.interactiveRequestParameters = parameters;
        this.futureReference = futureReference;
        this.publicClientApplication = publicClientApplication;
        validateRedirectUrl(parameters.redirectUri());
    }

    URL authorizationUrl() {
        if (this.authorizationUrl == null) {
            authorizationUrl = createAuthorizationUrl();
        }
        return authorizationUrl;
    }

    private void validateRedirectUrl(URI redirectUri) {
        try {
            if (!InetAddress.getByName(redirectUri.getHost()).isLoopbackAddress()) {
                throw new MsalClientException(String.format(
                        "Only loopback redirect uri is supported, but %s was found " +
                                "Configure http://localhost or http://localhost:port both during app registration" +
                                "and when you create the create the InteractiveRequestParameters object", redirectUri.getHost()),
                        AuthenticationErrorCode.LOOPBACK_REDIRECT_URI);
            }

            if (!redirectUri.getScheme().equals("http")) {
                throw new MsalClientException(String.format(
                        "Only http uri scheme is supported but %s was found. Configure http://localhost" +
                                "or http://localhost:port both during app registration and when you create" +
                                " the create the InteractiveRequestParameters object", redirectUri.toString()),
                        AuthenticationErrorCode.LOOPBACK_REDIRECT_URI);
            }
        } catch (Exception exception) {
            throw new MsalClientException(exception);
        }
    }

    private URL createAuthorizationUrl() {

        AuthorizationRequestUrlParameters.Builder authorizationRequestUrlBuilder =
                AuthorizationRequestUrlParameters
                        .builder(interactiveRequestParameters.redirectUri().toString(),
                                interactiveRequestParameters.scopes())
                        .prompt(interactiveRequestParameters.prompt())
                        .claimsChallenge(interactiveRequestParameters.claimsChallenge())
                        .loginHint(interactiveRequestParameters.loginHint())
                        .domainHint(interactiveRequestParameters.domainHint())
                        .correlationId(publicClientApplication.correlationId())
                        .instanceAware(interactiveRequestParameters.instanceAware());

        addPkceAndState(authorizationRequestUrlBuilder);
        return publicClientApplication.getAuthorizationRequestUrl(
                authorizationRequestUrlBuilder.build());
    }

    private void addPkceAndState(AuthorizationRequestUrlParameters.Builder builder) {

        // Create code verifier and code challenge as described in https://tools.ietf.org/html/rfc7636
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        state = UUID.randomUUID().toString() + UUID.randomUUID().toString();

        builder.codeChallenge(StringHelper.createBase64EncodedSha256Hash(verifier))
                .codeChallengeMethod("S256")
                .state(state);
    }
}
