// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.Set;
import java.util.function.Consumer;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotEmpty;

/**
 * Object containing parameters for device code flow. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(DeviceCodeFlowParameters)}
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceCodeFlowParameters {

    @NonNull
    private Set<String> scopes;

    @NonNull
    private Consumer<DeviceCode> deviceCodeConsumer;

    private static DeviceCodeFlowParametersBuilder builder() {

        return new DeviceCodeFlowParametersBuilder();
    }

    /**
     * Builder for {@link DeviceCodeFlowParameters}
     * @param scopes scopes application is requesting access to
     * @param deviceCodeConsumer {@link Consumer} of {@link DeviceCode}
     * @return builder that can be used to construct DeviceCodeFlowParameters
     */
    public static DeviceCodeFlowParametersBuilder builder
            (Set<String> scopes, Consumer<DeviceCode> deviceCodeConsumer) {

        validateNotEmpty("scopes", scopes);

        return builder()
                .scopes(scopes)
                .deviceCodeConsumer(deviceCodeConsumer);
    }
}
