// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.util.URLUtils;

class ClientAuthenticationPost extends ClientAuthentication {

    protected ClientAuthenticationPost(ClientAuthenticationMethod method,
                                       ClientID clientID) {
        super(method, clientID);
    }

    Map<String, List<String>> toParameters() {

        Map<String, List<String>> params = new HashMap<>();

        params.put("client_id", Collections.singletonList(getClientID().getValue()));

        return params;
    }

    @Override
    public void applyTo(HTTPRequest httpRequest) throws SerializeException {

        if (httpRequest.getMethod() != HTTPRequest.Method.POST)
            throw new SerializeException("The HTTP request method must be POST");

        String ct = String.valueOf(httpRequest.getEntityContentType());

        if (ct == null)
            throw new SerializeException("Missing HTTP Content-Type header");

        if (!ct.equals(HTTPContentType.ApplicationURLEncoded.contentType))
            throw new SerializeException(
                    "The HTTP Content-Type header must be "
                            + HTTPContentType.ApplicationURLEncoded.contentType);

        Map<String, List<String>> params = httpRequest.getQueryParameters();

        params.putAll(toParameters());

        String queryString = URLUtils.serializeParameters(params);

        httpRequest.setQuery(queryString);
    }
}
