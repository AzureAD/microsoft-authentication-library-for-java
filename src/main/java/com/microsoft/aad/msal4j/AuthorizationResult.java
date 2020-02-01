package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Accessors(fluent = true)
class AuthorizationResult {

    private String code;
    private String state;
    private AuthorizationStatus status;
    private String error;
    private String errorDescription;

    enum AuthorizationStatus {
        Success,
        ErrorHttp,
        ProtocolError,
        UserCancel,
        UnknownError
    }

    public static AuthorizationResult fromResponseBody(String responseBody){

        if(StringHelper.isBlank(responseBody)){
            return new AuthorizationResult(
                    AuthorizationStatus.UnknownError,
                    AuthenticationErrorCode.AUTHORIZATION_RESULT_BLANK,
                    "The authorization server returned an invalid response");
        }

        Map<String, String > queryParameters = parseParameters(responseBody);

        if(queryParameters.containsKey("error")){
            return new AuthorizationResult(
                    AuthorizationStatus.ProtocolError,
                    queryParameters.get("error"),
                    !StringHelper.isBlank(queryParameters.get("error_description")) ?
                            queryParameters.get("error_description") :
                            null);
        }

        AuthorizationResult result = new AuthorizationResult();

        if(queryParameters.containsKey("code")){
            result.code = queryParameters.get("code");
            result.status = AuthorizationStatus.Success;
        }

        if(queryParameters.containsKey("state")){
            result.state = queryParameters.get("state");
        }
        return result;
    }

    private AuthorizationResult(){
    }
    private AuthorizationResult(AuthorizationStatus status, String error, String errorDescription){
        this.status = status;
        this.error = error;
        this.errorDescription = errorDescription;
    }

    private static Map<String, String> parseParameters(String serverResponse) {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        try {
            String[] pairs = serverResponse.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                query_pairs.put(key, value);
            }
        } catch(Exception ex){
            //TODO better exception
            System.out.println(ex.getMessage());
        }

        return query_pairs;
    }


}
