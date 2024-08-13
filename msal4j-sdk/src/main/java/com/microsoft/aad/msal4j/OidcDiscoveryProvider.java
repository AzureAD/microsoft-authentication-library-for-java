// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

class OidcDiscoveryProvider {

    static OidcDiscoveryResponse performOidcDiscovery(OidcAuthority authority, AbstractClientApplicationBase clientApplication) {
        HttpRequest httpRequest = new HttpRequest(
                HttpMethod.GET,
                authority.canonicalAuthorityUrl.toString());

        IHttpResponse httpResponse = ((HttpHelper)clientApplication.serviceBundle.getHttpHelper()).executeHttpRequest(httpRequest);

        OidcDiscoveryResponse response = JsonHelper.convertJsonToObject(httpResponse.body(), OidcDiscoveryResponse.class);

        if (httpResponse.statusCode() != HttpHelper.HTTP_STATUS_200) {
            throw MsalServiceExceptionFactory.fromHttpResponse(httpResponse);
        }

        return response;
    }
}
