// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class MsalServiceExceptionFactory {

    private MsalServiceExceptionFactory() {
    }

    static MsalServiceException fromHttpResponse(HTTPResponse httpResponse) {

        String responseContent = httpResponse.getContent();
        if (responseContent == null || StringHelper.isBlank(responseContent)) {
            return new MsalServiceException(
                    String.format(
                            "Unknown Service Exception. Service returned status code %s",
                            httpResponse.getStatusCode()),
                    AuthenticationErrorCode.UNKNOWN);
        }

        ErrorResponse errorResponse = null;
        try {
            errorResponse = ErrorResponse.convertJsonToObject(responseContent);
        } catch (IOException e) {
            throw new MsalClientException(e);
        }

        errorResponse.statusCode(httpResponse.getStatusCode());
        errorResponse.statusMessage(httpResponse.getStatusMessage());

        if (errorResponse.error() != null &&
                errorResponse.error().equalsIgnoreCase(AuthenticationErrorCode.INVALID_GRANT)) {

            if (isInteractionRequired(errorResponse.subError)) {
                return new MsalInteractionRequiredException(errorResponse, httpResponse.getHeaderMap());
            }
        }

        return new MsalServiceException(
                errorResponse,
                httpResponse.getHeaderMap());
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

        ErrorResponse errorResponse = null;
        try {
            errorResponse = ErrorResponse.convertJsonToObject(
                    responseBody);
        } catch (IOException e) {
            throw new MsalClientException(e);
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
