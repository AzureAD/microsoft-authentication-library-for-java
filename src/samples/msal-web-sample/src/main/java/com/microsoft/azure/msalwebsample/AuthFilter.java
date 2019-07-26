// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.msalwebsample;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.*;

import javax.naming.ServiceUnavailableException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.microsoft.aad.msal4j.*;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.openid.connect.sdk.AuthenticationErrorResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthFilter implements Filter {

    private static final String STATES = "states";
    private static final String STATE = "state";
    private static final Integer STATE_TTL = 3600;
    private static final String FAILED_TO_VALIDATE_MESSAGE = "Failed to validate data received from Authorization service - ";

    private List<String> excludedUrls = Arrays.asList("/", "/msal4jsample/");

    @Autowired
    AuthHelper authHelper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            try {
                String currentUri = httpRequest.getRequestURL().toString();
                String path = httpRequest.getServletPath();
                String queryStr = httpRequest.getQueryString();
                String fullUrl = currentUri + (queryStr != null ? "?" + queryStr : "");

                // exclude home page
                if(excludedUrls.contains(path)){
                    chain.doFilter(request, response);
                    return;
                }
                // check if user has a AuthData in the session
                if (!AuthHelper.isAuthenticated(httpRequest)) {
                    if(AuthHelper.containsAuthenticationCode(httpRequest)){
                        // response should have authentication code, which will be used to acquire access token
                        processAuthenticationCodeRedirect(httpRequest, currentUri, fullUrl);
                    } else {
                        // not authenticated, redirecting to login.microsoft.com so user can authenticate
                        sendAuthRedirect(httpRequest, httpResponse);
                        return;
                    }
                }
                if (isAccessTokenExpired(httpRequest)) {
                    authHelper.updateAuthDataUsingSilentFlow(httpRequest);
                }
            } catch (MsalException authException) {
                // something went wrong (like expiration or revocation of token)
                // we should invalidate AuthData stored in session and redirect to Authorization server
                authHelper.removePrincipalFromSession(httpRequest);
                sendAuthRedirect(httpRequest, httpResponse);
                return;
            } catch (Throwable exc) {
                httpResponse.setStatus(500);
                request.setAttribute("error", exc.getMessage());
                request.getRequestDispatcher("/error").forward(request, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isAccessTokenExpired(HttpServletRequest httpRequest) {
        IAuthenticationResult result = AuthHelper.getAuthSessionObject(httpRequest);
        return result.expiresOnDate().before(new Date());
    }

    private void processAuthenticationCodeRedirect(HttpServletRequest httpRequest, String currentUri, String fullUrl)
            throws Throwable {

        Map<String, List<String>> params = new HashMap<>();
        for (String key : httpRequest.getParameterMap().keySet()) {
            params.put(key, Collections.singletonList(httpRequest.getParameterMap().get(key)[0]));
        }
        // validate that state in response equals to state in request
        StateData stateData = validateState(httpRequest.getSession(), params.get(STATE).get(0));

        AuthenticationResponse authResponse = AuthenticationResponseParser.parse(new URI(fullUrl), params);
        if (AuthHelper.isAuthenticationSuccessful(authResponse)) {
            AuthenticationSuccessResponse oidcResponse = (AuthenticationSuccessResponse) authResponse;
            // validate that OIDC Auth Response matches Code Flow (contains only requested artifacts)
            validateAuthRespMatchesAuthCodeFlow(oidcResponse);

            IAuthenticationResult result = authHelper.getAuthResultByAuthCode(
                    httpRequest,
                    oidcResponse.getAuthorizationCode(),
                    currentUri);

            // validate nonce to prevent reply attacks (code maybe substituted to one with broader access)
            validateNonce(stateData, getNonceClaimValueFromIdToken(result.idToken()));
            authHelper.setSessionPrincipal(httpRequest, result);
        } else {
            AuthenticationErrorResponse oidcResponse = (AuthenticationErrorResponse) authResponse;
            throw new Exception(String.format("Request for auth code failed: %s - %s",
                    oidcResponse.getErrorObject().getCode(),
                    oidcResponse.getErrorObject().getDescription()));
        }
    }

    private void sendAuthRedirect(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        // state parameter to validate response from Authorization server and nonce parameter to validate idToken
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        storeStateInSession(httpRequest.getSession(), state, nonce);

        httpResponse.setStatus(302);
        String redirectUrl = getRedirectUrl(httpRequest.getParameter("claims"), state, nonce);
        httpResponse.sendRedirect(redirectUrl);
    }

    private void validateNonce(StateData stateData, String nonce) throws Exception {
        if (StringUtils.isEmpty(nonce) || !nonce.equals(stateData.getNonce())) {
            throw new Exception(FAILED_TO_VALIDATE_MESSAGE + "could not validate nonce");
        }
    }

    private String getNonceClaimValueFromIdToken(String idToken) throws ParseException {
        return (String) JWTParser.parse(idToken).getJWTClaimsSet().getClaim("nonce");
    }

    private StateData validateState(HttpSession session, String state) throws Exception {
        if (StringUtils.isNotEmpty(state)) {
            StateData stateDataInSession = removeStateFromSession(session, state);
            if (stateDataInSession != null) {
                return stateDataInSession;
            }
        }
        throw new Exception(FAILED_TO_VALIDATE_MESSAGE + "could not validate state");
    }

    private void validateAuthRespMatchesAuthCodeFlow(AuthenticationSuccessResponse oidcResponse) throws Exception {
        if (oidcResponse.getIDToken() != null || oidcResponse.getAccessToken() != null ||
                oidcResponse.getAuthorizationCode() == null) {
            throw new Exception(FAILED_TO_VALIDATE_MESSAGE + "unexpected set of artifacts received");
        }
    }

    private void storeStateInSession(HttpSession session, String state, String nonce) {
        if (session.getAttribute(STATES) == null) {
            session.setAttribute(STATES, new HashMap<String, StateData>());
        }
        ((Map<String, StateData>) session.getAttribute(STATES)).put(state, new StateData(nonce, new Date()));
    }

    private StateData removeStateFromSession(HttpSession session, String state) {
        Map<String, StateData> states = (Map<String, StateData>) session.getAttribute(STATES);
        if (states != null) {
            eliminateExpiredStates(states);
            StateData stateData = states.get(state);
            if (stateData != null) {
                states.remove(state);
                return stateData;
            }
        }
        return null;
    }

    private void eliminateExpiredStates(Map<String, StateData> map) {
        Iterator<Map.Entry<String, StateData>> it = map.entrySet().iterator();

        Date currTime = new Date();
        while (it.hasNext()) {
            Map.Entry<String, StateData> entry = it.next();
            long diffInSeconds = TimeUnit.MILLISECONDS.
                    toSeconds(currTime.getTime() - entry.getValue().getExpirationDate().getTime());

            if (diffInSeconds > STATE_TTL) {
                it.remove();
            }
        }
    }

    private String getRedirectUrl(String claims, String state, String nonce)
            throws UnsupportedEncodingException {

        String redirectUrl = authHelper.getAuthority() + "oauth2/v2.0/authorize?" +
                "response_type=code&" +
                "response_mode=form_post&" +
                "redirect_uri=" + URLEncoder.encode(authHelper.getRedirectUri(), "UTF-8") +
                "&client_id=" + authHelper.getClientId() +
                "&scope=" + URLEncoder.encode("openid offline_access profile", "UTF-8") +
                (StringUtils.isEmpty(claims) ? "" : "&claims=" + claims) +
                "&prompt=select_account" +
                "&state=" + state
                + "&nonce=" + nonce;

        return redirectUrl;
    }
}
