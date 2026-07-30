// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for authorization code flow. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(AuthorizationCodeParameters)} or to
 * {@link ConfidentialClientApplication#acquireToken(AuthorizationCodeParameters)}
 */
public class AuthorizationCodeParameters implements IAcquireTokenParameters {

    private String authorizationCode;

    private URI redirectUri;

    private Set<String> scopes;

    private ClaimsRequest claims;

    private String codeVerifier;

    private Map<String, String> extraHttpHeaders;

    private Map<String, String> extraQueryParameters;

    private String tenant;

    private String clientClaims;

    // Generic extended cache key. The hash of the contributed components isolates cache
    // entries so that requests with different client-claims values do not collide.
    private final ExtendedCacheKey extendedCacheKey;

    private AuthorizationCodeParameters(String authorizationCode, URI redirectUri,
                                        Set<String> scopes, ClaimsRequest claims,
                                        String codeVerifier, Map<String, String> extraHttpHeaders,
                                        Map<String, String> extraQueryParameters, String tenant,
                                        String clientClaims) {
        this.authorizationCode = authorizationCode;
        this.redirectUri = redirectUri;
        this.scopes = scopes;
        this.claims = claims;
        this.codeVerifier = codeVerifier;
        this.extraHttpHeaders = extraHttpHeaders;
        this.extraQueryParameters = extraQueryParameters;
        this.tenant = tenant;
        this.clientClaims = clientClaims;

        // Build cache key components from any parameters that require cache isolation.
        this.extendedCacheKey = new ExtendedCacheKey(buildCacheKeyComponents());
    }

    private static AuthorizationCodeParametersBuilder builder() {

        return new AuthorizationCodeParametersBuilder();
    }

    /**
     * Builder for {@link AuthorizationCodeParameters}
     *
     * @param authorizationCode code received from the service authorization endpoint
     * @param redirectUri       URI where authorization code was received
     * @return builder object that can be used to construct {@link AuthorizationCodeParameters}
     */
    public static AuthorizationCodeParametersBuilder builder(String authorizationCode, URI redirectUri) {

        validateNotBlank("authorizationCode", authorizationCode);

        return builder()
                .authorizationCode(authorizationCode)
                .redirectUri(redirectUri);
    }

    public String authorizationCode() {
        return this.authorizationCode;
    }

    public URI redirectUri() {
        return this.redirectUri;
    }

    public Set<String> scopes() {
        return this.scopes;
    }

    public ClaimsRequest claims() {
        return this.claims;
    }

    public String codeVerifier() {
        return this.codeVerifier;
    }

    public Map<String, String> extraHttpHeaders() {
        return this.extraHttpHeaders;
    }

    /**
     * @deprecated Not recommended for production scenarios. It will be removed in a future release, and the behavior may be replaced by a new API.
     */
    @Deprecated
    public Map<String, String> extraQueryParameters() {
        return this.extraQueryParameters;
    }

    public String tenant() {
        return this.tenant;
    }

    /**
     * Client-originated claims set via {@link AuthorizationCodeParametersBuilder#claimsFromClient(String)}.
     * Forwarded to the token endpoint as the OAuth {@code claims} parameter and used as part of the
     * extended cache key so that distinct claim values are cached separately.
     */
    @Override
    public String clientClaims() {
        return this.clientClaims;
    }

    /**
     * Builds the sorted map of cache key components from the parameters that require cache isolation.
     * Returns null if no components are present.
     */
    private SortedMap<String, String> buildCacheKeyComponents() {
        TreeMap<String, String> components = null;
        if (!StringHelper.isBlank(clientClaims)) {
            components = new TreeMap<>();
            components.put("client_claims", clientClaims);
        }
        return components;
    }

    /**
     * Computes the extended cache key hash from all cache key components, or an empty string when
     * there are none. The result is memoized since the parameters are immutable after construction.
     */
    @Override
    public String computeExtCacheKeyHash() {
        return extendedCacheKey.computeHash();
    }

    public static class AuthorizationCodeParametersBuilder {
        private String authorizationCode;
        private URI redirectUri;
        private Set<String> scopes;
        private ClaimsRequest claims;
        private String codeVerifier;
        private Map<String, String> extraHttpHeaders;
        private Map<String, String> extraQueryParameters;
        private String tenant;
        private String clientClaims;

        AuthorizationCodeParametersBuilder() {
        }

        /**
         * Authorization code acquired in the first step of OAuth2.0 authorization code flow. For more
         * details, see https://aka.ms/msal4j-authorization-code-flow
         * <p>
         * Cannot be null.
         */
        public AuthorizationCodeParametersBuilder authorizationCode(String authorizationCode) {
            validateNotNull("authorizationCode", authorizationCode);

            this.authorizationCode = authorizationCode;
            return this;
        }

        /**
         * Redirect URI registered in the Azure portal, and which was used in the first step of OAuth2.0
         * authorization code flow. For more details, see https://aka.ms/msal4j-authorization-code-flow
         * <p>
         * Cannot be null.
         */
        public AuthorizationCodeParametersBuilder redirectUri(URI redirectUri) {
            validateNotNull("redirectUri", redirectUri);

            this.redirectUri = redirectUri;
            return this;
        }

        /**
         * Scopes to which the application is requesting access
         */
        public AuthorizationCodeParametersBuilder scopes(Set<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        /**
         * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
         */
        public AuthorizationCodeParametersBuilder claims(ClaimsRequest claims) {
            this.claims = claims;
            return this;
        }

        /**
         * Code verifier used for PKCE. For more details, see https://tools.ietf.org/html/rfc7636
         */
        public AuthorizationCodeParametersBuilder codeVerifier(String codeVerifier) {
            this.codeVerifier = codeVerifier;
            return this;
        }

        /**
         * Adds additional headers to the token request
         */
        public AuthorizationCodeParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
            this.extraHttpHeaders = extraHttpHeaders;
            return this;
        }

        /**
         * Adds additional query parameters to the token request
         * @deprecated Not recommended for production scenarios. It will be removed in a future release, and the behavior may be replaced by a new API.
         */
        @Deprecated
        public AuthorizationCodeParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
            this.extraQueryParameters = extraQueryParameters;
            return this;
        }

        /**
         * Overrides the tenant value in the authority URL for this request
         */
        public AuthorizationCodeParametersBuilder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        /**
         * Specifies client-originated claims (a raw JSON object string) to forward to the token
         * endpoint as the OAuth {@code claims} request parameter. Unlike {@link #claims(ClaimsRequest)}
         * (server-issued claims challenges, which bypass the cache), tokens acquired with client claims
         * are cached and the cache entry is keyed on the claims value, so distinct claim values produce
         * separate cache entries. Use stable, non-dynamic values to avoid cache fragmentation. Send the identical value on every
         * request for a given token; because the raw value is part of the cache key, changing or
         * omitting it routes the request to a different cache partition.
         * A blank value is ignored; an invalid JSON object throws {@link MsalClientException}.
         * <p>
         * Client claims are primarily intended for confidential-client web apps, but
         * {@code AuthorizationCodeParameters} is also accepted by {@link PublicClientApplication}, so this
         * method is visible there too. The same cache caveat applies to <em>both</em> application types: a
         * token acquired with client claims is stored under the extended cache key, while a later
         * {@code acquireTokenSilently} call (which uses {@link SilentParameters} and cannot carry client
         * claims) will not match that entry and will instead refresh without the client claims. The claims
         * are applied only on the acquire/redemption call that sets them; to apply them again, redeem
         * through this builder rather than relying on a silent refresh.
         *
         * @param claimsJson a valid JSON object string containing the client claims
         * @return this builder instance
         */
        public AuthorizationCodeParametersBuilder claimsFromClient(String claimsJson) {
            if (StringHelper.isBlank(claimsJson)) {
                return this;
            }

            JsonHelper.validateJsonObjectFormat(claimsJson);
            this.clientClaims = claimsJson;
            return this;
        }

        public AuthorizationCodeParameters build() {
            return new AuthorizationCodeParameters(this.authorizationCode, this.redirectUri, this.scopes, this.claims, this.codeVerifier, this.extraHttpHeaders, this.extraQueryParameters, this.tenant, this.clientClaims);
        }

        public String toString() {
            return "AuthorizationCodeParameters.AuthorizationCodeParametersBuilder(authorizationCode=" + this.authorizationCode + ", redirectUri=" + this.redirectUri + ", scopes=" + this.scopes + ", claims=" + this.claims + ", codeVerifier=" + this.codeVerifier + ", extraHttpHeaders=" + this.extraHttpHeaders + ", extraQueryParameters=" + this.extraQueryParameters + ", tenant=" + this.tenant + ")";
        }
    }
}
