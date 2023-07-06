// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class OAuthRequestValidationUnitT extends OAuthRequestValidationTest {
    @Test
    public void oAuthRequest_for_acquireTokenByClientCertificate() throws Exception {
        try {
            IClientCertificate clientCertificate = CertificateHelper.getClientCertificate();

            ConfidentialClientApplication app = ConfidentialClientApplication.builder(CLIENT_ID, clientCertificate)
                    .authority(AUTHORITY)
                    .validateAuthority(false).build();

            // Using UserAssertion as Authorization Grants
            OnBehalfOfParameters parameters =
                    OnBehalfOfParameters.builder(Collections.singleton(SCOPES), new UserAssertion(JWT))
                            .build();

            app.acquireToken(parameters).get();
        } catch (ExecutionException ex) {
            Assert.assertTrue(ex.getCause() instanceof MsalException);
        }

        Map<String, String> queryParams = splitQuery(query);
        Assert.assertEquals(queryParams.size(), 8);

        // validate Authorization Grants query params
        Assert.assertEquals(queryParams.get("grant_type"), GRANT_TYPE_JWT);
        Assert.assertEquals(queryParams.get("assertion"), JWT);

        // validate Client Authentication query params
        Assert.assertFalse(StringUtils.isEmpty(queryParams.get("client_assertion")));

        Set<String> scopes = new HashSet<>(
                Arrays.asList(queryParams.get("scope").split(AbstractMsalAuthorizationGrant.SCOPES_DELIMITER)));

        // validate custom scopes
        Assert.assertTrue(scopes.contains(SCOPES));

        // validate common scopes
        Assert.assertTrue(scopes.contains(AbstractMsalAuthorizationGrant.SCOPE_OPEN_ID));
        Assert.assertTrue(scopes.contains(AbstractMsalAuthorizationGrant.SCOPE_PROFILE));
        Assert.assertTrue(scopes.contains(AbstractMsalAuthorizationGrant.SCOPE_OFFLINE_ACCESS));

        Assert.assertEquals(queryParams.get("client_assertion_type"), CLIENT_ASSERTION_TYPE_JWT);
        Assert.assertEquals(queryParams.get("requested_token_use"), ON_BEHALF_OF_USE_JWT);

        Assert.assertEquals(queryParams.get("client_info"), CLIENT_INFO_VALUE);
        Assert.assertEquals(queryParams.get("client_id"), CLIENT_ID);

    }

    @Test
    public void oAuthRequest_for_acquireTokenByClientAssertion() throws Exception {

        try {
            IClientCertificate clientCertificate = CertificateHelper.getClientCertificate();

            ConfidentialClientApplication app =
                    ConfidentialClientApplication.builder(
                                    CLIENT_ID,
                                    clientCertificate)
                            .authority(AUTHORITY)
                            .validateAuthority(false)
                            .build();

            // Using ClientAssertion for Client Authentication and as the authorization grant

            app.acquireToken(ClientCredentialParameters.builder(Collections.singleton(SCOPES))
                            .build())
                    .get();

        } catch (ExecutionException ex) {
            Assert.assertTrue(ex.getCause() instanceof MsalException);
        }

        Map<String, String> queryParams = splitQuery(query);

        Assert.assertEquals(queryParams.size(), 6);

        // validate Authorization Grants query params
        Assert.assertEquals(queryParams.get("grant_type"), CLIENT_CREDENTIALS_GRANT_TYPE);

        // validate Client Authentication query params
        Assert.assertTrue(StringUtils.isNotEmpty(queryParams.get("client_assertion")));
        Assert.assertEquals(queryParams.get("client_assertion_type"), CLIENT_ASSERTION_TYPE_JWT);

        // to do validate scopes
        Assert.assertEquals(queryParams.get("scope"), "https://SomeResource.azure.net openid profile offline_access");

        Assert.assertEquals(queryParams.get("client_info"), CLIENT_INFO_VALUE);
        Assert.assertEquals(queryParams.get("client_id"), CLIENT_ID);
    }
}