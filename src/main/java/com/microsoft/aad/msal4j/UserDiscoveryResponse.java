// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.msal4j;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter(AccessLevel.PACKAGE)
class UserDiscoveryResponse {

    @SerializedName("ver")
    private float version;

    @SerializedName("account_type")
    private String accountType;

    @SerializedName("federation_metadata_url")
    private String federationMetadataUrl;

    @SerializedName("federation_protocol")
    private String federationProtocol;

    @SerializedName("federation_active_auth_url")
    private String federationActiveAuthUrl;

    @SerializedName("cloud_audience_urn")
    private String cloudAudienceUrn;

    boolean isAccountFederated() {
        return !StringHelper.isBlank(this.accountType)
                && this.accountType.equalsIgnoreCase("Federated");
    }

    boolean isAccountManaged() {
        return !StringHelper.isBlank(this.accountType)
                && this.accountType.equalsIgnoreCase("Managed");
    }
}
