// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
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

        requestAuthority = getAuthorityWithPrefNetworkHost(requestAuthority.authority());

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

    private void processPasswordGrant(OAuthAuthorizationGrant authGrant) throws Exception {

        //Additional processing is only needed if it's a password grant with an AAD authority
        if (!(authGrant.getParamValue(GrantConstants.GRANT_TYPE_PARAMETER).equals(GrantConstants.PASSWORD))
                || msalRequest.application().authenticationAuthority.authorityType != AuthorityType.AAD) {
            return;
        }

        UserDiscoveryResponse userDiscoveryResponse = UserDiscoveryRequest.execute(
                this.clientApplication.authenticationAuthority.getUserRealmEndpoint(authGrant.getParamValue(GrantConstants.USERNAME_PARAMETER)),
                msalRequest.headers().getReadonlyHeaderMap(),
                msalRequest.requestContext(),
                this.clientApplication.serviceBundle());

        if (userDiscoveryResponse.isAccountFederated()) {
            WSTrustResponse response = WSTrustRequest.execute(
                    userDiscoveryResponse.federationMetadataUrl(),
                    authGrant.getParamValue(GrantConstants.USERNAME_PARAMETER),
                    authGrant.getParamValue(GrantConstants.PASSWORD_PARAMETER),
                    userDiscoveryResponse.cloudAudienceUrn(),
                    msalRequest.requestContext(),
                    this.clientApplication.serviceBundle(),
                    this.clientApplication.logPii());

            authGrant.addAndReplaceParams(getSAMLAuthGrantParameters(response));
        }
    }

    private Map<String, String> getSAMLAuthGrantParameters(WSTrustResponse response) {
        Map<String, String> params = new LinkedHashMap<>();

        if (response.isTokenSaml2()) {
            params.put(GrantConstants.GRANT_TYPE_PARAMETER, GrantConstants.SAML_2_BEARER);
        } else {
            params.put(GrantConstants.GRANT_TYPE_PARAMETER, GrantConstants.SAML_1_1_BEARER);
        }

        params.put(GrantConstants.ASSERTION_PARAMETER, Base64.getUrlEncoder().encodeToString(response.getToken().getBytes(StandardCharsets.UTF_8)));

        return params;
    }

    private Map<String, String> getAuthorizationGrantIntegrated(String userName) throws Exception {
        Map<String, String> params;

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
            String message = "Password is required for managed user";
            clientApplication.log.error(
                    LogHelper.createMessage(message, msalRequest.requestContext().correlationId()));
            throw new MsalClientException(
                    message,
                    AuthenticationErrorCode.PASSWORD_REQUIRED_FOR_MANAGED_USER,
                    msalRequest.requestContext().correlationId());
        } else {
            String message = "User Realm request failed";
            clientApplication.log.error(
                    LogHelper.createMessage(message, msalRequest.requestContext().correlationId()));
            throw new MsalClientException(
                    message,
                    AuthenticationErrorCode.USER_REALM_DISCOVERY_FAILED,
                    msalRequest.requestContext().correlationId());
        }

        return params;
    }
}
