// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class AuthorizationRequestUrlParametersTest {

    @Test
    public void testBuilder_onlyRequiredParameters() throws UnsupportedEncodingException {
        PublicClientApplication app = PublicClientApplication.builder("client_id").build();

        String redirectUri = "http://localhost:8080";
        Set<String> scope = Collections.singleton("scope");

        AuthorizationRequestUrlParameters parameters =
                AuthorizationRequestUrlParameters
                        .builder(redirectUri, scope)
                        .build();

        Assert.assertEquals(parameters.responseMode(), ResponseMode.FORM_POST);
        Assert.assertEquals(parameters.redirectUri(), redirectUri);
        Assert.assertEquals(parameters.scopes().size(), 4);

        Assert.assertNull(parameters.loginHint());
        Assert.assertNull(parameters.codeChallenge());
        Assert.assertNull(parameters.codeChallengeMethod());
        Assert.assertNull(parameters.correlationId());
        Assert.assertNull(parameters.nonce());
        Assert.assertNull(parameters.prompt());
        Assert.assertNull(parameters.state());

        URL authorizationUrl = app.getAuthorizationRequestUrl(parameters);

        Assert.assertEquals(authorizationUrl.getHost(), "login.microsoftonline.com");
        Assert.assertEquals(authorizationUrl.getPath(), "/common/oauth2/v2.0/authorize");

        Map<String, String> queryParameters = new HashMap<>();
        String query = authorizationUrl.getQuery();

        String[] queryPairs = query.split("&");
        for(String pair: queryPairs){
            int idx = pair.indexOf("=");
            queryParameters.put(
                    URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                    URLDecoder.decode(pair.substring(idx+1), "UTF-8"));
        }

        Assert.assertEquals(queryParameters.get("scope"), "offline_access openid profile scope");
        Assert.assertEquals(queryParameters.get("response_type"), "code");
        Assert.assertEquals(queryParameters.get("redirect_uri"), "http://localhost:8080");
        Assert.assertEquals(queryParameters.get("client_id"), "client_id");
        Assert.assertEquals(queryParameters.get("response_mode"), "form_post");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBuilder_invalidRequiredParameters(){
        String redirectUri = "";
        Set<String> scope = Collections.singleton("scope");

        AuthorizationRequestUrlParameters parameters =
                AuthorizationRequestUrlParameters
                        .builder(redirectUri, scope)
                        .build();
    }

    @Test
    public void testBuilder_optionalParameters() throws UnsupportedEncodingException{
        Set<String> clientCapabilities = new HashSet<>();
        clientCapabilities.add("llt");
        clientCapabilities.add("ssm");

        PublicClientApplication app = PublicClientApplication.builder("client_id").clientCapabilities(clientCapabilities).build();

        String redirectUri = "http://localhost:8080";
        Set<String> scope = Collections.singleton("scope");

        AuthorizationRequestUrlParameters parameters =
                AuthorizationRequestUrlParameters
                        .builder(redirectUri, scope)
                        .responseMode(ResponseMode.QUERY)
                        .codeChallenge("challenge")
                        .codeChallengeMethod("method")
                        .state("app_state")
                        .nonce("app_nonce")
                        .correlationId("corr_id")
                        .loginHint("hint")
                        .domainHint("domain_hint")
                        .claims("{\"id_token\":{\"auth_time\":{\"essential\":true}}}")
                        .prompt(Prompt.SELECT_ACCOUNT)
                        .build();

        URL authorizationUrl = app.getAuthorizationRequestUrl(parameters);

        Map<String, String> queryParameters = new HashMap<>();
        String query = authorizationUrl.getQuery();

        String[] queryPairs = query.split("&");
        for(String pair: queryPairs){
            int idx = pair.indexOf("=");
            queryParameters.put(
                    URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                    URLDecoder.decode(pair.substring(idx+1), "UTF-8"));
        }

        Assert.assertEquals(queryParameters.get("scope"), "offline_access openid profile scope");
        Assert.assertEquals(queryParameters.get("response_type"), "code");
        Assert.assertEquals(queryParameters.get("redirect_uri"), "http://localhost:8080");
        Assert.assertEquals(queryParameters.get("client_id"), "client_id");
        Assert.assertEquals(queryParameters.get("prompt"), "select_account");
        Assert.assertEquals(queryParameters.get("response_mode"), "query");
        Assert.assertEquals(queryParameters.get("code_challenge"), "challenge");
        Assert.assertEquals(queryParameters.get("code_challenge_method"), "method");
        Assert.assertEquals(queryParameters.get("state"), "app_state");
        Assert.assertEquals(queryParameters.get("nonce"), "app_nonce");
        Assert.assertEquals(queryParameters.get("correlation_id"), "corr_id");
        Assert.assertEquals(queryParameters.get("login_hint"), "hint");
        Assert.assertEquals(queryParameters.get("domain_hint"), "domain_hint");
        Assert.assertEquals(queryParameters.get("claims"), "{\"id_token\":{\"auth_time\":{\"essential\":true}},\"access_token\":{\"xms_cc\":{\"values\":[\"llt\",\"ssm\"]}}}");
    }
}
