// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Factory for creating client credentials used in confidential client flows. For more details, see
 * https://aka.ms/msal4j-client-credentials
 */
public class ClientCredentialFactory {

    /**
     * Static method to create a {@link ClientSecret} instance from a client secret
     *
     * @param secret secret of application requesting a token
     * @return {@link ClientSecret}
     */
    public static IClientSecret createFromSecret(String secret) {
        return new ClientSecret(secret);
    }

    /**
     * Static method to create a {@link ClientCertificate} instance from a password-protected certificate.
     *
     * @param pkcs12Certificate InputStream containing PCKS12 formatted certificate
     * @param password          certificate password
     * @return {@link ClientCertificate}
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws NoSuchProviderException
     * @throws IOException
     */
    public static IClientCertificate createFromCertificate(final InputStream pkcs12Certificate, final String password)
            throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, NoSuchProviderException, IOException {
        return ClientCertificate.create(pkcs12Certificate, password);
    }

    /**
     * Static method to create a {@link ClientCertificate} instance from a private key/public certificate pair.
     *
     * @param key                  RSA private key to sign the assertion.
     * @param publicKeyCertificate x509 public certificate used for thumbprint
     * @return {@link ClientCertificate}
     */
    public static IClientCertificate createFromCertificate(final PrivateKey key, final X509Certificate publicKeyCertificate) {
        validateNotNull("publicKeyCertificate", publicKeyCertificate);

        return ClientCertificate.create(key, publicKeyCertificate);
    }

    /**
     * Static method to create a {@link ClientCertificate} instance from a certificate chain.
     *
     * @param key                       RSA private key to sign the assertion.
     * @param publicKeyCertificateChain ordered with the user's certificate first followed by zero or more certificate authorities
     * @return {@link ClientCertificate}
     */
    public static IClientCertificate createFromCertificateChain(PrivateKey key, List<X509Certificate> publicKeyCertificateChain) {
        if (key == null || publicKeyCertificateChain == null || publicKeyCertificateChain.isEmpty()) {
            throw new IllegalArgumentException("null or empty input parameter");
        }
        return new ClientCertificate(key, publicKeyCertificateChain);
    }

    /**
     * Static method to create a {@link ClientAssertion} instance from a JWT token encoded as a base64 URL encoded string.
     *
     * @param clientAssertion JWT token encoded as a base64 URL encoded string
     * @return {@link ClientAssertion}
     */
    public static IClientAssertion createFromClientAssertion(String clientAssertion) {
        return new ClientAssertion(clientAssertion);
    }

    /**
     * Static method to create a {@link ClientAssertion} instance from a provided Callable.
     * The callable will be invoked each time the assertion is needed, allowing for dynamic
     * generation of assertions.
     *
     * @param callable Callable that produces a JWT token encoded as a base64 URL encoded string
     * @return {@link ClientAssertion} that will invoke the callable each time assertion() is called
     * @throws NullPointerException if callable is null
     */
    public static IClientAssertion createFromCallback(Callable<String> callable) {
        if (callable == null) {
            throw new NullPointerException("callable");
        }

        return new ClientAssertion(callable);
    }

    /**
     * Static method to create a {@link ClientAssertion} instance from a provided Function that
     * receives {@link AssertionRequestOptions} context. The function will be invoked each time
     * the assertion is needed, allowing for dynamic generation of assertions based on the
     * request context (such as the FMI path in agent identity scenarios).
     *
     * @param assertionProvider Function that receives {@link AssertionRequestOptions} and produces
     *                          a JWT token encoded as a base64 URL encoded string
     * @return {@link ClientAssertion} that will invoke the function each time assertion() is called
     * @throws NullPointerException if assertionProvider is null
     */
    public static IClientAssertion createFromCallback(Function<AssertionRequestOptions, String> assertionProvider) {
        if (assertionProvider == null) {
            throw new NullPointerException("assertionProvider");
        }

        return new ClientAssertion(assertionProvider);
    }

    /**
     * Static method to create a {@link ClientAssertion} instance from a provided Function that
     * receives {@link AssertionRequestOptions} context and returns an {@link AssertionResponse}.
     * This overload allows the callback to supply both the assertion JWT and an optional
     * token-binding certificate for mTLS PoP scenarios.
     *
     * <p>When the returned {@link AssertionResponse} includes a
     * {@link AssertionResponse#tokenBindingCertificate()}, MSAL uses
     * {@code client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-pop}
     * instead of the default {@code jwt-bearer}.</p>
     *
     * @param assertionProvider Function that receives {@link AssertionRequestOptions} and produces
     *                          an {@link AssertionResponse} containing the assertion and optional certificate
     * @return {@link ClientAssertion} that will invoke the function each time assertion() is called
     * @throws NullPointerException if assertionProvider is null
     */
    public static IClientAssertion createFromAssertionResponseCallback(
            Function<AssertionRequestOptions, AssertionResponse> assertionProvider) {
        if (assertionProvider == null) {
            throw new NullPointerException("assertionProvider");
        }

        return new ClientAssertion(assertionProvider, true);
    }
}
