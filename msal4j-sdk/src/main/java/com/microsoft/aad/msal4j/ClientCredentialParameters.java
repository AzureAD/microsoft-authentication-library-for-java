// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for client credential flow. Can be used as parameter to
 * {@link ConfidentialClientApplication#acquireToken(ClientCredentialParameters)}
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientCredentialParameters implements IAcquireTokenParameters {

    /**
     * Scopes for which the application is requesting access to.
     */
    @NonNull
    private Set<String> scopes;

    /**
     * Indicates whether the request should skip looking into the token cache. Be default it is
     * set to false.
     */
    @Builder.Default
    private Boolean skipCache = false;

    /**
     * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
     */
    private ClaimsRequest claims;

    /**
     * Adds additional headers to the token request
     */
    private Map<String, String> extraHttpHeaders;

    /**
     * Adds additional query parameters to the token request
     */
    private Map<String, String> extraQueryParameters;

    /**
     * Overrides the tenant value in the authority URL for this request
     */
    private String tenant;

    /**
     * Overrides the client credentials for this request
     */
    private IClientCredential clientCredential;

    private static ClientCredentialParametersBuilder builder() {

        return new ClientCredentialParametersBuilder();
    }

    /**
     * Builder for {@link ClientCredentialParameters}
     *
     * @param scopes scopes application is requesting access to
     * @return builder that can be used to construct ClientCredentialParameters
     */
    public static ClientCredentialParametersBuilder builder(Set<String> scopes) {

        validateNotNull("scopes", scopes);

        return builder().scopes(scopes);
    }
}
