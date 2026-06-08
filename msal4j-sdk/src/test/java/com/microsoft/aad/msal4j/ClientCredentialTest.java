// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientCredentialTest {

    @Test
    void testAssertionNullAndEmpty() {
        assertThrows(NullPointerException.class, () ->
                new ClientAssertion(""));

        // Cast to String to resolve ambiguity between constructors
        assertThrows(NullPointerException.class, () ->
                new ClientAssertion((String) null));
    }

    @Test
    void testSecretNullAndEmpty() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new ClientSecret(""));

        assertTrue(ex.getMessage().contains("clientSecret is null or empty"));

        assertThrows(IllegalArgumentException.class, () ->
                new ClientSecret(null));

        assertTrue(ex.getMessage().contains("clientSecret is null or empty"));
    }

    @Test
    void clientCredential_InternalCacheLookup_Success() throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        TestHelper.mockSuccessfulTokenResponse(httpClientMock);

        ConfidentialClientApplication cca = TestHelper.buildCca(httpClientMock);

        ClientCredentialParameters parameters = ClientCredentialParameters.builder(TestHelper.TEST_SCOPE_SET).build();

        IAuthenticationResult result = cca.acquireToken(parameters).get();
        IAuthenticationResult result2 = cca.acquireToken(parameters).get();

        //Client credential flow should perform an internal cache lookup, so similar parameters should only cause one HTTP client call
        assertEquals(result.accessToken(), result2.accessToken());
        verify(httpClientMock, times(1)).send(any());
    }

    @Test
    void clientCredential_TenantOverride() throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        ConfidentialClientApplication cca = TestHelper.buildCca(httpClientMock);

        HashMap<String, String> tokenResponseValues = new HashMap<>();
        tokenResponseValues.put("access_token", "accessTokenFirstCall");

        TestHelper.mockSuccessfulTokenResponse(httpClientMock, tokenResponseValues);
        ClientCredentialParameters parameters = ClientCredentialParameters.builder(TestHelper.TEST_SCOPE_SET).build();

        //The two acquireToken calls have the same parameters...
        IAuthenticationResult resultAppLevelTenant = cca.acquireToken(parameters).get();
        IAuthenticationResult resultAppLevelTenantCached = cca.acquireToken(parameters).get();
        //...so only one token should be added to the cache, and the mocked HTTP client's "send" method should only have been called once
        assertEquals(1, cca.tokenCache.accessTokens.size());
        assertEquals(resultAppLevelTenant.accessToken(), resultAppLevelTenantCached.accessToken());
        verify(httpClientMock, times(1)).send(any());

        tokenResponseValues.put("access_token", "accessTokenSecondCall");

        TestHelper.mockSuccessfulTokenResponse(httpClientMock, tokenResponseValues);
        parameters = ClientCredentialParameters.builder(TestHelper.TEST_SCOPE_SET).tenant("otherTenant").build();

        //Overriding the tenant parameter in the request should lead to a new token call being made...
        IAuthenticationResult resultRequestLevelTenant = cca.acquireToken(parameters).get();
        IAuthenticationResult resultRequestLevelTenantCached = cca.acquireToken(parameters).get();
        //...which should be different from the original token, and thus the cache should have two tokens created from two HTTP calls
        assertEquals(2, cca.tokenCache.accessTokens.size());
        assertEquals(resultRequestLevelTenant.accessToken(), resultRequestLevelTenantCached.accessToken());
        assertNotEquals(resultAppLevelTenant.accessToken(), resultRequestLevelTenant.accessToken());
        verify(httpClientMock, times(2)).send(any());
    }

    @Test
    void testCredentialPrecedenceAndMixing() throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        // Create different credential types for testing
        IClientCredential appLevelCredential = ClientCredentialFactory.createFromSecret("appLevelSecret");
        IClientCredential requestLevelSecret = ClientCredentialFactory.createFromSecret("requestLevelSecret");
        String assertionValue = "test_assertion_value";
        IClientCredential requestLevelAssertion = ClientCredentialFactory.createFromClientAssertion(assertionValue);

        // Create the application with the app-level credential
        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", appLevelCredential)
                        .authority("https://login.microsoftonline.com/tenant")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        // Set up the mock to check which credential is being used
        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            String requestBody = request.body();

            // Check which credential type is included in the request and return a matching token
            if (requestBody.contains("client_secret=requestLevelSecret")) {
                HashMap<String, String> responseParams = new HashMap<>();
                responseParams.put("access_token", "request_secret_token");
                return TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(responseParams));
            } else if (requestBody.contains("client_secret=appLevelSecret")) {
                HashMap<String, String> responseParams = new HashMap<>();
                responseParams.put("access_token", "app_secret_token");
                return TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(responseParams));
            } else if (requestBody.contains("client_assertion=" + assertionValue)) {
                HashMap<String, String> responseParams = new HashMap<>();
                responseParams.put("access_token", "assertion_token");
                return TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(responseParams));
            }
            return null;
        });

        // Test 1: Request with same credential type (secret) at request level
        ClientCredentialParameters parametersWithRequestSecret =
                ClientCredentialParameters.builder(Collections.singleton("scope"))
                        .clientCredential(requestLevelSecret)
                        .skipCache(true)
                        .build();

        IAuthenticationResult result1 = cca.acquireToken(parametersWithRequestSecret).get();
        assertEquals("request_secret_token", result1.accessToken(),
                "Request-level secret should be used when provided");

        // Test 2: Request with different credential type (assertion) at request level
        ClientCredentialParameters parametersWithAssertion =
                ClientCredentialParameters.builder(Collections.singleton("scope"))
                        .clientCredential(requestLevelAssertion)
                        .skipCache(true)
                        .build();

        IAuthenticationResult result2 = cca.acquireToken(parametersWithAssertion).get();
        assertEquals("assertion_token", result2.accessToken(),
                "Request-level assertion should be used when provided");

        // Test 3: Request without credential specified should fall back to app-level
        ClientCredentialParameters parametersWithoutCredential =
                ClientCredentialParameters.builder(Collections.singleton("scope"))
                        .skipCache(true)
                        .build();

        IAuthenticationResult result3 = cca.acquireToken(parametersWithoutCredential).get();
        assertEquals("app_secret_token", result3.accessToken(),
                "App-level credential should be used when request-level credential is not provided");
    }

    @Test
    void acquireTokenClientCredentials_Callback() {
        // Create a counter to track how many times our callback is invoked
        final AtomicInteger callCounter = new AtomicInteger(0);

        // Create a callable that returns a different value each time it's called
        // by including the counter value in the returned string
        Callable<String> callable = () -> {
            int currentCount = callCounter.incrementAndGet();
            return "assertion_" + currentCount;
        };

        // Create the client assertion using our callback
        IClientAssertion credential = ClientCredentialFactory.createFromCallback(callable);

        // Each call to assertion() should invoke our callable
        String assertion1 = credential.assertion();
        String assertion2 = credential.assertion();
        String assertion3 = credential.assertion();

        // Verify the callable was called three times, generating three different assertions
        assertEquals(3, callCounter.get(), "Callable should have been invoked exactly three times");
        assertEquals("assertion_1", assertion1, "First assertion value should match first call");
        assertEquals("assertion_2", assertion2, "Second assertion value should match second call");
        assertEquals("assertion_3", assertion3, "Third assertion value should match third call");

        // Verify assertions are different from each other
        assertNotEquals(assertion1, assertion2, "First and second assertions should be different");
        assertNotEquals(assertion2, assertion3, "Second and third assertions should be different");
    }

    // ========== ClientAssertion: Context-Aware Provider ==========

    @Test
    void clientAssertion_contextAwareProvider_returnsAssertion() {
        Function<AssertionRequestOptions, String> provider = options -> "context-assertion";
        ClientAssertion assertion = new ClientAssertion(provider);

        AssertionRequestOptions options = new AssertionRequestOptions("client-id", "https://endpoint", "/fmi");
        assertEquals("context-assertion", assertion.assertion(options));
    }

    @Test
    void clientAssertion_contextAwareProvider_nullReturnThrows() {
        Function<AssertionRequestOptions, String> provider = options -> null;
        ClientAssertion assertion = new ClientAssertion(provider);

        AssertionRequestOptions options = new AssertionRequestOptions("client-id", "https://endpoint", null);
        MsalClientException ex = assertThrows(MsalClientException.class,
                () -> assertion.assertion(options));
        assertEquals(AuthenticationErrorCode.INVALID_JWT, ex.errorCode());
    }

    @Test
    void clientAssertion_contextAwareProvider_emptyReturnThrows() {
        Function<AssertionRequestOptions, String> provider = options -> "";
        ClientAssertion assertion = new ClientAssertion(provider);

        MsalClientException ex = assertThrows(MsalClientException.class,
                () -> assertion.assertion(new AssertionRequestOptions(null, null, null)));
        assertEquals(AuthenticationErrorCode.INVALID_JWT, ex.errorCode());
    }

    @Test
    void clientAssertion_contextAwareProvider_exceptionWrapped() {
        Function<AssertionRequestOptions, String> provider = options -> {
            throw new RuntimeException("provider failed");
        };
        ClientAssertion assertion = new ClientAssertion(provider);

        MsalClientException ex = assertThrows(MsalClientException.class,
                () -> assertion.assertion(new AssertionRequestOptions(null, null, null)));
        assertTrue(ex.getCause() instanceof RuntimeException);
    }

    @Test
    void clientAssertion_contextAwareProvider_noArgAssertionDelegatesToOptions() {
        // assertion() with no args should delegate to assertion(options) with empty options
        Function<AssertionRequestOptions, String> provider = options -> "from-no-arg";
        ClientAssertion assertion = new ClientAssertion(provider);

        assertEquals("from-no-arg", assertion.assertion());
    }

    @Test
    void clientAssertion_isContextAware_trueForFunctionProvider() {
        Function<AssertionRequestOptions, String> provider = options -> "test";
        ClientAssertion assertion = new ClientAssertion(provider);
        assertTrue(assertion.isContextAware());
    }

    @Test
    void clientAssertion_isContextAware_falseForCallable() {
        ClientAssertion assertion = new ClientAssertion((Callable<String>) () -> "test");
        assertFalse(assertion.isContextAware());
    }

    @Test
    void clientAssertion_isContextAware_falseForStaticString() {
        ClientAssertion assertion = new ClientAssertion("static-assertion");
        assertFalse(assertion.isContextAware());
    }

    // ========== ClientAssertion: Callable Error Paths ==========

    @Test
    void clientAssertion_callableReturnsNull_throwsMsalClientException() {
        ClientAssertion assertion = new ClientAssertion((Callable<String>) () -> null);

        MsalClientException ex = assertThrows(MsalClientException.class, assertion::assertion);
        assertEquals(AuthenticationErrorCode.INVALID_JWT, ex.errorCode());
    }

    @Test
    void clientAssertion_callableReturnsEmpty_throwsMsalClientException() {
        ClientAssertion assertion = new ClientAssertion((Callable<String>) () -> "");

        MsalClientException ex = assertThrows(MsalClientException.class, assertion::assertion);
        assertEquals(AuthenticationErrorCode.INVALID_JWT, ex.errorCode());
    }

    @Test
    void clientAssertion_callableThrowsException_wrappedInMsalClientException() {
        ClientAssertion assertion = new ClientAssertion((Callable<String>) () -> {
            throw new Exception("callable failed");
        });

        MsalClientException ex = assertThrows(MsalClientException.class, assertion::assertion);
        assertTrue(ex.getCause().getMessage().contains("callable failed"));
    }

    @Test
    void clientAssertion_nullCallable_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new ClientAssertion((Callable<String>) null));
    }

    @Test
    void clientAssertion_nullFunction_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new ClientAssertion((Function<AssertionRequestOptions, String>) null));
    }

    // ========== ClientAssertion: Equals & HashCode ==========

    @Test
    void clientAssertion_equals_sameStaticAssertion() {
        ClientAssertion a = new ClientAssertion("test-jwt");
        ClientAssertion b = new ClientAssertion("test-jwt");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void clientAssertion_equals_differentStaticAssertion() {
        ClientAssertion a = new ClientAssertion("jwt-1");
        ClientAssertion b = new ClientAssertion("jwt-2");

        assertNotEquals(a, b);
    }

    @Test
    void clientAssertion_equals_sameCallable() {
        Callable<String> callable = () -> "test";
        ClientAssertion a = new ClientAssertion(callable);
        ClientAssertion b = new ClientAssertion(callable);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void clientAssertion_equals_differentCallables() {
        ClientAssertion a = new ClientAssertion((Callable<String>) () -> "test");
        ClientAssertion b = new ClientAssertion((Callable<String>) () -> "test");

        // Different callable instances are compared by identity, so they're not equal
        assertNotEquals(a, b);
    }

    @Test
    void clientAssertion_equals_sameContextAwareProvider() {
        Function<AssertionRequestOptions, String> provider = opts -> "test";
        ClientAssertion a = new ClientAssertion(provider);
        ClientAssertion b = new ClientAssertion(provider);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void clientAssertion_equals_differentContextAwareProviders() {
        ClientAssertion a = new ClientAssertion((Function<AssertionRequestOptions, String>) opts -> "test");
        ClientAssertion b = new ClientAssertion((Function<AssertionRequestOptions, String>) opts -> "test");

        assertNotEquals(a, b);
    }

    @Test
    void clientAssertion_equals_selfAndNull() {
        ClientAssertion a = new ClientAssertion("jwt");
        assertEquals(a, a);
        assertFalse(a.equals(null));
        assertFalse(a.equals("not-a-ClientAssertion"));
    }

    // ========== ClientSecret: Equals & HashCode ==========

    @Test
    void clientSecret_equals_sameSecret() {
        ClientSecret a = new ClientSecret("secret-1");
        ClientSecret b = new ClientSecret("secret-1");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void clientSecret_equals_differentSecret() {
        ClientSecret a = new ClientSecret("secret-1");
        ClientSecret b = new ClientSecret("secret-2");

        assertNotEquals(a, b);
    }

    @Test
    void clientSecret_equals_selfAndNull() {
        ClientSecret a = new ClientSecret("secret");
        assertEquals(a, a);
        assertFalse(a.equals(null));
        assertFalse(a.equals("not-a-ClientSecret"));
    }

    // ========== ClientCredentialFactory ==========

    @Test
    void clientCredentialFactory_createFromCertificateChain_validInput() {
        ClientCertificate cert = ClientCertificate.create(
                TestHelper.getPrivateKey(), TestHelper.getX509Cert());

        assertNotNull(cert);
        assertNotNull(cert.privateKey());
    }

    @Test
    void clientCredentialFactory_createFromCertificateChain_nullKey() {
        assertThrows(IllegalArgumentException.class,
                () -> ClientCredentialFactory.createFromCertificateChain(null,
                        Collections.singletonList(TestHelper.getX509Cert())));
    }

    @Test
    void clientCredentialFactory_createFromCertificateChain_nullChain() {
        assertThrows(IllegalArgumentException.class,
                () -> ClientCredentialFactory.createFromCertificateChain(
                        TestHelper.getPrivateKey(), null));
    }

    @Test
    void clientCredentialFactory_createFromCertificateChain_emptyChain() {
        assertThrows(IllegalArgumentException.class,
                () -> ClientCredentialFactory.createFromCertificateChain(
                        TestHelper.getPrivateKey(), Collections.emptyList()));
    }

    @Test
    void clientCredentialFactory_createFromCallback_nullCallable() {
        assertThrows(NullPointerException.class,
                () -> ClientCredentialFactory.createFromCallback((Callable<String>) null));
    }

    @Test
    void clientCredentialFactory_createFromCallback_nullFunction() {
        assertThrows(NullPointerException.class,
                () -> ClientCredentialFactory.createFromCallback(
                        (Function<AssertionRequestOptions, String>) null));
    }
}
