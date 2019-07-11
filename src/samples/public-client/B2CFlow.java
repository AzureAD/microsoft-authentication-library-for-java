// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.UserNamePasswordParameters;


import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class B2CFlow {

    public static void main(String args[]) throws Exception {
        getAccessTokenFromUserCredentials();
    }

    private static void getAccessTokenFromUserCredentials() throws Exception {

        PublicClientApplication app = PublicClientApplication.builder(TestData.PUBLIC_CLIENT_ID)
                .b2cAuthority(TestData.B2C_AUTHORITY)
                .build();

        CompletableFuture<IAuthenticationResult> future = app.acquireToken(
                UserNamePasswordParameters.builder(
                        Collections.singleton(TestData.LAB_DEFAULT_B2C_SCOPE),
                        TestData.USER_NAME,
                        TestData.USER_PASSWORD.toCharArray()).build());

        future.handle((res, ex) -> {
            if(ex != null) {
                System.out.println("Oops! We have an exception - " + ex.getMessage());
                return "Unknown!";
            }
            System.out.println("Returned ok - " + res);

            System.out.println("Access Token - " + res.accessToken());
            System.out.println("ID Token - " + res.idToken());
            return res;
        }).join();
    }
}