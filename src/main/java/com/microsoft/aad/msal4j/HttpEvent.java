package com.microsoft.aad.msal4j;

import com.google.common.base.Strings;

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

    public HttpEvent(){
        super(TelemetryConstants.HTTP_EVENT_NAME_KEY);
    }

    public void setHttpPath(URI httpPath){
        this.put(HTTP_PATH_KEY, scrubTenant(httpPath));
    }

    public void setUserAgent(String userAgent){
        this.put(USER_AGENT_KEY, userAgent.toLowerCase(Locale.ROOT));
    }

    public void setQueryParameters(String queryParameters){
        this.put(QUERY_PARAMETERS_KEY, String.join("&", parseQueryParametersAndReturnKeys(queryParameters)));
    }

    public void setApiVersion(String apiVersion){
        this.put(API_VERSION_KEY, apiVersion.toLowerCase());
    }

    public void setHttpResponseStatus(Integer httpResponseStatus){
        this.put(RESPONSE_CODE_KEY, httpResponseStatus.toString().toLowerCase());
    }

    public void setHTTpMethod(String httpMethod){
        this.put(HTTP_METHOD_KEY, httpMethod);
    }

    public void setOauthErrorCode(String oauthErrorCode){
        this.put(OAUTH_ERROR_CODE_KEY, oauthErrorCode.toLowerCase());
    }

    public void setRequestIdHeader(String requestIdHeader){
        this.put(REQUEST_ID_HEADER_KEY, requestIdHeader.toLowerCase());
    }

    public void setTokenAge(String tokenAge){
        this.put(TOKEN_AGEN_KEY, tokenAge.toLowerCase());
    }

    public void setSpeInfo(String speInfo){
        this.put(SPE_INFO_KEY, speInfo.toLowerCase());
    }

    public void setServerErrorCode(String serverErrorCode){
        this.put(SERVER_ERROR_CODE_KEY, serverErrorCode.toLowerCase());
    }

    public void setSubServerErrorCode(String subServerErrorCode){
        this.put(SERVER_SUB_ERROR_CODE_KEY, subServerErrorCode.toLowerCase());
    }

    private ArrayList<String> parseQueryParametersAndReturnKeys(String queryParams){
        ArrayList<String> queryKeys = new ArrayList<>();
        String[] queryStrings = queryParams.split("&");
        for(String queryString: queryStrings){
            String[] queryPairs =  queryString.split("=");
            if(queryPairs.length == 2 &&
                    !Strings.isNullOrEmpty(queryPairs[0]) &&
                    !Strings.isNullOrEmpty(queryPairs[1])){
                queryKeys.add(queryPairs[0].toLowerCase(Locale.ROOT));
            }
        }
        return queryKeys;
    }




}
