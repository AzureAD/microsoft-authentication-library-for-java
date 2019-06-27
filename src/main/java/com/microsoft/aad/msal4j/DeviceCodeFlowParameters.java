// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

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
