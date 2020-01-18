// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.springsecuritywebapp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@EnableSpringHttpSession
@Configuration
public class SessionConfig {

    @Bean
    public MapSessionRepository sessionRepository() {
        return new MapSessionRepository(new ConcurrentHashMap<>());
    }

    @Bean
    public CookieSerializer defaultCookieSerializer() {
        BrowserAwareSameSiteAttributeSerializer cookieSerializer = new BrowserAwareSameSiteAttributeSerializer();

        cookieSerializer.setCookieName("JSESSIONID");
        cookieSerializer.setSameSite("None");

        return cookieSerializer;
    }

    /**
     * Check whether user agent support "None" value of "SameSite" attribute of cookies
     *
     * The following code is for demonstration only: It should not be considered complete.
     * It is not maintained or supported.
     *
     * @param userAgent
     * @return true if user agent supports "None" value of "SameSite" attribute of cookies,
     * false otherwise
     */
    private boolean isUserAgentAwareOfSameSiteNone(String userAgent){

        // Cover all iOS based browsers here. This includes:
        // - Safari on iOS 12 for iPhone, iPod Touch, iPad
        // - WkWebview on iOS 12 for iPhone, iPod Touch, iPad
        // - Chrome on iOS 12 for iPhone, iPod Touch, iPad
        // All of which are broken by SameSite=None, because they use the iOS networking
        // stack.
        if(userAgent.contains("CPU iPhone OS 12") || userAgent.contains("iPad; CPU OS 12")){
            return false;
        }

        // Cover Mac OS X based browsers that use the Mac OS networking stack.
        // This includes:
        // - Safari on Mac OS X.
        // This does not include:
        // - Chrome on Mac OS X
        // Because they do not use the Mac OS networking stack.
        if (userAgent.contains("Macintosh; Intel Mac OS X 10_14") &&
                userAgent.contains("Version/") && userAgent.contains("Safari")) {
            return false;
        }

        // Cover Chrome 50-69, because some versions are broken by SameSite=None,
        // and none in this range require it.
        // Note: this covers some pre-Chromium Edge versions,
        // but pre-Chromium Edge does not require SameSite=None.
        if(userAgent.contains("Chrome/5") || userAgent.contains("Chrome/6")){
            return false;
        }

        return true;
    }

    class BrowserAwareSameSiteAttributeSerializer extends DefaultCookieSerializer{
        @Override
        public void writeCookieValue(CookieValue cookieValue) {
            super.writeCookieValue(cookieValue);

            HttpServletRequest request = cookieValue.getRequest();
            HttpServletResponse response = cookieValue.getResponse();

            if(!isUserAgentAwareOfSameSiteNone(request.getHeader("User-Agent"))){
                // remove sameSite attribute from cookies
                List<String> headers = new ArrayList<>(response.getHeaders("Set-Cookie"));

                for (int i = 0; i < headers.size(); i++) {
                    String noSameSiteHeaderVal = headers.get(i).replaceAll("; SameSite=[a-zA-Z]*", "");
                    if (i == 0) {
                        response.setHeader("Set-Cookie", noSameSiteHeaderVal);
                    }
                    else {
                        response.addHeader("Set-Cookie", noSameSiteHeaderVal);
                    }
                }
            }
        }
    }
}
