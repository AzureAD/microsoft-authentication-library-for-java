// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class MsalServiceExceptionFactory {

    private MsalServiceExceptionFactory(){
    }

    static MsalServiceException fromHttpResponse(HTTPResponse httpResponse){

        String responseContent = httpResponse.getContent();
        if(responseContent == null || StringHelper.isBlank(responseContent)){
            return new MsalServiceException(
                    "Unknown Service Exception",
                    AuthenticationErrorCode.UNKNOWN);
        }

        ErrorResponse errorResponse = JsonHelper.convertJsonToObject(
                responseContent,
                ErrorResponse.class);

        errorResponse.statusCode(httpResponse.getStatusCode());
        errorResponse.statusMessage(httpResponse.getStatusMessage());


        boolean bool = errorResponse.error().equalsIgnoreCase(AuthenticationErrorCode.INVALID_GRANT);

        if(errorResponse.error() != null &&
                errorResponse.error().equalsIgnoreCase(AuthenticationErrorCode.INVALID_GRANT)) {

            if(isInteractionRequired(errorResponse.subError)){
                return new MsalInteractionRequiredException(errorResponse, httpResponse.getHeaderMap());
            }
        }

        return new MsalServiceException(
                errorResponse,
                httpResponse.getHeaderMap());
        }

    private static boolean isInteractionRequired(String subError){

        String[] nonUiSubErrors = {"client_mismatch", "protection_policy_required"};
        Set<String> set = new HashSet<>(Arrays.asList(nonUiSubErrors));

        if(StringHelper.isBlank(subError)){
            return true;
        }

        return !set.contains(subError);
    }
}
