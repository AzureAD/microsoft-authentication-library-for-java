// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Class to be used to acquire tokens for managed identity.
 * For details see {@link IManagedIdentityApplication}
 * <p>
 * Conditionally thread-safe
 */
public class ManagedIdentityApplication extends AbstractApplicationBase implements IManagedIdentityApplication {

    private final ManagedIdentityId managedIdentityId;
    private List<String> clientCapabilities;
    private volatile CompletableFuture<ManagedIdentityCapabilities>
            managedIdentityCapabilities;
    private volatile Boolean managedIdentityCapabilitiesImdsV2Disabled;
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
    public synchronized CompletableFuture<ManagedIdentityCapabilities>
    getManagedIdentityCapabilities() {
        boolean imdsV2Disabled =
                ManagedIdentityEnvironment.isImdsV2Disabled();
        if (managedIdentityCapabilities != null) {
            if (managedIdentityCapabilitiesImdsV2Disabled != null
                    && managedIdentityCapabilitiesImdsV2Disabled
                    == imdsV2Disabled
                    && (imdsV2Disabled
                    || !shouldRetryCompletedDiscovery(
                            managedIdentityCapabilities))) {
                return managedIdentityCapabilities;
            }
            managedIdentityCapabilities = null;
        }
        Supplier<ManagedIdentityCapabilities> supplier =
                () -> detectManagedIdentityCapabilities(imdsV2Disabled);
        ExecutorService executorService =
                serviceBundle().getExecutorService();
        CompletableFuture<ManagedIdentityCapabilities> discovery =
                executorService == null
                ? CompletableFuture.supplyAsync(supplier)
                : CompletableFuture.supplyAsync(supplier, executorService);
        managedIdentityCapabilities = discovery;
        managedIdentityCapabilitiesImdsV2Disabled = imdsV2Disabled;
        discovery.whenComplete((capabilities, error) -> {
            if (error != null
                    || (!imdsV2Disabled
                    && shouldRetryCapabilityDiscovery(capabilities))) {
                synchronized (ManagedIdentityApplication.this) {
                    if (managedIdentityCapabilities == discovery) {
                        managedIdentityCapabilities = null;
                        managedIdentityCapabilitiesImdsV2Disabled = null;
                    }
                }
            }
        });
        return discovery;
    }

    private static boolean shouldRetryCompletedDiscovery(
            CompletableFuture<ManagedIdentityCapabilities> discovery) {
        if (!discovery.isDone()) {
            return false;
        }
        try {
            return shouldRetryCapabilityDiscovery(discovery.getNow(null));
        } catch (CompletionException | CancellationException e) {
            return true;
        }
    }

    private static boolean shouldRetryCapabilityDiscovery(
            ManagedIdentityCapabilities capabilities) {
        if (capabilities == null
                || capabilities.isMtlsPopSupportedByHost()) {
            return false;
        }
        return capabilities.source() == ManagedIdentitySourceType.IMDS
                || capabilities.source()
                == ManagedIdentitySourceType.DEFAULT_TO_IMDS;
    }

    private ManagedIdentityCapabilities detectManagedIdentityCapabilities(
            boolean imdsV2Disabled) {
        ManagedIdentitySourceType source =
                ManagedIdentityClient.getManagedIdentitySource();
        if (source != ManagedIdentitySourceType.DEFAULT_TO_IMDS
                && source != ManagedIdentitySourceType.IMDS) {
            return new ManagedIdentityCapabilities(
                    source,
                    MtlsBindingStrength.NONE,
                    "Managed identity mTLS PoP is supported only on the IMDS v2 VM/VMSS source.");
        }
        if (imdsV2Disabled) {
            return new ManagedIdentityCapabilities(
                    source,
                    MtlsBindingStrength.NONE,
                    Constants.MSAL_MI_DISABLE_IMDS_V2
                            + " disables IMDS v2 for this process.");
        }

        ManagedIdentityParameters parameters = ManagedIdentityParameters
                .builder("https://management.azure.com")
                .build();
        RequestContext requestContext = new RequestContext(
                this,
                managedIdentityId.getIdType()
                        == ManagedIdentityIdType.SYSTEM_ASSIGNED
                        ? PublicApi.ACQUIRE_TOKEN_BY_SYSTEM_ASSIGNED_MANAGED_IDENTITY
                        : PublicApi.ACQUIRE_TOKEN_BY_USER_ASSIGNED_MANAGED_IDENTITY,
                parameters);

        try {
            IManagedIdentityMtlsProvider provider =
                    ManagedIdentityMtlsProviderLoader.load();
            ManagedIdentityMtlsRequest request =
                    AcquireTokenByManagedIdentitySupplier
                            .createMtlsProviderRequest(
                                    this,
                                    requestContext,
                                    false);
            MtlsBindingStrength strength =
                    provider.getMaxSupportedBindingStrength(request);
            return new ManagedIdentityCapabilities(
                    strength == MtlsBindingStrength.NONE
                            ? source
                            : ManagedIdentitySourceType.IMDS,
                    strength,
                    strength == MtlsBindingStrength.NONE
                            ? "The configured mTLS provider did not report a supported binding."
                            : null);
        } catch (RuntimeException e) {
            return new ManagedIdentityCapabilities(
                    source,
                    MtlsBindingStrength.NONE,
                    e.getMessage());
        }
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

        private ManagedIdentityId managedIdentityId;
        private List<String> clientCapabilities;

        private Builder(ManagedIdentityId managedIdentityId) {
            super(managedIdentityId.getIdType() == ManagedIdentityIdType.SYSTEM_ASSIGNED ?
                    "system_assigned_managed_identity" : managedIdentityId.getUserAssignedId());

            this.managedIdentityId = managedIdentityId;
        }

        /**
         * @deprecated This method has no effect as the resource field is not used in the ManagedIdentityApplication itself.
         * Use {@link ManagedIdentityParameters#builder(String)} to set the resource when calling
         * {@link ManagedIdentityApplication#acquireTokenForManagedIdentity(ManagedIdentityParameters)}.
         *
         * @param resource Resource to access (unused)
         * @return instance of Builder of ManagedIdentityApplication
         */
        @Deprecated
        public Builder resource(String resource) {
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