// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.jose.util.Base64URL;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        if (!(authGrant.toParameters().get(GrantConstants.GRANT_TYPE_PARAMETER).get(0).equals(GrantConstants.PASSWORD))) {
            return authGrant;
        }

        if (msalRequest.application().authenticationAuthority.authorityType != AuthorityType.AAD) {
            return authGrant;
        }

        UserDiscoveryResponse userDiscoveryResponse = UserDiscoveryRequest.execute(
                this.clientApplication.authenticationAuthority.getUserRealmEndpoint(authGrant.toParameters().get("username").get(0)),
                msalRequest.headers().getReadonlyHeaderMap(),
                msalRequest.requestContext(),
                this.clientApplication.serviceBundle());

        if (userDiscoveryResponse.isAccountFederated()) {
            WSTrustResponse response = WSTrustRequest.execute(
                    userDiscoveryResponse.federationMetadataUrl(),
                    authGrant.toParameters().get(GrantConstants.USERNAME_PARAMETER).get(0),
                    authGrant.toParameters().get(GrantConstants.PASSWORD_PARAMETER).get(0),
                    userDiscoveryResponse.cloudAudienceUrn(),
                    msalRequest.requestContext(),
                    this.clientApplication.serviceBundle(),
                    this.clientApplication.logPii());

            Map<String, List<String>> params = getSAMLAuthGrantParameters(response);
            params.putAll(authGrant.toParameters());

            authGrant = new OAuthAuthorizationGrant(params, null);
        }
        return authGrant;
    }

    private Map<String, List<String>> getSAMLAuthGrantParameters(WSTrustResponse response) {
        Map<String, List<String>> params = new LinkedHashMap<>();

        if (response.isTokenSaml2()) {
            params.put(GrantConstants.GRANT_TYPE_PARAMETER, Collections.singletonList(GrantConstants.SAML_2_BEARER));
        } else {
            params.put(GrantConstants.GRANT_TYPE_PARAMETER, Collections.singletonList(GrantConstants.SAML_1_1_BEARER));
        }

        params.put(GrantConstants.ASSERTION_PARAMETER, Collections.singletonList(new Base64URL(
                Base64.getEncoder().encodeToString(response.getToken()
                        .getBytes(StandardCharsets.UTF_8))).toString()));

        return params;
    }

    private Map<String, List<String>> getAuthorizationGrantIntegrated(String userName) throws Exception {
        Map<String, List<String>> params;

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

            params = getSAMLAuthGrantParameters(wsTrustResponse);
        } else if (userRealmResponse.isAccountManaged()) {
            throw new MsalClientException(
                    "Password is required for managed user",
                    AuthenticationErrorCode.PASSWORD_REQUIRED_FOR_MANAGED_USER);
        } else {
            throw new MsalClientException(
                    "User Realm request failed",
                    AuthenticationErrorCode.USER_REALM_DISCOVERY_FAILED);
        }

        return params;
    }
}
