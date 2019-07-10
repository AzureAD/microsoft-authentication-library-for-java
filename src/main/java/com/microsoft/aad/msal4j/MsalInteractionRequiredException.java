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

    @Accessors(fluent = true)
    @Getter
    private ServiceExceptionClassification classification;

    public MsalInteractionRequiredException(
            ErrorResponse errorResponse,
            Map<String,List<String>> headerMap) {
        super(errorResponse, headerMap);

        classification = ServiceExceptionClassification.fromSubErrorString(errorResponse.subError);
    }
}
