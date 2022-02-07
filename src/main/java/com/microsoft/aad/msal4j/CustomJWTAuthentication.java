// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.common.contenttype.ContentType;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.JWTAuthentication;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.util.URLUtils;

import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomJWTAuthentication extends ClientAuthentication {
    private ClientAssertion clientAssertion;

    protected CustomJWTAuthentication(ClientAuthenticationMethod method, ClientAssertion clientAssertion, ClientID clientID) {
        super(method, clientID);
        this.clientAssertion = clientAssertion;
    }

    @Override
    public void applyTo(HTTPRequest httpRequest) {
        if (httpRequest.getMethod() != HTTPRequest.Method.POST) {
            throw new SerializeException("The HTTP request method must be POST");
        } else {
            ContentType ct = httpRequest.getEntityContentType();
            if (ct == null) {
                throw new SerializeException("Missing HTTP Content-Type header");
            } else if (!ct.matches(ContentType.APPLICATION_URLENCODED)) {
                throw new SerializeException("The HTTP Content-Type header must be " + ContentType.APPLICATION_URLENCODED);
            } else {
                Map<String, List<String>> params = httpRequest.getQueryParameters();
                params.putAll(this.toParameters());
                String queryString = URLUtils.serializeParameters(params);
                httpRequest.setQuery(queryString);
            }
        }
    }

    public Map<String, List<String>> toParameters() {
        HashMap<String, List<String>> params = new HashMap<>();

        try {
            params.put("client_assertion", Collections.singletonList(this.clientAssertion.assertion()));
        } catch (IllegalStateException var3) {
            throw new SerializeException("Couldn't serialize JWT to a client assertion string: " + var3.getMessage(), var3);
        }

        params.put("client_assertion_type", Collections.singletonList(JWTAuthentication.CLIENT_ASSERTION_TYPE));
        params.put("client_id", Collections.singletonList(getClientID().getValue()));
        return params;
    }
}
