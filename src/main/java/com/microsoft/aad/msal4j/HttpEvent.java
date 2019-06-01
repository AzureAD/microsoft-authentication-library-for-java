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

package com.microsoft.aad.msal4j;

import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;

import static com.microsoft.aad.msal4j.TelemetryConstants.EVENT_NAME_PREFIX;

class HttpEvent extends Event{
    private final static String HTTP_PATH_KEY = EVENT_NAME_PREFIX + "http_path";
    private final static String USER_AGENT_KEY = EVENT_NAME_PREFIX + "user_agent";
    private final static String QUERY_PARAMETERS_KEY = EVENT_NAME_PREFIX + "query_parameters";
    private final static String API_VERSION_KEY = EVENT_NAME_PREFIX + "api_version";
    private final static String RESPONSE_CODE_KEY = EVENT_NAME_PREFIX + "response_code";
    private final static String OAUTH_ERROR_CODE_KEY = EVENT_NAME_PREFIX + "oauth_error_code";
    private final static String HTTP_METHOD_KEY = EVENT_NAME_PREFIX + "http_method";
    private final static String REQUEST_ID_HEADER_KEY = EVENT_NAME_PREFIX + "request_id_header";
    private final static String TOKEN_AGEN_KEY = EVENT_NAME_PREFIX + "token_age";
    private final static String SPE_INFO_KEY = EVENT_NAME_PREFIX + "spe_info";
    private final static String SERVER_ERROR_CODE_KEY = EVENT_NAME_PREFIX  + "server_error_code";
    private final static String SERVER_SUB_ERROR_CODE_KEY = EVENT_NAME_PREFIX + "server_sub_error_code";

    HttpEvent(){
        super(TelemetryConstants.HTTP_EVENT_NAME_KEY);
    }

    void setHttpPath(URI httpPath){
        this.put(HTTP_PATH_KEY, scrubTenant(httpPath));
    }

    void setUserAgent(String userAgent){
        this.put(USER_AGENT_KEY, userAgent.toLowerCase(Locale.ROOT));
    }

    void setQueryParameters(String queryParameters){
        this.put(QUERY_PARAMETERS_KEY, String.join("&", parseQueryParametersAndReturnKeys(queryParameters)));
    }

    void setApiVersion(String apiVersion){
        this.put(API_VERSION_KEY, apiVersion.toLowerCase());
    }

    void setHttpResponseStatus(Integer httpResponseStatus){
        this.put(RESPONSE_CODE_KEY, httpResponseStatus.toString().toLowerCase());
    }

    void setHttpMethod(String httpMethod){
        this.put(HTTP_METHOD_KEY, httpMethod);
    }

    void setOauthErrorCode(String oauthErrorCode){
        this.put(OAUTH_ERROR_CODE_KEY, oauthErrorCode.toLowerCase());
    }

    void setRequestIdHeader(String requestIdHeader){
        this.put(REQUEST_ID_HEADER_KEY, requestIdHeader.toLowerCase());
    }

    private void setTokenAge(String tokenAge){
        this.put(TOKEN_AGEN_KEY, tokenAge.toLowerCase());
    }

    private void setSpeInfo(String speInfo){
        this.put(SPE_INFO_KEY, speInfo.toLowerCase());
    }

    private void setServerErrorCode(String serverErrorCode){
        this.put(SERVER_ERROR_CODE_KEY, serverErrorCode.toLowerCase());
    }

    private void setSubServerErrorCode(String subServerErrorCode){
        this.put(SERVER_SUB_ERROR_CODE_KEY, subServerErrorCode.toLowerCase());
    }

    void setXmsClientTelemetryInfo(XmsClientTelemetryInfo xmsClientTelemetryInfo){
        this.setTokenAge(xmsClientTelemetryInfo.getTokenAge());
        this.setSpeInfo(xmsClientTelemetryInfo.getSpeInfo());
        this.setServerErrorCode(xmsClientTelemetryInfo.getServerErrorCode());
        this.setSubServerErrorCode(xmsClientTelemetryInfo.getServerSubErrorCode());
    }

    private ArrayList<String> parseQueryParametersAndReturnKeys(String queryParams){
        ArrayList<String> queryKeys = new ArrayList<>();
        String[] queryStrings = queryParams.split("&");
        for(String queryString: queryStrings){
            String[] queryPairs =  queryString.split("=");
            if(queryPairs.length == 2 &&
                    !StringHelper.isBlank(queryPairs[0]) &&
                    !StringHelper.isBlank(queryPairs[1])){
                queryKeys.add(queryPairs[0].toLowerCase(Locale.ROOT));
            }
        }
        return queryKeys;
    }
}
