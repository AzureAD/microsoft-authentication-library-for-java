// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.Set;

/**
 * Object containing parameters for managed identity flow. Can be used as parameter to
 * {@link ManagedIdentityApplication#acquireTokenForManagedIdentity(ManagedIdentityParameters)}
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ManagedIdentityParameters implements IAcquireTokenParameters {

    @Getter
    String resource;
    
    boolean forceRefresh;

    IEnvironmentVariables environmentVariables;

    @Override
    public Set<String> scopes() {
        return null;
    }

    @Override
    public ClaimsRequest claims() {
        return null;
    }

    @Override
    public Map<String, String> extraHttpHeaders() {
        return null;
    }

    @Override
    public String tenant() {
        return "managed_identity";
    }

    @Override
    public Map<String, String> extraQueryParameters() {
        return null;
    }

    void setEnvironmentVariablesConfig(IEnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    private static ManagedIdentityParametersBuilder builder() {
        return new ManagedIdentityParametersBuilder();
    }

    /**
     * Builder for {@link ManagedIdentityParameters}
     * @param resource scopes application is requesting access to
     * @return builder that can be used to construct ManagedIdentityParameters
     */
    public static ManagedIdentityParametersBuilder builder(String resource) {
        return builder().resource(resource);
    }
}
