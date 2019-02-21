//----------------------------------------------------------------------
//
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
//
//------------------------------------------------------------------------------

package lapapi;

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
        conn.setReadTimeout(30000);
        conn.setConnectTimeout(30000);

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
