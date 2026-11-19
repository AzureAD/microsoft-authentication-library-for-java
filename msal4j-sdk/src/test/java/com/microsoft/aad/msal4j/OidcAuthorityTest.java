package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OidcAuthorityTest {

    private static final String TENANT = "11111111-2222-3333-4444-555555555555";
    private OidcDiscoveryResponse mockDiscoveryResponse;

    @BeforeEach
    void setup() {
        mockDiscoveryResponse = Mockito.mock(OidcDiscoveryResponse.class);
        when(mockDiscoveryResponse.authorizationEndpoint()).thenReturn("https://login.example.com/authorize");
        when(mockDiscoveryResponse.tokenEndpoint()).thenReturn("https://login.example.com/token");
        when(mockDiscoveryResponse.deviceCodeEndpoint()).thenReturn("https://login.example.com/devicecode");
    }

    @Test
    void testSetAuthorityProperties_IssuerMatchesAuthority() throws MalformedURLException {
        // Arrange
        URL authorityUrl = new URL("https://login.example.com/tenant1/");
        OidcAuthority authority = new OidcAuthority(authorityUrl);

        // Match the issuer to the authority URL (without the well-known segment)
        when(mockDiscoveryResponse.issuer()).thenReturn("https://login.example.com/tenant1/");

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> authority.setAuthorityProperties(mockDiscoveryResponse));

        // Verify properties were set
        assertEquals("https://login.example.com/authorize", authority.authorizationEndpoint());
        assertEquals("https://login.example.com/token", authority.tokenEndpoint());
        assertEquals("https://login.example.com/devicecode", authority.deviceCodeEndpoint());
        assertEquals("https://login.example.com/token", authority.selfSignedJwtAudience);
    }

    @Test
    void testSetAuthorityProperties_IssuerFollowsCiamPattern() throws MalformedURLException {
        // Arrange
        String tenant = "contoso";
        URL authorityUrl = new URL("https://" + tenant + ".ciamlogin.com/" + tenant + "/");
        OidcAuthority authority = new OidcAuthority(authorityUrl);

        // Set an issuer that follows CIAM pattern but doesn't exactly match the authority
        String ciamIssuer = "https://" + tenant + ".ciamlogin.com/" + tenant + "/v2.0";
        when(mockDiscoveryResponse.issuer()).thenReturn(ciamIssuer);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> authority.setAuthorityProperties(mockDiscoveryResponse));
    }

    @Test
    void testSetAuthorityProperties_IssuerInvalid() throws MalformedURLException {
        // Arrange
        URL authorityUrl = new URL("https://login.example.com/tenant1/");
        OidcAuthority authority = new OidcAuthority(authorityUrl);

        // Set an issuer that doesn't match the authority and doesn't follow CIAM pattern
        when(mockDiscoveryResponse.issuer()).thenReturn("https://login.different.com/tenant1/");

        // Act & Assert - Should throw MsalClientException
        MsalClientException exception = assertThrows(MsalClientException.class,
                () -> authority.setAuthorityProperties(mockDiscoveryResponse));

        // Verify exception details
        assertEquals("issuer_validation", exception.errorCode());
        assertTrue(exception.getMessage().contains("Invalid issuer from OIDC discovery"));
    }

    @Test
    void testSetAuthorityProperties_IssuerIsNull() throws MalformedURLException {
        // Arrange
        URL authorityUrl = new URL("https://login.example.com/tenant1/");
        OidcAuthority authority = new OidcAuthority(authorityUrl);

        // Set null issuer
        when(mockDiscoveryResponse.issuer()).thenReturn(null);

        // Act & Assert - Should throw MsalClientException
        MsalClientException exception = assertThrows(MsalClientException.class,
                () -> authority.setAuthorityProperties(mockDiscoveryResponse));

        // Verify exception details
        assertEquals("issuer_validation", exception.errorCode());
        assertTrue(exception.getMessage().contains("Invalid issuer from OIDC discovery"));
    }

    @Test
    void testSetAuthorityProperties_TrailingSlashNormalization() throws MalformedURLException {
        // Arrange
        URL authorityUrl = new URL("https://login.example.com/tenant1/");
        OidcAuthority authority = new OidcAuthority(authorityUrl);

        // Match the issuer to the authority but without trailing slash
        when(mockDiscoveryResponse.issuer()).thenReturn("https://login.example.com/tenant1");

        // Act & Assert - Should not throw exception because normalization happens
        assertDoesNotThrow(() -> authority.setAuthorityProperties(mockDiscoveryResponse));
    }

    static Stream<Arguments> issuerPolicyCases() {
        String custom = "https://custom.example.com/" + TENANT;
        String ciam = "https://contoso.ciamlogin.com/" + TENANT;
        String known = "https://login.microsoftonline.com/" + TENANT;
        return Stream.of(
                Arguments.of("S01", custom + "/v2.0", known + "/v2.0", true),
                Arguments.of("S02", ciam + "/v2.0", known + "/v2.0", true),
                Arguments.of("S03", ciam + "/v2.0", ciam + "/v2.0", true),
                Arguments.of("S04", custom, "https://eastus.login.microsoftonline.com/" + TENANT, false),
                Arguments.of("S05", custom, "https://evil.login.microsoftonline.com/" + TENANT, false),
                Arguments.of("S06", known, "https://evil.login.microsoftonline.com/" + TENANT, true),
                Arguments.of("S07", "https://login.microsoftonline.com/attacker.onmicrosoft.com",
                        "https://attacker.onmicrosoft.com.ciamlogin.com/attacker.onmicrosoft.com/v2.0", false),
                Arguments.of("S08", custom, "https://login.microsoftonline.evil.com/" + TENANT, false));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("issuerPolicyCases")
    void testIssuerPolicy(String name, String configuredAuthority, String issuer, boolean accepted)
            throws MalformedURLException {
        OidcAuthority authority = new OidcAuthority(new URL(configuredAuthority + "/"));
        when(mockDiscoveryResponse.issuer()).thenReturn(issuer);

        if (accepted) {
            assertDoesNotThrow(() -> authority.setAuthorityProperties(mockDiscoveryResponse));
            assertEquals("https://login.example.com/token", authority.tokenEndpoint());
        } else {
            assertRejectedWithoutEndpoints(authority);
        }
    }

    static Stream<Arguments> knownHostCases() {
        return AadInstanceDiscoveryProvider.TRUSTED_HOSTS_SET.stream().flatMap(host -> Stream.of(
                Arguments.of("https://custom.example.com/tenant/", "https://" + host + "/tenant"),
                Arguments.of("https://contoso.ciamlogin.com/tenant/", "https://" + host + "/tenant"),
                Arguments.of("https://login.microsoftonline.com/tenant/", "https://" + host + "/tenant"),
                Arguments.of("https://" + host + "/tenant/", "https://arbitrary-label." + host + "/tenant"),
                Arguments.of("https://LOGIN.MICROSOFTONLINE.COM/tenant/",
                        "HTTPS://REGION." + host.toUpperCase(Locale.ROOT) + "/tenant"),
                Arguments.of("https://custom.example.com/tenant/",
                        "HTTPS://" + host.toUpperCase(Locale.ROOT) + "/tenant")));
    }

    @ParameterizedTest
    @MethodSource("knownHostCases")
    void testKnownMicrosoftHostsAndRegionalAliases(String configuredAuthority, String issuer)
            throws MalformedURLException {
        OidcAuthority authority = new OidcAuthority(new URL(configuredAuthority));
        when(mockDiscoveryResponse.issuer()).thenReturn(issuer);
        assertDoesNotThrow(() -> authority.setAuthorityProperties(mockDiscoveryResponse));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "not a URI", "/relative/path", "//login.microsoftonline.com/tenant",
            "http://login.microsoftonline.com/tenant", "ftp://login.microsoftonline.com/tenant",
            "https:login.microsoftonline.com/tenant", "https:///login.microsoftonline.com/tenant",
            "https://login.microsoftonline.com/tenant with space",
            "https://login.microsoftonline.com/%ZZ",
            "https://login.microsoftonline.com\\@evil.example/tenant",
            "https://login.microsoftonline.com@evil.example/tenant",
            "https://evil.example@login.microsoftonline.com/tenant",
            "https://login.microsoftonline.com/tenant?query=value",
            "https://login.microsoftonline.com/tenant#fragment",
            "https://login.microsoftonline.com:99999/tenant",
            "https://one.two.login.microsoftonline.com/tenant",
            "https://.login.microsoftonline.com/tenant",
            "https://-region.login.microsoftonline.com/tenant",
            "https://region-.login.microsoftonline.com/tenant",
            "https://region_name.login.microsoftonline.com/tenant",
            "https://login.microsoftonline.com.evil.example/tenant",
            "https://region.login.microsoftonline.com.evil.example/tenant",
            "https://evillogin.microsoftonline.com/tenant",
            "https://contoso.ciamlogin.com/contoso/v2.0",
            "https://contoso.ciamlogin.com/contoso",
            "https://contoso.ciamlogin.com/contoso-unexpected"
    })
    void testKnownAuthorityRejectsInvalidOrUntrustedIssuer(String issuer) throws MalformedURLException {
        OidcAuthority authority = new OidcAuthority(new URL("https://login.microsoftonline.com/contoso/"));
        when(mockDiscoveryResponse.issuer()).thenReturn(issuer);
        assertRejectedWithoutEndpoints(authority);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://custom.example.com/contoso/",
            "https://contoso.ciamlogin.com/contoso/"
    })
    void testPreservesNonMicrosoftCiamMapping(String configuredAuthority) throws MalformedURLException {
        OidcAuthority authority = new OidcAuthority(new URL(configuredAuthority));
        when(mockDiscoveryResponse.issuer()).thenReturn("https://contoso.ciamlogin.com/contoso/v2.0");
        assertDoesNotThrow(() -> authority.setAuthorityProperties(mockDiscoveryResponse));
    }

    static Stream<Arguments> nativeIssuerCases() {
        return Stream.of(
                Arguments.of("https://custom.example.com:8443/Contoso/",
                        "HTTPS://CUSTOM.EXAMPLE.COM:8443/contoso", true),
                Arguments.of("https://custom.example.com:443/contoso/",
                        "https://custom.example.com/contoso", false),
                Arguments.of("https://custom.example.com/contoso/",
                        "https://custom.example.com/other", false),
                Arguments.of("https://custom.example.com/contoso/",
                        "https://contoso.ciamlogin.com/contoso/v2.0?query=value", true),
                Arguments.of("https://custom.example.com/contoso/",
                        "https://contoso.ciamlogin.com/contoso/v2.0#fragment", true),
                Arguments.of("https://custom.example.com/contoso/",
                        "https://contoso.ciamlogin.com/contoso/%ZZ", true),
                Arguments.of("https://custom.example.com/contoso/",
                        "https://contoso.ciamlogin.com/contoso/with space", true),
                Arguments.of("https://custom.example.com/contoso/",
                        "HTTPS://CONTOSO.CIAMLOGIN.COM/contoso/v2.0", false),
                Arguments.of("https://custom.example.com/contoso/",
                        "http://contoso.ciamlogin.com/contoso/v2.0", false),
                Arguments.of("https://custom.example.com/contoso/",
                        "https://other.ciamlogin.com/contoso/v2.0", false));
    }

    @ParameterizedTest
    @MethodSource("nativeIssuerCases")
    void testPreservesNativeEqualityAndCiamFallback(String configuredAuthority, String issuer, boolean accepted)
            throws MalformedURLException {
        OidcAuthority authority = new OidcAuthority(new URL(configuredAuthority));
        when(mockDiscoveryResponse.issuer()).thenReturn(issuer);
        if (accepted) {
            assertDoesNotThrow(() -> authority.setAuthorityProperties(mockDiscoveryResponse));
        } else {
            assertRejectedWithoutEndpoints(authority);
        }
    }

    static Stream<Arguments> knownAuthorityCiamCases() {
        return AadInstanceDiscoveryProvider.TRUSTED_HOSTS_SET.stream().flatMap(host -> Stream.of(
                Arguments.of("https://" + host.toUpperCase(Locale.ROOT) + "/attacker.onmicrosoft.com/",
                        "https://attacker.onmicrosoft.com.ciamlogin.com/attacker.onmicrosoft.com/v2.0"),
                Arguments.of("https://" + host + "/contoso/", "https://contoso.ciamlogin.com/contoso/v2.0")));
    }

    @ParameterizedTest
    @MethodSource("knownAuthorityCiamCases")
    void testKnownAuthorityAliasesCannotUseCiamFallback(String configuredAuthority, String issuer)
            throws MalformedURLException {
        OidcAuthority authority = new OidcAuthority(new URL(configuredAuthority));
        when(mockDiscoveryResponse.issuer()).thenReturn(issuer);
        assertRejectedWithoutEndpoints(authority);
    }

    static Stream<Arguments> rootDotAuthorityCases() {
        return AadInstanceDiscoveryProvider.TRUSTED_HOSTS_SET.stream().flatMap(host -> Stream.of(
                Arguments.of("https://" + host.toUpperCase(Locale.ROOT) + "./contoso/",
                        "https://arbitrary-label." + host + "/contoso", true),
                Arguments.of("https://" + host + "./contoso/",
                        "https://contoso.ciamlogin.com/contoso/v2.0", false),
                Arguments.of("https://" + host + "../contoso/",
                        "https://arbitrary-label." + host + "/contoso", false),
                Arguments.of("https://" + host + "./contoso/",
                        "https://" + host + "./contoso", true)));
    }

    @ParameterizedTest
    @MethodSource("rootDotAuthorityCases")
    void testRootDotOnlyAffectsKnownAuthorityEligibility(String configuredAuthority, String issuer, boolean accepted)
            throws MalformedURLException {
        URL configuredUrl = new URL(configuredAuthority);
        assertTrue(configuredUrl.getHost().endsWith("."));
        OidcAuthority authority = new OidcAuthority(configuredUrl);
        when(mockDiscoveryResponse.issuer()).thenReturn(issuer);
        if (accepted) {
            assertDoesNotThrow(() -> authority.setAuthorityProperties(mockDiscoveryResponse));
        } else {
            assertRejectedWithoutEndpoints(authority);
        }
        assertEquals(configuredAuthority + ".well-known/openid-configuration",
                authority.canonicalAuthorityUrl().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://login.microsoftonline.com./contoso",
            "https://eastus.login.microsoftonline.com./contoso",
            "https://login.microsoftonline.com../contoso"
    })
    void testNewMicrosoftIssuerAcceptanceDoesNotNormalizeIssuerRootDot(String issuer) throws MalformedURLException {
        OidcAuthority authority = new OidcAuthority(new URL("https://login.microsoftonline.com/contoso/"));
        when(mockDiscoveryResponse.issuer()).thenReturn(issuer);
        assertRejectedWithoutEndpoints(authority);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://custom.example.com./contoso/",
            "https://eastus.login.microsoftonline.com./contoso/",
            "https://login.microsoftonline.com.evil.example./contoso/"
    })
    void testRootDotDoesNotPromoteUnknownAuthority(String configuredAuthority) throws MalformedURLException {
        OidcAuthority authority = new OidcAuthority(new URL(configuredAuthority));
        when(mockDiscoveryResponse.issuer()).thenReturn("https://westus.login.microsoftonline.com/contoso");
        assertRejectedWithoutEndpoints(authority);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://eastus.login.microsoftonline.com/contoso",
            "https://contoso.ciamlogin.com/contoso/v2.0"
    })
    void testPublicRootDotAuthorityPreservesDiscoveryOrigin(String issuer) throws Exception {
        IHttpClient httpClient = mock(IHttpClient.class);
        when(httpClient.send(any(HttpRequest.class))).thenReturn(discoveryResponse(issuer));
        PublicClientApplication.Builder builder = PublicClientApplication.builder("client-id")
                .httpClient(httpClient).oidcAuthority("https://LOGIN.MICROSOFTONLINE.COM./contoso");
        if (issuer.contains("ciamlogin.com")) {
            assertEquals("issuer_validation", assertThrows(MsalClientException.class, builder::build).errorCode());
        } else {
            assertDoesNotThrow(builder::build);
        }
        verify(httpClient).send(argThat(request -> request.httpMethod() == HttpMethod.GET
                && request.url().toString().equals(
                        "https://login.microsoftonline.com./contoso/.well-known/openid-configuration")));
        verifyNoMoreInteractions(httpClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://login.microsoftonline.com/tenant",
            "https://login.microsoftonline.com/%ZZ",
            "https://login.microsoftonline.com/with space",
            "https://user@login.microsoftonline.com/tenant",
            "https://login.microsoftonline.com/tenant?query=value",
            "https://login.microsoftonline.com/tenant#fragment",
            "https://login.microsoftonline.com:99999/tenant",
            "https:///login.microsoftonline.com/tenant"
    })
    void testNewCustomAuthorityAcceptanceRequiresValidHttpsIssuer(String issuer) throws MalformedURLException {
        OidcAuthority authority = new OidcAuthority(new URL("https://custom.example.com/tenant/"));
        when(mockDiscoveryResponse.issuer()).thenReturn(issuer);
        assertRejectedWithoutEndpoints(authority);
    }

    @Test
    void testRuntimeMetadataDoesNotPromoteCustomAuthorityToKnownHost() throws MalformedURLException {
        String customHost = "oidc-policy-custom.example.com";
        InstanceDiscoveryMetadataEntry previous = AadInstanceDiscoveryProvider.cache.put(customHost,
                new InstanceDiscoveryMetadataEntry(customHost, "login.microsoftonline.com",
                        java.util.Collections.singleton("login.microsoftonline.com")));
        try {
            OidcAuthority authority = new OidcAuthority(new URL("https://" + customHost + "/tenant/"));
            when(mockDiscoveryResponse.issuer()).thenReturn("https://eastus.login.microsoftonline.com/tenant");
            assertRejectedWithoutEndpoints(authority);
        } finally {
            if (previous == null) {
                AadInstanceDiscoveryProvider.cache.remove(customHost);
            } else {
                AadInstanceDiscoveryProvider.cache.put(customHost, previous);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://custom.example.com/tenant/",
            "https://contoso.ciamlogin.com/tenant/",
            "https://eastus.login.microsoftonline.com/tenant/",
            "https://login.microsoftonline.com.evil.example/tenant/"
    })
    void testRegionalIssuerRequiresExactKnownAuthorityHost(String configuredAuthority) throws MalformedURLException {
        OidcAuthority authority = new OidcAuthority(new URL(configuredAuthority));
        when(mockDiscoveryResponse.issuer()).thenReturn("https://westus.login.microsoftonline.com/tenant");
        assertRejectedWithoutEndpoints(authority);
    }

    @Test
    void testPreservesCaseAndTrailingSlashNormalization() throws MalformedURLException {
        OidcAuthority authority = new OidcAuthority(new URL("https://custom.example.com/Tenant/V2.0/"));
        when(mockDiscoveryResponse.issuer()).thenReturn("HTTPS://CUSTOM.EXAMPLE.COM/tenant/v2.0");
        assertDoesNotThrow(() -> authority.setAuthorityProperties(mockDiscoveryResponse));
    }

    @ParameterizedTest(name = "public construction {0}")
    @MethodSource("issuerPolicyCases")
    void testPublicOidcAuthorityConstruction(String name, String configuredAuthority, String issuer,
                                             boolean accepted) throws Exception {
        IHttpClient httpClient = mock(IHttpClient.class);
        when(httpClient.send(any(HttpRequest.class))).thenReturn(discoveryResponse(issuer));
        PublicClientApplication.Builder builder = PublicClientApplication.builder("client-id")
                .httpClient(httpClient).oidcAuthority(configuredAuthority);

        if (accepted) {
            PublicClientApplication application = assertDoesNotThrow(builder::build);
            assertEquals("https://advertised.example/token", application.authenticationAuthority.tokenEndpoint());
        } else {
            MsalClientException exception = assertThrows(MsalClientException.class, builder::build);
            assertEquals("issuer_validation", exception.errorCode());
        }

        verify(httpClient).send(argThat(request -> request.httpMethod() == HttpMethod.GET
                && request.url().toString().equals(configuredAuthority + "/.well-known/openid-configuration")));
        verifyNoMoreInteractions(httpClient);
    }

    @Test
    void testInvalidConstructionCanRecoverWithValidDiscovery() throws Exception {
        IHttpClient httpClient = mock(IHttpClient.class);
        when(httpClient.send(any(HttpRequest.class))).thenReturn(
                discoveryResponse("https://contoso.ciamlogin.com/contoso/v2.0"),
                discoveryResponse("https://contoso.ciamlogin.com/contoso/v2.0"),
                discoveryResponse("https://login.microsoftonline.com/contoso"));
        PublicClientApplication.Builder builder = PublicClientApplication.builder("client-id")
                .httpClient(httpClient).oidcAuthority("https://login.microsoftonline.com/contoso");

        MsalClientException exception = assertThrows(MsalClientException.class, builder::build);
        assertEquals("issuer_validation", exception.errorCode());
        assertEquals("issuer_validation", assertThrows(MsalClientException.class, builder::build).errorCode());
        PublicClientApplication application = assertDoesNotThrow(builder::build);
        assertEquals("https://advertised.example/token", application.authenticationAuthority.tokenEndpoint());
        verify(httpClient, times(3)).send(argThat(request -> request.httpMethod() == HttpMethod.GET
                && request.url().toString().equals(
                        "https://login.microsoftonline.com/contoso/.well-known/openid-configuration")));
        verifyNoMoreInteractions(httpClient);
    }

    private void assertRejectedWithoutEndpoints(OidcAuthority authority) {
        MsalClientException exception = assertThrows(MsalClientException.class,
                () -> authority.setAuthorityProperties(mockDiscoveryResponse));
        assertEquals("issuer_validation", exception.errorCode());
        assertNull(authority.authorizationEndpoint());
        assertNull(authority.tokenEndpoint());
        assertNull(authority.deviceCodeEndpoint());
        assertNull(authority.selfSignedJwtAudience());
        verify(mockDiscoveryResponse, never()).authorizationEndpoint();
        verify(mockDiscoveryResponse, never()).tokenEndpoint();
        verify(mockDiscoveryResponse, never()).deviceCodeEndpoint();
    }

    private static HttpResponse discoveryResponse(String issuer) {
        return new HttpResponse().statusCode(HttpStatus.HTTP_OK).body("{\"issuer\":\"" + issuer + "\","
                + "\"authorization_endpoint\":\"https://advertised.example/authorize\","
                + "\"token_endpoint\":\"https://advertised.example/token\","
                + "\"device_authorization_endpoint\":\"https://advertised.example/devicecode\"}");
    }
}