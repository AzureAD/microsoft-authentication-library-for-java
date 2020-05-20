// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.util.URLUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Parameters for {@link ClientApplicationBase#getAuthorizationRequestUrl(AuthorizationRequestUrlParameters)}
 */
@Accessors(fluent = true)
@Getter
public class AuthorizationRequestUrlParameters {

    @NonNull
    private String redirectUri;
    @NonNull
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

    Map<String, List<String>> requestParameters  = new HashMap<>();

    public static Builder builder(String redirectUri,
                                  Set<String> scopes) {

        ParameterValidationUtils.validateNotBlank("redirect_uri", redirectUri);
        ParameterValidationUtils.validateNotEmpty("scopes", scopes);

        return builder()
                .redirectUri(redirectUri)
                .scopes(scopes);
    }

    private static Builder builder() {
        return new Builder();
    }

    private AuthorizationRequestUrlParameters(Builder builder){
        //required parameters
        this.redirectUri = builder.redirectUri;
        requestParameters.put("redirect_uri", Collections.singletonList(this.redirectUri));
        this.scopes = builder.scopes;

        Set<String> scopesParam = new TreeSet<>(builder.scopes);
        String[] commonScopes = AbstractMsalAuthorizationGrant.COMMON_SCOPES_PARAM.split(" ");
        scopesParam.addAll(Arrays.asList(commonScopes));

        this.scopes = scopesParam;
        requestParameters.put("scope", Collections.singletonList(String.join(" ", scopesParam)));
        requestParameters.put("response_type",Collections.singletonList("code"));

        // Optional parameters
        if(builder.claims != null && builder.claims.trim().length() > 0){
            JsonHelper.validateJsonFormat(builder.claims);
            requestParameters.put("claims", Collections.singletonList(builder.claims));
        }

        if(builder.codeChallenge != null){
            this.codeChallenge = builder.codeChallenge;
            requestParameters.put("code_challenge", Collections.singletonList(builder.codeChallenge));
        }

        if(builder.codeChallengeMethod != null){
            this.codeChallengeMethod = builder.codeChallengeMethod;
            requestParameters.put("code_challenge_method", Collections.singletonList(builder.codeChallengeMethod));
        }

        if(builder.state != null){
            this.state = builder.state;
            requestParameters.put("state", Collections.singletonList(builder.state));
        }

        if(builder.nonce != null){
            this.nonce = builder.nonce;
            requestParameters.put("nonce", Collections.singletonList(builder.nonce));
        }

        if(builder.responseMode != null){
            this.responseMode = builder.responseMode;
            requestParameters.put("response_mode", Collections.singletonList(
                    builder.responseMode.toString()));
        } else {
            this.responseMode = ResponseMode.FORM_POST;
            requestParameters.put("response_mode", Collections.singletonList(
                    ResponseMode.FORM_POST.toString()));
        }

        if(builder.loginHint != null){
            this.loginHint = loginHint();
            requestParameters.put("login_hint", Collections.singletonList(builder.loginHint));
        }

        if(builder.domainHint != null){
            this.domainHint = domainHint();
            requestParameters.put("domain_hint", Collections.singletonList(builder.domainHint));
        }

        if(builder.prompt != null){
            this.prompt = builder.prompt;
            requestParameters.put("prompt", Collections.singletonList(builder.prompt.toString()));
        }

        if(builder.correlationId != null){
            this.correlationId = builder.correlationId;
            requestParameters.put("correlation_id", Collections.singletonList(builder.correlationId));
        }
    }

    URL createAuthorizationURL(Authority authority,
                               Map<String, List<String>> requestParameters){
        URL authorizationRequestUrl;
        try {
            String authorizationCodeEndpoint = authority.authorizationEndpoint();
            String uriString = authorizationCodeEndpoint + "?" +
                    URLUtils.serializeParameters(requestParameters);

            authorizationRequestUrl = new URL(uriString);
        } catch(MalformedURLException ex){
            throw new MsalClientException(ex);
        }
        return authorizationRequestUrl;
    }

    public static class Builder {

        private String redirectUri;
        private Set<String> scopes;
        private String claims;
        private String codeChallenge;
        private String codeChallengeMethod;
        private String state;
        private String nonce;
        private ResponseMode responseMode;
        private String loginHint;
        private String domainHint;
        private Prompt prompt;
        private String correlationId;

        public AuthorizationRequestUrlParameters build(){
            return new AuthorizationRequestUrlParameters(this);
        }

        private Builder self() {
            return this;
        }

        /**
         * The redirect URI where authentication responses can be received by your application. It
         * must exactly match one of the redirect URIs registered in the Azure portal.
         */
        public Builder redirectUri(String val){
            this.redirectUri = val;
            return self();
        }

        /**
         * Scopes which the application is requesting access to and the user will consent to.
         */
        public Builder scopes(Set<String> val){
            this.scopes = val;
            return self();
        }

        /**
         * In cases where Azure AD tenant admin has enabled conditional access policies, and the
         * policy has not been met,{@link MsalServiceException} will contain claims that need be
         * consented to.
         */
        public Builder claims(String val){
            this.claims = val;
            return self();
        }

        /**
         * Used to secure authorization code grant via Proof of Key for Code Exchange (PKCE).
         * Required if codeChallenge is included. For more information, see the PKCE RCF:
         * https://tools.ietf.org/html/rfc7636
         */
        public Builder codeChallenge(String val){
            this.codeChallenge = val;
            return self();
        }

        /**
         * The method used to encode the code verifier for the code challenge parameter. Can be one
         * of plain or S256. If excluded, code challenge is assumed to be plaintext. For more
         * information, see the PKCE RCF: https://tools.ietf.org/html/rfc7636
         */
        public Builder codeChallengeMethod(String val){
            this.codeChallengeMethod = val;
            return self();
        }

        /**
         * A value included in the request that is also returned in the token response. A randomly
         * generated unique value is typically used for preventing cross site request forgery attacks.
         * The state is also used to encode information about the user's state in the app before the
         * authentication request occurred.
         * */
        public Builder state(String val){
            this.state = val;
            return self();
        }

        /**
         *  A value included in the request that is also returned in the token response. A randomly
         *  generated unique value is typically used for preventing cross site request forgery attacks.
         */
        public Builder nonce(String val){
            this.nonce = val;
            return self();
        }

        /**
         * Specifies the method that should be used to send the authentication result to your app.
         */
        public Builder responseMode(ResponseMode val){
            this.responseMode = val;
            return self();
        }

        /**
         * Can be used to pre-fill the username/email address field of the sign-in page for the user,
         * if you know the username/email address ahead of time. Often apps use this parameter during
         * re-authentication, having already extracted the username from a previous sign-in using the
         * preferred_username claim.
         */
        public Builder loginHint(String val){
            this.loginHint = val;
            return self();
        }

        /**
         * Provides a hint about the tenant or domain that the user should use to sign in. The value
         * of the domain hint is a registered domain for the tenant.
         **/
        public Builder domainHint(String val){
            this.domainHint = val;
            return self();
        }

        /**
         * Indicates the type of user interaction that is required. Possible values are
         * {@link Prompt}
         */
        public Builder prompt(Prompt val){
            this.prompt = val;
            return self();
        }

        /**
         * Identifier used to correlate requests for telemetry purposes. Usually a GUID.
         */
        public Builder correlationId(String val){
            this.correlationId = val;
            return self();
        }
    }
}
