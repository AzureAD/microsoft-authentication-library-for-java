// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for the composite agent token acquisition flow.
 * This orchestrates the full three-leg FMI/FIC token exchange: the developer passes
 * scopes and an {@link AgentIdentity}, and MSAL handles Legs 1-3 internally.
 * <p>
 * Can be used as parameter to
 * {@link ConfidentialClientApplication#acquireTokenForAgent(AcquireTokenForAgentParameters)}
 */
public class AcquireTokenForAgentParameters implements IAcquireTokenParameters {

    private Set<String> scopes;
    private AgentIdentity agentIdentity;
    private boolean forceRefresh;
    private ClaimsRequest claims;
    private Map<String, String> extraHttpHeaders;
    private Map<String, String> extraQueryParameters;
    private String tenant;

    private AcquireTokenForAgentParameters(
            Set<String> scopes,
            AgentIdentity agentIdentity,
            boolean forceRefresh,
            ClaimsRequest claims,
            Map<String, String> extraHttpHeaders,
            Map<String, String> extraQueryParameters,
            String tenant) {
        this.scopes = scopes;
        this.agentIdentity = agentIdentity;
        this.forceRefresh = forceRefresh;
        this.claims = claims;
        this.extraHttpHeaders = extraHttpHeaders;
        this.extraQueryParameters = extraQueryParameters;
        this.tenant = tenant;
    }

    /**
     * Builder for {@link AcquireTokenForAgentParameters}.
     *
     * @param scopes        scopes application is requesting access to
     * @param agentIdentity the identity of the agent and (optionally) the target user
     * @return builder that can be used to construct AcquireTokenForAgentParameters
     */
    public static AcquireTokenForAgentParametersBuilder builder(
            Set<String> scopes, AgentIdentity agentIdentity) {
        validateNotNull("scopes", scopes);
        validateNotNull("agentIdentity", agentIdentity);

        return new AcquireTokenForAgentParametersBuilder()
                .scopes(scopes)
                .agentIdentity(agentIdentity);
    }

    public Set<String> scopes() {
        return this.scopes;
    }

    public AgentIdentity agentIdentity() {
        return this.agentIdentity;
    }

    public boolean forceRefresh() {
        return this.forceRefresh;
    }

    public ClaimsRequest claims() {
        return this.claims;
    }

    public Map<String, String> extraHttpHeaders() {
        return this.extraHttpHeaders;
    }

    public Map<String, String> extraQueryParameters() {
        return this.extraQueryParameters;
    }

    public String tenant() {
        return this.tenant;
    }

    public static class AcquireTokenForAgentParametersBuilder {
        private Set<String> scopes;
        private AgentIdentity agentIdentity;
        private boolean forceRefresh;
        private ClaimsRequest claims;
        private Map<String, String> extraHttpHeaders;
        private Map<String, String> extraQueryParameters;
        private String tenant;

        AcquireTokenForAgentParametersBuilder() {
        }

        AcquireTokenForAgentParametersBuilder scopes(Set<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        AcquireTokenForAgentParametersBuilder agentIdentity(AgentIdentity agentIdentity) {
            this.agentIdentity = agentIdentity;
            return this;
        }

        /**
         * If true, the request will ignore cached access tokens on read, but will still write
         * them to the cache once obtained from the identity provider. The default is false.
         *
         * @param forceRefresh whether to bypass the user token cache
         * @return this builder
         */
        public AcquireTokenForAgentParametersBuilder forceRefresh(boolean forceRefresh) {
            this.forceRefresh = forceRefresh;
            return this;
        }

        /**
         * Claims to be requested through the OIDC claims request parameter, allowing requests
         * for standard and custom claims.
         *
         * @param claims {@link ClaimsRequest}
         * @return this builder
         */
        public AcquireTokenForAgentParametersBuilder claims(ClaimsRequest claims) {
            this.claims = claims;
            return this;
        }

        /**
         * Adds additional headers to the token request.
         *
         * @param extraHttpHeaders headers to include
         * @return this builder
         */
        public AcquireTokenForAgentParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
            this.extraHttpHeaders = extraHttpHeaders;
            return this;
        }

        /**
         * Adds additional query parameters to the token request.
         *
         * @param extraQueryParameters query parameters to include
         * @return this builder
         */
        public AcquireTokenForAgentParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
            this.extraQueryParameters = extraQueryParameters;
            return this;
        }

        /**
         * Sets the tenant for the request, overriding the application's configured authority.
         *
         * @param tenant tenant ID or domain
         * @return this builder
         */
        public AcquireTokenForAgentParametersBuilder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        public AcquireTokenForAgentParameters build() {
            return new AcquireTokenForAgentParameters(
                    scopes, agentIdentity, forceRefresh, claims,
                    extraHttpHeaders, extraQueryParameters, tenant);
        }
    }
}
