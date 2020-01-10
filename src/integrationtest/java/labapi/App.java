// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class App {

    @JsonProperty("appType")
    String appType;

    @JsonProperty("appName")
    String appName;

    @JsonProperty("appId")
    String appId;

    @JsonProperty("redirectUri")
    String redirectUri;

    @JsonProperty("authority")
    String authority;

    @JsonProperty("labName")
    String labName;

}
