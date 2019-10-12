// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.msalwebsample;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import javax.naming.ServiceUnavailableException;
import javax.servlet.http.HttpServletRequest;

import com.microsoft.aad.msal4j.*;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Getter
class AuthHelper {

    static final String PRINCIPAL_SESSION_NAME = "principal";
    static final String TOKEN_CACHE_SESSION_ATTRIBUTE = "token_cache";

    @Autowired
    BasicConfiguration configuration;

    private ConfidentialClientApplication createClientApplication() throws MalformedURLException {
        return ConfidentialClientApplication.builder(configuration.clientId,
                ClientCredentialFactory.create(configuration.secret))
                .b2cAuthority(configuration.signUpSignInAuthority)
                .build();
    }

    IAuthenticationResult getAuthResultBySilentFlow(HttpServletRequest httpRequest, String scope) throws Throwable {
        IAuthenticationResult result =  AuthHelper.getAuthSessionObject(httpRequest);

        IAuthenticationResult updatedResult;
        ConfidentialClientApplication app;
        try {
            app = createClientApplication();

            Object tokenCache =  httpRequest.getSession().getAttribute("token_cache");
            if(tokenCache != null){
                app.tokenCache().deserialize(tokenCache.toString());
            }

            SilentParameters parameters = SilentParameters.builder(
                    Collections.singleton(scope),
                    result.account()).build();

            CompletableFuture<IAuthenticationResult> future = app.acquireTokenSilently(parameters);

            updatedResult = future.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }

        if (updatedResult == null) {
            throw new ServiceUnavailableException("authentication result was null");
        }

        //update session with latest token cache
        storeTokenCacheInSession(httpRequest, app.tokenCache().serialize());

        return updatedResult;
    }

    IAuthenticationResult getAuthResultByAuthCode(
            HttpServletRequest httpServletRequest,
            AuthorizationCode authorizationCode,
            String currentUri, Set<String> scopes) throws Throwable {

        IAuthenticationResult result;
        ConfidentialClientApplication app;
        try {
            app = createClientApplication();

            String authCode = authorizationCode.getValue();
            AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(
                    authCode,
                    new URI(currentUri))
                    .scopes(scopes)
                    .build();

            Future<IAuthenticationResult> future = app.acquireToken(parameters);

            result = future.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }

        if (result == null) {
            throw new ServiceUnavailableException("authentication result was null");
        }

        storeTokenCacheInSession(httpServletRequest, app.tokenCache().serialize());

        return result;
    }

    private void storeTokenCacheInSession(HttpServletRequest httpServletRequest, String tokenCache){
        httpServletRequest.getSession().setAttribute(AuthHelper.TOKEN_CACHE_SESSION_ATTRIBUTE, tokenCache);
    }

    void setSessionPrincipal(HttpServletRequest httpRequest, IAuthenticationResult result) {
        httpRequest.getSession().setAttribute(AuthHelper.PRINCIPAL_SESSION_NAME, result);
    }

    void removePrincipalFromSession(HttpServletRequest httpRequest) {
        httpRequest.getSession().removeAttribute(AuthHelper.PRINCIPAL_SESSION_NAME);
    }

    void updateAuthDataUsingSilentFlow(HttpServletRequest httpRequest) throws Throwable {
        IAuthenticationResult authResult = getAuthResultBySilentFlow(httpRequest, "https://graph.microsoft.com/.default");
        setSessionPrincipal(httpRequest, authResult);
    }

    static boolean isAuthenticationSuccessful(AuthenticationResponse authResponse) {
        return authResponse instanceof AuthenticationSuccessResponse;
    }

    static boolean isAuthenticated(HttpServletRequest request) {
        return request.getSession().getAttribute(PRINCIPAL_SESSION_NAME) != null;
    }

    static IAuthenticationResult getAuthSessionObject(HttpServletRequest request) {
        Object principalSession = request.getSession().getAttribute(PRINCIPAL_SESSION_NAME);
        if(principalSession instanceof IAuthenticationResult){
            return (IAuthenticationResult) principalSession;
        } else {
            throw new IllegalStateException();
        }
    }

    static boolean containsAuthenticationCode(HttpServletRequest httpRequest) {
        Map<String, String[]> httpParameters = httpRequest.getParameterMap();

        boolean isPostRequest = httpRequest.getMethod().equalsIgnoreCase("POST");
        boolean containsErrorData = httpParameters.containsKey("error");
        boolean containIdToken = httpParameters.containsKey("id_token");
        boolean containsCode = httpParameters.containsKey("code");

        return isPostRequest && containsErrorData || containsCode || containIdToken;
    }
}
