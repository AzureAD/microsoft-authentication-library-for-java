// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Class to be used to acquire tokens for confidential client applications (Web Apps, Web APIs,
 * and daemon applications).
 * For details see {@link IConfidentialClientApplication}
 * <p>
 * Conditionally thread-safe
 */
public class ManagedIdentityApplication extends AbstractClientApplicationBase{

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

    public static Builder builder() {
        return new Builder();
    }

    public CompletableFuture<IAuthenticationResult> acquireTokenForManagedIdentity(ManagedIdentityParameters managedIdentityParameters) throws Exception {

        RequestContext requestContext = new RequestContext(
                this,
                PublicApi.ACQUIRE_TOKEN_BY_MANAGED_IDENTITY,
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
            super();
            this.managedIdentityId = managedIdentityId;
        }

        private Builder() {
            super();

        }

        public ManagedIdentityApplication.Builder resource(String resource){
            this.resource = resource;
            return self();
        }

        @Override
        public ManagedIdentityApplication build() {

            return new ManagedIdentityApplication(this);
        }

        @Override
        protected ManagedIdentityApplication.Builder self() {
            return this;
        }
    }
}