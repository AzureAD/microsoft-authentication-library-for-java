// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.UUID;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Represents the identity of an agent application and the user it acts on behalf of.
 * Used with {@link IConfidentialClientApplication#acquireTokenForAgent(AcquireTokenForAgentParameters)}
 * to acquire tokens for agent scenarios using Federated Managed Identity (FMI) and
 * User Federated Identity Credentials (UserFIC).
 */
public final class AgentIdentity {

    private final String agentApplicationId;
    private UUID userObjectId;
    private String username;

    private AgentIdentity(String agentApplicationId) {
        validateNotBlank("agentApplicationId", agentApplicationId);
        this.agentApplicationId = agentApplicationId;
    }

    /**
     * Creates an {@link AgentIdentity} that identifies the user by their object ID (OID).
     * This is the recommended approach for identifying users in agent scenarios.
     *
     * @param agentApplicationId the client ID of the agent application
     * @param userObjectId       the object ID (OID) of the user the agent acts on behalf of
     */
    public AgentIdentity(String agentApplicationId, UUID userObjectId) {
        this(agentApplicationId);
        validateNotNull("userObjectId", userObjectId);
        this.userObjectId = userObjectId;
    }

    /**
     * Creates an {@link AgentIdentity} that identifies the user by their UPN (User Principal Name).
     *
     * @param agentApplicationId the client ID of the agent application
     * @param username           the UPN of the user the agent acts on behalf of
     * @return an {@link AgentIdentity} configured with the user's UPN
     */
    public static AgentIdentity withUsername(String agentApplicationId, String username) {
        validateNotBlank("username", username);
        AgentIdentity identity = new AgentIdentity(agentApplicationId);
        identity.username = username;
        return identity;
    }

    /**
     * Creates an {@link AgentIdentity} for app-only (no user) scenarios, where only Legs 1-2
     * of the agent token acquisition are performed.
     *
     * @param agentApplicationId the client ID of the agent application
     * @return an {@link AgentIdentity} configured for app-only access
     */
    public static AgentIdentity appOnly(String agentApplicationId) {
        return new AgentIdentity(agentApplicationId);
    }

    /**
     * Gets the client ID of the agent application.
     *
     * @return the agent application's client ID
     */
    public String agentApplicationId() {
        return agentApplicationId;
    }

    /**
     * Gets the object ID (OID) of the user, if specified.
     *
     * @return the user's OID, or null if not specified
     */
    public UUID userObjectId() {
        return userObjectId;
    }

    /**
     * Gets the UPN of the user, if specified.
     *
     * @return the user's UPN, or null if not specified
     */
    public String username() {
        return username;
    }

    /**
     * Returns whether this identity includes a user identifier (OID or UPN).
     * When false, the agent flow is app-only (Legs 1-2 only).
     *
     * @return true if a user OID or UPN is present
     */
    public boolean hasUserIdentifier() {
        return userObjectId != null || !StringHelper.isBlank(username);
    }
}
