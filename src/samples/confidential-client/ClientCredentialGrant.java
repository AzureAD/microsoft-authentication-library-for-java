// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientCredential;
import com.microsoft.aad.msal4j.MsalException;
import com.microsoft.aad.msal4j.SilentParameters;

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

        // Load token cache from file and initialize token cache aspect. The token cache will have
        // dummy data, so the acquireTokenSilently call will fail.
        TokenCacheAspect tokenCacheAspect = new TokenCacheAspect("sample_cache.json");

        // This is the secret that is created in the Azure portal when registering the application
        IClientCredential credential = ClientCredentialFactory.createFromSecret(CLIENT_SECRET);
        ConfidentialClientApplication cca =
                ConfidentialClientApplication
                        .builder(CLIENT_ID, credential)
                        .authority(AUTHORITY)
                        .setTokenCacheAccessAspect(tokenCacheAspect)
                        .build();

        IAuthenticationResult result;
        try {
            SilentParameters silentParameters =
                    SilentParameters
                            .builder(SCOPE)
                            .build();

            // try to acquire token silently. This call will fail since the token cache does not
            // have a token for the application you are requesting an access token for
            result = cca.acquireTokenSilently(silentParameters).join();
        } catch (Exception ex) {
            if (ex.getCause() instanceof MsalException) {

                ClientCredentialParameters parameters =
                        ClientCredentialParameters
                                .builder(SCOPE)
                                .build();

                // Try to acquire a token. If successful, you should see
                // the token information printed out to console
                result = cca.acquireToken(parameters).join();
            } else {
                // Handle other exceptions accordingly
                throw ex;
            }
        }
        return result;
    }
}
