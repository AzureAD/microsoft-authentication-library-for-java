// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * Represents the additional information that can be sent to an authorization server for a request claim in the claim request parameter
 *
 * @see <a href="https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter">https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter</a>
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestedClaimAdditionalInfo {

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    @JsonProperty("essential")
    boolean essential;

    @JsonProperty("value")
    String value;

    @JsonProperty("values")
    List<String> values;
}
