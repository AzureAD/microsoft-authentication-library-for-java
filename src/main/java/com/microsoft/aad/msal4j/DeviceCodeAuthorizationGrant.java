// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for device code grant.
 */
class DeviceCodeAuthorizationGrant extends AbstractMsalAuthorizationGrant {
    private final static String GRANT_TYPE = "device_code";

    private final DeviceCode deviceCode;
    private final String scopes;
    private String correlationId;

    /**
     *  Create a new device code grant object from a device code and a resource.
     *
     * @param scopes    The resource for which the device code was acquired.
     */
    DeviceCodeAuthorizationGrant(DeviceCode deviceCode, final String scopes) {
        this.deviceCode = deviceCode;
        this.correlationId = deviceCode.correlationId();
        this.scopes = scopes;
    }

    /**
     * Converts the device code grant to a map of HTTP paramters.
     *
     * @return The map with HTTP parameters.
     */
    @Override
    public Map<String, List<String>> toParameters() {
        final Map<String, List<String>> outParams = new LinkedHashMap<>();
        outParams.put(SCOPE_PARAM_NAME, Collections.singletonList(COMMON_SCOPES_PARAM + SCOPES_DELIMITER + scopes));
        outParams.put("grant_type", Collections.singletonList(GRANT_TYPE));
        outParams.put("device_code", Collections.singletonList(deviceCode.deviceCode()));
        outParams.put("client_info", Collections.singletonList("1"));

        return outParams;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
