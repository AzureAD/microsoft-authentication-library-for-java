// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import com.microsoft.aad.msal4j.*;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DeviceCodeFlow {
    public static void main(String args[]) throws Exception {
        getAccessTokenByDeviceCodeGrant();
    }

    private static void getAccessTokenByDeviceCodeGrant() throws Exception {
        PublicClientApplication app = PublicClientApplication.builder(TestData.PUBLIC_CLIENT_ID)
                .authority(TestData.AUTHORITY_COMMON)
                .build();

        Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) -> {
            System.out.println(deviceCode.message());
        };

        CompletableFuture<IAuthenticationResult> future = app.acquireToken(
                DeviceCodeFlowParameters.builder(
                        Collections.singleton(TestData.GRAPH_DEFAULT_SCOPE),
                        deviceCodeConsumer)
                        .build());

        future.handle((res, ex) -> {
            if(ex != null) {
                System.out.println("Oops! We have an exception of type - " + ex.getClass());
                System.out.println("message - " + ex.getMessage());
                return "Unknown!";
            }
            System.out.println("Returned ok - " + res);

            System.out.println("Access Token - " + res.accessToken());
            System.out.println("ID Token - " + res.idToken());
            return res;
        });

        future.join();
    }
}
