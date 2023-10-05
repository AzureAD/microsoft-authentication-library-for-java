// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.concurrent.CompletableFuture;

/**
 * Interface representing a managed identity application.
 * Managed identity applications are used to acquire a token for managed identity assigned to
 * an azure resource such as Azure function, app service, virtual machine, etc. to acquire a token
 * without using credentials.
 * For details see https://aka.ms/msal4jclientapplications
 */
public interface IManagedIdentityApplication extends IApplicationBase {

    /**
     * Acquires tokens from the configured managed identity on an azure resource.
     *
     * @param parameters instance of {@link ManagedIdentityParameters}
     * @return {@link CompletableFuture} containing an {@link IAuthenticationResult}
     */
    CompletableFuture<IAuthenticationResult> acquireTokenForManagedIdentity(ManagedIdentityParameters parameters)
        throws Exception;
}
