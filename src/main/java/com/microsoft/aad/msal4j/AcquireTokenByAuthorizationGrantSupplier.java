// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.msal4j;

import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant;
import com.nimbusds.oauth2.sdk.SAML2BearerGrant;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

class AcquireTokenByAuthorizationGrantSupplier extends AuthenticationResultSupplier {

    private Authority requestAuthority;
    private MsalRequest msalRequest;

    AcquireTokenByAuthorizationGrantSupplier(ClientApplicationBase clientApplication,
                                             MsalRequest msalRequest,
                                             Authority authority) {
        super(clientApplication, msalRequest);
        this.msalRequest = msalRequest;
        this.requestAuthority = authority;
    }

    AuthenticationResult execute() throws Exception {
        AbstractMsalAuthorizationGrant authGrant = msalRequest.msalAuthorizationGrant();
        if (authGrant instanceof OAuthAuthorizationGrant) {
            msalRequest.msalAuthorizationGrant =
                    processPasswordGrant((OAuthAuthorizationGrant) authGrant);
        }

        if (authGrant instanceof IntegratedWindowsAuthorizationGrant) {
            IntegratedWindowsAuthorizationGrant integratedAuthGrant =
                    (IntegratedWindowsAuthorizationGrant) authGrant;
            msalRequest.msalAuthorizationGrant =
                    new OAuthAuthorizationGrant(getAuthorizationGrantIntegrated(
                            integratedAuthGrant.getUserName()), integratedAuthGrant.getScopes());
        }

        if(requestAuthority == null){
            requestAuthority = clientApplication.authenticationAuthority;
        }

        if(requestAuthority.authorityType != AuthorityType.B2C){
            requestAuthority = getAuthorityWithPrefNetworkHost(requestAuthority.authority());
        }

        return clientApplication.acquireTokenCommon(msalRequest, requestAuthority);
    }

    private OAuthAuthorizationGrant processPasswordGrant(
            OAuthAuthorizationGrant authGrant) throws Exception {

        if (!(authGrant.getAuthorizationGrant() instanceof ResourceOwnerPasswordCredentialsGrant)) {
            return authGrant;
        }

        if(msalRequest.application().authenticationAuthority.authorityType == AuthorityType.B2C){
            return authGrant;
        }

        ResourceOwnerPasswordCredentialsGrant grant =
                (ResourceOwnerPasswordCredentialsGrant) authGrant.getAuthorizationGrant();

        UserDiscoveryResponse userDiscoveryResponse = UserDiscoveryRequest.execute(
                this.clientApplication.authenticationAuthority.getUserRealmEndpoint(grant.getUsername()),
                msalRequest.headers().getReadonlyHeaderMap(),
                msalRequest.requestContext(),
                this.clientApplication.getServiceBundle());

        if (userDiscoveryResponse.isAccountFederated()) {
            WSTrustResponse response = WSTrustRequest.execute(
                    userDiscoveryResponse.getFederationMetadataUrl(),
                    grant.getUsername(),
                    grant.getPassword().getValue(),
                    userDiscoveryResponse.getCloudAudienceUrn(),
                    msalRequest.requestContext(),
                    this.clientApplication.getServiceBundle(),
                    this.clientApplication.logPii());

            AuthorizationGrant updatedGrant = getSAMLAuthorizationGrant(response);

            authGrant = new OAuthAuthorizationGrant(updatedGrant, authGrant.getCustomParameters());
        }
        return authGrant;
    }

    private AuthorizationGrant getSAMLAuthorizationGrant(WSTrustResponse response) throws UnsupportedEncodingException {
        AuthorizationGrant updatedGrant;
        if (response.isTokenSaml2()) {
            updatedGrant = new SAML2BearerGrant(new Base64URL(
                    Base64.encodeBase64String(response.getToken().getBytes(
                            "UTF-8"))));
        } else {
            updatedGrant = new SAML11BearerGrant(new Base64URL(
                    Base64.encodeBase64String(response.getToken()
                            .getBytes())));
        }
        return updatedGrant;
    }

    private AuthorizationGrant getAuthorizationGrantIntegrated(String userName) throws Exception {
        AuthorizationGrant updatedGrant;

        String userRealmEndpoint = this.clientApplication.authenticationAuthority.
                getUserRealmEndpoint(URLEncoder.encode(userName, "UTF-8"));

        // Get the realm information
        UserDiscoveryResponse userRealmResponse = UserDiscoveryRequest.execute(
                userRealmEndpoint,
                msalRequest.headers().getReadonlyHeaderMap(),
                msalRequest.requestContext(),
                this.clientApplication.getServiceBundle());

        if (userRealmResponse.isAccountFederated() &&
                "WSTrust".equalsIgnoreCase(userRealmResponse.getFederationProtocol())) {

            String mexURL = userRealmResponse.getFederationMetadataUrl();
            String cloudAudienceUrn = userRealmResponse.getCloudAudienceUrn();

            // Discover the policy for authentication using the Metadata Exchange Url.
            // Get the WSTrust Token (Web Service Trust Token)
            WSTrustResponse wsTrustResponse = WSTrustRequest.execute(
                    mexURL,
                    cloudAudienceUrn,
                    msalRequest.requestContext(),
                    this.clientApplication.getServiceBundle(),
                    this.clientApplication.logPii());

            updatedGrant = getSAMLAuthorizationGrant(wsTrustResponse);
        }
        else if (userRealmResponse.isAccountManaged()) {
            throw new AuthenticationException("Password is required for managed user");
        }
        else{
            throw new AuthenticationException("Unknown User Type");
        }

        return updatedGrant;
    }
}
