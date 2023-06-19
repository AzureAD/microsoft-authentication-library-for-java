// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
public class User {
    @JsonProperty("appId")
    private String appId;

    @JsonProperty("userType")
    private String userType;

    @JsonProperty("upn")
    @Setter
    private String upn;

    @JsonProperty("homeDomain")
    private String homeDomain;

    @JsonProperty("homeUPN")
    private String homeUPN;

    @JsonProperty("labName")
    private String labName;

    @Setter
    private String password;

    @Setter
    private String federationProvider;
}
