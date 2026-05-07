// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Orchestrates a multi-leg token acquisition for agent scenarios.
 * <p>
 * Two CCA instances are involved:
 * <ol>
 *   <li><b>Blueprint CCA</b> — the developer-created CCA that holds the real credential
 *       (certificate, secret, etc.). It only participates in Leg 1: acquiring an FMI
 *       credential via AcquireTokenForClient + WithFmiPath. Its app token cache stores
 *       the FMI credential.</li>
 *   <li><b>Agent CCA</b> — an internal CCA keyed by the agent's app ID, created and cached
 *       by this class. Its client assertion callback delegates to the Blueprint for FMI
 *       credentials (Leg 1). It handles both Leg 2 (AcquireTokenForClient for the assertion
 *       token) and Leg 3 (AcquireTokenByUserFederatedIdentityCredential for the user token).</li>
 * </ol>
 * <p>
 * Caching behavior:
 * <ul>
 *   <li>The Agent CCA instance is persisted in {@code ConfidentialClientApplication.agentCcaCache}
 *       so that subsequent calls for the same agent reuse its in-memory token caches.</li>
 *   <li>On each call, the agent CCA's user token cache is checked first via AcquireTokenSilent.
 *       If a cached user token is found, it is returned immediately without executing Legs 2-3.</li>
 *   <li>ForceRefresh skips this silent check, but the Leg 1 (FMI credential) and Leg 2
 *       (assertion token) caches are still honored.</li>
 * </ul>
 */
class AcquireTokenForAgentSupplier extends AuthenticationResultSupplier {

    private static final Logger LOG = LoggerFactory.getLogger(AcquireTokenForAgentSupplier.class);
    private static final Set<String> TOKEN_EXCHANGE_SCOPE =
            Collections.singleton("api://AzureADTokenExchange/.default");
    private static final String AGENT_CCA_KEY_PREFIX = "agent_";

    private final AcquireTokenForAgentRequest agentRequest;
    private final ConfidentialClientApplication blueprintApplication;

    AcquireTokenForAgentSupplier(ConfidentialClientApplication clientApplication,
                                 AcquireTokenForAgentRequest agentRequest) {
        super(clientApplication, agentRequest);
        this.agentRequest = agentRequest;
        this.blueprintApplication = clientApplication;
    }

    @Override
    AuthenticationResult execute() throws Exception {
        AgentIdentity agentIdentity = agentRequest.parameters.agentIdentity();
        String agentAppId = agentIdentity.agentApplicationId();
        Set<String> callerScopes = agentRequest.parameters.scopes();

        // Retrieve (or create) the internal Agent CCA for this agent app ID.
        ConfidentialClientApplication agentCca = getOrCreateAgentCca(agentAppId);

        if (!agentIdentity.hasUserIdentifier()) {
            // App-only flow: AcquireTokenForClient has built-in cache-first logic,
            // so no explicit silent pre-check is needed.
            LOG.debug("App-only agent flow for agent app ID: {}", agentAppId);
            return (AuthenticationResult) joinAndUnwrap(
                    agentCca.acquireToken(
                            propagateToClientCredentialParams(
                                    ClientCredentialParameters.builder(callerScopes)).build()));
        }

        // --- User identity flow ---

        // Check the Agent CCA's user token cache for a previously-acquired token for this user.
        // ForceRefresh skips this check so a fresh user token is always obtained from the network.
        if (!agentRequest.parameters.forceRefresh()) {
            AuthenticationResult cachedResult = tryAcquireTokenSilent(agentCca, agentIdentity, callerScopes);
            if (cachedResult != null) {
                LOG.debug("Returning cached user token for agent app ID: {}", agentAppId);
                return cachedResult;
            }
        }

        // Cache miss (or ForceRefresh) — execute Leg 2 + Leg 3.

        // Leg 2: Acquire an assertion token from the Agent CCA's app token cache (or network).
        // The Agent CCA's assertion callback will invoke Leg 1 (FMI credential from Blueprint),
        // but AcquireTokenForClient's built-in cache handles repeat calls.
        LOG.debug("Executing Leg 2 (assertion token) for agent app ID: {}", agentAppId);
        IAuthenticationResult assertionResult = joinAndUnwrap(
                agentCca.acquireToken(
                        propagateToClientCredentialParams(
                                ClientCredentialParameters.builder(TOKEN_EXCHANGE_SCOPE)
                                        .credentialFmiPath(agentAppId)).build()));

        String assertion = assertionResult.accessToken();

        // Leg 3: Exchange the assertion for a user-scoped token via UserFIC.
        // The result is written to the Agent CCA's user token cache for future silent retrieval.
        LOG.debug("Executing Leg 3 (user FIC token) for agent app ID: {}", agentAppId);
        UserFederatedIdentityCredentialParameters ficParams = buildLeg3FicParams(
                callerScopes, agentIdentity, assertion);

        return (AuthenticationResult) joinAndUnwrap(agentCca.acquireToken(ficParams));
    }

