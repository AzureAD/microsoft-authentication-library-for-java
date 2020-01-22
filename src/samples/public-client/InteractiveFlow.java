// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.InteractiveRequestParameters;
import com.microsoft.aad.msal4j.PublicClientApplication;

import java.net.URI;
import java.util.Collections;

public class InteractiveFlow {
    public static void main(String[] args) throws Exception{
         IAuthenticationResult result = getAccessTokenByInteractiveFlow();
         System.out.println(result.accessToken());
         System.out.println(result.account());
     System.out.println(result.idToken());
    }

    private static IAuthenticationResult getAccessTokenByInteractiveFlow() throws Exception {

        PublicClientApplication publicClientApplication = PublicClientApplication
                .builder(TestData.PUBLIC_CLIENT_ID)
                .authority(TestData.TENANT_SPECIFIC_AUTHORITY)
                .build();

        InteractiveRequestParameters parameters = InteractiveRequestParameters.builder()
                .redirectUri(new URI("http://localhost:8080"))
                .scopes(Collections.singleton(TestData.testScope))
                .build();

        IAuthenticationResult result = publicClientApplication.acquireToken(parameters).join();
        return result;
    }
}