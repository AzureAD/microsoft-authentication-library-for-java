// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

final class ClientAssertion implements IClientAssertion {

    static final String ASSERTION_TYPE_JWT_BEARER = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private final String assertion;
    private final Callable<String> assertionProvider;
    private final Function<AssertionRequestOptions, String> contextAwareAssertionProvider;

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
    }

    /**
     * Gets the JWT assertion for client authentication.
     *
     * <p>Dispatch logic:
     * <ul>
     *   <li>Context-aware provider: delegates to {@link #assertion(AssertionRequestOptions)} with empty context</li>
     *   <li>Callable provider: invokes the callable to generate a fresh assertion</li>
     *   <li>Static string: returns the stored assertion directly</li>
     * </ul>
     *
     * @return A JWT assertion string
     * @throws MsalClientException if the assertion provider returns null/empty or throws an exception
     */
    public String assertion() {
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
     *
     * <p>Dispatch logic:
     * <ul>
     *   <li>Context-aware provider: invokes the Function with the provided options</li>
     *   <li>Callable or static: context is ignored, falls back to {@link #assertion()}</li>
     * </ul>
     *
     * <p>This method is the primary entry point used by {@code TokenRequestExecutor} when
     * building token requests, as it can pass FMI path and token endpoint context.
     *
     * @param options context information for the assertion request (may contain nulls for non-FMI flows)
     * @return A JWT assertion string
     * @throws MsalClientException if the assertion provider returns null/empty or throws an exception
     */
    String assertion(AssertionRequestOptions options) {
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
     * Returns true if this assertion uses a context-aware provider.
     */
    boolean isContextAware() {
        return contextAwareAssertionProvider != null;
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