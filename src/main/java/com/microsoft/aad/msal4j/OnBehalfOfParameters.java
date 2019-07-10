// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotEmpty;

/**
 * Object containing parameters for On-Behalf-Of flow. Can be used as parameter to
 * {@link ConfidentialClientApplication#acquireToken(OnBehalfOfParameters)}
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OnBehalfOfParameters {

    @NonNull
    private Set<String> scopes;

    @NonNull
    private IUserAssertion userAssertion;

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
