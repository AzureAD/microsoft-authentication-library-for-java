// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import com.microsoft.aad.msal4j.*;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class UsernamePasswordFlow {

    public static void main(String args[]) throws Exception {
        getAccessTokenFromUserCredentials();
    }

    private static void getAccessTokenFromUserCredentials() throws Exception {
        PublicClientApplication app = PublicClientApplication.builder(TestData.PUBLIC_CLIENT_ID)
                .authority(TestData.AUTHORITY_ORGANIZATION)
                .build();

        UserNamePasswordParameters parameters = UserNamePasswordParameters.builder(
                Collections.singleton(TestData.GRAPH_DEFAULT_SCOPE),
                TestData.USER_NAME,
                TestData.USER_PASSWORD.toCharArray())
                .build();

        CompletableFuture<IAuthenticationResult> future = app.acquireToken(parameters);

        future.handle((res, ex) -> {
            if(ex != null) {
                System.out.println("Oops! We have an exception - " + ex.getMessage());
                return "Unknown!";
            }

            Collection<IAccount> accounts = app.getAccounts().join();

            CompletableFuture<IAuthenticationResult> future1;
            try {
                future1 = app.acquireTokenSilently
                        (SilentParameters.builder(Collections.singleton(TestData.GRAPH_DEFAULT_SCOPE),
                                accounts.iterator().next())
                                .forceRefresh(true)
                                .build());

            } catch (MalformedURLException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }

            future1.join();

            IAccount account = app.getAccounts().join().iterator().next();
            app.removeAccount(account).join();
            accounts = app.getAccounts().join();

            System.out.println("Num of account - " + accounts.size());
            System.out.println("Returned ok - " + res);
            System.out.println("Access Token - " + res.accessToken());
            System.out.println("ID Token - " + res.idToken());
            return res;
        }).join();

    }
}
