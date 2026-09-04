// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ManagedIdentityEnvironment {

    private static final Logger LOG =
            LoggerFactory.getLogger(ManagedIdentityEnvironment.class);

    private ManagedIdentityEnvironment() {
    }

    static boolean isImdsV2Disabled() {
        String value = AbstractManagedIdentitySource
                .getEnvironmentVariables()
                .getEnvironmentVariable(Constants.MSAL_MI_DISABLE_IMDS_V2);
        if (StringHelper.isNullOrBlank(value)) {
            return false;
        }

        String normalized = value.trim();
        if ("1".equals(normalized)
                || "true".equalsIgnoreCase(normalized)) {
            return true;
        }
        if (!"false".equalsIgnoreCase(normalized)
                && !"0".equals(normalized)) {
            LOG.warn(
                    "Ignoring unrecognized {} value. Use 'true' or '1' to disable IMDS v2.",
                    Constants.MSAL_MI_DISABLE_IMDS_V2);
        }
        return false;
    }
}
