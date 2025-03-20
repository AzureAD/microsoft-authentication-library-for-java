// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for device code flow. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(DeviceCodeFlowParameters)}. For more details,
 * see https://aka.ms/msal4j-device-code
 */
public class DeviceCodeFlowParameters implements IAcquireTokenParameters {

    private Set<String> scopes;
    private Consumer<DeviceCode> deviceCodeConsumer;
    private ClaimsRequest claims;
    private Map<String, String> extraHttpHeaders;
    private Map<String, String> extraQueryParameters;
    private String tenant;

    private DeviceCodeFlowParameters(Set<String> scopes, Consumer<DeviceCode> deviceCodeConsumer, ClaimsRequest claims, Map<String, String> extraHttpHeaders, Map<String, String> extraQueryParameters, String tenant) {
        this.scopes = scopes;
        this.deviceCodeConsumer = deviceCodeConsumer;
        this.claims = claims;
        this.extraHttpHeaders = extraHttpHeaders;
        this.extraQueryParameters = extraQueryParameters;
        this.tenant = tenant;
    }

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

    public Set<String> scopes() {
        return this.scopes;
    }

    public Consumer<DeviceCode> deviceCodeConsumer() {
        return this.deviceCodeConsumer;
    }

    public ClaimsRequest claims() {
        return this.claims;
    }

    public Map<String, String> extraHttpHeaders() {
        return this.extraHttpHeaders;
    }

    public Map<String, String> extraQueryParameters() {
        return this.extraQueryParameters;
    }

    public String tenant() {
        return this.tenant;
    }

    public static class DeviceCodeFlowParametersBuilder {
        private Set<String> scopes;
        private Consumer<DeviceCode> deviceCodeConsumer;
        private ClaimsRequest claims;
        private Map<String, String> extraHttpHeaders;
        private Map<String, String> extraQueryParameters;
        private String tenant;

        DeviceCodeFlowParametersBuilder() {
        }

        /**
         * Scopes to which the application is requesting access to.
         * <p>
         * Cannot be null.
         */
        public DeviceCodeFlowParametersBuilder scopes(Set<String> scopes) {
            validateNotNull("scopes", scopes);

            this.scopes = scopes;
            return this;
        }

        /**
         * Receives the device code returned from the first step of Oauth2.0 device code flow. The
         * {@link DeviceCode#verificationUri} and the {@link DeviceCode#userCode} should be shown
         * to the end user.
         * <p>
         * For more details, see https://aka.ms/msal4j-device-code
         * <p>
         * Cannot be null.
         */
        public DeviceCodeFlowParametersBuilder deviceCodeConsumer(Consumer<DeviceCode> deviceCodeConsumer) {
            validateNotNull("deviceCodeConsumer", scopes);

            this.deviceCodeConsumer = deviceCodeConsumer;
            return this;
        }

        /**
         * Claims to be requested through the OIDC claims request parameter, allowing requests for standard and custom claims
         */
        public DeviceCodeFlowParametersBuilder claims(ClaimsRequest claims) {
            this.claims = claims;
            return this;
        }

        /**
         * Adds additional headers to the token request
         */
        public DeviceCodeFlowParametersBuilder extraHttpHeaders(Map<String, String> extraHttpHeaders) {
            this.extraHttpHeaders = extraHttpHeaders;
            return this;
        }

        /**
         * Adds additional query parameters to the token request
         */
        public DeviceCodeFlowParametersBuilder extraQueryParameters(Map<String, String> extraQueryParameters) {
            this.extraQueryParameters = extraQueryParameters;
            return this;
        }

        /**
         * Overrides the tenant value in the authority URL for this request
         */
        public DeviceCodeFlowParametersBuilder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        public DeviceCodeFlowParameters build() {
            return new DeviceCodeFlowParameters(this.scopes, this.deviceCodeConsumer, this.claims, this.extraHttpHeaders, this.extraQueryParameters, this.tenant);
        }

        public String toString() {
            return "DeviceCodeFlowParameters.DeviceCodeFlowParametersBuilder(scopes=" + this.scopes + ", deviceCodeConsumer=" + this.deviceCodeConsumer + ", claims=" + this.claims + ", extraHttpHeaders=" + this.extraHttpHeaders + ", extraQueryParameters=" + this.extraQueryParameters + ", tenant=" + this.tenant + ")";
        }
    }
}
