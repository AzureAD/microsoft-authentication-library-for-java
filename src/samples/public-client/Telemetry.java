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

import com.microsoft.aad.msal4j.AuthenticationResult;
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

        CompletableFuture<AuthenticationResult> future = app.acquireToken
                (UserNamePasswordParameters.builder(Collections.singleton(TestData.GRAPH_DEFAULT_SCOPE),
                        TestData.USER_NAME,
                        TestData.USER_PASSWORD.toCharArray())
                        .build());

        AuthenticationResult result = future.get();
        System.out.println(result.accessToken());
        System.out.println(result.refreshToken());
        System.out.println(result.idToken());
    }
}
