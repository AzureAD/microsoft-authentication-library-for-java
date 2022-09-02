// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import com.microsoft.aad.msal4j.*;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

public class DeviceCodeFlow {

    private final static String CLIENT_ID = "";
    private final static String AUTHORITY = "https://login.microsoftonline.com/common/";
    private final static Set<String> SCOPE = Collections.singleton("");

    public static void main(String args[]) throws Exception {
        IAuthenticationResult result = acquireTokenDeviceCode();
        System.out.println("Access token: " + result.accessToken());
        System.out.println("Id token: " + result.idToken());
        System.out.println("Account username: " + result.account().username());
    }

    private static IAuthenticationResult acquireTokenDeviceCode() throws Exception {

        // Load token cache from file and initialize token cache aspect. The token cache will have
        // dummy data, so the acquireTokenSilently call will fail.
        TokenCacheAspect tokenCacheAspect = new TokenCacheAspect("sample_cache.json");

        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .authority(AUTHORITY)
                .setTokenCacheAccessAspect(tokenCacheAspect)
                .build();

        Set<IAccount> accountsInCache = pca.getAccounts().join();
        // Take first account in the cache. In a production application, you would filter
        // accountsInCache to get the right account for the user authenticating.
        IAccount account = accountsInCache.iterator().next();

        IAuthenticationResult result;
        try {
            SilentParameters silentParameters =
                    SilentParameters
                            .builder(SCOPE, account)
                            .build();

            // try to acquire token silently. This call will fail since the token cache
            // does not have any data for the user you are trying to acquire a token for
            result = pca.acquireTokenSilently(silentParameters).join();
        } catch (Exception ex) {
            if (ex.getCause() instanceof MsalException) {

                Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) ->
                        System.out.println(deviceCode.message());

                DeviceCodeFlowParameters parameters =
                        DeviceCodeFlowParameters
                                .builder(SCOPE, deviceCodeConsumer)
                                .build();

                // Try to acquire a token via device code flow. If successful, you should see
                // the token and account information printed out to console, and the sample_cache.json
                // file should have been updated with the latest tokens.
                result = pca.acquireToken(parameters).join();
            } else {
                // Handle other exceptions accordingly
                throw ex;
            }
        }
        return result;
    }
}
