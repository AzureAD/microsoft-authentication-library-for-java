// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AllArgsConstructor;

/**
 * Represents an individual requested claims that's part of a complete claims request parameter
 *
 * @see <a href="https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter">https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter</a>
 */
@AllArgsConstructor
public class RequestedClaim {
    String name;
    RequestedClaimAdditionalInfo requestedClaimAdditionalInfo;

    String formatAsJSONString () {
        if (requestedClaimAdditionalInfo != null) {
            return String.format("\"%s\":{%s}", name, requestedClaimAdditionalInfo.formatAsJSONString());
        } else {
            return String.format("\"%s\":null", name);
        }
    }
}
