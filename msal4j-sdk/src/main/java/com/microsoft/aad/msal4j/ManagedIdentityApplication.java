// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Class to be used to acquire tokens for managed identity.
 * For details see {@link IManagedIdentityApplication}
 * <p>
 * Conditionally thread-safe
 */
public class ManagedIdentityApplication extends AbstractApplicationBase implements IManagedIdentityApplication {

    private final ManagedIdentityId managedIdentityId;
    private List<String> clientCapabilities;
    static TokenCache sharedTokenCache = new TokenCache();

    //Deprecated the field in favor of the static getManagedIdentitySource method
    @Deprecated
    ManagedIdentitySourceType managedIdentitySource = ManagedIdentityClient.getManagedIdentitySource();

    static IEnvironmentVariables environmentVariables;

    static void setEnvironmentVariables(IEnvironmentVariables environmentVariables) {
        ManagedIdentityApplication.environmentVariables = environmentVariables;
    }

    private ManagedIdentityApplication(Builder builder) {
        super(builder);

        super.tokenCache = sharedTokenCache;
        super.serviceBundle = new ServiceBundle(
                builder.executorService,
                new TelemetryManager(telemetryConsumer, builder.onlySendFailureTelemetry),
                new HttpHelper(this, new ManagedIdentityRetryPolicy())
        );
        log = LoggerFactory.getLogger(ManagedIdentityApplication.class);

        this.managedIdentityId = builder.managedIdentityId;
        this.tenant = Constants.MANAGED_IDENTITY_DEFAULT_TENTANT;
        this.clientCapabilities = builder.clientCapabilities;
    }

    public static TokenCache getSharedTokenCache() {
        return ManagedIdentityApplication.sharedTokenCache;
    }

    static IEnvironmentVariables getEnvironmentVariables() {
        return ManagedIdentityApplication.environmentVariables;
    }

    public ManagedIdentityId getManagedIdentityId() {
        return this.managedIdentityId;
    }

    public List<String> getClientCapabilities() { return this.clientCapabilities; }
    
    @Override
    public CompletableFuture<IAuthenticationResult> acquireTokenForManagedIdentity(ManagedIdentityParameters managedIdentityParameters)
            throws Exception {
        RequestContext requestContext = new RequestContext(
                this,
                managedIdentityId.getIdType() == ManagedIdentityIdType.SYSTEM_ASSIGNED ?
                        PublicApi.ACQUIRE_TOKEN_BY_SYSTEM_ASSIGNED_MANAGED_IDENTITY :
                        PublicApi.ACQUIRE_TOKEN_BY_USER_ASSIGNED_MANAGED_IDENTITY,
                managedIdentityParameters);

        ManagedIdentityRequest managedIdentityRequest = new ManagedIdentityRequest(this, requestContext);

        return this.executeRequest(managedIdentityRequest);
    }

    /**
     * Creates instance of Builder of ManagedIdentityApplication
     *
     * @param managedIdentityId ManagedIdentityId to specify if System Assigned or User Assigned
     *                          and provide id if it is user assigned.
     * @return instance of Builder of ManagedIdentityApplication
     */
    public static Builder builder(ManagedIdentityId managedIdentityId) {
        return new Builder(managedIdentityId);
    }

    public static class Builder extends AbstractApplicationBase.Builder<Builder> {

        private String resource;
        private ManagedIdentityId managedIdentityId;
        private List<String> clientCapabilities;

        private Builder(ManagedIdentityId managedIdentityId) {
            super(managedIdentityId.getIdType() == ManagedIdentityIdType.SYSTEM_ASSIGNED ?
                    "system_assigned_managed_identity" : managedIdentityId.getUserAssignedId());

            this.managedIdentityId = managedIdentityId;
        }

        public Builder resource(String resource) {
            this.resource = resource;
            return self();
        }

        /**
         * Informs the token issuer that the application is able to perform complex authentication actions.
         * For example, "cp1" means that the application is able to perform conditional access evaluation,
         * because the application has been set up to parse WWW-Authenticate headers associated with a 401 response from the protected APIs,
         * and to retry the request with claims API.
         * 
         * @param clientCapabilities a list of capabilities (e.g., ["cp1"]) recognized by the token service.
         * @return instance of Builder of ManagedIdentityApplication.
         */
        public Builder clientCapabilities(List<String> clientCapabilities) {
            this.clientCapabilities = clientCapabilities;
            return self();
        }

        @Override
        public ManagedIdentityApplication build() {
            return new ManagedIdentityApplication(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

    /**
     * Returns a {@link ManagedIdentitySourceType} value, which is based primarily on environment variables set on the system.
     *
     * @return ManagedIdentitySourceType enum for source type
     */
    public static ManagedIdentitySourceType getManagedIdentitySource() {
       return ManagedIdentityClient.getManagedIdentitySource();
    }
}