    /**
     * Searches the Agent CCA's user token cache for a previously-acquired token
     * matching the specified user identity (by OID or UPN).
     * Returns null if no matching account exists or the cached token is expired.
     */
    private AuthenticationResult tryAcquireTokenSilent(
            ConfidentialClientApplication agentCca,
            AgentIdentity agentIdentity,
            Set<String> scopes) {
        try {
            Set<IAccount> accounts = joinAndUnwrap(agentCca.getAccounts());
            IAccount matchedAccount = findMatchingAccount(accounts, agentIdentity);
            if (matchedAccount == null) {
                return null;
            }

            SilentParameters.SilentParametersBuilder silentBuilder = SilentParameters
                    .builder(scopes, matchedAccount);

            // Propagate outer request parameters so that claims challenges cause cache bypass
            propagateToSilentParams(silentBuilder);

            return (AuthenticationResult) joinAndUnwrap(
                    agentCca.acquireTokenSilently(silentBuilder.build()));
        } catch (Exception ex) {
            // Token expired or requires interaction — fall through to full Leg 2 + Leg 3 flow
            LOG.debug("Silent token acquisition failed for agent: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Finds an account in the Agent CCA's cache that matches the user identity.
     * Matches by OID (HomeAccountId objectId) if the caller specified a UUID,
     * otherwise by UPN (Account.username). Both comparisons are case-insensitive.
     */
    private static IAccount findMatchingAccount(Set<IAccount> accounts, AgentIdentity agentIdentity) {
        if (agentIdentity.userObjectId() != null) {
            String targetOid = agentIdentity.userObjectId().toString();
            return accounts.stream()
                    .filter(a -> a.homeAccountId() != null &&
                            targetOid.equalsIgnoreCase(extractOid(a.homeAccountId())))
                    .findFirst()
                    .orElse(null);
        }

        return accounts.stream()
                .filter(a -> agentIdentity.username().equalsIgnoreCase(a.username()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Extracts the OID portion from a homeAccountId (format: "oid.tid").
     */
    private static String extractOid(String homeAccountId) {
        int dotIndex = homeAccountId.indexOf('.');
        return dotIndex >= 0 ? homeAccountId.substring(0, dotIndex) : homeAccountId;
    }

    /**
     * Builds the Leg 3 (UserFIC) parameters, selecting the OID or UPN overload as
     * appropriate. ForceRefresh is always set because the caller-level silent check
     * (tryAcquireTokenSilent) has already run; if we reach Leg 3 the token must be
     * fetched from the network.
     */
    private UserFederatedIdentityCredentialParameters buildLeg3FicParams(
            Set<String> callerScopes,
            AgentIdentity agentIdentity,
            String assertion) {
        UserFederatedIdentityCredentialParameters.UserFederatedIdentityCredentialParametersBuilder builder;
        if (agentIdentity.userObjectId() != null) {
            builder = UserFederatedIdentityCredentialParameters
                    .builder(callerScopes, agentIdentity.userObjectId(), assertion);
        } else {
            builder = UserFederatedIdentityCredentialParameters
                    .builder(callerScopes, agentIdentity.username(), assertion);
        }
        return propagateToUserFicParams(builder.forceRefresh(true)).build();
    }

    // ========================================================================
    // Outer Request Parameter Propagation
    // ========================================================================

    /**
     * Propagates per-request parameters from the outer AcquireTokenForAgent call to a
     * ClientCredentialParameters builder (used for Legs 1-2 and app-only).
     * This ensures caller-specified claims, tenant overrides, extra query parameters,
     * and extra HTTP headers flow through to inner network calls.
     */
    private ClientCredentialParameters.ClientCredentialParametersBuilder propagateToClientCredentialParams(
            ClientCredentialParameters.ClientCredentialParametersBuilder builder) {
        AcquireTokenForAgentParameters outerParams = agentRequest.parameters;

        if (outerParams.claims() != null) {
            builder.claims(outerParams.claims());
        }
        if (!StringHelper.isBlank(outerParams.tenant())) {
            builder.tenant(outerParams.tenant());
        }
        if (outerParams.extraQueryParameters() != null && !outerParams.extraQueryParameters().isEmpty()) {
            builder.extraQueryParameters(outerParams.extraQueryParameters());
        }
        if (outerParams.extraHttpHeaders() != null && !outerParams.extraHttpHeaders().isEmpty()) {
            builder.extraHttpHeaders(outerParams.extraHttpHeaders());
        }
        return builder;
    }

    /**
     * Propagates per-request parameters from the outer AcquireTokenForAgent call to a
     * UserFederatedIdentityCredentialParameters builder (used for Leg 3).
     */
    private UserFederatedIdentityCredentialParameters.UserFederatedIdentityCredentialParametersBuilder propagateToUserFicParams(
            UserFederatedIdentityCredentialParameters.UserFederatedIdentityCredentialParametersBuilder builder) {
        AcquireTokenForAgentParameters outerParams = agentRequest.parameters;

        if (outerParams.claims() != null) {
            builder.claims(outerParams.claims());
        }
        if (!StringHelper.isBlank(outerParams.tenant())) {
            builder.tenant(outerParams.tenant());
        }
        if (outerParams.extraQueryParameters() != null && !outerParams.extraQueryParameters().isEmpty()) {
            builder.extraQueryParameters(outerParams.extraQueryParameters());
        }
        if (outerParams.extraHttpHeaders() != null && !outerParams.extraHttpHeaders().isEmpty()) {
            builder.extraHttpHeaders(outerParams.extraHttpHeaders());
        }
        return builder;
    }

    /**
     * Propagates per-request parameters from the outer AcquireTokenForAgent call to a
     * SilentParameters builder (used for the cache-first silent check).
     * Claims propagation is important here: if a claims challenge is present, the silent
     * check should recognize the cached token as insufficient and force a refresh.
     */
    private void propagateToSilentParams(SilentParameters.SilentParametersBuilder builder) {
        AcquireTokenForAgentParameters outerParams = agentRequest.parameters;

        if (outerParams.claims() != null) {
            builder.claims(outerParams.claims());
        }
        if (!StringHelper.isBlank(outerParams.tenant())) {
            builder.tenant(outerParams.tenant());
        }
        if (outerParams.extraQueryParameters() != null && !outerParams.extraQueryParameters().isEmpty()) {
            builder.extraQueryParameters(outerParams.extraQueryParameters());
        }
        if (outerParams.extraHttpHeaders() != null && !outerParams.extraHttpHeaders().isEmpty()) {
            builder.extraHttpHeaders(outerParams.extraHttpHeaders());
        }
    }

    // ========================================================================
    // Agent CCA Construction and Configuration
    // ========================================================================

    /**
     * Retrieves the cached internal Agent CCA for the given agent app ID, or creates one
     * if this is the first call. The Agent CCA is stored in the Blueprint's agentCcaCache
     * so its app and user token caches persist across calls.
     */
    private ConfidentialClientApplication getOrCreateAgentCca(String agentAppId) {
        String key = AGENT_CCA_KEY_PREFIX + agentAppId;
        return blueprintApplication.agentCcaCache.computeIfAbsent(key, k -> {
            try {
                return buildAgentCca(agentAppId);
            } catch (MalformedURLException e) {
                throw new MsalClientException(e);
            }
        });
    }

    /**
     * Builds a new internal Agent CCA configured with:
     * <ul>
     *   <li>Client ID = the agent's app ID</li>
     *   <li>Authority = the Blueprint's resolved authority</li>
     *   <li>Client assertion callback = Leg 1 (FMI credential from Blueprint)</li>
     *   <li>App-level config = propagated from the Blueprint</li>
     * </ul>
     */
    private ConfidentialClientApplication buildAgentCca(String agentAppId)
            throws MalformedURLException {
        // Capture only the blueprint reference (long-lived) in the assertion callback.
        // Do NOT capture 'this' (per-request state) to avoid pinning stale request data.
        final ConfidentialClientApplication blueprint = blueprintApplication;

        IClientCredential assertionCredential = ClientCredentialFactory.createFromCallback(
                (AssertionRequestOptions opts) -> {
                    try {
                        // Leg 1: Acquire an FMI credential from the Blueprint CCA.
                        // AcquireTokenForClient has built-in cache-first logic — only the
                        // first call hits the network; subsequent calls return cached credential.
                        IAuthenticationResult result = joinAndUnwrap(
                                blueprint.acquireToken(
                                        ClientCredentialParameters.builder(TOKEN_EXCHANGE_SCOPE)
                                                .fmiPath(agentAppId)
                                                .build()));
                        return result.accessToken();
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new MsalClientException(e);
                    }
                });

        ConfidentialClientApplication.Builder builder =
                ConfidentialClientApplication.builder(agentAppId, assertionCredential)
                        .authority(blueprint.authority());

        propagateBlueprintConfig(builder, blueprint);
        return builder.build();
    }

    /**
     * Propagates app-level configuration from the Blueprint CCA to the Agent CCA builder.
     * This ensures the Agent CCA shares the Blueprint's HTTP behavior, logging, instance
     * discovery settings, and telemetry identity.
     */
    private static void propagateBlueprintConfig(
            ConfidentialClientApplication.Builder builder,
            ConfidentialClientApplication blueprint) {
        // HTTP: share the same HTTP client
        if (blueprint.httpClient() != null) {
            builder.httpClient(blueprint.httpClient());
        }

        // Logging
        builder.logPii(blueprint.logPii());

        // Instance discovery: honor the Blueprint's settings
        builder.instanceDiscovery(blueprint.instanceDiscovery());
        builder.validateAuthority(blueprint.validateAuthority());

        // Telemetry: attribute network calls to the same caller
        if (blueprint.applicationName() != null) {
            builder.applicationName(blueprint.applicationName());
        }
        if (blueprint.applicationVersion() != null) {
            builder.applicationVersion(blueprint.applicationVersion());
        }
    }

    /**
     * Calls {@link CompletableFuture#join()} and unwraps any {@link CompletionException}
     * so the original exception propagates with its correct type.
     */
    private static <T> T joinAndUnwrap(CompletableFuture<T> future) throws Exception {
        try {
            return future.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }
    }
}
