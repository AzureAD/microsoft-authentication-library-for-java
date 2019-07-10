//----------------------------------------------------------------------
//
// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
//
//------------------------------------------------------------------------------

package labapi;

import com.google.gson.annotations.SerializedName;

public class LabResponse{

    @SerializedName("AppID")
    private String appId;
    @SerializedName("Users")
    private LabUser user;

    public LabResponse(String appId, LabUser user){
        this.appId = appId;
        this.user = user;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId){
        this.appId = appId;
    }

    public LabUser getUser() {
        return user;
    }
}