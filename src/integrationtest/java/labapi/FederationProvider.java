//----------------------------------------------------------------------
//
// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
//
//------------------------------------------------------------------------------

package labapi;

import com.google.gson.annotations.SerializedName;

public enum FederationProvider {
    @SerializedName("ADFSv2")
    ADFSV2(2),
    @SerializedName("ADFSv3")
    ADFSV3(3),
    @SerializedName("ADFSv4")
    ADFSV4(4),
    @SerializedName("ADFSv2019")
    ADFSv2019(5),
    @SerializedName("PingFederatev83")
    PINGFEDERATEV83(5),
    @SerializedName("Shibboleth")
    SHIBBOLETH(6);

    private final int labId;

    FederationProvider(int labId){
        this.labId = labId;
    }
}

