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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.URL;

@Accessors(fluent=true)
@Getter(AccessLevel.PACKAGE)
class B2CAuthority extends Authority{

    final static String B2CTokenEndpointFormat = "https://%s/{tenant}" + TOKEN_ENDPOINT + "?p={policy}";
    String policy;

    B2CAuthority(final URL authorityUrl){
        super(authorityUrl);
        validateAuthorityUrl();
        setAuthorityProperties();
    }

    private void setAuthorityProperties() {
        String[] segments = canonicalAuthorityUrl.getPath().substring(1).split("/");

        if(segments.length < 3){
            throw new IllegalArgumentException(
                    "B2C 'authority' Uri should have at least 3 segments in the path " +
                            "(i.e. https://<host>/tfp/<tenant>/<policy>/...)");
        }
        policy = segments[2];

        final String b2cAuthorityFormat = "https://%s/%s/%s/%s/";
        this.authority = String.format(
                b2cAuthorityFormat,
                canonicalAuthorityUrl.getAuthority(),
                segments[0],
                segments[1],
                segments[2]);

        this.tokenEndpoint = String.format(B2CTokenEndpointFormat, host);
        this.tokenEndpoint = this.tokenEndpoint.replace("{tenant}", tenant);
        this.tokenEndpoint = this.tokenEndpoint.replace("{policy}", policy);
        this.selfSignedJwtAudience = this.tokenEndpoint;
    }
}
