//----------------------------------------------------------------------
//
// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
//
//------------------------------------------------------------------------------

package labapi;

import com.google.gson.annotations.SerializedName;

public enum UserType {
    @SerializedName("member")
    MEMBER(0),
    @SerializedName("guest")
    GUEST(1),
    @SerializedName("B2C")
    B2C(2);

    private int labId;

    UserType(int labId){
        this.labId = labId;
    }
}
