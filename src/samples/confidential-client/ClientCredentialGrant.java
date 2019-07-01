// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import com.microsoft.aad.msal4j.AuthenticationResult;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

class ClientCredentialGrant {

    public static void main(String args[]) throws Exception {
        getAccessTokenByClientCredentialGrant();
    }

    private static void getAccessTokenByClientCredentialGrant() throws Exception {

        ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                TestData.CONFIDENTIAL_CLIENT_ID,
                ClientCredentialFactory.create(TestData.CONFIDENTIAL_CLIENT_SECRET))
                .authority(TestData.TENANT_SPECIFIC_AUTHORITY)
                .build();

        ClientCredentialParameters parameters = ClientCredentialParameters.builder(
                Collections.singleton(TestData.GRAPH_DEFAULT_SCOPE))
                .build();

        CompletableFuture<AuthenticationResult> future = app.acquireToken(parameters);

        future.handle((res, ex) -> {
            if (ex != null) {
                System.out.println("Oops! We have an exception - " + ex.getMessage());
                return "Unknown!";
            }
            System.out.println("Returned ok - " + res);

            System.out.println("Access Token - " + res.accessToken());
            System.out.println("Refresh Token - " + res.refreshToken());
            System.out.println("ID Token - " + res.idToken());
            return res;
        });

        future.join();
    }
}
