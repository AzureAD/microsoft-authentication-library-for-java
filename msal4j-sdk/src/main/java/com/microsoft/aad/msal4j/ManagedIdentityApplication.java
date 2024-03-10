// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Getter;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Class to be used to acquire tokens for managed identity.
 * For details see {@link IManagedIdentityApplication}
 * <p>
 * Conditionally thread-safe
 */
public class ManagedIdentityApplication extends AbstractApplicationBase implements IManagedIdentityApplication {

    @Getter
    private final ManagedIdentityId managedIdentityId;

    @Getter
    static TokenCache sharedTokenCache = new TokenCache();

    private ManagedIdentityApplication(Builder builder) {
        super(builder);
        super.tokenCache = sharedTokenCache;
        super.serviceBundle = new ServiceBundle(
                builder.executorService,
                builder.httpClient == null ?
                        new DefaultHttpClientManagedIdentity(builder.proxy, builder.sslSocketFactory, builder.connectTimeoutForDefaultHttpClient, builder.readTimeoutForDefaultHttpClient) :
                        builder.httpClient,
                new TelemetryManager(telemetryConsumer, builder.onlySendFailureTelemetry));

        log = LoggerFactory.getLogger(ManagedIdentityApplication.class);

        this.managedIdentityId = builder.managedIdentityId;
        this.tenant = Constants.MANAGED_IDENTITY_DEFAULT_TENTANT;
    }

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

        private Builder(ManagedIdentityId managedIdentityId) {
            super(managedIdentityId.getIdType() == ManagedIdentityIdType.SYSTEM_ASSIGNED ?
                    "system_assigned_managed_identity" : managedIdentityId.getUserAssignedId());

            this.managedIdentityId = managedIdentityId;
        }

        public Builder resource(String resource) {
            this.resource = resource;
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
}