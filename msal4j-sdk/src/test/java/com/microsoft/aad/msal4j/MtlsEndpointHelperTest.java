// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MtlsEndpointHelperTest {

    private static URL url(String spec) throws Exception {
        return new URL(spec);
    }

    @Test
    void deriveMtlsTokenEndpoint_noRegion_usesGlobalMtlsHost() throws Exception {
        URL result = MtlsEndpointHelper.deriveMtlsTokenEndpoint(
                url("https://login.microsoftonline.com/contoso.onmicrosoft.com/oauth2/v2.0/token"));

        assertEquals("mtlsauth.microsoft.com", result.getHost());
        assertEquals("/contoso.onmicrosoft.com/oauth2/v2.0/token", result.getPath());
        assertEquals("https", result.getProtocol());
    }

    @Test
    void deriveMtlsTokenEndpoint_regionalHost_preservesRegionPrefix() throws Exception {
        URL result = MtlsEndpointHelper.deriveMtlsTokenEndpoint(
                url("https://westus.login.microsoft.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/oauth2/v2.0/token"));

        assertEquals("westus.mtlsauth.microsoft.com", result.getHost());
        assertEquals("/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/oauth2/v2.0/token", result.getPath());
    }

    @Test
    void deriveMtlsHost_variants() {
        assertEquals("mtlsauth.microsoft.com", MtlsEndpointHelper.deriveMtlsHost("login.microsoftonline.com"));
        assertEquals("mtlsauth.microsoft.com", MtlsEndpointHelper.deriveMtlsHost("login.microsoft.com"));
        assertEquals("mtlsauth.microsoft.com", MtlsEndpointHelper.deriveMtlsHost("login.windows.net"));
        assertEquals("eastus.mtlsauth.microsoft.com", MtlsEndpointHelper.deriveMtlsHost("eastus.login.microsoft.com"));
    }

    @Test
    void extractTenant_returnsFirstPathSegment() throws Exception {
        assertEquals("mytenant", MtlsEndpointHelper.extractTenant(
                url("https://login.microsoftonline.com/mytenant/oauth2/v2.0/token")));
    }

    @Test
    void deriveMtlsTokenEndpoint_commonAuthority_isRejected() {
        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                MtlsEndpointHelper.deriveMtlsTokenEndpoint(
                        url("https://login.microsoftonline.com/common/oauth2/v2.0/token")));

        assertEquals(AuthenticationErrorCode.MTLS_POP_ERROR, ex.errorCode());
        assertTrue(ex.getMessage().toLowerCase().contains("tenanted"));
    }

    @Test
    void deriveMtlsTokenEndpoint_organizationsAuthority_isRejected() {
        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                MtlsEndpointHelper.deriveMtlsTokenEndpoint(
                        url("https://login.microsoftonline.com/organizations/oauth2/v2.0/token")));

        assertEquals(AuthenticationErrorCode.MTLS_POP_ERROR, ex.errorCode());
    }

    @Test
    void deriveMtlsTokenEndpoint_usGovCloud_failsFast() {
        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                MtlsEndpointHelper.deriveMtlsTokenEndpoint(
                        url("https://login.microsoftonline.us/contoso.onmicrosoft.com/oauth2/v2.0/token")));

        assertEquals(AuthenticationErrorCode.MTLS_POP_ERROR, ex.errorCode());
        assertTrue(ex.getMessage().toLowerCase().contains("cloud"));
    }

    @Test
    void deriveMtlsTokenEndpoint_chinaCloud_failsFast() {
        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                MtlsEndpointHelper.deriveMtlsTokenEndpoint(
                        url("https://login.partner.microsoftonline.cn/contoso/oauth2/v2.0/token")));

        assertEquals(AuthenticationErrorCode.MTLS_POP_ERROR, ex.errorCode());
    }

    @Test
    void isMtlsPoPUnsupportedCloud_predicate() {
        assertFalse(MtlsEndpointHelper.isMtlsPoPUnsupportedCloud("login.microsoftonline.com"));
        assertFalse(MtlsEndpointHelper.isMtlsPoPUnsupportedCloud("westus.login.microsoft.com"));

        assertTrue(MtlsEndpointHelper.isMtlsPoPUnsupportedCloud("login.microsoftonline.us"));
        assertTrue(MtlsEndpointHelper.isMtlsPoPUnsupportedCloud("login.partner.microsoftonline.cn"));
        assertTrue(MtlsEndpointHelper.isMtlsPoPUnsupportedCloud("login.chinacloudapi.cn"));
    }

    // Regression: every sovereign host in the trusted set must fail fast (none may fall through to the
    // public mtlsauth endpoint). The last five previously slipped past the substring denylist.
    @Test
    void isMtlsPoPUnsupportedCloud_coversAllSovereignHosts() {
        String[] sovereignHosts = {
                "login.chinacloudapi.cn",
                "login.partner.microsoftonline.cn",
                "login-us.microsoftonline.com",
                "login.microsoftonline.de",
                "login.microsoftonline.us",
                "login.usgovcloudapi.net",
                "login.sovcloud-identity.fr",
                "login.sovcloud-identity.de",
                "login.sovcloud-identity.sg"
        };
        for (String host : sovereignHosts) {
            assertTrue(MtlsEndpointHelper.isMtlsPoPUnsupportedCloud(host),
                    host + " must be treated as an unsupported (sovereign) cloud");
        }
        // Regionalized sovereign hosts must also fail fast.
        assertTrue(MtlsEndpointHelper.isMtlsPoPUnsupportedCloud("westus.login.microsoftonline.us"));
    }

    @Test
    void deriveMtlsTokenEndpoint_germanCloud_failsFast() {
        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                MtlsEndpointHelper.deriveMtlsTokenEndpoint(
                        url("https://login.microsoftonline.de/contoso/oauth2/v2.0/token")));

        assertEquals(AuthenticationErrorCode.MTLS_POP_ERROR, ex.errorCode());
        assertTrue(ex.getMessage().toLowerCase().contains("cloud"));
    }

    @Test
    void deriveMtlsTokenEndpoint_legacyUsCloud_failsFast() {
        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                MtlsEndpointHelper.deriveMtlsTokenEndpoint(
                        url("https://login-us.microsoftonline.com/contoso/oauth2/v2.0/token")));

        assertEquals(AuthenticationErrorCode.MTLS_POP_ERROR, ex.errorCode());
    }

    @Test
    void deriveMtlsTokenEndpoint_sovereignFranceCloud_failsFast() {
        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                MtlsEndpointHelper.deriveMtlsTokenEndpoint(
                        url("https://login.sovcloud-identity.fr/contoso/oauth2/v2.0/token")));

        assertEquals(AuthenticationErrorCode.MTLS_POP_ERROR, ex.errorCode());
    }

    // A non-"login.*" host must be rejected rather than rewritten to (and its client cert presented at)
    // the public mtlsauth endpoint.
    @Test
    void deriveMtlsTokenEndpoint_nonLoginHost_isRejected() {
        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                MtlsEndpointHelper.deriveMtlsTokenEndpoint(
                        url("https://contoso.example.com/contoso/oauth2/v2.0/token")));

        assertEquals(AuthenticationErrorCode.MTLS_POP_ERROR, ex.errorCode());
    }

    // An unrecognized but "login.*"-shaped host keeps its own domain (login -> mtlsauth) rather than
    // collapsing to the public host, so the request stays within its cloud boundary.
    @Test
    void deriveMtlsHost_unknownLoginHost_preservesDomain() {
        assertEquals("mtlsauth.example.com", MtlsEndpointHelper.deriveMtlsHost("login.example.com"));
    }

    @Test
    void deriveMtlsHost_nonLoginHost_returnsNull() {
        assertNull(MtlsEndpointHelper.deriveMtlsHost("contoso.example.com"));
    }
}
