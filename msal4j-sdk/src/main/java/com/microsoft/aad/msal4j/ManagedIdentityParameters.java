// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Object containing parameters for managed identity flow. Can be used as parameter to
 * {@link ManagedIdentityApplication#acquireTokenForManagedIdentity(ManagedIdentityParameters)}
 */
public class ManagedIdentityParameters implements IAcquireTokenParameters {

    String resource;
    boolean forceRefresh;
    String claims;
    String revokedTokenHash;
    boolean mtlsProofOfPossession;
    boolean attestationSupport;
    
    private ManagedIdentityParameters(
            String resource,
            boolean forceRefresh,
            String claims,
            boolean mtlsProofOfPossession,
            boolean attestationSupport) {
        this.resource = resource;
        this.forceRefresh = forceRefresh;
        this.claims = claims;
        this.mtlsProofOfPossession = mtlsProofOfPossession;
        this.attestationSupport = attestationSupport;
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

    public String revokedTokenHash() {
        return this.revokedTokenHash;
    }

    public boolean mtlsProofOfPossession() {
        return mtlsProofOfPossession;
    }

    public boolean attestationSupport() {
        return attestationSupport;
    }

    @Override
    public String computeExtCacheKeyHash() {
        return "";
    }

    String computeMtlsExtCacheKeyHash(String bindingKeyId) {
        if (!mtlsProofOfPossession || StringHelper.isBlank(bindingKeyId)) {
            return "";
        }
        SortedMap<String, String> components = new TreeMap<>();
        components.put("token_type", "mtls_pop");
        components.put("key_id", bindingKeyId);
        components.put("attestation", attestationSupport ? "att1" : "att0");
        return StringHelper.computeExtCacheKeyHash(components);
    }

    public static class ManagedIdentityParametersBuilder {
        private String resource;
        private boolean forceRefresh;
        private String claims;
        private boolean mtlsProofOfPossession;
        private boolean attestationSupport;

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
         * Requests the KeyGuard managed identity v2 mTLS PoP flow.
         */
        public ManagedIdentityParametersBuilder withMtlsProofOfPossession() {
            this.mtlsProofOfPossession = true;
            return this;
        }

        /**
         * Requires MAA attestation for the KeyGuard binding key.
         * This option requires {@link #withMtlsProofOfPossession()}.
         */
        public ManagedIdentityParametersBuilder withAttestationSupport() {
            this.attestationSupport = true;
            return this;
        }

        public ManagedIdentityParameters build() {
            if (attestationSupport && !mtlsProofOfPossession) {
                throw new IllegalArgumentException(
                        "Attestation support requires managed identity mTLS PoP.");
            }
            return new ManagedIdentityParameters(
                    this.resource,
                    this.forceRefresh,
                    this.claims,
                    this.mtlsProofOfPossession,
                    this.attestationSupport);
        }

        public String toString() {
            return "ManagedIdentityParameters.ManagedIdentityParametersBuilder(resource=" + this.resource
                    + ", forceRefresh=" + this.forceRefresh
                    + ", mtlsProofOfPossession=" + this.mtlsProofOfPossession
                    + ", attestationSupport=" + this.attestationSupport + ")";
        }
    }
}
