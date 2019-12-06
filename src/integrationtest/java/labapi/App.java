// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class App {

    @SerializedName("appType")
    String appType;

    @SerializedName("appName")
    String appName;

    @SerializedName("appId")
    String appId;

    @SerializedName("redirectUri")
    String redirectUri;

    @SerializedName("authority")
    String authority;

    @SerializedName("labName")
    String labName;

}
