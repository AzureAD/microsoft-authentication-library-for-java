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

import java.net.URLEncoder;

class AcquireTokenByAuthorizationGrantSupplier extends AuthenticationResultSupplier {

    private MsalRequest msalRequest;

    AcquireTokenByAuthorizationGrantSupplier(ClientApplicationBase clientApplication,
                                             MsalRequest msalRequest) {
        super(clientApplication, msalRequest.getHeaders());
        this.msalRequest = msalRequest;
    }

    AuthenticationResult execute() throws Exception {
        AbstractMsalAuthorizationGrant authGrant = msalRequest.getMsalAuthorizationGrant();
        if (authGrant instanceof MsalOAuthAuthorizationGrant) {
            msalRequest.setMsalAuthorizationGrant(
                    processPasswordGrant((MsalOAuthAuthorizationGrant) authGrant));
        }

        if (authGrant instanceof MsalIntegratedAuthorizationGrant) {
            MsalIntegratedAuthorizationGrant integratedAuthGrant =
                    (MsalIntegratedAuthorizationGrant) authGrant;
            msalRequest.setMsalAuthorizationGrant(
                    new MsalOAuthAuthorizationGrant(getAuthorizationGrantIntegrated(
                            integratedAuthGrant.getUserName()), integratedAuthGrant.getScopes()));
        }

        return this.clientApplication.acquireTokenCommon(msalRequest);
    }

    private MsalOAuthAuthorizationGrant processPasswordGrant(
            MsalOAuthAuthorizationGrant authGrant) throws Exception {

        if (!(authGrant.getAuthorizationGrant() instanceof ResourceOwnerPasswordCredentialsGrant)) {
            return authGrant;
        }
        ResourceOwnerPasswordCredentialsGrant grant =
                (ResourceOwnerPasswordCredentialsGrant) authGrant.getAuthorizationGrant();

        UserDiscoveryResponse userDiscoveryResponse = UserDiscoveryRequest.execute(
                this.clientApplication.authenticationAuthority.getUserRealmEndpoint(grant.getUsername()),
                msalRequest.getHeaders().getReadonlyHeaderMap(),
                this.clientApplication.getServiceBundle());

        if (userDiscoveryResponse.isAccountFederated()) {
            WSTrustResponse response = WSTrustRequest.execute(
                    userDiscoveryResponse.getFederationMetadataUrl(),
                    grant.getUsername(),
                    grant.getPassword().getValue(),
                    userDiscoveryResponse.getCloudAudienceUrn(),
                    this.clientApplication.getServiceBundle(),
                    this.clientApplication.isLogPii());

            AuthorizationGrant updatedGrant;
            if (response.isTokenSaml2()) {
                updatedGrant = new SAML2BearerGrant(new Base64URL(
                        Base64.encodeBase64String(response.getToken().getBytes(
                                "UTF-8"))));
            }
            else {
                updatedGrant = new SAML11BearerGrant(new Base64URL(
                        Base64.encodeBase64String(response.getToken()
                                .getBytes())));
            }
            authGrant = new MsalOAuthAuthorizationGrant(updatedGrant,
                    authGrant.getCustomParameters());
        }
        return authGrant;
    }

    private AuthorizationGrant getAuthorizationGrantIntegrated(String userName) throws Exception {
        AuthorizationGrant updatedGrant;

        String userRealmEndpoint = this.clientApplication.authenticationAuthority.
                getUserRealmEndpoint(URLEncoder.encode(userName, "UTF-8"));

        // Get the realm information
        UserDiscoveryResponse userRealmResponse = UserDiscoveryRequest.execute(
                userRealmEndpoint,
                msalRequest.getHeaders().getReadonlyHeaderMap(),
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
                    this.clientApplication.getServiceBundle(),
                    this.clientApplication.isLogPii());

            if (wsTrustResponse.isTokenSaml2()) {
                updatedGrant =
                        new SAML2BearerGrant(new Base64URL(Base64.encodeBase64String(
                                        wsTrustResponse.getToken().getBytes("UTF-8"))));
            }
            else {
                updatedGrant =
                        new SAML11BearerGrant(new Base64URL(Base64.encodeBase64String(
                                wsTrustResponse.getToken().getBytes())));
            }
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
