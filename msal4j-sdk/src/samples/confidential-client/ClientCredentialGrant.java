// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientCredential;

import java.util.Collections;
import java.util.Set;

class ClientCredentialGrant {

    private final static String CLIENT_ID = "";
    private final static String AUTHORITY = "https://login.microsoftonline.com/<tenant>/";
    private final static String CLIENT_SECRET = "";
    private final static Set<String> SCOPE = Collections.singleton("");

    public static void main(String args[]) throws Exception {
        IAuthenticationResult result = acquireToken();
        System.out.println("Access token: " + result.accessToken());
    }

    private static IAuthenticationResult acquireToken() throws Exception {

        // This is the secret that is created in the Azure portal when registering the application
        IClientCredential credential = ClientCredentialFactory.createFromSecret(CLIENT_SECRET);
        ConfidentialClientApplication cca =
                ConfidentialClientApplication
                        .builder(CLIENT_ID, credential)
                        .authority(AUTHORITY)
                        .build();

        // Client credential requests will by default try to look for a valid token in the
        // in-memory token cache. If found, it will return this token. If a token is not found, or the
        // token is not valid, it will fall back to acquiring a token from the AAD service. Although
        // not recommended unless there is a reason for doing so, you can skip the cache lookup
        // by using .skipCache(true) in ClientCredentialParameters.
        ClientCredentialParameters parameters =
                ClientCredentialParameters
                        .builder(SCOPE)
                        .build();

        return cca.acquireToken(parameters).join();
    }
}
