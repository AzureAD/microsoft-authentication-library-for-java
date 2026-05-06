// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

final class ClientAssertion implements IClientAssertion {

    static final String ASSERTION_TYPE_JWT_BEARER = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    static final String ASSERTION_TYPE_JWT_POP = "urn:ietf:params:oauth:client-assertion-type:jwt-pop";
    private final String assertion;
    private final Callable<String> assertionProvider;
    private final Function<AssertionRequestOptions, String> contextAwareAssertionProvider;
    private final Function<AssertionRequestOptions, AssertionResponse> contextAwareResponseProvider;

    /**
     * Constructor that accepts a static assertion string
     *
     * @param assertion The JWT assertion string to use
     * @throws NullPointerException if assertion is null or empty
     */
    ClientAssertion(final String assertion) {
        if (StringHelper.isBlank(assertion)) {
            throw new NullPointerException("assertion");
        }

        this.assertion = assertion;
        this.assertionProvider = null;
        this.contextAwareAssertionProvider = null;
        this.contextAwareResponseProvider = null;
    }

    /**
     * Constructor that accepts a callable that provides the assertion string
     *
     * @param assertionProvider A callable that returns a JWT assertion string
     * @throws NullPointerException if assertionProvider is null
     */
    ClientAssertion(final Callable<String> assertionProvider) {
        if (assertionProvider == null) {
            throw new NullPointerException("assertionProvider");
        }

        this.assertion = null;
        this.assertionProvider = assertionProvider;
        this.contextAwareAssertionProvider = null;
        this.contextAwareResponseProvider = null;
    }

    /**
     * Constructor that accepts a context-aware function that provides the assertion string.
     * The function receives {@link AssertionRequestOptions} containing context such as
     * the client ID, token endpoint, and FMI path.
     *
     * @param contextAwareAssertionProvider A function that receives context and returns a JWT assertion string
     * @throws NullPointerException if contextAwareAssertionProvider is null
     */
    ClientAssertion(final Function<AssertionRequestOptions, String> contextAwareAssertionProvider) {
        if (contextAwareAssertionProvider == null) {
            throw new NullPointerException("contextAwareAssertionProvider");
        }

        this.assertion = null;
        this.assertionProvider = null;
        this.contextAwareAssertionProvider = contextAwareAssertionProvider;
        this.contextAwareResponseProvider = null;
    }

    /**
     * Constructor that accepts a context-aware function returning an {@link AssertionResponse}.
     * This allows the callback to supply both the assertion JWT and an optional token-binding
     * certificate for mTLS PoP scenarios.
     *
     * @param contextAwareResponseProvider A function that receives context and returns an AssertionResponse
     * @throws NullPointerException if contextAwareResponseProvider is null
     */
    ClientAssertion(final Function<AssertionRequestOptions, AssertionResponse> contextAwareResponseProvider,
                    boolean responseProvider) {
        if (contextAwareResponseProvider == null) {
            throw new NullPointerException("contextAwareResponseProvider");
        }

        this.assertion = null;
        this.assertionProvider = null;
        this.contextAwareAssertionProvider = null;
        this.contextAwareResponseProvider = contextAwareResponseProvider;
    }

    /**
     * Gets the JWT assertion for client authentication.
     * If this ClientAssertion was created with a Callable, the callable will be
     * invoked each time this method is called to generate a fresh assertion.
     * If created with a context-aware Function, it is invoked with a default (empty) context.
     *
     * @return A JWT assertion string
     * @throws MsalClientException if the assertion provider returns null/empty or throws an exception
     */
    public String assertion() {
        if (contextAwareResponseProvider != null) {
            AssertionResponse response = assertionResponse(new AssertionRequestOptions(null, null, null));
            return response.assertion();
        }

        if (contextAwareAssertionProvider != null) {
            return assertion(new AssertionRequestOptions(null, null, null));
        }

        if (assertionProvider != null) {
            return invokeCallable();
        }

        return this.assertion;
    }

