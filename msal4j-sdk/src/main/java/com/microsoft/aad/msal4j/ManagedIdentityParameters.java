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
    String revokedTokenHash;
    boolean mtlsProofOfPossession;
    boolean withAttestationSupport;

    private ManagedIdentityParameters(String resource, boolean forceRefresh, String claims,
                                      boolean mtlsProofOfPossession, boolean withAttestationSupport) {
        this.resource = resource;
        this.forceRefresh = forceRefresh;
        this.claims = claims;
        this.mtlsProofOfPossession = mtlsProofOfPossession;
        this.withAttestationSupport = withAttestationSupport;
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
        return this.mtlsProofOfPossession;
    }

    public boolean withAttestationSupport() {
        return this.withAttestationSupport;
    }

    public static class ManagedIdentityParametersBuilder {
        private String resource;
        private boolean forceRefresh;
        private String claims;
        private boolean mtlsProofOfPossession;
        private boolean withAttestationSupport;

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
         * Requests an mTLS Proof-of-Possession (PoP) token instead of a standard Bearer token.
         * <p>
         * When set to {@code true}, the MSI v2 flow will be used if {@code withAttestationSupport}
         * is also set to {@code true}. This requires Virtualization Based Security (VBS) and
         * KeyGuard to be available on the host (Windows only).
         *
         * @param mtlsProofOfPossession {@code true} to request an mTLS PoP token
         * @return this builder instance
         */
        public ManagedIdentityParametersBuilder mtlsProofOfPossession(boolean mtlsProofOfPossession) {
            this.mtlsProofOfPossession = mtlsProofOfPossession;
            return this;
        }

        /**
         * Enables KeyGuard attestation support for the MSI v2 mTLS PoP flow.
         * <p>
         * When set to {@code true}, the SDK will use the Windows KeyGuard (VBS-backed) RSA key
         * to obtain a hardware-attested certificate from IMDS and use it for mTLS token acquisition.
         * <p>
         * Requires {@code mtlsProofOfPossession=true}. If this flag is set without
         * {@code mtlsProofOfPossession=true}, an exception will be thrown.
         *
         * @param withAttestationSupport {@code true} to require KeyGuard attestation
         * @return this builder instance
         */
        public ManagedIdentityParametersBuilder withAttestationSupport(boolean withAttestationSupport) {
            this.withAttestationSupport = withAttestationSupport;
            return this;
        }

        public ManagedIdentityParameters build() {
            return new ManagedIdentityParameters(this.resource, this.forceRefresh, this.claims,
                    this.mtlsProofOfPossession, this.withAttestationSupport);
        }

        public String toString() {
            return "ManagedIdentityParameters.ManagedIdentityParametersBuilder(resource=" + this.resource
                    + ", forceRefresh=" + this.forceRefresh
                    + ", mtlsProofOfPossession=" + this.mtlsProofOfPossession
                    + ", withAttestationSupport=" + this.withAttestationSupport + ")";
        }
    }
}
