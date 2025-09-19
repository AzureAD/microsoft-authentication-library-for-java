// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Parameters for {@link AbstractClientApplicationBase#getAuthorizationRequestUrl(AuthorizationRequestUrlParameters)}
 */
public class AuthorizationRequestUrlParameters {

    private String redirectUri;
    private Set<String> scopes;
    private String codeChallenge;
    private String codeChallengeMethod;
    private String state;
    private String nonce;
    private ResponseMode responseMode;
    private String loginHint;
    private String domainHint;
    private Prompt prompt;
    private String correlationId;
    private boolean instanceAware;

    //Unlike other prompts (which are sent as query parameters), admin consent has its own endpoint format
    private static final String ADMIN_CONSENT_ENDPOINT = "https://login.microsoftonline.com/{tenant}/adminconsent";

    Map<String, String> extraQueryParameters;

    Map<String, String> requestParameters = new HashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationRequestUrlParameters.class);

    public static Builder builder(String redirectUri,
                                  Set<String> scopes) {

        ParameterValidationUtils.validateNotBlank("redirect_uri", redirectUri);
        ParameterValidationUtils.validateNotNull("scopes", scopes);

        return builder()
                .redirectUri(redirectUri)
                .scopes(scopes);
    }

    private static Builder builder() {
        return new Builder();
    }

    private AuthorizationRequestUrlParameters(Builder builder) {
        //required parameters
        this.redirectUri = builder.redirectUri;
        requestParameters.put("redirect_uri", this.redirectUri);
        this.scopes = builder.scopes;

        Set<String> scopesParam = new LinkedHashSet<>(AbstractMsalAuthorizationGrant.COMMON_SCOPES);

        scopesParam.addAll(builder.scopes);

        if (builder.extraScopesToConsent != null) {
            scopesParam.addAll(builder.extraScopesToConsent);
        }

        this.scopes = scopesParam;
        requestParameters.put("scope", String.join(" ", scopesParam));
        requestParameters.put("response_type", "code");

        // Optional parameters
        if (builder.claims != null) {
            String claimsParam = String.join(" ", builder.claims);
            requestParameters.put("claims", claimsParam);
        }

        if (builder.claimsChallenge != null && !builder.claimsChallenge.trim().isEmpty()) {
            JsonHelper.validateJsonFormat(builder.claimsChallenge);
            requestParameters.put("claims", builder.claimsChallenge);
        }

        if (builder.claimsRequest != null) {
            String claimsRequest = builder.claimsRequest.formatAsJSONString();
            //If there are other claims (such as part of a claims challenge), merge them with this claims request.
            if (requestParameters.get("claims") != null) {
                claimsRequest = JsonHelper.mergeJSONString(claimsRequest, requestParameters.get("claims"));
            }
            requestParameters.put("claims", claimsRequest);
        }

        if (builder.codeChallenge != null) {
            this.codeChallenge = builder.codeChallenge;
            requestParameters.put("code_challenge", builder.codeChallenge);
        }

        if (builder.codeChallengeMethod != null) {
            this.codeChallengeMethod = builder.codeChallengeMethod;
            requestParameters.put("code_challenge_method", builder.codeChallengeMethod);
        }

        if (builder.state != null) {
            this.state = builder.state;
            requestParameters.put("state", builder.state);
        }

        if (builder.nonce != null) {
            this.nonce = builder.nonce;
            requestParameters.put("nonce", builder.nonce);
        }

        if (builder.responseMode != null) {
            this.responseMode = builder.responseMode;
            requestParameters.put("response_mode",
                    builder.responseMode.toString());
        } else {
            this.responseMode = ResponseMode.FORM_POST;
            requestParameters.put("response_mode",
                    ResponseMode.FORM_POST.toString());
        }

        if (builder.loginHint != null) {
            this.loginHint = loginHint();
            requestParameters.put("login_hint", builder.loginHint);

            // For CCS routing
            requestParameters.put(HttpHeaders.X_ANCHOR_MAILBOX,
                    String.format(HttpHeaders.X_ANCHOR_MAILBOX_UPN_FORMAT, builder.loginHint));
        }

        if (builder.domainHint != null) {
            this.domainHint = domainHint();
            requestParameters.put("domain_hint", builder.domainHint);
        }

        if (builder.prompt != null) {
            this.prompt = builder.prompt;
            requestParameters.put("prompt", builder.prompt.toString());
        }

        if (builder.correlationId != null) {
            this.correlationId = builder.correlationId;
            requestParameters.put("correlation_id", builder.correlationId);
        }

        if (builder.instanceAware) {
            this.instanceAware = builder.instanceAware;
            requestParameters.put("instance_aware", String.valueOf(instanceAware));
        }

        if(null != builder.extraQueryParameters && !builder.extraQueryParameters.isEmpty()){
            this.extraQueryParameters = builder.extraQueryParameters;
            for(Map.Entry<String, String> entry: this.extraQueryParameters.entrySet()){
                String key = entry.getKey();
                String value = entry.getValue();
                if(requestParameters.containsKey(key)){
                    LOG.warn("A query parameter {} has been provided with values multiple times.", key);
                }
                requestParameters.put(key, value);
            }
        }
    }

    URL createAuthorizationURL(Authority authority,
                               Map<String, String> requestParameters) {
        URL authorizationRequestUrl;
        try {
            String authorizationCodeEndpoint;
            if (prompt == Prompt.ADMIN_CONSENT) {
                authorizationCodeEndpoint = ADMIN_CONSENT_ENDPOINT
                        .replace("{tenant}", authority.tenant);
            } else {
                authorizationCodeEndpoint = authority.authorizationEndpoint();
            }

            String uriString = authorizationCodeEndpoint + "?" +
                    StringHelper.serializeQueryParameters(requestParameters);

            authorizationRequestUrl = new URL(uriString);
        } catch (MalformedURLException ex) {
            throw new MsalClientException(ex);
        }
        return authorizationRequestUrl;
    }

    public String redirectUri() {
        return this.redirectUri;
    }

    public Set<String> scopes() {
        return this.scopes;
    }

    public String codeChallenge() {
        return this.codeChallenge;
    }

    public String codeChallengeMethod() {
        return this.codeChallengeMethod;
    }

    public String state() {
        return this.state;
    }

    public String nonce() {
        return this.nonce;
    }

    public ResponseMode responseMode() {
        return this.responseMode;
    }

    public String loginHint() {
        return this.loginHint;
    }

    public String domainHint() {
        return this.domainHint;
    }

    public Prompt prompt() {
        return this.prompt;
    }

    public String correlationId() {
        return this.correlationId;
    }

    public boolean instanceAware() {
        return this.instanceAware;
    }

    public Map<String, String> extraQueryParameters() {
        return this.extraQueryParameters;
    }

    public Map<String, List<String>> requestParameters() {
        return StringHelper.convertToMultiValueMap(this.requestParameters);
    }

    public Logger log() {
        return LOG;
    }

    public static class Builder {

        private String redirectUri;
        private Set<String> scopes;
        private Set<String> extraScopesToConsent;
        private Set<String> claims;
        private String claimsChallenge;
        private ClaimsRequest claimsRequest;
        private String codeChallenge;
        private String codeChallengeMethod;
        private String state;
        private String nonce;
        private ResponseMode responseMode;
        private String loginHint;
        private String domainHint;
        private Prompt prompt;
        private String correlationId;
        private boolean instanceAware;
        private Map<String, String> extraQueryParameters;

        public AuthorizationRequestUrlParameters build() {
            return new AuthorizationRequestUrlParameters(this);
        }

        private Builder self() {
            return this;
        }

        /**
         * The redirect URI where authentication responses can be received by your application. It
         * must exactly match one of the redirect URIs registered in the Azure portal.
         */
        public Builder redirectUri(String val) {
            validateNotNull("redirectUri", val);

            this.redirectUri = val;
            return self();
        }

        /**
         * Scopes which the application is requesting access to and the user will consent to.
         */
        public Builder scopes(Set<String> val) {
            validateNotNull("scopes", val);

            this.scopes = val;
            return self();
        }

        /**
         * Scopes that you can request the end user to consent upfront,
         * in addition to scopes which the application is requesting access to.
         */
        public Builder extraScopesToConsent(Set<String> val) {
            this.extraScopesToConsent = val;
            return self();
        }

        /**
         * In cases where Azure AD tenant admin has enabled conditional access policies, and the
         * policy has not been met,{@link MsalServiceException} will contain claims that need be
         * consented to.
         */
        public Builder claimsChallenge(String val) {
            this.claimsChallenge = val;
            return self();
        }

        /**
         * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
         */
        public Builder claims(ClaimsRequest val) {
            this.claimsRequest = val;
            return self();
        }

        /**
         * Used to secure authorization code grant via Proof of Key for Code Exchange (PKCE).
         * Required if codeChallenge is included. For more information, see the PKCE RCF:
         * https://tools.ietf.org/html/rfc7636
         */
        public Builder codeChallenge(String val) {
            this.codeChallenge = val;
            return self();
        }

        /**
         * The method used to encode the code verifier for the code challenge parameter. Can be one
         * of plain or S256. If excluded, code challenge is assumed to be plaintext. For more
         * information, see the PKCE RCF: https://tools.ietf.org/html/rfc7636
         */
        public Builder codeChallengeMethod(String val) {
            this.codeChallengeMethod = val;
            return self();
        }

        /**
         * A value included in the request that is also returned in the token response. A randomly
         * generated unique value is typically used for preventing cross site request forgery attacks.
         * The state is also used to encode information about the user's state in the app before the
         * authentication request occurred.
         */
        public Builder state(String val) {
            this.state = val;
            return self();
        }

        /**
         * A value included in the request that is also returned in the token response. A randomly
         * generated unique value is typically used for preventing cross site request forgery attacks.
         */
        public Builder nonce(String val) {
            this.nonce = val;
            return self();
        }

        /**
         * Specifies the method that should be used to send the authentication result to your app.
         */
        public Builder responseMode(ResponseMode val) {
            this.responseMode = val;
            return self();
        }

        /**
         * Can be used to pre-fill the username/email address field of the sign-in page for the user,
         * if you know the username/email address ahead of time. Often apps use this parameter during
         * re-authentication, having already extracted the username from a previous sign-in using the
         * preferred_username claim.
         */
        public Builder loginHint(String val) {
            this.loginHint = val;
            return self();
        }

        /**
         * Provides a hint about the tenant or domain that the user should use to sign in. The value
         * of the domain hint is a registered domain for the tenant.
         **/
        public Builder domainHint(String val) {
            this.domainHint = val;
            return self();
        }

        /**
         * Indicates the type of user interaction that is required. Possible values are
         * {@link Prompt}
         */
        public Builder prompt(Prompt val) {
            this.prompt = val;
            return self();
        }

        /**
         * Identifier used to correlate requests for telemetry purposes. Usually a GUID.
         */
        public Builder correlationId(String val) {
            this.correlationId = val;
            return self();
        }

        /**
         * If set to true, the authorization result will contain the authority for the user's home cloud, and this authority
         * will be used for the token request instead of the authority set in the application.
         */
        public Builder instanceAware(boolean val) {
            this.instanceAware = val;
            return self();
        }

        /**
         * Query parameters that you can add to the request,
         * in addition to the list of parameters already provided.
         */
        public Builder extraQueryParameters(Map<String, String> val) {
            this.extraQueryParameters = val;
            return self();
        }
    }
}
