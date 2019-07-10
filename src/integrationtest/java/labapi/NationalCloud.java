// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.google.gson.annotations.SerializedName;

public enum NationalCloud {

    @SerializedName("azurecloud")
    AZURE_CLOUD(1),
    @SerializedName("azuregermanycloud")
    GERMAN_CLOUD(2),
    @SerializedName("azurechinacloud")
    CHINA_CLOUD(3),
    @SerializedName("azuregovernmentcloud")
    GOVERNMENT_CLOUD(4);

    private final int labId;

    NationalCloud(int labId){
        this.labId = labId;
    }
}
