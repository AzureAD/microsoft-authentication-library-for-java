// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for client credential flow. Can be used as parameter to
 * {@link ConfidentialClientApplication#acquireToken(ClientCredentialParameters)}
 */
public class ClientCredentialParameters implements IAcquireTokenParameters {

    private Set<String> scopes;

    private Boolean skipCache = false;

    private ClaimsRequest claims;

    private Map<String, String> extraHttpHeaders;

    private Map<String, String> extraQueryParameters;

    private String tenant;

    private IClientCredential clientCredential;

    private boolean mtlsProofOfPossession;

    private ClientCredentialParameters(Set<String> scopes, Boolean skipCache, ClaimsRequest claims, Map<String, String> extraHttpHeaders, Map<String, String> extraQueryParameters, String tenant, IClientCredential clientCredential, boolean mtlsProofOfPossession) {
        this.scopes = scopes;
        this.skipCache = skipCache;
        this.claims = claims;
        this.extraHttpHeaders = extraHttpHeaders;
        this.extraQueryParameters = extraQueryParameters;
        this.tenant = tenant;
        this.clientCredential = clientCredential;
        this.mtlsProofOfPossession = mtlsProofOfPossession;
    }

    private static ClientCredentialParametersBuilder builder() {

        return new ClientCredentialParametersBuilder();
    }

    /**
     * Builder for {@link ClientCredentialParameters}
     *
     * @param scopes scopes application is requesting access to
     * @return builder that can be used to construct ClientCredentialParameters
     */
    public static ClientCredentialParametersBuilder builder(Set<String> scopes) {
        validateNotNull("scopes", scopes);

        return builder().scopes(scopes);
    }

    public Set<String> scopes() {
        return this.scopes;
    }

    public Boolean skipCache() {
        return this.skipCache;
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

    public IClientCredential clientCredential() {
        return this.clientCredential;
    }

    /**
     * Whether to request an mTLS Proof-of-Possession token instead of a standard Bearer token.
     *
     * <p>When {@code true}, the token request is sent to the {@code mtlsauth.*} endpoint over
     * a mutual-TLS connection using the application's {@link ClientCertificate} credential.
     * The response token type will be {@code "mtls_pop"} and the access token will contain a
     * {@code cnf.x5t#S256} claim binding it to the certificate.</p>
     *
     * @return {@code true} if mTLS PoP is requested
     */
    public boolean mtlsProofOfPossession() {
        return this.mtlsProofOfPossession;
    }

    public static class ClientCredentialParametersBuilder {
        private Set<String> scopes;
        private Boolean skipCache = false;
        private ClaimsRequest claims;
        private Map<String, String> extraHttpHeaders;
        private Map<String, String> extraQueryParameters;
        private String tenant;
        private IClientCredential clientCredential;
        private boolean mtlsProofOfPossession;

        ClientCredentialParametersBuilder() {
        }

        /**
         * Scopes application is requesting access to.
         * <p>
         * Cannot be null.
         */
        public ClientCredentialParametersBuilder scopes(Set<String> scopes) {
            validateNotNull("scopes", scopes);

            this.scopes = scopes;
            return this;
        }

        /**
         * Indicates whether the request should skip looking into the token cache. Be default it is
         * set to false.
         */
        public ClientCredentialParametersBuilder skipCache(Boolean skipCache) {
            this.skipCache = skipCache;
            return this;
        }

        /**
         * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
         */
        public ClientCredentialParametersBuilder claims(ClaimsRequest claims) {
            this.claims = claims;
            return this;
        }

        /**
         * Adds additional headers to the token request
         */
        public ClientCredentialParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
            this.extraHttpHeaders = extraHttpHeaders;
            return this;
        }

        /**
         * Adds additional query parameters to the token request
         * @deprecated Not recommended for production scenarios. It will be removed in a future release, and the behavior may be replaced by a new API.
         */
        @Deprecated
        public ClientCredentialParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
            this.extraQueryParameters = extraQueryParameters;
            return this;
        }

        /**
         * Overrides the tenant value in the authority URL for this request
         */
        public ClientCredentialParametersBuilder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        /**
         * Overrides the client credentials for this request
         */
        public ClientCredentialParametersBuilder clientCredential(IClientCredential clientCredential) {
            this.clientCredential = clientCredential;
            return this;
        }

        /**
         * Requests an mTLS Proof-of-Possession token instead of a standard Bearer token.
         *
         * <p>Requires that the application was created with a {@link ClientCertificate} credential,
         * the authority is a tenanted AAD authority (not {@code /common} or {@code /organizations}),
         * and an Azure region is configured via
         * {@link ConfidentialClientApplication.Builder#azureRegion(String)} or
         * {@link ConfidentialClientApplication.Builder#autoDetectRegion(boolean)}.</p>
         */
        public ClientCredentialParametersBuilder withMtlsProofOfPossession() {
            this.mtlsProofOfPossession = true;
            return this;
        }

        public ClientCredentialParameters build() {
            return new ClientCredentialParameters(this.scopes, this.skipCache, this.claims, this.extraHttpHeaders, this.extraQueryParameters, this.tenant, this.clientCredential, this.mtlsProofOfPossession);
        }

        public String toString() {
            return "ClientCredentialParameters.ClientCredentialParametersBuilder(scopes=" + this.scopes + ", skipCache=" + this.skipCache + ", claims=" + this.claims + ", extraHttpHeaders=" + this.extraHttpHeaders + ", extraQueryParameters=" + this.extraQueryParameters + ", tenant=" + this.tenant + ", clientCredential=" + this.clientCredential + ", mtlsProofOfPossession=" + this.mtlsProofOfPossession + ")";
        }
    }
}
