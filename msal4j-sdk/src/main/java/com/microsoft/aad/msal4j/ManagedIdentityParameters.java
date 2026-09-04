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
    boolean requestOverMtls;
    IManagedIdentityMtlsProvider mtlsProvider;
    MtlsBindingStrength minimumBindingStrength;
    
    private ManagedIdentityParameters(
            String resource,
            boolean forceRefresh,
            String claims,
            boolean mtlsProofOfPossession,
            boolean requestOverMtls,
            IManagedIdentityMtlsProvider mtlsProvider,
            MtlsBindingStrength minimumBindingStrength) {
        this.resource = resource;
        this.forceRefresh = forceRefresh;
        this.claims = claims;
        this.mtlsProofOfPossession = mtlsProofOfPossession;
        this.requestOverMtls = requestOverMtls;
        this.mtlsProvider = mtlsProvider;
        this.minimumBindingStrength = minimumBindingStrength;
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

    public boolean requestOverMtls() {
        return requestOverMtls;
    }

    boolean attestationSupport() {
        return mtlsProvider != null && mtlsProvider.isAttestationEnabled();
    }

    IManagedIdentityMtlsProvider mtlsProvider() {
        return mtlsProvider;
    }

    public MtlsBindingStrength minimumBindingStrength() {
        return minimumBindingStrength;
    }

    @Override
    public String computeExtCacheKeyHash() {
        return "";
    }

    String computeMtlsExtCacheKeyHash(String bindingKeyId) {
        if (!mtlsProofOfPossession && !requestOverMtls) {
            return "";
        }
        SortedMap<String, String> components = new TreeMap<>();
        if (mtlsProofOfPossession) {
            if (StringHelper.isBlank(bindingKeyId)) {
                return "";
            }
            components.put("token_type", "mtls_pop");
            components.put("key_id", bindingKeyId);
        } else {
            components.put("token_type", "mtls_bearer");
        }
        components.put("attestation", attestationSupport() ? "att1" : "att0");
        if (minimumBindingStrength != MtlsBindingStrength.NONE) {
            components.put("min_strength", minimumBindingStrength.name());
        }
        return StringHelper.computeExtCacheKeyHash(components);
    }

    public static class ManagedIdentityParametersBuilder {
        private String resource;
        private boolean forceRefresh;
        private String claims;
        private boolean mtlsProofOfPossession;
        private boolean requestOverMtls;
        private IManagedIdentityMtlsProvider mtlsProvider;
        private MtlsBindingStrength minimumBindingStrength =
                MtlsBindingStrength.NONE;

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
            return withMtlsProofOfPossession(MtlsPopOptions.builder().build());
        }

        /**
         * Requests mTLS PoP and requires the configured minimum binding strength.
         */
        public ManagedIdentityParametersBuilder withMtlsProofOfPossession(
                MtlsPopOptions options) {
            if (options == null) {
                throw new NullPointerException("options");
            }
            if (requestOverMtls) {
                throw new IllegalStateException(
                        "withMtlsProofOfPossession() and withRequestOverMtls() "
                                + "are mutually exclusive.");
            }
            this.mtlsProofOfPossession = true;
            this.minimumBindingStrength = options.minimumBindingStrength();
            return this;
        }

        /**
         * Uses the managed identity v2 KeyGuard mTLS flow to request an ordinary
         * bearer token. The binding certificate authenticates the token request,
         * but the returned token is not certificate-bound.
         */
        public ManagedIdentityParametersBuilder withRequestOverMtls() {
            if (mtlsProofOfPossession) {
                throw new IllegalStateException(
                        "withMtlsProofOfPossession() and withRequestOverMtls() "
                                + "are mutually exclusive.");
            }
            this.requestOverMtls = true;
            return this;
        }

        /**
         * Configures the optional provider that supplies the managed identity mTLS binding.
         *
         * <p>This is an extension integration point. Applications enabling Microsoft
         * KeyGuard attestation should use the public helper from the optional
         * {@code msal4j-key-attestation} artifact instead of calling this method directly.</p>
         */
        public ManagedIdentityParametersBuilder withManagedIdentityMtlsProvider(
                IManagedIdentityMtlsProvider mtlsProvider) {
            if (mtlsProvider == null) {
                throw new NullPointerException("mtlsProvider");
            }
            this.mtlsProvider = mtlsProvider;
            return this;
        }

        public ManagedIdentityParameters build() {
            if (mtlsProvider != null
                    && mtlsProvider.isAttestationEnabled()
                    && !mtlsProofOfPossession
                    && !requestOverMtls) {
                throw new IllegalArgumentException(
                        "Attestation support requires managed identity mTLS.");
            }
            return new ManagedIdentityParameters(
                    this.resource,
                    this.forceRefresh,
                    this.claims,
                    this.mtlsProofOfPossession,
                    this.requestOverMtls,
                    this.mtlsProvider,
                    this.minimumBindingStrength);
        }

        public String toString() {
            return "ManagedIdentityParameters.ManagedIdentityParametersBuilder(resource=" + this.resource
                    + ", forceRefresh=" + this.forceRefresh
                    + ", mtlsProofOfPossession=" + this.mtlsProofOfPossession
                    + ", requestOverMtls=" + this.requestOverMtls
                    + ", attestationSupport="
                    + (this.mtlsProvider != null
                    && this.mtlsProvider.isAttestationEnabled())
                    + ", minimumBindingStrength=" + this.minimumBindingStrength + ")";
        }
    }
}
