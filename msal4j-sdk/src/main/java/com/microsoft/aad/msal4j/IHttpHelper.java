// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

interface IHttpHelper {

    IHttpResponse executeHttpRequest(HttpRequest httpRequest,
                                     RequestContext requestContext,
                                     ServiceBundle serviceBundle);
}
