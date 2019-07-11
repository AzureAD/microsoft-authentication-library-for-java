// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * This exception class is to inform developers that UI interaction is required for authentication
 * to succeed.
 */
public class MsalInteractionRequiredException extends MsalServiceException{

    /**
     * Reason for the MsalInteractionRequiredException, enabling you to do more actions or inform the
     * user depending on your scenario.
     */
    @Accessors(fluent = true)
    @Getter
    private InteractionRequiredExceptionReason reason;

    /**
     * Initializes a new instance of the exception class
     * @param errorResponse response object contain information about error returned by server
     * @param headerMap http headers from the server response
     */
    public MsalInteractionRequiredException(
            ErrorResponse errorResponse,
            Map<String,List<String>> headerMap) {
        super(errorResponse, headerMap);

        reason = InteractionRequiredExceptionReason.fromSubErrorString(errorResponse.subError);
    }
}
