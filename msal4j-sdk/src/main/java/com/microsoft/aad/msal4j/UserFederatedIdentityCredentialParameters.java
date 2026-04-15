// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for the User Federated Identity Credential (user_fic) flow.
 * This is used for Leg 3 of the agent identity protocol, where a federated identity credential
 * (obtained from Leg 2) is exchanged for a user-scoped token.
 * <p>
 * Can be used as parameter to
 * {@link ConfidentialClientApplication#acquireToken(UserFederatedIdentityCredentialParameters)}
 */
public class UserFederatedIdentityCredentialParameters implements IAcquireTokenParameters {

    private Set<String> scopes;
    private String username;
    private UUID userObjectId;
    private String assertion;
    private boolean forceRefresh;
    private ClaimsRequest claims;
    private Map<String, String> extraHttpHeaders;
    private Map<String, String> extraQueryParameters;
    private String tenant;

    private UserFederatedIdentityCredentialParameters(
            Set<String> scopes,
            String username,
            UUID userObjectId,
            String assertion,
            boolean forceRefresh,
            ClaimsRequest claims,
            Map<String, String> extraHttpHeaders,
            Map<String, String> extraQueryParameters,
            String tenant) {
        this.scopes = scopes;
        this.username = username;
        this.userObjectId = userObjectId;
        this.assertion = assertion;
        this.forceRefresh = forceRefresh;
        this.claims = claims;
        this.extraHttpHeaders = extraHttpHeaders;
        this.extraQueryParameters = extraQueryParameters;
        this.tenant = tenant;
    }

    /**
     * Builder for {@link UserFederatedIdentityCredentialParameters} using a UPN (User Principal Name).
     *
     * @param scopes    scopes application is requesting access to
     * @param username  the UPN of the target user (e.g., "user@contoso.com")
     * @param assertion the federated identity credential assertion (JWT) obtained from Leg 2
     * @return builder that can be used to construct UserFederatedIdentityCredentialParameters
     */
    public static UserFederatedIdentityCredentialParametersBuilder builder(
            Set<String> scopes, String username, String assertion) {
        validateNotNull("scopes", scopes);
        validateNotBlank("username", username);
        validateNotBlank("assertion", assertion);

        return new UserFederatedIdentityCredentialParametersBuilder()
                .scopes(scopes)
                .username(username)
                .assertion(assertion);
    }

    /**
     * Builder for {@link UserFederatedIdentityCredentialParameters} using a user Object ID.
     *
     * @param scopes       scopes application is requesting access to
     * @param userObjectId the Object ID (OID) of the target user
     * @param assertion    the federated identity credential assertion (JWT) obtained from Leg 2
     * @return builder that can be used to construct UserFederatedIdentityCredentialParameters
     */
    public static UserFederatedIdentityCredentialParametersBuilder builder(
            Set<String> scopes, UUID userObjectId, String assertion) {
        validateNotNull("scopes", scopes);
        validateNotNull("userObjectId", userObjectId);
        validateNotBlank("assertion", assertion);

        return new UserFederatedIdentityCredentialParametersBuilder()
                .scopes(scopes)
                .userObjectId(userObjectId)
                .assertion(assertion);
    }

    public Set<String> scopes() {
        return this.scopes;
    }

    /**
     * @return the UPN of the target user, or null if user was identified by Object ID
     */
    public String username() {
        return this.username;
    }

    /**
     * @return the Object ID of the target user, or null if user was identified by UPN
     */
    public UUID userObjectId() {
        return this.userObjectId;
    }

    /**
     * @return the federated identity credential assertion (JWT)
     */
    public String assertion() {
        return this.assertion;
    }

    /**
     * @return whether to bypass the token cache and force a fresh token request
     */
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

    public static class UserFederatedIdentityCredentialParametersBuilder {
        private Set<String> scopes;
        private String username;
        private UUID userObjectId;
        private String assertion;
        private boolean forceRefresh;
        private ClaimsRequest claims;
        private Map<String, String> extraHttpHeaders;
        private Map<String, String> extraQueryParameters;
        private String tenant;

        UserFederatedIdentityCredentialParametersBuilder() {
        }

        UserFederatedIdentityCredentialParametersBuilder scopes(Set<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        UserFederatedIdentityCredentialParametersBuilder username(String username) {
            this.username = username;
            return this;
        }

        UserFederatedIdentityCredentialParametersBuilder userObjectId(UUID userObjectId) {
            this.userObjectId = userObjectId;
            return this;
        }

        UserFederatedIdentityCredentialParametersBuilder assertion(String assertion) {
            this.assertion = assertion;
            return this;
        }

        /**
         * Forces MSAL to refresh the token from the identity provider even if a cached token is available.
         *
         * @param forceRefresh true to bypass the cache; otherwise false. Default is false.
         * @return the builder
         */
        public UserFederatedIdentityCredentialParametersBuilder forceRefresh(boolean forceRefresh) {
            this.forceRefresh = forceRefresh;
            return this;
        }

        /**
         * Claims to be requested through the OIDC claims request parameter.
         */
        public UserFederatedIdentityCredentialParametersBuilder claims(ClaimsRequest claims) {
            this.claims = claims;
            return this;
        }

        /**
         * Adds additional headers to the token request.
         */
        public UserFederatedIdentityCredentialParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
            this.extraHttpHeaders = extraHttpHeaders;
            return this;
        }

        /**
         * Adds additional parameters to the token request.
         */
        public UserFederatedIdentityCredentialParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
            this.extraQueryParameters = extraQueryParameters;
            return this;
        }

        /**
         * Overrides the tenant value in the authority URL for this request.
         */
        public UserFederatedIdentityCredentialParametersBuilder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        public UserFederatedIdentityCredentialParameters build() {
            return new UserFederatedIdentityCredentialParameters(
                    this.scopes, this.username, this.userObjectId, this.assertion,
                    this.forceRefresh, this.claims, this.extraHttpHeaders,
                    this.extraQueryParameters, this.tenant);
        }
    }
}
