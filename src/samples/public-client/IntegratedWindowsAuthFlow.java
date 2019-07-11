// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IntegratedWindowsAuthenticationParameters;
import com.microsoft.aad.msal4j.PublicClientApplication;

import java.util.Collections;
import java.util.concurrent.Future;

public class IntegratedWindowsAuthFlow {
    public static void main(String args[]) throws Exception {

        IAuthenticationResult result = getAccessTokenByIntegratedAuth();

        System.out.println("Access Token - " + result.accessToken());
        System.out.println("ID Token - " + result.idToken());
    }

    private static IAuthenticationResult getAccessTokenByIntegratedAuth() throws Exception {
        PublicClientApplication app = PublicClientApplication.builder(TestData.PUBLIC_CLIENT_ID)
                .authority(TestData.AUTHORITY_ORGANIZATION)
                .telemetryConsumer(new Telemetry.MyTelemetryConsumer().telemetryConsumer)
                .build();

        IntegratedWindowsAuthenticationParameters parameters =
                IntegratedWindowsAuthenticationParameters.builder(
                        Collections.singleton(TestData.GRAPH_DEFAULT_SCOPE), TestData.USER_NAME)
                        .build();

        Future<IAuthenticationResult> future = app.acquireToken(parameters);

        IAuthenticationResult result = future.get();

        return result;
    }
}
