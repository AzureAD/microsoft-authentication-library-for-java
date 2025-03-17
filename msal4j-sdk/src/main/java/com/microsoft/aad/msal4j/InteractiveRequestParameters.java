// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

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
public class InteractiveRequestParameters implements IAcquireTokenParameters {

    private URI redirectUri;

    private ClaimsRequest claims;

    private Set<String> scopes;

    private Prompt prompt;

    private String loginHint;

    private String domainHint;

    private SystemBrowserOptions systemBrowserOptions;

    private String claimsChallenge;

    private Map<String, String> extraHttpHeaders;

    private Map<String, String> extraQueryParameters;

    private String tenant;

    private int httpPollingTimeoutInSeconds;

    private boolean instanceAware;

    private long windowHandle;

    private PopParameters proofOfPossession;

    private InteractiveRequestParameters(URI redirectUri, ClaimsRequest claims, Set<String> scopes, Prompt prompt, String loginHint, String domainHint, SystemBrowserOptions systemBrowserOptions, String claimsChallenge, Map<String, String> extraHttpHeaders, Map<String, String> extraQueryParameters, String tenant, int httpPollingTimeoutInSeconds, boolean instanceAware, long windowHandle, PopParameters proofOfPossession) {
        this.redirectUri = redirectUri;
        this.claims = claims;
        this.scopes = scopes;
        this.prompt = prompt;
        this.loginHint = loginHint;
        this.domainHint = domainHint;
        this.systemBrowserOptions = systemBrowserOptions;
        this.claimsChallenge = claimsChallenge;
        this.extraHttpHeaders = extraHttpHeaders;
        this.extraQueryParameters = extraQueryParameters;
        this.tenant = tenant;
        this.httpPollingTimeoutInSeconds = httpPollingTimeoutInSeconds;
        this.instanceAware = instanceAware;
        this.windowHandle = windowHandle;
        this.proofOfPossession = proofOfPossession;
    }

    private static InteractiveRequestParametersBuilder builder() {
        return new InteractiveRequestParametersBuilder();
    }

    public static InteractiveRequestParametersBuilder builder(URI redirectUri) {

        validateNotNull("redirect_uri", redirectUri);

        return builder()
                .redirectUri(redirectUri);
    }

    public URI redirectUri() {
        return this.redirectUri;
    }

    public ClaimsRequest claims() {
        return this.claims;
    }

    public Set<String> scopes() {
        return this.scopes;
    }

    public Prompt prompt() {
        return this.prompt;
    }

    public String loginHint() {
        return this.loginHint;
    }

    public String domainHint() {
        return this.domainHint;
    }

    public SystemBrowserOptions systemBrowserOptions() {
        return this.systemBrowserOptions;
    }

    public String claimsChallenge() {
        return this.claimsChallenge;
    }

    public Map<String, String> extraHttpHeaders() {
        return this.extraHttpHeaders;
    }

    public Map<String, String> extraQueryParameters() {
        return this.extraQueryParameters;
    }

    public String tenant() {
        return this.tenant;
    }

    public int httpPollingTimeoutInSeconds() {
        return this.httpPollingTimeoutInSeconds;
    }

    public boolean instanceAware() {
        return this.instanceAware;
    }

    public long windowHandle() {
        return this.windowHandle;
    }

    public PopParameters proofOfPossession() {
        return this.proofOfPossession;
    }

    void redirectUri(URI redirectUri) {
        this.redirectUri = redirectUri;
    }

    public static class InteractiveRequestParametersBuilder {

        private URI redirectUri;
        private ClaimsRequest claims;
        private Set<String> scopes;
        private Prompt prompt;
        private String loginHint;
        private String domainHint;
        private SystemBrowserOptions systemBrowserOptions;
        private String claimsChallenge;
        private Map<String, String> extraHttpHeaders;
        private Map<String, String> extraQueryParameters;
        private String tenant;
        private int httpPollingTimeoutInSeconds = 120;
        private boolean instanceAware;
        private long windowHandle;
        private PopParameters proofOfPossession;

        InteractiveRequestParametersBuilder() {
        }

        /**
         * Sets the PopParameters for this request, allowing the request to retrieve proof-of-possession tokens rather than bearer tokens
         * <p>
         * For more information, see {@link PopParameters} and https://aka.ms/msal4j-pop
         *
         * @param httpMethod a valid HTTP method, such as "GET" or "POST"
         * @param uri        the URI on the downstream protected API which the application is trying to access, e.g. https://graph.microsoft.com/beta/me/profile
         * @param nonce      a string obtained by calling the resource (e.g. Microsoft Graph) un-authenticated and parsing the WWW-Authenticate header associated with pop authentication scheme and extracting the nonce parameter, or, on subsequent calls, by parsing the Autheticate-Info header and extracting the nextnonce parameter.
         */
        public InteractiveRequestParametersBuilder proofOfPossession(HttpMethod httpMethod, URI uri, String nonce) {
            this.proofOfPossession = new PopParameters(httpMethod, uri, nonce);

            return this;
        }

