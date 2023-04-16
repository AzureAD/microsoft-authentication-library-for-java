// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
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
        String host = redirectUri.getHost();
        String scheme = redirectUri.getScheme();
        InetAddress address;

        //Validate URI scheme. Only http is valid, as determined by the HttpListener created in AcquireTokenByInteractiveFlowSupplier.startHttpListener()
        if (scheme == null || !scheme.equals("http")) {
            throw new MsalClientException(String.format(
                    "Only http is supported for the redirect URI of an interactive request, but \"%s\" was found. For more information about redirect URI formats, see https://aka.ms/msal4j-interactive-request", scheme),
                    AuthenticationErrorCode.LOOPBACK_REDIRECT_URI);
        }

        //Ensure that the given redirect URI has a known address
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new MsalClientException(String.format(
                    "Unknown host exception for host \"%s\". For more information about redirect URI formats, see https://aka.ms/msal4j-interactive-request", host),
                    AuthenticationErrorCode.LOOPBACK_REDIRECT_URI);
        }

        //Ensure that the redirect URI is considered a loopback address
        if (address == null || !address.isLoopbackAddress()) {
            throw new MsalClientException(
                    "Only loopback redirect URI is supported for interactive requests. For more information about redirect URI formats, see https://aka.ms/msal4j-interactive-request",
                    AuthenticationErrorCode.LOOPBACK_REDIRECT_URI);
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
                        .instanceAware(interactiveRequestParameters.instanceAware())
                        .extraQueryParameters(interactiveRequestParameters.extraQueryParameters());

        addPkceAndState(authorizationRequestUrlBuilder);
        AuthorizationRequestUrlParameters authorizationRequestUrlParameters =
                authorizationRequestUrlBuilder.build();

        return publicClientApplication.getAuthorizationRequestUrl(
                authorizationRequestUrlParameters);
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
