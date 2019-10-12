// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.msalwebsample;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.microsoft.aad.msal4j.*;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import static com.microsoft.azure.msalwebsample.AuthHelper.getAuthSessionObject;

@Controller
public class AuthPageController {

    @Autowired
    AuthHelper authHelper;

    @Autowired
    AuthFilter authFilter;

    @RequestMapping("/msal4jsample")
    public String homepage(){
        return "index";
    }

    @RequestMapping("/msal4jsample/secure/aad")
    public ModelAndView securePage(HttpServletRequest httpRequest) throws ParseException {
        ModelAndView mav = new ModelAndView("auth_page");

        setAccountInfo(mav, httpRequest);

        return mav;
    }

    @RequestMapping("/msal4jsample/sign_out")
    public String signOut(HttpServletRequest httpRequest) {
        httpRequest.getSession().invalidate();

        return "index";
    }

    private void setAccountInfo(ModelAndView model, HttpServletRequest httpRequest) throws ParseException {
        IAuthenticationResult auth = getAuthSessionObject(httpRequest);

        model.addObject("idTokenClaims", JWTParser.parse(auth.idToken()).getJWTClaimsSet().getClaims());

        model.addObject("account", getAuthSessionObject(httpRequest).account());
    }

    @RequestMapping("/b2c-api")
    public ModelAndView callB2CApi(HttpServletRequest httpRequest) throws Throwable {

        ModelAndView mav = new ModelAndView("auth_page");
        setAccountInfo(mav, httpRequest);

        IAuthenticationResult result =  authHelper.getAuthResultBySilentFlow(httpRequest, authHelper.configuration.apiScope);

        String b2cApiCallRes = callB2CApi(result.accessToken());

        mav.addObject("b2c_api_call_res", b2cApiCallRes);

        return mav;
    }

    private String callB2CApi(String accessToken){
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        String result = restTemplate.exchange(authHelper.configuration.api, HttpMethod.GET,
                entity, String.class).getBody();

        return new Date() + result;
    }

    @RequestMapping("/edit-profile")
    public void executeEditProfileB2CPolicy(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Throwable {
        authFilter.sendAuthRedirect(authHelper.configuration.editProfileAuthority, httpRequest, httpResponse);
    }
}
