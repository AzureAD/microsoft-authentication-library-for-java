// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserSecret {

    @JsonProperty("secret")
    String secret;

    @JsonProperty("value")
    String value;
}