        /**
         * Redirect URI where MSAL will listen to for the authorization code returned by Azure AD.
         * Should be a loopback address with a port specified (for example, http://localhost:3671). If no
         * port is specified, MSAL will find an open port. For more information, see
         * https://aka.ms/msal4j-interactive-request.
         * <p>
         * Cannot be null.
         */
        public InteractiveRequestParametersBuilder redirectUri(URI redirectUri) {
            validateNotNull("redirectUri", redirectUri);

            this.redirectUri = redirectUri;
            return this;
        }

        /**
         * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
         */
        public InteractiveRequestParametersBuilder claims(ClaimsRequest claims) {
            this.claims = claims;
            return this;
        }

        /**
         * Scopes that the application is requesting access to and the user will consent to.
         */
        public InteractiveRequestParametersBuilder scopes(Set<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        /**
         * Indicate the type of user interaction that is required.
         */
        public InteractiveRequestParametersBuilder prompt(Prompt prompt) {
            this.prompt = prompt;
            return this;
        }

        /**
         * Can be used to pre-fill the username/email address field of the sign-in page for the user,
         * if you know the username/email address ahead of time. Often apps use this parameter during
         * re-authentication, having already extracted the username from a previous sign-in using the
         * preferred_username claim.
         */
        public InteractiveRequestParametersBuilder loginHint(String loginHint) {
            this.loginHint = loginHint;
            return this;
        }

        /**
         * Provides a hint about the tenant or domain that the user should use to sign in. The value
         * of the domain hint is a registered domain for the tenant.
         */
        public InteractiveRequestParametersBuilder domainHint(String domainHint) {
            this.domainHint = domainHint;
            return this;
        }

        /**
         * Sets {@link SystemBrowserOptions} to be used by the PublicClientApplication
         */
        public InteractiveRequestParametersBuilder systemBrowserOptions(SystemBrowserOptions systemBrowserOptions) {
            this.systemBrowserOptions = systemBrowserOptions;
            return this;
        }

        /**
         * Used when a token request fails and the response has a challenge string
         */
        public InteractiveRequestParametersBuilder claimsChallenge(String claimsChallenge) {
            this.claimsChallenge = claimsChallenge;
            return this;
        }

        /**
         * Adds additional headers to the token request
         */
        public InteractiveRequestParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
            this.extraHttpHeaders = extraHttpHeaders;
            return this;
        }

        /**
         * Adds additional query parameters to the token request
         */
        public InteractiveRequestParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
            this.extraQueryParameters = extraQueryParameters;
            return this;
        }

        /**
         * Overrides the tenant value in the authority URL for this request
         */
        public InteractiveRequestParametersBuilder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        /**
         * The amount of time in seconds that the library will wait for an authentication result. 120 seconds is the default timeout.
         * <p>
         * If this timeout is set to 0 or less it will be ignored, and the library will use a 1-second timeout instead
         */
        public InteractiveRequestParametersBuilder httpPollingTimeoutInSeconds(int httpPollingTimeoutInSeconds) {
            this.httpPollingTimeoutInSeconds = httpPollingTimeoutInSeconds;
            return this;
        }

        /**
         * If set to true, the authorization result will contain the authority for the user's home cloud, and this authority
         * will be used for the token request instead of the authority set in the application.
         */
        public InteractiveRequestParametersBuilder instanceAware(boolean instanceAware) {
            this.instanceAware = instanceAware;
            return this;
        }

        /**
         * The parent window handle used to open UI elements with the correct parent
         * <p>
         * For browser scenarios and Windows console applications, this value should not need to be set
         * <p>
         * For Windows console applications, MSAL Java will attempt to discover the console's window handle if this parameter is not set
         * <p>
         * For scenarios where MSAL Java is responsible for opening UI elements (such as when using MSALRuntime), this parameter is required and an exception will be thrown if not set
         */
        public InteractiveRequestParametersBuilder windowHandle(long windowHandle) {
            this.windowHandle = windowHandle;
            return this;
        }

        public InteractiveRequestParameters build() {
            return new InteractiveRequestParameters(this.redirectUri, this.claims, this.scopes, this.prompt, this.loginHint, this.domainHint, this.systemBrowserOptions, this.claimsChallenge, this.extraHttpHeaders, this.extraQueryParameters, this.tenant, this.httpPollingTimeoutInSeconds, this.instanceAware, this.windowHandle, this.proofOfPossession);
        }

        public String toString() {
            return "InteractiveRequestParameters.InteractiveRequestParametersBuilder(redirectUri=" + this.redirectUri + ", claims=" + this.claims + ", scopes=" + this.scopes + ", prompt=" + this.prompt + ", loginHint=" + this.loginHint + ", domainHint=" + this.domainHint + ", systemBrowserOptions=" + this.systemBrowserOptions + ", claimsChallenge=" + this.claimsChallenge + ", extraHttpHeaders=" + this.extraHttpHeaders + ", extraQueryParameters=" + this.extraQueryParameters + ", tenant=" + this.tenant + ", httpPollingTimeoutInSeconds=" + this.httpPollingTimeoutInSeconds + ", instanceAware=" + this.instanceAware + ", windowHandle=" + this.windowHandle + ", proofOfPossession=" + this.proofOfPossession + ")";
        }
    }
}
