// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;

/**
 * Object containing parameters for managed identity flow. Can be used as parameter to
 * {@link ManagedIdentityApplication#acquireTokenForManagedIdentity(ManagedIdentityParameters)}
 */
public class ManagedIdentityParameters implements IAcquireTokenParameters {

    String resource;
    boolean forceRefresh;
    String claims;
    private String tokenToRevoke;    
    private ManagedIdentityParameters(String resource, boolean forceRefresh, String claims, String tokenToRevoke) {
        this.resource = resource;
        this.forceRefresh = forceRefresh;
        this.claims = claims;
        this.tokenToRevoke = tokenToRevoke;
    }

    @Override
    public Set<String> scopes() {
        return null;
    }

    @Override
    public ClaimsRequest claims() {
        if (claims == null || claims.isEmpty()) {
            return null;
        }

        try {
            return ClaimsRequest.formatAsClaimsRequest(claims);
        } catch (Exception ex) {
            // Log the exception if the claims JSON is invalid
            throw new MsalClientException("Failed to parse claims JSON: " + ex.getMessage(),
                                         AuthenticationErrorCode.INVALID_JSON);
        }
    }

    @Override
    public Map<String, String> extraHttpHeaders() {
        return null;
    }

    @Override
    public String tenant() {
        return Constants.MANAGED_IDENTITY_DEFAULT_TENTANT;
    }

    @Override
    public Map<String, String> extraQueryParameters() {
        return null;
    }

    private static ManagedIdentityParametersBuilder builder() {
        return new ManagedIdentityParametersBuilder();
    }

    /**
     * Builder for {@link ManagedIdentityParameters}
     * @param resource scopes application is requesting access to
     * @return builder that can be used to construct ManagedIdentityParameters
     */
    public static ManagedIdentityParametersBuilder builder(String resource) {
        return builder().resource(resource);
    }

    public boolean forceRefresh() {
        return this.forceRefresh;
    }

    public String resource() {
        return this.resource;
    }

    /**
     * Gets the token to be revoked in supported Managed Identity environments.
     * @return the token to be revoked
     */
    public String getTokenToRevoke() {
        return this.tokenToRevoke;
    }

    public static class ManagedIdentityParametersBuilder {
        private String resource;
        private boolean forceRefresh;
        private String claims;
        private String tokenToRevoke;

        ManagedIdentityParametersBuilder() {
        }

        public ManagedIdentityParametersBuilder resource(String resource) {
            this.resource = resource;
            return this;
        }

        public ManagedIdentityParametersBuilder forceRefresh(boolean forceRefresh) {
            this.forceRefresh = forceRefresh;
            return this;
        }

        /**
         * Instructs the SDK to bypass any token caches and to request new tokens with an additional claims challenge.
         * The claims challenge string is opaque to applications and should not be parsed.
         * The claims challenge string is issued either by the STS as part of an error response or by the resource,
         * as part of an HTTP 401 response, in the WWW-Authenticate header.
         * For more details see https://learn.microsoft.com/entra/identity-platform/app-resilience-continuous-access-evaluation?tabs=dotnet
         *
         * @param claims a valid JSON string representing additional claims
         * @return this builder instance
         */
        public ManagedIdentityParametersBuilder claims(String claims) {
            ParameterValidationUtils.validateNotBlank("claims", claims);

            this.claims = claims;
            return this;
        }
        
        /**
         * Specifies a token to be revoked in supported Managed Identity environments (App Service, Service Fabric).
         * The token will be converted to a SHA256 hash for revocation to avoid transmitting the original token.
         * 
         * @param tokenToRevoke The access token to revoke
         * @return this builder instance
         */
        public ManagedIdentityParametersBuilder tokenToRevoke(String tokenToRevoke) {
            ParameterValidationUtils.validateNotBlank("tokenToRevoke", tokenToRevoke);
            
            this.tokenToRevoke = tokenToRevoke;
            return this;
        }

        public ManagedIdentityParameters build() {
            return new ManagedIdentityParameters(this.resource, this.forceRefresh, this.claims, this.tokenToRevoke);
        }

        public String toString() {
            return "ManagedIdentityParameters.ManagedIdentityParametersBuilder(resource=" + this.resource + ", forceRefresh=" + this.forceRefresh + ")";
        }
    }
}
