// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.google.gson.annotations.SerializedName;

public class UserSecret {

    @SerializedName("secret")
    String secret;

    @SerializedName("value")
    String value;
}
