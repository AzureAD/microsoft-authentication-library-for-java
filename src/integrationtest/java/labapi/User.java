// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
public class User
{
    @SerializedName("appId")
    private String appId;

    @SerializedName("objectId")
    private String objectId;

    @SerializedName("userType")
    private String userType;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("licenses")
    private String licenses;

    @SerializedName("upn")
    private String upn;

    @SerializedName("mfa")
    private String mfa;

    @SerializedName("protectionPolicy")
    private String protectionPolicy;

    @SerializedName("homeDomain")
    private String homeDomain;

    @SerializedName("homeUPN")
    private String homeUPN;

    @SerializedName("b2cProvider")
    private String b2cProvider;

    @SerializedName("labName")
    private String labName;

    @SerializedName("lastUpdatedBy")
    private String lastUpdatedBy;

    @SerializedName("lastUpdatedDate")
    private String lastUpdatedDate;

    @Setter
    private String password;

    @Setter
    private String federationProvider;
}
