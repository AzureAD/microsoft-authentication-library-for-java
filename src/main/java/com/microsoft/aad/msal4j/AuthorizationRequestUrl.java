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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Accessors(fluent = true)
@Getter
public class AuthorizationRequestUrl {

    private URL authorizationRequestUrl;

    @NonNull
    private String clientId;
    @NonNull
    private String redirectUri;
    @NonNull
    private Set<String> scopes;
    private String authority;
    private String codeChallenge;
    private String codeChallengeMethod;
    private String state;
    private String nonce;
    private ResponseMode responseMode;
    private String loginHint;
    private Prompt prompt;
    private String correlationId;

    public static Builder builder(String clientId,
                                  String redirectUri,
                                  Set<String> scopes) {

        ParameterValidationUtils.validateNotBlank("clientId", clientId);
        ParameterValidationUtils.validateNotBlank("redirect_uri", redirectUri);
        ParameterValidationUtils.validateNotEmpty("scopes", scopes);

        return builder()
                .clientId(clientId)
                .redirectUri(redirectUri)
                .scopes(scopes);
    }

    private static Builder builder() {
        return new Builder();
    }

    private AuthorizationRequestUrl(Builder builder){
        Map<String, List<String>> requestParameters = new HashMap<>();

        //required parameters
        this.clientId = builder.clientId;
        requestParameters.put("client_id", Collections.singletonList(this.clientId));
        this.redirectUri = builder.redirectUri;
        requestParameters.put("redirect_uri", Collections.singletonList(this.redirectUri));
        this.scopes = builder.scopes;

        String scopesParam = AbstractMsalAuthorizationGrant.COMMON_SCOPES_PARAM +
                AbstractMsalAuthorizationGrant.SCOPES_DELIMITER +
                String.join(" ", builder.scopes);
        this.scopes = new HashSet<>(Arrays.asList(scopesParam.split(" ")));
        requestParameters.put("scope", Collections.singletonList(scopesParam));
        requestParameters.put("response_type",Collections.singletonList("code"));

        // Optional parameters
        if(builder.authority != null){
            this.authority = ClientApplicationBase.canonicalizeUrl(builder.authority);
        }

        if(builder.b2cAuthority != null){
            this.authority = ClientApplicationBase.canonicalizeUrl(builder.authority);
        }

        if(builder.claims != null){
            String claimsParam = String.join(" ", builder.claims);
            requestParameters.put("claims", Collections.singletonList(claimsParam));
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

        if(builder.prompt != null){
            this.prompt = builder.prompt;
            requestParameters.put("prompt", Collections.singletonList(builder.prompt.toString()));
        }

        if(builder.correlationId != null){
            this.correlationId = builder.correlationId;
            requestParameters.put("correlation_id", Collections.singletonList(builder.correlationId));
        }

        try {
            if(this.authority == null){
                this.authority = IClientApplicationBase.DEFAULT_AUTHORITY;
            }

            String authorizationCodeEndpoint = buildAuthorizationCodeEndpoint(requestParameters);
            String uriString = authorizationCodeEndpoint + "?" +
                    URLUtils.serializeParameters(requestParameters);

            this.authorizationRequestUrl = new URL(uriString);
        } catch(MalformedURLException ex){
            throw new MsalClientException(ex);
        }
    }

    private String buildAuthorizationCodeEndpoint(Map<String, String> requestParameters){
        AuthorityType authorityType = Authority.detectAuthorityType(this.authority);

        String authorizationCodeEndpoint = null;
        if(authorityType == AuthorityType.AAD){
            authorizationCodeEndpoint = this.authority + "oauth2/v2.0/authorize";
        } else if(authorityType == AuthorityType.ADFS){
            authorizationCodeEndpoint = this.authority = "oauth2/authorize";
        } else if(authorityType == AuthorityType.B2C){
            
        }
        return authorizationCodeEndpoint;
    }


    public static class Builder {

        private String authority;
        private String b2cAuthority;
        private String clientId;
        private String redirectUri;
        private Set<String> scopes;
        private Set<String> claims;
        private String codeChallenge;
        private String codeChallengeMethod;
        private String state;
        private String nonce;
        private ResponseMode responseMode;
        private String loginHint;
        private Prompt prompt;
        private String correlationId;

        public AuthorizationRequestUrl build(){
            return new AuthorizationRequestUrl(this);
        }

        private Builder self() {
            return this;
        }

        public Builder clientId(String val){
            this.clientId = val;
            return self();
        }

        public Builder redirectUri(String val){
            this.redirectUri = val;
            return self();
        }

        public Builder scopes(Set<String> val){
            this.scopes = val;
            return self();
        }

        public Builder authority(String val){
            this.authority = val;
            return self();
        }

        public Builder b2cAuthority(String val){
            this.b2cAuthority = val;
            return self();
        }

        public Builder claims(Set<String> val){
            this.claims = val;
            return self();
        }

        public Builder codeChallenge(String val){
            this.codeChallenge = val;
            return self();
        }

        public Builder codeChallengeMethod(String val){
            this.codeChallengeMethod = val;
            return self();
        }

        public Builder state(String val){
            this.state = val;
            return self();
        }

        public Builder nonce(String val){
            this.nonce = val;
            return self();
        }

        public Builder responseMode(ResponseMode val){
            this.responseMode = val;
            return self();
        }

        public Builder loginHint(String val){
            this.loginHint = val;
            return self();
        }

        public Builder prompt(Prompt val){
            this.prompt = val;
            return self();
        }

        public Builder correlationId(String val){
            this.correlationId = val;
            return self();
        }
    }
}
