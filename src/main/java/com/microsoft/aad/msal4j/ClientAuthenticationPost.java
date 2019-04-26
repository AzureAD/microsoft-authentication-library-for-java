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

import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.ContentType;

import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.http.CommonContentTypes;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.util.URLUtils;

class ClientAuthenticationPost extends ClientAuthentication {

    protected ClientAuthenticationPost(ClientAuthenticationMethod method,
            ClientID clientID) {
        super(method, clientID);
    }

    Map<String, String> toParameters() {

        Map<String, String> params = new HashMap<String, String>();

        params.put("client_id", getClientID().getValue());

        return params;
    }

    @Override
    public void applyTo(HTTPRequest httpRequest) throws SerializeException {

        if (httpRequest.getMethod() != HTTPRequest.Method.POST)
            throw new SerializeException("The HTTP request method must be POST");

        ContentType ct = httpRequest.getContentType();

        if (ct == null)
            throw new SerializeException("Missing HTTP Content-Type header");

        if (!ct.match(CommonContentTypes.APPLICATION_URLENCODED))
            throw new SerializeException(
                    "The HTTP Content-Type header must be "
                            + CommonContentTypes.APPLICATION_URLENCODED);

        Map<String, String> params = httpRequest.getQueryParameters();

        params.putAll(toParameters());

        String queryString = URLUtils.serializeParameters(params);

        httpRequest.setQuery(queryString);
    }

}
