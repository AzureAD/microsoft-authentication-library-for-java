// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jsample;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.microsoft.aad.msal4j.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class AadController {

    @Autowired
    ServletContext servletContext;

    private String aadGraphDefaultScope =  "https://graph.windows.net/.default";

    private void setUserInfoAndTenant(ModelMap model, AuthenticationResult authenticationResult, HttpSession session){
                String tenant = session.getServletContext().getInitParameter("tenant");
                model.addAttribute("tenant", tenant);
    }

    @RequestMapping("/secure/aad")
    public String getDirectoryObjects(ModelMap model, HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession();
        AuthenticationResult result = (AuthenticationResult) session.getAttribute(AuthHelper.PRINCIPAL_SESSION_NAME);
        if (result == null) {
            model.addAttribute("error", new Exception("AuthenticationResult not found in session."));
            return "/error";
        } else {
            setUserInfoAndTenant(model, result, session);

            String data;
            try {
                String tenant = session.getServletContext().getInitParameter("tenant");
                data = getUserNamesFromGraph(result.accessToken(), tenant);
                model.addAttribute("users", data);
            } catch (Exception e) {
                model.addAttribute("error", e);
                return "/error";
            }
        }
        return "secure/aad";
    }

    private String getUserNamesFromGraph(String accessToken, String tenant) throws Exception {
        URL url = new URL(String.format("https://graph.windows.net/%s/users?api-version=1.6", tenant));

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // Set the appropriate header fields in the request header.
        //conn.setRequestProperty("api-version", "2013-04-05");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/json;");

        int responseCode = conn.getResponseCode();

        String goodRespStr = HttpClientHelper.getResponseStringFromConn(conn, responseCode == conn.HTTP_OK);

        JSONObject response = HttpClientHelper.processGoodRespStr(responseCode, goodRespStr);
        JSONArray users;

        users = JSONHelper.fetchDirectoryObjectJSONArray(response);

        StringBuilder builder = new StringBuilder();
        User user;
        for (int i = 0; i < users.length(); i++) {
            JSONObject thisUserJSONObject = users.optJSONObject(i);
            user = new User();
            //JSONHelper.convertJSONObjectToDirectoryObject(thisUserJSONObject, user);
            //builder.append(user.getUserPrincipalName() + "<br/>");
        }
        return builder.toString();
    }

    private AuthenticationResult acquireTokenForWebApiByRT(String refreshToken)
            throws MalformedURLException, ExecutionException, InterruptedException {
        String clientId = servletContext.getInitParameter("client_id");
        String authority = servletContext.getInitParameter("authority");
        String clientSecret = servletContext.getInitParameter("secret_key");
        String apiIdUri = servletContext.getInitParameter("api_id_uri");

        ConfidentialClientApplication app =
                new ConfidentialClientApplication.
                        Builder(clientId, ClientCredentialFactory.create(clientSecret))
                        .authority(authority).build();

        Future<AuthenticationResult> future =
                app.acquireTokenByRefreshToken(refreshToken, Collections.singleton(apiIdUri));

        AuthenticationResult result = future.get();

        return result;
    }

    private AuthenticationResult acquireTokenForGraphByWebApiUsingObo(String accessToken)
            throws MalformedURLException, ExecutionException, InterruptedException {
        String oboClientId = servletContext.getInitParameter("obo_client_id");
        String oboClientSecret = servletContext.getInitParameter("obo_secret_key");
        String authority = servletContext.getInitParameter("authority");

        ConfidentialClientApplication app =
                    new ConfidentialClientApplication.
                            Builder(oboClientId, ClientCredentialFactory.create(oboClientSecret))
                            .authority(authority).build();

        Future<AuthenticationResult> future =
                    app.acquireTokenOnBehalfOf(Collections.singleton(aadGraphDefaultScope), new UserAssertion(accessToken));

        AuthenticationResult result = future.get();

        return result;
    }

    @RequestMapping(value = "/secure/GetAtForApiByRT", method = RequestMethod.GET)
    public String getATForApiUsingRT(ModelMap model, HttpServletRequest httpRequest) throws MalformedURLException, InterruptedException {
        HttpSession session = httpRequest.getSession();
        AuthenticationResult result = (AuthenticationResult) session.getAttribute(AuthHelper.PRINCIPAL_SESSION_NAME);
        if (result == null) {
            model.addAttribute("error", new Exception("AuthenticationResult not found in session."));
            return "/error";
        } else {
            setUserInfoAndTenant(model, result, session);

            try{
                result = acquireTokenForWebApiByRT(result.refreshToken());

                model.addAttribute("acquiredToken", result.accessToken());
            } catch (ExecutionException e) {
                return "/error";
            }
        }
        return "secure/aad";
    }

    @RequestMapping(value = "/secure/GetAtForApiUsingOboService", method = RequestMethod.GET)
    public String getATForCaProtectedApiUsingOboService(ModelMap model, HttpServletRequest httpRequest) throws MalformedURLException, InterruptedException {
        HttpSession session = httpRequest.getSession();
        AuthenticationResult result = (AuthenticationResult) session.getAttribute(AuthHelper.PRINCIPAL_SESSION_NAME);
        if (result == null) {
            model.addAttribute("error", new Exception("AuthenticationResult not found in session."));
            return "/error";
        } else {

            setUserInfoAndTenant(model, result, session);

            try{
                // get AT for OBO service
                result = acquireTokenForWebApiByRT(result.refreshToken());

                // get AT for AAD graph by OBO service
                result = acquireTokenForGraphByWebApiUsingObo(result.accessToken());

                String tenant = session.getServletContext().getInitParameter("tenant");
                String data = getUserNamesFromGraph(result.accessToken(), tenant);
                model.addAttribute("usersInfo", data);

            } catch (Exception e) {
                return "/error";
            }
        }
        return "secure/aad";
    }
}
