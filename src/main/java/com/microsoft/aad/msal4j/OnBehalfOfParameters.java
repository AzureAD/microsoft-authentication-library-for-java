// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotEmpty;

/**
 * Object containing parameters for On-Behalf-Of flow. Can be used as parameter to
 * {@link ConfidentialClientApplication#acquireToken(OnBehalfOfParameters)}
 *
 * For more details, see https://aka.ms/msal4j-on-behalf-of
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OnBehalfOfParameters implements IApiParameters {

    @NonNull
    private Set<String> scopes;

    /**
     * Indicates whether the request should skip looking into the token cache. Be default it is
     * set to false.
     */
    @Builder.Default
    private Boolean skipCache = false;

    @NonNull
    private IUserAssertion userAssertion;

    /**
     * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
     */
    private ClaimsRequest claims;

    /**
     * Adds additional headers to the token request
     */
    private Map<String, String> extraHttpHeaders;

    private static OnBehalfOfParametersBuilder builder() {

        return new OnBehalfOfParametersBuilder();
    }

    /**
     * Builder for {@link OnBehalfOfParameters}
     * @param scopes scopes application is requesting access to
     * @param userAssertion {@link UserAssertion} created from access token received
     * @return builder that can be used to construct OnBehalfOfParameters
     */
    public static OnBehalfOfParametersBuilder builder(Set<String> scopes, UserAssertion userAssertion) {

        validateNotEmpty("scopes", scopes);

        return builder()
                .scopes(scopes)
                .userAssertion(userAssertion);
    }
}
