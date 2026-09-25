// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MtlsEndpointHelperTest {

    @Test
    void preservesGlobalRegionalAndSovereignCloudBoundaries() throws Exception {
        assertEquals("mtlsauth.microsoft.com",
                endpoint("https://login.microsoftonline.com/tenant/oauth2/v2.0/token").getHost());
        assertEquals("westus.mtlsauth.microsoft.com",
                endpoint("https://westus.login.microsoft.com/tenant/oauth2/v2.0/token").getHost());
        assertEquals("mtlsauth.microsoftonline.us",
                endpoint("https://login.microsoftonline.us/tenant/oauth2/v2.0/token").getHost());
        assertEquals("east-us-2.mtlsauth.microsoft.com",
                endpoint("https://east-us-2.login.microsoft.com/tenant/oauth2/v2.0/token").getHost());
    }

    @Test
    void rejectsTenantlessUnsupportedAndUntrustedAuthorities() {
        assertThrows(MsalClientException.class, () ->
                endpoint("https://login.microsoftonline.com/common/oauth2/v2.0/token"));
        assertThrows(MsalClientException.class, () ->
                endpoint("https://login.chinacloudapi.cn/tenant/oauth2/v2.0/token"));
        assertThrows(MsalClientException.class, () ->
                endpoint("https://example.com/tenant/oauth2/v2.0/token"));
        assertThrows(MsalClientException.class, () ->
                endpoint("https://login.attacker.example/tenant/oauth2/v2.0/token"));
        assertThrows(MsalClientException.class, () ->
                endpoint("https://foo.bar.login.microsoft.com/tenant/oauth2/v2.0/token"));
        assertThrows(MsalClientException.class, () ->
                endpoint("https://-westus.login.microsoft.com/tenant/oauth2/v2.0/token"));
        assertThrows(MsalClientException.class, () ->
                endpoint("https://login.microsoftonline.com/%63ommon/oauth2/v2.0/token"));
        assertThrows(MsalClientException.class, () ->
                endpoint("https://login.microsoftonline.com/%43OMMON/oauth2/v2.0/token"));
    }

    private static URL endpoint(String value) throws Exception {
        return MtlsEndpointHelper.deriveMtlsTokenEndpoint(new URL(value));
    }
}
