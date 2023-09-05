// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Class to be used to acquire tokens for managed identity.
 * For details see {@link IManagedIdentityApplication}
 * <p>
 * Conditionally thread-safe
 */
public class ManagedIdentityApplication extends AbstractClientApplicationBase implements IManagedIdentityApplication {

    private String resource;

    @Getter
    private ManagedIdentityId managedIdentityId;

    private ManagedIdentityApplication(Builder builder) {
        super(builder);
        this.managedIdentityId = builder.managedIdentityId;
        log = LoggerFactory.getLogger(ManagedIdentityApplication.class);
    }

    /**
     * Creates instance of Builder of ManagedIdentityApplication
     *
     * @param managedIdentityId ManagedIdentityId to specify if it System Assigned or User Assigned
     *                          and provide id if it is user assigned.
     * @return instance of Builder of ManagedIdentityApplication
     */
    public static Builder builder(ManagedIdentityId managedIdentityId) {
        return new Builder(managedIdentityId);
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireTokenForManagedIdentity(ManagedIdentityParameters managedIdentityParameters)
            throws Exception {
        RequestContext requestContext = new RequestContext(
                this,
                managedIdentityId.getIdType() == ManagedIdentityIdType.SystemAssigned ?
                        PublicApi.ACQUIRE_TOKEN_BY_SYSTEM_ASSIGNED_MANAGED_IDENTITY :
                        PublicApi.ACQUIRE_TOKEN_BY_USER_ASSIGNED_MANAGED_IDENTITY,
                managedIdentityParameters);

        ManagedIdentityRequest managedIdentityRequest = new ManagedIdentityRequest(this, requestContext);

        return this.executeRequest(managedIdentityRequest);
    }

    @Override
    protected ClientAuthentication clientAuthentication() {
        return null;
    }

    public static class Builder extends AbstractClientApplicationBase.Builder<Builder> {
        private String resource;

        private ManagedIdentityId managedIdentityId;

        private Builder(ManagedIdentityId managedIdentityId) {
            super(managedIdentityId.getIdType() == ManagedIdentityIdType.SystemAssigned ?
                    "system_assigned_managed_identity" : managedIdentityId.getUserAssignedId());

            this.managedIdentityId = managedIdentityId;
            this.isInstanceDiscoveryEnabled = false;
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