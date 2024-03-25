// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant;
import com.nimbusds.oauth2.sdk.SAML2BearerGrant;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

class AcquireTokenByAuthorizationGrantSupplier extends AuthenticationResultSupplier {

    private Authority requestAuthority;
    private MsalRequest msalRequest;

    AcquireTokenByAuthorizationGrantSupplier(AbstractApplicationBase clientApplication,
                                             MsalRequest msalRequest,
                                             Authority authority) {
        super(clientApplication, msalRequest);
        this.msalRequest = msalRequest;
        this.requestAuthority = authority;
    }

    AuthenticationResult execute() throws Exception {
        AbstractMsalAuthorizationGrant authGrant = msalRequest.msalAuthorizationGrant();

        if (IsUiRequiredCacheSupported()) {
            MsalInteractionRequiredException cachedEx =
                    InteractionRequiredCache.getCachedInteractionRequiredException(
                            ((RefreshTokenRequest) msalRequest).getFullThumbprint());
            if (cachedEx != null) {
                throw cachedEx;
            }
        }

        if (authGrant instanceof OAuthAuthorizationGrant) {
            msalRequest.msalAuthorizationGrant =
                    processPasswordGrant((OAuthAuthorizationGrant) authGrant);
        }

        if (authGrant instanceof IntegratedWindowsAuthorizationGrant) {
            IntegratedWindowsAuthorizationGrant integratedAuthGrant =
                    (IntegratedWindowsAuthorizationGrant) authGrant;
            msalRequest.msalAuthorizationGrant =
                    new OAuthAuthorizationGrant(getAuthorizationGrantIntegrated(
                            integratedAuthGrant.getUserName()), integratedAuthGrant.getScopes(), integratedAuthGrant.getClaims());
        }

        if (requestAuthority == null) {
            requestAuthority = clientApplication.authenticationAuthority;
        }

        if (requestAuthority.authorityType == AuthorityType.AAD) {
            requestAuthority = getAuthorityWithPrefNetworkHost(requestAuthority.authority());
        }

        try {
            return clientApplication.acquireTokenCommon(msalRequest, requestAuthority);
        } catch (MsalInteractionRequiredException ex) {
            if (IsUiRequiredCacheSupported()) {
                InteractionRequiredCache.set(((RefreshTokenRequest) msalRequest).getFullThumbprint(), ex);
            }
            throw ex;
        }
    }

    private boolean IsUiRequiredCacheSupported() {
        return msalRequest instanceof RefreshTokenRequest &&
                clientApplication instanceof PublicClientApplication;
    }

    private OAuthAuthorizationGrant processPasswordGrant(
            OAuthAuthorizationGrant authGrant) throws Exception {

        if (!(authGrant.getAuthorizationGrant() instanceof ResourceOwnerPasswordCredentialsGrant)) {
            return authGrant;
        }

        if (msalRequest.application().authenticationAuthority.authorityType != AuthorityType.AAD) {
            return authGrant;
        }

        ResourceOwnerPasswordCredentialsGrant grant =
                (ResourceOwnerPasswordCredentialsGrant) authGrant.getAuthorizationGrant();

        UserDiscoveryResponse userDiscoveryResponse = UserDiscoveryRequest.execute(
                this.clientApplication.authenticationAuthority.getUserRealmEndpoint(grant.getUsername()),
                msalRequest.headers().getReadonlyHeaderMap(),
                msalRequest.requestContext(),
                this.clientApplication.serviceBundle());

        if (userDiscoveryResponse.isAccountFederated()) {
            WSTrustResponse response = WSTrustRequest.execute(
                    userDiscoveryResponse.federationMetadataUrl(),
                    grant.getUsername(),
                    grant.getPassword().getValue(),
                    userDiscoveryResponse.cloudAudienceUrn(),
                    msalRequest.requestContext(),
                    this.clientApplication.serviceBundle(),
                    this.clientApplication.logPii());

            AuthorizationGrant updatedGrant = getSAMLAuthorizationGrant(response);

            authGrant = new OAuthAuthorizationGrant(updatedGrant, authGrant.getParameters());
        }
        return authGrant;
    }

    private AuthorizationGrant getSAMLAuthorizationGrant(WSTrustResponse response) throws UnsupportedEncodingException {
        AuthorizationGrant updatedGrant;
        if (response.isTokenSaml2()) {
            updatedGrant = new SAML2BearerGrant(new Base64URL(
                    Base64.getEncoder().encodeToString(response.getToken().getBytes(StandardCharsets.UTF_8))));
        } else {
            updatedGrant = new SAML11BearerGrant(new Base64URL(
                    Base64.getEncoder().encodeToString(response.getToken()
                            .getBytes(StandardCharsets.UTF_8))));
        }
        return updatedGrant;
    }

    private AuthorizationGrant getAuthorizationGrantIntegrated(String userName) throws Exception {
        AuthorizationGrant updatedGrant;

        String userRealmEndpoint = this.clientApplication.authenticationAuthority.
                getUserRealmEndpoint(URLEncoder.encode(userName, StandardCharsets.UTF_8.name()));

        // Get the realm information
        UserDiscoveryResponse userRealmResponse = UserDiscoveryRequest.execute(
                userRealmEndpoint,
                msalRequest.headers().getReadonlyHeaderMap(),
                msalRequest.requestContext(),
                this.clientApplication.serviceBundle());

        if (userRealmResponse.isAccountFederated() &&
                "WSTrust".equalsIgnoreCase(userRealmResponse.federationProtocol())) {

            String mexURL = userRealmResponse.federationMetadataUrl();
            String cloudAudienceUrn = userRealmResponse.cloudAudienceUrn();

            // Discover the policy for authentication using the Metadata Exchange Url.
            // Get the WSTrust Token (Web Service Trust Token)
            WSTrustResponse wsTrustResponse = WSTrustRequest.execute(
                    mexURL,
                    cloudAudienceUrn,
                    msalRequest.requestContext(),
                    this.clientApplication.serviceBundle(),
                    this.clientApplication.logPii());

            updatedGrant = getSAMLAuthorizationGrant(wsTrustResponse);
        } else if (userRealmResponse.isAccountManaged()) {
            throw new MsalClientException(
                    "Password is required for managed user",
                    AuthenticationErrorCode.PASSWORD_REQUIRED_FOR_MANAGED_USER);
        } else {
            throw new MsalClientException(
                    "User Realm request failed",
                    AuthenticationErrorCode.USER_REALM_DISCOVERY_FAILED);
        }

        return updatedGrant;
    }
}
