// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Internal request class for the composite agent token acquisition flow.
 * This request does not create its own grant; actual grants are produced
 * by the inner CCA calls orchestrated by {@link AcquireTokenForAgentSupplier}.
 */
class AcquireTokenForAgentRequest extends MsalRequest {

    AcquireTokenForAgentParameters parameters;

    AcquireTokenForAgentRequest(AcquireTokenForAgentParameters parameters,
                                ConfidentialClientApplication application,
                                RequestContext requestContext) {
        super(application, null, requestContext);
        this.parameters = parameters;
    }
}
