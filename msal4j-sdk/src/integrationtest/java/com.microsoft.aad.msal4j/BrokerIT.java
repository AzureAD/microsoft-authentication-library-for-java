package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4jbrokers.MsalRuntimeBroker;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BrokerIT {
    
    @Test
    public void acquireTokenSilent_usingBroker_DefaultOSAccount() throws MalformedURLException, ExecutionException, InterruptedException {
        //TODO: Hardcoded params for now, will hopefully be able to copy the Python interop's ID Labs queries. If not, will only be able to do automated testing in the interop package
        String clientId = "903c8a8a-9e74-415e-9921-711a293d90cb";
        String authority = "https://login.microsoftonline.com/common";
        String scopes = "https://graph.microsoft.com/.default";

        MsalRuntimeBroker broker = new MsalRuntimeBroker();

        PublicClientApplication pca = PublicClientApplication.builder(
                clientId).
                authority(authority).
                correlationId(UUID.randomUUID().toString()).
                broker(broker).
                build();

        SilentParameters parameters = SilentParameters.builder(Collections.singleton(scopes)).build();

        CompletableFuture<IAuthenticationResult> future = pca.acquireTokenSilently(parameters);

        IAuthenticationResult result = future.get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());

        System.out.println("Access token in result: " + result.accessToken());
        System.out.println("ID token in result: " + result.idToken());
        System.out.println("Account ID in result: " + result.account().homeAccountId());
        System.out.println("Username in result: " + result.account().username());
    }
}
