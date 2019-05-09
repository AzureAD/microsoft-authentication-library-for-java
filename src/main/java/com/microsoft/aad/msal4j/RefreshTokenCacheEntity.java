// Copyright (c) Microsoft Corporation.
//// All rights reserved.
////
//// This code is licensed under the MIT License.
////
//// Permission is hereby granted, free of charge, to any person obtaining a copy
//// of this software and associated documentation files(the "Software"), to deal
//// in the Software without restriction, including without limitation the rights
//// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//// copies of the Software, and to permit persons to whom the Software is
//// furnished to do so, subject to the following conditions :
////
//// The above copyright notice and this permission notice shall be included in
//// all copies or substantial portions of the Software.
////
//// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
//// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//// THE SOFTWARE.

package com.microsoft.aad.msal4j;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Accessors(fluent = true)
@Getter
@Setter
class RefreshTokenCacheEntity extends Credential {

    @SerializedName("credential_type")
    private String credentialType = "RefreshToken";

    @SerializedName("family_id")
    private String family_id;

    boolean isFamilyRT(){
        return !StringHelper.isBlank(family_id);
    }

    String getKey(){
        List<String> keyParts = new ArrayList<>();

        keyParts.add(homeAccountId);
        keyParts.add(environment);
        keyParts.add(credentialType);

        if(isFamilyRT()){
            keyParts.add(family_id);
        }
        else{
            keyParts.add(clientId);
        }

        // realm
        keyParts.add("");
        // target
        keyParts.add("");

        return String.join(Constants.CACHE_KEY_SEPARATOR, keyParts).toLowerCase();
    }
}
