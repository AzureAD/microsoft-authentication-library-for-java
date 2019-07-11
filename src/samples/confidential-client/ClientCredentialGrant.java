// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import com.microsoft.aad.msal4j.*;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

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

        ClientCredentialParameters clientCredentialParam = ClientCredentialParameters.builder(
                Collections.singleton(TestData.GRAPH_DEFAULT_SCOPE))
                .build();

        CompletableFuture<IAuthenticationResult> future = app.acquireToken(clientCredentialParam);

        BiConsumer<IAuthenticationResult, Throwable> processAuthResult = (res, ex) -> {
            if (ex != null) {
                System.out.println("Oops! We have an exception - " + ex.getMessage());
            }
            System.out.println("Returned ok - " + res);
            System.out.println("Access Token - " + res.accessToken());
            System.out.println("ID Token - " + res.idToken());
        };

        future.whenCompleteAsync(processAuthResult);
        future.join();

        SilentParameters silentParameters =
                SilentParameters.builder(Collections.singleton(TestData.GRAPH_DEFAULT_SCOPE)).build();

        future = app.acquireTokenSilently(silentParameters);

        future.whenCompleteAsync(processAuthResult);
        future.join();
    }
}
