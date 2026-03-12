package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvalidAuthorityIT extends SeleniumTest{

    @Test
    void acquireTokenWithAuthorizationCode_InvalidAuthority() throws Exception{
        PublicClientApplication app;
        app = PublicClientApplication.builder(
                        TestConfiguration.AAD_CLIENT_ID)
                .authority("https://dummy.microsoft.com/common") //invalid authority, request fails at instance discovery
                .build();

        CompletableFuture<IAuthenticationResult> future = app.acquireToken(
                AuthorizationCodeParameters.builder("auth_code", new URI(TestConfiguration.AAD_DEFAULT_REDIRECT_URI))
                        .scopes(Collections.singleton("default-scope"))
                        .authorizationCode("auth_code").redirectUri(new URI(TestConfiguration.AAD_DEFAULT_REDIRECT_URI)).build());

        ExecutionException ex = assertThrows(ExecutionException.class, future::get);

        assertTrue(ex.getMessage().contains("invalid instance"));
    }
}
