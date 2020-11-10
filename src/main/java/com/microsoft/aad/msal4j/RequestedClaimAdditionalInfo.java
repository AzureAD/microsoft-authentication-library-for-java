// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * Represents the additional information that can be sent to an authorization server for a request claim in the claim request parameter
 *
 * @see <a href="https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter">https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter</a>
 */
@Getter
@Setter
@AllArgsConstructor
public class RequestedClaimAdditionalInfo {
    boolean essential;
    String value;
    List<String> values;

    String formatAsJSONString () {
        String jsonString = "";
        if (essential) {
            jsonString += String.format("\"essential\":%b", essential);
        }

        if (value != null) {
            if (jsonString.length() > 0) jsonString += ",";
            jsonString += String.format("\"value\":\"%s\"", value);
        }

        if (values != null) {
            if (jsonString.length() > 0) jsonString += ",";
            jsonString += String.format("\"values\":[\"%s\"]", String.join("\",\"", values));
        }

        return jsonString;
    }
}
