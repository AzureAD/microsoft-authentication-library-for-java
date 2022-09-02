// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for device code flow. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(DeviceCodeFlowParameters)}. For more details,
 * see https://aka.ms/msal4j-device-code
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceCodeFlowParameters implements IAcquireTokenParameters {

    /**
     * Scopes to which the application is requesting access to.
     */
    @NonNull
    private Set<String> scopes;

    /**
     * Receives the device code returned from the first step of Oauth2.0 device code flow. The
     * {@link DeviceCode#verificationUri} and the {@link DeviceCode#userCode} should be shown
     * to the end user.
     * <p>
     * For more details, see https://aka.ms/msal4j-device-code
     */
    @NonNull
    private Consumer<DeviceCode> deviceCodeConsumer;

    /**
     * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
     */
    private ClaimsRequest claims;

    /**
     * Adds additional headers to the token request
     */
    private Map<String, String> extraHttpHeaders;

    /**
     * Overrides the tenant value in the authority URL for this request
     */
    private String tenant;

    private static DeviceCodeFlowParametersBuilder builder() {

        return new DeviceCodeFlowParametersBuilder();
    }

    /**
     * Builder for {@link DeviceCodeFlowParameters}
     *
     * @param scopes             scopes application is requesting access to
     * @param deviceCodeConsumer {@link Consumer} of {@link DeviceCode}
     * @return builder that can be used to construct DeviceCodeFlowParameters
     */
    public static DeviceCodeFlowParametersBuilder builder
    (Set<String> scopes, Consumer<DeviceCode> deviceCodeConsumer) {

        validateNotNull("scopes", scopes);

        return builder()
                .scopes(scopes)
                .deviceCodeConsumer(deviceCodeConsumer);
    }
}
