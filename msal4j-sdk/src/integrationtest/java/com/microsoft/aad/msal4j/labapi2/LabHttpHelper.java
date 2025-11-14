package com.microsoft.aad.msal4j.labapi2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

class LabHttpHelper {

    private static final Logger log = LoggerFactory.getLogger(LabHttpHelper.class);


    static String sendRequestToLab(String url, Map<String, String> queryMap, String accessToken) throws IOException {
        return sendRequestToLab(buildUrl(url, queryMap), accessToken);
    }

    static String sendRequestToLab(String url, String accessToken) throws IOException {
        return sendRequestToLab(new URL(url), accessToken);
    }

    static String sendRequestToLab(URL labUrl, String accessToken) throws IOException {
        log.debug("Sending HTTP GET request to: {}", labUrl);

        HttpsURLConnection conn = (HttpsURLConnection) labUrl.openConnection();

        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/json");
        conn.setReadTimeout(30000);  // 30 seconds
        conn.setConnectTimeout(30000);

        int responseCode = conn.getResponseCode();
        log.debug("HTTP response code: {}", responseCode);

        StringBuilder content = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
        } catch (IOException e) {
            log.error("Failed to read response from {}: {}", labUrl, e.getMessage());
            throw e;
        } finally {
            conn.disconnect();
        }

        String response = content.toString();
        log.debug("HTTP response received: {} characters", response.length());

        return response;
    }

    static URL buildUrl(String url, Map<String, String> queryMap) throws MalformedURLException {
        if (queryMap.isEmpty()) {
            return new URL(url);
        }

        StringBuilder queryParameters = new StringBuilder();
        for (Map.Entry<String, String> entry : queryMap.entrySet()) {
            if (queryParameters.length() > 0) {
                queryParameters.append("&");
            }
            queryParameters.append(encodeUTF8(entry.getKey()))
                    .append("=")
                    .append(encodeUTF8(entry.getValue()));
        }

        String urlString = url + "?" + queryParameters;
        return new URL(urlString);
    }

    private static String encodeUTF8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Error: cannot encode query parameter " + s);
        }
    }
}