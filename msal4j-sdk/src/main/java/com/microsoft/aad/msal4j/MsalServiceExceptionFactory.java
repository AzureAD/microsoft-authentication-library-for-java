// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class MsalServiceExceptionFactory {

    private MsalServiceExceptionFactory() {
    }

    static MsalServiceException fromHttpResponse(IHttpResponse response) {
        String responseBody = response.body();
        if (StringHelper.isBlank(responseBody)) {
            return new MsalServiceException(
                    String.format(
                            "Unknown service exception. Http request returned status code %s with no response body",
                            response.statusCode()),
                    AuthenticationErrorCode.UNKNOWN);
        }

        ErrorResponse errorResponse = JsonHelper.convertJsonStringToJsonSerializableObject(responseBody, ErrorResponse::fromJson);

        if (errorResponse.error() != null &&
                errorResponse.error().equalsIgnoreCase(AuthenticationErrorCode.INVALID_GRANT) && isInteractionRequired(errorResponse.subError)) {
            return new MsalInteractionRequiredException(errorResponse, response.headers());
        }


        if (!StringHelper.isBlank(errorResponse.error()) && !StringHelper.isBlank(errorResponse.errorDescription)) {

            errorResponse.statusCode(response.statusCode());
            return new MsalServiceException(
                    errorResponse,
                    response.headers());
        }

        return new MsalServiceException(
                String.format(
                        "Unknown service exception. Http request returned status code: %s with http body: %s",
                        response.statusCode(),
                        responseBody),
                AuthenticationErrorCode.UNKNOWN);
    }

    private static boolean isInteractionRequired(String subError) {

        String[] nonUiSubErrors = {"client_mismatch", "protection_policy_required"};
        Set<String> set = new HashSet<>(Arrays.asList(nonUiSubErrors));

        if (StringHelper.isBlank(subError)) {
            return true;
        }

        return !set.contains(subError);
    }
}
