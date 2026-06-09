// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for refresh token request. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(RefreshTokenParameters)} or to
 * {@link ConfidentialClientApplication#acquireToken(RefreshTokenParameters)}
 * <p>
 * RefreshTokenParameters should only be used for migration scenarios (when moving from ADAL to
 * MSAL). To acquire tokens silently, use {@link AbstractClientApplicationBase#acquireTokenSilently(SilentParameters)}
 */
public class RefreshTokenParameters implements IAcquireTokenParameters {

    private Set<String> scopes;
    private String refreshToken;
    private ClaimsRequest claims;
    private Map<String, String> extraHttpHeaders;
    private Map<String, String> extraQueryParameters;
    private String tenant;

    private RefreshTokenParameters(Set<String> scopes, String refreshToken, ClaimsRequest claims, Map<String, String> extraHttpHeaders, Map<String, String> extraQueryParameters, String tenant) {
        this.scopes = scopes;
        this.refreshToken = refreshToken;
        this.claims = claims;
        this.extraHttpHeaders = extraHttpHeaders;
        this.extraQueryParameters = extraQueryParameters;
        this.tenant = tenant;
    }

    private static RefreshTokenParametersBuilder builder() {

        return new RefreshTokenParametersBuilder();
    }

    /**
     * Builder for {@link RefreshTokenParameters}
     *
     * @param scopes       scopes application is requesting access to
     * @param refreshToken refresh token received form the STS
     * @return builder object that can be used to construct {@link RefreshTokenParameters}
     */
    public static RefreshTokenParametersBuilder builder(Set<String> scopes, String refreshToken) {

        validateNotBlank("refreshToken", refreshToken);

        return builder()
                .scopes(scopes)
                .refreshToken(refreshToken);
    }

    public Set<String> scopes() {
        return this.scopes;
    }

    public String refreshToken() {
        return this.refreshToken;
    }

    public ClaimsRequest claims() {
        return this.claims;
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

    public static class RefreshTokenParametersBuilder {
        private Set<String> scopes;
        private String refreshToken;
        private ClaimsRequest claims;
        private Map<String, String> extraHttpHeaders;
        private Map<String, String> extraQueryParameters;
        private String tenant;

        RefreshTokenParametersBuilder() {
        }

        /**
         * Scopes the application is requesting access to
         * <p>
         * Cannot be null.
         */
        public RefreshTokenParametersBuilder scopes(Set<String> scopes) {
            validateNotNull("scopes", scopes);

            this.scopes = scopes;
            return this;
        }

        /**
         * Refresh token received from the STS
         * <p>
         * Cannot be null.
         */
        public RefreshTokenParametersBuilder refreshToken(String refreshToken) {
            validateNotNull("refreshToken", refreshToken);

            this.refreshToken = refreshToken;
            return this;
        }

        /**
         * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
         */
        public RefreshTokenParametersBuilder claims(ClaimsRequest claims) {
            this.claims = claims;
            return this;
        }

        /**
         * Adds additional headers to the token request
         */
        public RefreshTokenParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
            this.extraHttpHeaders = extraHttpHeaders;
            return this;
        }

        /**
         * Adds additional parameters to the token request
         * @deprecated Not recommended for production scenarios. It will be removed in a future release, and the behavior may be replaced by a new API.
         */
        @Deprecated
        public RefreshTokenParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
            this.extraQueryParameters = extraQueryParameters;
            return this;
        }

        /**
         * Overrides the tenant value in the authority URL for this request
         */
        public RefreshTokenParametersBuilder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        public RefreshTokenParameters build() {
            return new RefreshTokenParameters(this.scopes, this.refreshToken, this.claims, this.extraHttpHeaders, this.extraQueryParameters, this.tenant);
        }

        public String toString() {
            return "RefreshTokenParameters.RefreshTokenParametersBuilder(scopes=" + this.scopes + ", refreshToken=" + this.refreshToken + ", claims=" + this.claims + ", extraHttpHeaders=" + this.extraHttpHeaders + ", extraQueryParameters=" + this.extraQueryParameters + ", tenant=" + this.tenant + ")";
        }
    }
}
