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

import com.google.gson.Gson;
import org.testng.util.Strings;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

class LabServiceApi{

    LabResponse getLabResponse(UserQuery query) throws LabUserNotFoundException{
        String result;
        try {
            result = sendRequestToLab(query);
        } catch(Exception ex){
            throw new UnsupportedOperationException("Error sending request to lab: " + ex.getMessage());
        }
        if(Strings.isNullOrEmpty(result)){
            throw new LabUserNotFoundException(query,
                    "Lab response is null or empty. No lab user with specified parameter exists");
        }
        Gson gson = new Gson();
        LabResponse labResponse = gson.fromJson(result, LabResponse.class);
        LabUser labUser = labResponse.getUser();

        if(labUser.getHomeTenantId() != null && labUser.getHomeUpn() != null){
            labUser.initializeHomeUser();
        }
        return labResponse;
    }
    private String sendRequestToLab(UserQuery query) throws LabUserNotFoundException, IOException {
        Map<String, String> queryMap = createLabQuery(query);

        final URL labUrl = buildUrl(queryMap);
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

    private Map<String, String> createLabQuery(UserQuery query) {
        Map<String, String> queryMap = new HashMap<String, String>();

        queryMap.put(LabConstants.MOBILE_APP_MANAGEMENT_WITH_CONDITIONAL_ACCESS,
                LabConstants.FALSE);
        queryMap.put(LabConstants.MOBILE_DEVICE_MANAGEMENT_WITH_CONDITIONAL_ACCESS,
                LabConstants.FALSE);

        queryMap.put(LabConstants.MOBILE_APP_MANAGEMENT, (query.isMamUser()) ?
                LabConstants.TRUE :
                LabConstants.FALSE);

        queryMap.put(LabConstants.EXTERNAL, (query.isExternalUser()) ?
                LabConstants.TRUE :
                LabConstants.FALSE);

        queryMap.put(LabConstants.MULTIFACTOR_AUTHENTICATION, (query.isMfaUser()) ?
                LabConstants.TRUE :
                LabConstants.FALSE);

        queryMap.put(LabConstants.FEDERATED_USER, (query.isFederatedUser() ?
                LabConstants.TRUE :
                LabConstants.FALSE));

        if (query.getFederationProvider() != null) {
            queryMap.put(LabConstants.FEDERATION_PROVIDER, query.getFederationProvider().toString());
        }

        if (query.getLicenses() != null && !query.getLicenses().isEmpty()) {
            queryMap.put(LabConstants.LICENSE, query.getLicenses().toArray().toString());
        }

        if (query.getUserType() != null) {
            queryMap.put(LabConstants.USERTYPE, query.getUserType().toString());
        }

        if (query.getB2CIdentityProvider() != null) {
            switch (query.getB2CIdentityProvider()) {
                case LOCAL:
                    queryMap.put(LabConstants.B2C_PROVIDER, LabConstants.B2C_LOCAL);
                    break;
                case GOOGLE:
                    queryMap.put(LabConstants.B2C_PROVIDER, LabConstants.B2C_GOOGLE);
                    break;
                case FACEBOOK:
                    queryMap.put(LabConstants.B2C_PROVIDER, LabConstants.B2C_FACEBOOK);
                    break;
            }
        }
        return queryMap;
    }

    private URL buildUrl(Map<String, String> queryMap) throws MalformedURLException,
            UnsupportedOperationException {
        String queryParameters;

        queryParameters = queryMap.entrySet().stream()
                .map(p -> encodeUTF8(p.getKey()) + "=" + encodeUTF8(p.getValue()))
                .reduce((p1, p2) -> p1 + "&" + p2)
                .orElse("");

        String urlString = LabConstants.LAB_ENDPOINT + "?" + queryParameters;
        return new URL(urlString);
    }

    private String encodeUTF8(String s){
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Error: cannot encode query parameter " + s );
        }
    }
}
