//----------------------------------------------------------------------
//
// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
//
//------------------------------------------------------------------------------

package labapi;

import com.google.gson.annotations.SerializedName;

public enum B2CIdentityProvider {
    @SerializedName("Local")
    LOCAL(1),
    @SerializedName("Facebook")
    FACEBOOK(2),
    @SerializedName("Google")
    GOOGLE(3);

    private final int labId;

    B2CIdentityProvider(int labId){
        this.labId= labId;
    }
}
