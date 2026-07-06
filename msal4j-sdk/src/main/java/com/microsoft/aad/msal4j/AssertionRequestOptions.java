// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Context information passed to assertion provider callbacks when MSAL needs a fresh client assertion.
 * This allows assertion providers to generate context-aware assertions, such as including the FMI path
 * in Federated Managed Identity (agent identity) scenarios.
 *
 * <p>For more details on agent identity scenarios, see the MSAL documentation on FMI/FIC flows.</p>
 */
public final class AssertionRequestOptions {

    private final String clientId;
    private final String tokenEndpoint;
    private final String clientAssertionFmiPath;

    AssertionRequestOptions(String clientId, String tokenEndpoint, String clientAssertionFmiPath) {
        this.clientId = clientId;
        this.tokenEndpoint = tokenEndpoint;
        this.clientAssertionFmiPath = clientAssertionFmiPath;
    }

    /**
     * Gets the client ID of the application requesting the assertion.
     *
     * @return the client ID
     */
    public String clientId() {
        return clientId;
    }

    /**
     * Gets the token endpoint URL that the assertion will be sent to.
     *
     * @return the token endpoint URL
     */
    public String tokenEndpoint() {
        return tokenEndpoint;
    }

    /**
     * Gets the FMI (Federated Managed Identity) path for agent identity scenarios.
     * When set, this indicates which agent identity the client assertion is being requested for.
     * The assertion provider can use this to include the FMI path in its token acquisition logic.
     *
     * @return the client assertion FMI path, or null if not applicable
     */
    public String clientAssertionFmiPath() {
        return clientAssertionFmiPath;
    }
}
