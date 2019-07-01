// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.UserNamePasswordParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Telemetry {

    private static List<HashMap<String,String>> eventsReceived = new ArrayList<>();

    public static void main(String args[]) throws Exception {
        getAccessTokenFromUserCredentials();
    }

    public static class MyTelemetryConsumer {

        Consumer<List<HashMap<String, String>>> telemetryConsumer =
                (List<HashMap<String, String>> telemetryEvents) -> {
                    eventsReceived.addAll(telemetryEvents);
                    System.out.println("Received " + telemetryEvents.size() + " events");
                    telemetryEvents.forEach(event -> {
                        System.out.print("Event Name: " + event.get("event_name"));
                        event.entrySet().forEach(entry -> System.out.println("   " + entry));
                    });
                };
    }

    private static void getAccessTokenFromUserCredentials() throws Exception {
        PublicClientApplication app = PublicClientApplication.builder(TestData.PUBLIC_CLIENT_ID)
                .authority(TestData.AUTHORITY_ORGANIZATION)
                .telemetryConsumer(new MyTelemetryConsumer().telemetryConsumer)
                .build();

        CompletableFuture<IAuthenticationResult> future = app.acquireToken
                (UserNamePasswordParameters.builder(Collections.singleton(TestData.GRAPH_DEFAULT_SCOPE),
                        TestData.USER_NAME,
                        TestData.USER_PASSWORD.toCharArray())
                        .build());

        IAuthenticationResult result = future.get();
        System.out.println(result.accessToken());
        System.out.println(result.idToken());
    }
}
