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

    @JsonProperty("objectId")
    private String objectId;

    @JsonProperty("userType")
    private String userType;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("licenses")
    private String licenses;

    @JsonProperty("upn")
    @Setter
    private String upn;

    @JsonProperty("mfa")
    private String mfa;

    @JsonProperty("protectionPolicy")
    private String protectionPolicy;

    @JsonProperty("homeDomain")
    private String homeDomain;

    @JsonProperty("homeUPN")
    private String homeUPN;

    @JsonProperty("b2cProvider")
    private String b2cProvider;

    @JsonProperty("labName")
    private String labName;

    @JsonProperty("lastUpdatedBy")
    private String lastUpdatedBy;

    @JsonProperty("lastUpdatedDate")
    private String lastUpdatedDate;

    @Setter
    private String password;

    @Setter
    private String federationProvider;
}
