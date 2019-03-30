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

package com.microsoft.aad.msal4jsample;

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

public class BasicFilter implements Filter {

    public static final String STATES = "states";
    public static final String STATE = "state";
    public static final Integer STATE_TTL = 3600;
    public static final String FAILED_TO_VALIDATE_MESSAGE = "Failed to validate data received from Authorization service - ";
    private String clientId;
    private String clientSecret;
    private String authority;
    private String redirectUri;

    public void destroy() {

    }

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            try {
                String currentUri = httpRequest.getRequestURL().toString();
                String queryStr = httpRequest.getQueryString();
                String fullUrl = currentUri + (queryStr != null ? "?" + queryStr : "");

                // check if user has a AuthData in the session
                if (!AuthHelper.isAuthenticated(httpRequest)) {
                    if (AuthHelper.containsAuthenticationData(httpRequest)) {
                        processAuthenticationData(httpRequest, currentUri, fullUrl);
                    } else {
                        // not authenticated
                        sendAuthRedirect(httpRequest, httpResponse);
                        return;
                    }
                }
                if (isAuthDataExpired(httpRequest)) {
                    updateAuthDataUsingRefreshToken(httpRequest);
                }
            } catch (AuthenticationException authException) {
                // something went wrong (like expiration or revocation of token)
                // we should invalidate AuthData stored in session and redirect to Authorization server
                removePrincipalFromSession(httpRequest);
                sendAuthRedirect(httpRequest, httpResponse);
                return;
            } catch (Throwable exc) {
                httpResponse.setStatus(500);
                request.setAttribute("error", exc.getMessage());
                request.getRequestDispatcher("/error.jsp").forward(request, response);
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isAuthDataExpired(HttpServletRequest httpRequest) {
        AuthenticationResult authRes = AuthHelper.getAuthSessionObject(httpRequest);
        Date expireDate = new Date(authRes.expiresOn());
        return expireDate.before(new Date()) ? true : false;
    }

    private void updateAuthDataUsingRefreshToken(HttpServletRequest httpRequest) throws Throwable {
        AuthenticationResult authData =
                getAccessTokenByRefreshToken(AuthHelper.getAuthSessionObject(httpRequest).refreshToken());
        setSessionPrincipal(httpRequest, authData);
    }

    private void processAuthenticationData(HttpServletRequest httpRequest, String currentUri, String fullUrl)
            throws Throwable {
        Map<String, String> params = new HashMap();
        for (String key : httpRequest.getParameterMap().keySet()) {
            params.put(key, httpRequest.getParameterMap().get(key)[0]);
        }
        // validate that state in response equals to state in request
        StateData stateData = validateState(httpRequest.getSession(), params.get(STATE));

        AuthenticationResponse authResponse = AuthenticationResponseParser.parse(new URI(fullUrl), params);
        if (AuthHelper.isAuthenticationSuccessful(authResponse)) {
            AuthenticationSuccessResponse oidcResponse = (AuthenticationSuccessResponse) authResponse;
            // validate that OIDC Auth Response matches Code Flow (contains only requested artifacts)
            validateAuthRespMatchesCodeFlow(oidcResponse);

            AuthenticationResult authData =
                    getAccessTokenByAuthCode(oidcResponse.getAuthorizationCode(), currentUri);
            // validate nonce to prevent reply attacks (code maybe substituted to one with broader access)
            validateNonce(stateData, getClaimValueFromIdToken(authData.idToken(), "nonce"));

            setSessionPrincipal(httpRequest, authData);
        } else {
            AuthenticationErrorResponse oidcResponse = (AuthenticationErrorResponse) authResponse;
            throw new Exception(String.format("Request for auth code failed: %s - %s",
                    oidcResponse.getErrorObject().getCode(),
                    oidcResponse.getErrorObject().getDescription()));
        }
    }

    private void validateNonce(StateData stateData, String nonce) throws Exception {
        if (StringUtils.isEmpty(nonce) || !nonce.equals(stateData.getNonce())) {
            throw new Exception(FAILED_TO_VALIDATE_MESSAGE + "could not validate nonce");
        }
    }

    private String getClaimValueFromIdToken(String idToken, String claimKey) throws ParseException {
        return (String) JWTParser.parse(idToken).getJWTClaimsSet().getClaim(claimKey);
    }

    private void sendAuthRedirect(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        httpResponse.setStatus(302);

        // use state parameter to validate response from Authorization server
        String state = UUID.randomUUID().toString();

        // use nonce parameter to validate idToken
        String nonce = UUID.randomUUID().toString();

        storeStateInSession(httpRequest.getSession(), state, nonce);

        String currentUri = httpRequest.getRequestURL().toString();

        httpResponse.sendRedirect(getRedirectUrl(currentUri, httpRequest.getParameter("claims"), state, nonce));
    }

    /**
     * make sure that state is stored in the session,
     * delete it from session - should be used only once
     *
     * @param session
     * @param state
     * @throws Exception
     */
    private StateData validateState(HttpSession session, String state) throws Exception {
        if (StringUtils.isNotEmpty(state)) {
            StateData stateDataInSession = removeStateFromSession(session, state);
            if (stateDataInSession != null) {
                return stateDataInSession;
            }
        }
        throw new Exception(FAILED_TO_VALIDATE_MESSAGE + "could not validate state");
    }

    private void validateAuthRespMatchesCodeFlow(AuthenticationSuccessResponse oidcResponse) throws Exception {
        if (oidcResponse.getIDToken() != null || oidcResponse.getAccessToken() != null ||
                oidcResponse.getAuthorizationCode() == null) {
            throw new Exception(FAILED_TO_VALIDATE_MESSAGE + "unexpected set of artifacts received");
        }
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

    private void storeStateInSession(HttpSession session, String state, String nonce) {
        if (session.getAttribute(STATES) == null) {
            session.setAttribute(STATES, new HashMap<String, StateData>());
        }
        ((Map<String, StateData>) session.getAttribute(STATES)).put(state, new StateData(nonce, new Date()));
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

    ConfidentialClientApplication createClientApplication() throws MalformedURLException {
        return new ConfidentialClientApplication.Builder(clientId, ClientCredentialFactory.create(clientSecret))
                .authority(authority).build();
    }

    private AuthenticationResult getAccessTokenByRefreshToken(
            String refreshToken) throws Throwable {
        AuthenticationResult result;

        try {
            ConfidentialClientApplication app = createClientApplication();

            CompletableFuture<AuthenticationResult> future = app.acquireTokenByRefreshToken(refreshToken, null);

            result = future.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }

        if (result == null) {
            throw new ServiceUnavailableException("authentication result was null");
        }
        return result;
    }

    private AuthenticationResult getAccessTokenByAuthCode(
            AuthorizationCode authorizationCode, String currentUri)
            throws Throwable {
        String authCode = authorizationCode.getValue();
        AuthenticationResult result;

        try {
            ConfidentialClientApplication app = createClientApplication();

            Future<AuthenticationResult> future = app
                    .acquireTokenByAuthorizationCode(null, authCode, new URI(currentUri));

            result = future.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }

        if (result == null) {
            throw new ServiceUnavailableException("authentication result was null");
        }
        return result;
    }

    private void setSessionPrincipal(HttpServletRequest httpRequest,
                                     AuthenticationResult result) {
        httpRequest.getSession().setAttribute(AuthHelper.PRINCIPAL_SESSION_NAME, result);
    }

    private void removePrincipalFromSession(HttpServletRequest httpRequest) {
        httpRequest.getSession().removeAttribute(AuthHelper.PRINCIPAL_SESSION_NAME);
    }

    private String getRedirectUrl(String currentUri, String claims, String state, String nonce)
            throws UnsupportedEncodingException {
        String redirectUrl = authority  + "oauth2/v2.0/authorize?" +
                "response_type=code&" +
                "response_mode=form_post&" +
                "redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8") +
                "&client_id=" + clientId +
                "&scope=" + URLEncoder.encode("openid offline_access profile https://graph.windows.net/.default", "UTF-8") +
                //"&scope=https%3a%2f%2fgraph.windows.net/.default" +
                (StringUtils.isEmpty(claims) ? "" : "&claims=" + claims)  +
                "&state=" + state
                + "&nonce=" + nonce;

        return redirectUrl;
    }

    public void init(FilterConfig config) throws ServletException {
        clientId = config.getServletContext().getInitParameter("client_id");
        authority = config.getServletContext().getInitParameter("authority");
        clientSecret = config.getServletContext().getInitParameter("secret_key");
        redirectUri = config.getServletContext().getInitParameter("redirect_uri");
    }

    private class StateData {
        private String nonce;
        private Date expirationDate;

        public StateData(String nonce, Date expirationDate) {
            this.nonce = nonce;
            this.expirationDate = expirationDate;
        }

        public String getNonce() {
            return nonce;
        }

        public Date getExpirationDate() {
            return expirationDate;
        }
    }
}
