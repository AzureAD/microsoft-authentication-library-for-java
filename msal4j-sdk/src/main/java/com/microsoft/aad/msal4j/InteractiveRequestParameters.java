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
import java.util.Map;
import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for interactive requests. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(InteractiveRequestParameters)}.
 * <p>
 * For more details, see https://aka.ms/msal4j-interactive-request.
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InteractiveRequestParameters implements IAcquireTokenParameters {

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
     * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
     */
    private ClaimsRequest claims;

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

    private String claimsChallenge;

    /**
     * Adds additional headers to the token request
     */
    private Map<String, String> extraHttpHeaders;

    /**
     * Adds additional query parameters to the token request
     */
    private Map<String, String> extraQueryParameters;

    /**
     * Overrides the tenant value in the authority URL for this request
     */
    private String tenant;

    /**
     * The amount of time in seconds that the library will wait for an authentication result. 120 seconds is the default timeout,
     * unless overridden here with some other positive integer
     *
     * If this timeout is set to 0 or less it will be ignored, and the library will use a 1 second timeout instead
     */
    @Builder.Default
    private int httpPollingTimeoutInSeconds = 120;

    /**
     * If set to true, the authorization result will contain the authority for the user's home cloud, and this authority
     * will be used for the token request instead of the authority set in the application.
     */
    private boolean instanceAware;

    /**
     * The parent window handle used to open UI elements with the correct parent
     *
     *
     * For browser scenarios and Windows console applications, this value should not need to be set
     *
     * For Windows console applications, MSAL Java will attempt to discover the console's window handle if this parameter is not set
     *
     * For scenarios where MSAL Java is responsible for opening UI elements (such as when using MSALRuntime), this parameter is required and an exception will be thrown if not set
     */
    private long windowHandle;

    private PopParameters proofOfPossession;

    private static InteractiveRequestParametersBuilder builder() {
        return new InteractiveRequestParametersBuilder();
    }

    public static InteractiveRequestParametersBuilder builder(URI redirectUri) {

        validateNotNull("redirect_uri", redirectUri);

        return builder()
                .redirectUri(redirectUri);
    }

    //This Builder class is used to override Lombok's default setter behavior for any fields defined in it
    public static class InteractiveRequestParametersBuilder {

        /**
         * Sets the PopParameters for this request, allowing the request to retrieve proof-of-possession tokens rather than bearer tokens
         *
         * For more information, see {@link PopParameters} and https://aka.ms/msal4j-pop
         *
         * @param httpMethod a valid HTTP method, such as "GET" or "POST"
         * @param uri the URI on the downstream protected API which the application is trying to access, e.g. https://graph.microsoft.com/beta/me/profile
         * @param nonce a string obtained by calling the resource (e.g. Microsoft Graph) un-authenticated and parsing the WWW-Authenticate header associated with pop authentication scheme and extracting the nonce parameter, or, on subsequent calls, by parsing the Autheticate-Info header and extracting the nextnonce parameter.
         */
        public InteractiveRequestParametersBuilder proofOfPossession(HttpMethod httpMethod, URI uri, String nonce) {
            this.proofOfPossession = new PopParameters(httpMethod, uri, nonce);

            return this;
        }
    }
}
