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

    private ManagedIdentityParameters(String resource, boolean forceRefresh, String claims) {
        this.resource = resource;
        this.forceRefresh = forceRefresh;
        this.claims = claims;
    }

    @Override
    public Set<String> scopes() {
        return null;
    }

    @Override
    public ClaimsRequest claims() {
        return (claims != null) ? ClaimsRequest.formatAsClaimsRequest(claims) : null;
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

    public static class ManagedIdentityParametersBuilder {
        private String resource;
        private boolean forceRefresh;
        private String claims;

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

        public ManagedIdentityParametersBuilder claims(String claims) {
            this.claims = claims;
            return this;
        }

        public ManagedIdentityParameters build() {
            return new ManagedIdentityParameters(this.resource, this.forceRefresh, this.claims);
        }

        public String toString() {
            return "ManagedIdentityParameters.ManagedIdentityParametersBuilder(resource=" + this.resource + ", forceRefresh=" + this.forceRefresh + ")";
        }
    }
}
