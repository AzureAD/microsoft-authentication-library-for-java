package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OidcAuthorityTest {

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
}