// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for interactive requests. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(InteractiveRequestParameters)}.
 *
 * For more details, see https://aka.ms/msal4j-interactive-request.
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InteractiveRequestParameters implements IApiParameters {

    /**
     * Redirect URI where MSAL will listen to for the authorization code returned by Azure AD.
     * Should be a loopback address with a port specified (for example, http://localhost:3671). If no
     * port is specified, MSAL will find an open port. For more information, see
     * https://aka.ms/msal4j-interactive-request.
     */
    @Setter(AccessLevel.PACKAGE)
    @NonNull
    private URI redirectUri;

    /**
     * Scopes that the application is requesting access to and the user will consent to.
     */
    private Set<String> scopes;

    /**
     * Indicate the type of user interaction that is required.
     */
    private Prompt prompt;

    /**
     * Can be used to pre-fill the username/email address field of the sign-in page for the user,
     * if you know the username/email address ahead of time. Often apps use this parameter during
     * re-authentication, having already extracted the username from a previous sign-in using the
     * preferred_username claim.
     */
    private String loginHint;

    /**
     * Provides a hint about the tenant or domain that the user should use to sign in. The value
     * of the domain hint is a registered domain for the tenant.
     **/
    private String domainHint;

    /**
     * Sets {@link SystemBrowserOptions} to be used by the PublicClientApplication
     */
    private SystemBrowserOptions systemBrowserOptions;

    private String claims;

    private static InteractiveRequestParametersBuilder builder() {
        return new InteractiveRequestParametersBuilder();
    }

    public static InteractiveRequestParametersBuilder builder(URI redirectUri) {

        validateNotNull("redirect_uri", redirectUri);

        return builder()
                .redirectUri(redirectUri);
    }
}
