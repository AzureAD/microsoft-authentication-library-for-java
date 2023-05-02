// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;

public class CIAMAuthority extends Authority{

    public static final String CIAM_HOST_SEGMENT = ".ciamlogin.com";

    static final String AUTHORIZATION_ENDPOINT = "oauth2/v2.0/authorize";
    static final String TOKEN_ENDPOINT = "oauth2/v2.0/token";
    static final String DEVICE_CODE_ENDPOINT = "oauth2/v2.0/devicecode";

    private static final String CIAM_AUTHORITY_FORMAT = "https://%s/%s/";
    private static final String DEVICE_CODE_ENDPOINT_FORMAT = CIAM_AUTHORITY_FORMAT + DEVICE_CODE_ENDPOINT;

    private static final String CIAM_AUTHORIZATION_ENDPOINT_FORMAT = CIAM_AUTHORITY_FORMAT + AUTHORIZATION_ENDPOINT;
    private static final String CIAM_TOKEN_ENDPOINT_FORMAT = CIAM_AUTHORITY_FORMAT + TOKEN_ENDPOINT;

    CIAMAuthority(URL authorityUrl) throws MalformedURLException {
        super(transformAuthority(authorityUrl), AuthorityType.CIAM);
        setAuthorityProperties();
        this.authority = String.format(CIAM_AUTHORITY_FORMAT,host,tenant);
    }

    /** This method takes a CIAM authority string of format "tenant.ciamlogin.com" or "https://tenant.ciamlogin.com"
     and converts it into a full authority url with a path segment of format "/tenant.onmicrosoft.com"
     * @param originalAuthority authority to be transformed
     * @return full CIAM authority with path
     */
    protected static URL transformAuthority(URL originalAuthority) throws MalformedURLException {
        String host = originalAuthority.getHost() + originalAuthority.getPath();
        String transformedAuthority = originalAuthority.toString();
        if(originalAuthority.getPath().equals("/")){
            int ciamHostIndex = host.indexOf(CIAMAuthority.CIAM_HOST_SEGMENT);
            String tenant = host.substring(0 , ciamHostIndex);
            transformedAuthority = originalAuthority + tenant + ".onmicrosoft.com/";
        }
        return new URL(transformedAuthority);
    }

    private void setAuthorityProperties() {
        this.authorizationEndpoint = String.format(CIAM_AUTHORIZATION_ENDPOINT_FORMAT, host, tenant);
        this.tokenEndpoint = String.format(CIAM_TOKEN_ENDPOINT_FORMAT, host, tenant);
        this.deviceCodeEndpoint = String.format(DEVICE_CODE_ENDPOINT_FORMAT, host, tenant);
        this.selfSignedJwtAudience = this.tokenEndpoint;
    }
}
