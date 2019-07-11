//----------------------------------------------------------------------
//
// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
//
//------------------------------------------------------------------------------

package labapi;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

class HttpClientHelper {

    static String sendRequestToLab( Map<String, String> queryMap, boolean useBetaEndpoint) throws
            IOException {
        final URL labUrl = buildUrl(queryMap, useBetaEndpoint);
        HttpsURLConnection conn = (HttpsURLConnection) labUrl.openConnection();
        conn.setReadTimeout(20000);
        conn.setConnectTimeout(20000);

        StringBuilder content;
        try(BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))){
            String inputLine;
            content = new StringBuilder();
            while((inputLine = in.readLine()) != null){
                content.append(inputLine);
            }
        }
        conn.disconnect();
        return content.toString();
    }

    private static URL buildUrl(Map<String, String> queryMap, boolean useBetaEndpoint) throws
            MalformedURLException, UnsupportedOperationException {
        String queryParameters;
        queryParameters = queryMap.entrySet().stream()
                .map(p -> encodeUTF8(p.getKey()) + "=" + encodeUTF8(p.getValue()))
                .reduce((p1, p2) -> p1 + "&" + p2)
                .orElse("");

        String labEndpoint = (useBetaEndpoint)?
                LabConstants.LAB_BETA_ENDPOINT :
                LabConstants.LAB_ENDPOINT;
        String urlString = labEndpoint + "?" + queryParameters;
        return new URL(urlString);
    }

    private static String encodeUTF8(String s){
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Error: cannot encode query parameter " + s );
        }
    }
}
