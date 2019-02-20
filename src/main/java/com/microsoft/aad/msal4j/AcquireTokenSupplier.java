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
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import org.apache.commons.codec.binary.Base64;;
import java.net.URLEncoder;

public class AcquireTokenSupplier extends AuthenticationResultSupplier {

    private AbstractMsalAuthorizationGrant authGrant;
    private ClientAuthentication clientAuth;

    AcquireTokenSupplier(ClientApplicationBase clientApplication,
                         AbstractMsalAuthorizationGrant authGrant, ClientAuthentication clientAuth) {
        super(clientApplication);
        this.authGrant = authGrant;
        this.clientAuth = clientAuth;

        String correlationId = clientApplication.getCorrelationId();
        if (StringHelper.isBlank(correlationId) &&
                authGrant instanceof MsalDeviceCodeAuthorizationGrant) {
            correlationId = ((MsalDeviceCodeAuthorizationGrant) authGrant).getCorrelationId();
        }

        this.headers = new ClientDataHttpHeaders(correlationId);
    }

    AuthenticationResult execute() throws Exception {
        if (authGrant instanceof MsalOAuthAuthorizationGrant) {
            authGrant = processPasswordGrant((MsalOAuthAuthorizationGrant) authGrant);
        }

        if (this.authGrant instanceof MsalIntegratedAuthorizationGrant) {
            MsalIntegratedAuthorizationGrant integratedAuthGrant = (MsalIntegratedAuthorizationGrant) authGrant;
            authGrant = new MsalOAuthAuthorizationGrant(
                    getAuthorizationGrantIntegrated(integratedAuthGrant.getUserName()),
                    integratedAuthGrant.getScopes());
        }

        return clientApplication.acquireTokenCommon(this.authGrant, this.clientAuth, this.headers);
    }

    /**
     * @param authGrant
     */
    private MsalOAuthAuthorizationGrant processPasswordGrant(
            MsalOAuthAuthorizationGrant authGrant) throws Exception {

        if (!(authGrant.getAuthorizationGrant() instanceof ResourceOwnerPasswordCredentialsGrant)) {
            return authGrant;
        }

        ResourceOwnerPasswordCredentialsGrant grant = (ResourceOwnerPasswordCredentialsGrant) authGrant
                .getAuthorizationGrant();

        UserDiscoveryResponse userDiscoveryResponse = UserDiscoveryRequest.execute(
                clientApplication.authenticationAuthority.getUserRealmEndpoint(grant.getUsername()),
                this.headers.getReadonlyHeaderMap(),
                clientApplication.getProxy(),
                clientApplication.getSslSocketFactory());
        if (userDiscoveryResponse.isAccountFederated()) {
            WSTrustResponse response = WSTrustRequest.execute(
                    userDiscoveryResponse.getFederationMetadataUrl(),
                    grant.getUsername(), grant.getPassword().getValue(), userDiscoveryResponse.getCloudAudienceUrn(),
                    clientApplication.getProxy(), clientApplication.getSslSocketFactory(), clientApplication.isLogPii());

            AuthorizationGrant updatedGrant = null;
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

    AuthorizationGrant getAuthorizationGrantIntegrated(String userName) throws Exception {
        AuthorizationGrant updatedGrant;

        String userRealmEndpoint = clientApplication.authenticationAuthority.
                getUserRealmEndpoint(URLEncoder.encode(userName, "UTF-8"));

        // Get the realm information
        UserDiscoveryResponse userRealmResponse = UserDiscoveryRequest.execute(
                userRealmEndpoint,
                this.headers.getReadonlyHeaderMap(),
                clientApplication.getProxy(),
                clientApplication.getSslSocketFactory());

        if (userRealmResponse.isAccountFederated() &&
                "WSTrust".equalsIgnoreCase(userRealmResponse.getFederationProtocol())) {
            String mexURL = userRealmResponse.getFederationMetadataUrl();
            String cloudAudienceUrn = userRealmResponse.getCloudAudienceUrn();

            // Discover the policy for authentication using the Metadata Exchange Url.
            // Get the WSTrust Token (Web Service Trust Token)
            WSTrustResponse wsTrustResponse = WSTrustRequest.execute
                    (mexURL, cloudAudienceUrn, clientApplication.getProxy(),
                            clientApplication.getSslSocketFactory(), clientApplication.isLogPii());

            if (wsTrustResponse.isTokenSaml2()) {
                updatedGrant = new SAML2BearerGrant(
                        new Base64URL(Base64.encodeBase64String(wsTrustResponse.getToken().getBytes("UTF-8"))));
            }
            else {
                updatedGrant = new SAML11BearerGrant(
                        new Base64URL(Base64.encodeBase64String(wsTrustResponse.getToken().getBytes())));
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