    /**
     * Gets the JWT assertion for client authentication with context information.
     * If this ClientAssertion was created with a context-aware Function, the function will receive
     * the provided context. If created with a Callable or static string, the context is ignored.
     *
     * @param options context information for the assertion request
     * @return A JWT assertion string
     * @throws MsalClientException if the assertion provider returns null/empty or throws an exception
     */
    String assertion(AssertionRequestOptions options) {
        if (contextAwareResponseProvider != null) {
            AssertionResponse response = assertionResponse(options);
            return response.assertion();
        }

        if (contextAwareAssertionProvider != null) {
            try {
                String generatedAssertion = contextAwareAssertionProvider.apply(options);

                if (StringHelper.isBlank(generatedAssertion)) {
                    throw new MsalClientException(
                        "Assertion provider returned null or empty assertion",
                        AuthenticationErrorCode.INVALID_JWT);
                }

                return generatedAssertion;
            } catch (MsalClientException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new MsalClientException(ex);
            }
        }

        // Fall back to non-context-aware assertion
        return assertion();
    }

    /**
     * Gets the full AssertionResponse from the context-aware response provider.
     * Returns null if this ClientAssertion does not use a response provider.
     *
     * @param options context information for the assertion request
     * @return An AssertionResponse, or null if not using a response provider
     * @throws MsalClientException if the provider returns null or throws an exception
     */
    AssertionResponse assertionResponse(AssertionRequestOptions options) {
        if (contextAwareResponseProvider == null) {
            return null;
        }

        try {
            AssertionResponse response = contextAwareResponseProvider.apply(options);

            if (response == null || StringHelper.isBlank(response.assertion())) {
                throw new MsalClientException(
                    "Assertion provider returned null or empty assertion",
                    AuthenticationErrorCode.INVALID_JWT);
            }

            return response;
        } catch (MsalClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MsalClientException(ex);
        }
    }

    /**
     * Returns true if this assertion uses a context-aware provider (either string or response).
     */
    boolean isContextAware() {
        return contextAwareAssertionProvider != null || contextAwareResponseProvider != null;
    }

    private String invokeCallable() {
        try {
            String generatedAssertion = assertionProvider.call();

            if (StringHelper.isBlank(generatedAssertion)) {
                throw new MsalClientException(
                    "Assertion provider returned null or empty assertion",
                    AuthenticationErrorCode.INVALID_JWT);
            }

            return generatedAssertion;
        } catch (MsalClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MsalClientException(ex);
        }
    }

    //These methods are based on those generated by Lombok's @EqualsAndHashCode annotation.
    //They have the same functionality as the generated methods, but were refactored for readability.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientAssertion)) return false;

        ClientAssertion other = (ClientAssertion) o;

        // For context-aware response providers, we consider them equal if they're the same object
        if (this.contextAwareResponseProvider != null && other.contextAwareResponseProvider != null) {
            return this.contextAwareResponseProvider == other.contextAwareResponseProvider;
        }

        // For context-aware providers, we consider them equal if they're the same object
        if (this.contextAwareAssertionProvider != null && other.contextAwareAssertionProvider != null) {
            return this.contextAwareAssertionProvider == other.contextAwareAssertionProvider;
        }

        // For assertion providers, we consider them equal if they're the same object
        if (this.assertionProvider != null && other.assertionProvider != null) {
            return this.assertionProvider == other.assertionProvider;
        }

        // For static assertions, compare the assertion strings
        return Objects.equals(assertion(), other.assertion());
    }

    @Override
    public int hashCode() {
        // For context-aware response providers, use the provider's identity hash code
        if (contextAwareResponseProvider != null) {
            return System.identityHashCode(contextAwareResponseProvider);
        }

        // For context-aware providers, use the provider's identity hash code
        if (contextAwareAssertionProvider != null) {
            return System.identityHashCode(contextAwareAssertionProvider);
        }

        // For assertion providers, use the provider's identity hash code
        if (assertionProvider != null) {
            return System.identityHashCode(assertionProvider);
        }

        // For static assertions, hash the assertion string
        int result = 1;
        result = result * 59 + (this.assertion == null ? 43 : this.assertion.hashCode());
        return result;
    }
}