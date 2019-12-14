// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class Lab {
    @SerializedName("labName")
    String labName;

    @SerializedName("domain")
    String domain;

    @SerializedName("tenantId")
    String tenantId;

    @SerializedName("federationProvider")
    String federationProvider;

    @SerializedName("azureEnvironment")
    String azureEnvironment;

    @SerializedName("authority")
    String authority;
}
