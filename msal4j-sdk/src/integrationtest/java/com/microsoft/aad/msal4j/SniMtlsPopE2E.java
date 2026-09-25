// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SniMtlsPopE2E {
    static final int HTTP_CONNECT_TIMEOUT_MILLISECONDS = 30000;
    static final int HTTP_READ_TIMEOUT_MILLISECONDS = 30000;
    static final int TOKEN_ACQUISITION_TIMEOUT_SECONDS = 60;

    private static final String CLIENT_ID = "163ffef9-a313-45b4-ab2f-c7e2f5e0e23e";
    private static final String AUTHORITY =
            "https://login.microsoftonline.com/bea21ebe-8b64-4d06-9f6d-6a889b120a7c";
    private static final String CERTIFICATE_SIMPLE_NAME = "LabAuth.MSIDLab.com";
    private static final String GRAPH_SCOPE = "https://graph.microsoft.com/.default";
    private static final String GRAPH_MTLS_ENDPOINT =
            "https://mtlstb.graph.microsoft.com/v1.0/applications?$top=1";

    @Test
    void nonExportableVbsKeyAcquiresCachesAndCallsGraph() throws Exception {
        CertificateCredential credential = findLocalMachineCertificate();
        validateNonExportableVbsKey(credential);

        ConfidentialClientApplication application = application(credential);
        IAuthenticationResult first = acquire(application);

        assertFalse(first.accessToken().isEmpty());
        assertEquals("mtls_pop", first.tokenType());
        assertEquals(TokenSource.IDENTITY_PROVIDER, first.metadata().tokenSource());
        assertBinding(first, credential.leaf);

        GraphResponse graphResponse = callGraph(first, first.mtlsBindingContext());
        assertEquals(200, graphResponse.statusCode,
                "Graph mTLS request should succeed when the returned binding context is used.");

        IAuthenticationResult cached = acquire(application);
        assertEquals(TokenSource.CACHE, cached.metadata().tokenSource());
        assertEquals(first.accessToken(), cached.accessToken());
        assertBinding(cached, credential.leaf);
        assertEquals(first.mtlsBindingContext().keyId(),
                cached.mtlsBindingContext().keyId());
        assertArrayEquals(first.mtlsBindingContext().bindingCertificate().getEncoded(),
                cached.mtlsBindingContext().bindingCertificate().getEncoded());
    }

    @Test
    void missingBindingCertificateIsRejectedByGraph() throws Exception {
        CertificateCredential credential = findLocalMachineCertificate();
        validateNonExportableVbsKey(credential);
        IAuthenticationResult result = acquire(application(credential));

        GraphResponse graphResponse = callGraph(result, null);

        assertEquals(401, graphResponse.statusCode,
                "Graph should reject an mTLS PoP token when the binding certificate is not presented.");
        assertNotNull(graphResponse.wwwAuthenticate,
                "Graph should return a WWW-Authenticate challenge.");
        assertFalse(graphResponse.wwwAuthenticate.isEmpty(),
                "Graph should return a WWW-Authenticate challenge.");

        Map<String, Object> error = jsonObject(graphResponse.body, "error");
        assertEquals("InvalidAuthenticationToken", error.get("code"));
        assertEquals("MissingClientCertificate", error.get("message"));
    }

    private static ConfidentialClientApplication application(
            CertificateCredential credential) throws Exception {
        IClientCertificate certificate = ClientCredentialFactory
                .createFromCertificateChain(credential.privateKey, credential.chain);
        return ConfidentialClientApplication.builder(CLIENT_ID, certificate)
                .authority(AUTHORITY)
                .sendX5c(true)
                .connectTimeoutForDefaultHttpClient(
                        HTTP_CONNECT_TIMEOUT_MILLISECONDS)
                .readTimeoutForDefaultHttpClient(
                        HTTP_READ_TIMEOUT_MILLISECONDS)
                .build();
    }

    private static IAuthenticationResult acquire(
            ConfidentialClientApplication application) throws Exception {
        CompletableFuture<IAuthenticationResult> acquisition =
                application.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(GRAPH_SCOPE))
                .mtlsProofOfPossession()
                .build());
        return awaitAcquisition(
                acquisition,
                TOKEN_ACQUISITION_TIMEOUT_SECONDS,
                TimeUnit.SECONDS);
    }

    static IAuthenticationResult awaitAcquisition(
            CompletableFuture<IAuthenticationResult> acquisition,
            long timeout,
            TimeUnit unit) throws Exception {
        try {
            return acquisition.get(timeout, unit);
        } catch (TimeoutException e) {
            acquisition.cancel(true);
            throw new AssertionError(
                    "SNI mTLS token acquisition timed out after "
                            + timeout + " " + unit.toString().toLowerCase() + ".",
                    e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new AssertionError(
                    "SNI mTLS token acquisition failed: " + cause.getMessage(),
                    cause);
        }
    }

    private static void assertBinding(
            IAuthenticationResult result,
            X509Certificate expectedLeaf) throws Exception {
        assertNotNull(result.bindingCertificate(),
                "Binding certificate should be returned for mTLS PoP.");
        assertNotNull(result.mtlsBindingContext(),
                "Binding context should be returned for downstream mTLS.");
        assertArrayEquals(expectedLeaf.getEncoded(),
                result.bindingCertificate().certificateChain().get(0).getEncoded(),
                "Binding certificate must match the non-exportable SNI certificate.");
        assertArrayEquals(expectedLeaf.getEncoded(),
                result.mtlsBindingContext().bindingCertificate().getEncoded(),
                "Binding context must use the non-exportable SNI certificate.");

        Map<String, Object> cnf = SignedJWT.parse(result.accessToken())
                .getJWTClaimsSet()
                .getJSONObjectClaim("cnf");
        assertNotNull(cnf, "Access token should contain a cnf claim.");
        String tokenThumbprint = (String) cnf.get("x5t#S256");
        assertNotNull(tokenThumbprint, "cnf claim should contain x5t#S256.");
        String expectedThumbprint = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256")
                        .digest(expectedLeaf.getEncoded()));
        assertEquals(expectedThumbprint, tokenThumbprint,
                "Token x5t#S256 must match the supplied SNI certificate.");
    }

    private static CertificateCredential findLocalMachineCertificate()
            throws Exception {
        KeyStore store = KeyStore.getInstance(
                "Windows-MY-LOCALMACHINE", "SunMSCAPI");
        store.load(null, null);
        return selectCertificate(
                store, CERTIFICATE_SIMPLE_NAME, new Date());
    }

    static CertificateCredential selectCertificate(
            KeyStore store,
            String certificateSimpleName,
            Date now) throws Exception {
        CertificateCredential selected = null;
        List<Exception> privateKeyFailures = new ArrayList<>();

        Enumeration<String> aliases = store.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate certificate = store.getCertificate(alias);
            if (!(certificate instanceof X509Certificate)) {
                continue;
            }
            X509Certificate candidate = (X509Certificate) certificate;
            if (!certificateSimpleName.equalsIgnoreCase(simpleName(candidate))
                    || now.before(candidate.getNotBefore())
                    || now.after(candidate.getNotAfter())) {
                continue;
            }
            Key key;
            try {
                key = store.getKey(alias, null);
            } catch (Exception e) {
                privateKeyFailures.add(new IllegalStateException(
                        "Private key for certificate alias '" + alias
                                + "' could not be accessed.",
                        e));
                continue;
            }
            if (!(key instanceof PrivateKey)) {
                privateKeyFailures.add(new IllegalStateException(
                        "Certificate alias '" + alias
                                + "' did not expose an accessible private key."));
                continue;
            }
            List<X509Certificate> chain = certificateChain(store, alias, candidate);
            CertificateCredential current = new CertificateCredential(
                    (PrivateKey) key, chain);
            if (selected == null
                    || candidate.getNotBefore().after(selected.leaf.getNotBefore())) {
                selected = current;
            }
        }

        if (selected == null) {
            AssertionError failure = new AssertionError(
                    "A currently valid " + certificateSimpleName
                            + " certificate with an accessible private key must "
                            + "exist in LocalMachine\\My. Private-key access "
                            + "failed for " + privateKeyFailures.size()
                            + " matching alias(es).");
            for (Exception privateKeyFailure : privateKeyFailures) {
                failure.addSuppressed(privateKeyFailure);
            }
            throw failure;
        }
        return selected;
    }

    private static List<X509Certificate> certificateChain(
            KeyStore store,
            String alias,
            X509Certificate leaf) throws Exception {
        Certificate[] storedChain = store.getCertificateChain(alias);
        List<X509Certificate> chain = new ArrayList<>();
        if (storedChain != null) {
            for (Certificate certificate : storedChain) {
                chain.add((X509Certificate) certificate);
            }
        }
        if (chain.isEmpty()) {
            chain.add(leaf);
        }
        return chain;
    }

    static String simpleName(X509Certificate certificate)
            throws Exception {
        for (Rdn rdn : new LdapName(certificate.getSubjectX500Principal().getName())
                .getRdns()) {
            if ("CN".equalsIgnoreCase(rdn.getType())) {
                return String.valueOf(rdn.getValue());
            }
        }
        return "";
    }

    private static void validateNonExportableVbsKey(
            CertificateCredential credential) throws Exception {
        assertEquals("RSA", credential.privateKey.getAlgorithm());
        assertNull(credential.privateKey.getFormat(),
                "Provider-backed private key must not expose an encoding format.");
        assertNull(credential.privateKey.getEncoded(),
                "Provider-backed private key must be non-exportable.");

        byte[] challenge = "msal4j-sni-mtls-key-association"
                .getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(credential.privateKey);
        signer.update(challenge);
        byte[] signature = signer.sign();
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(credential.leaf.getPublicKey());
        verifier.update(challenge);
        assertTrue(verifier.verify(signature),
                "Provider-backed key must match the selected certificate leaf.");

        String thumbprint = hex(MessageDigest.getInstance("SHA-1")
                .digest(credential.leaf.getEncoded()));
        String script =
                "$ErrorActionPreference='Stop';"
                + "$c=Get-Item ('Cert:\\LocalMachine\\My\\'+"
                + "$env:MSAL_SNI_CERT_THUMBPRINT);"
                + "$r=[System.Security.Cryptography.X509Certificates."
                + "RSACertificateExtensions]::GetRSAPrivateKey($c);"
                + "try {"
                + "if(-not ($r -is [System.Security.Cryptography.RSACng]))"
                + "{throw 'Private key is not CNG-backed.'};"
                + "if($r.Key.ExportPolicy -ne "
                + "[System.Security.Cryptography.CngExportPolicies]::None)"
                + "{throw 'Private key is exportable.'};"
                + "$p=$r.Key.GetProperty('Virtual Iso',"
                + "[System.Security.Cryptography.CngPropertyOptions]::None);"
                + "if([BitConverter]::ToInt32($p.GetValue(),0) -eq 0)"
                + "{throw 'Private key is not VBS virtual-isolation protected.'}"
                + "} finally {if($null -ne $r){$r.Dispose()}}";
        ProcessBuilder processBuilder = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-NonInteractive",
                "-Command", script)
                .redirectErrorStream(true);
        processBuilder.environment().put(
                "MSAL_SNI_CERT_THUMBPRINT", thumbprint);
        Process process = processBuilder.start();
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            assertTrue(process.waitFor(10, TimeUnit.SECONDS),
                    "PowerShell VBS validation did not terminate after timeout.");
        }
        String output = readAll(process.getInputStream());
        assertTrue(completed,
                "Timed out while validating CNG VBS protection: " + output);
        assertEquals(0, process.exitValue(),
                "CNG VBS validation failed: " + output);
    }

    private static GraphResponse callGraph(
            IAuthenticationResult result,
            IMtlsBindingContext bindingContext) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection)
                new URL(GRAPH_MTLS_ENDPOINT).openConnection();
        if (bindingContext != null) {
            connection.setSSLSocketFactory(
                    bindingContext.sslContext().getSocketFactory());
        }
        connection.setRequestProperty(
                "Authorization", result.tokenType() + " " + result.accessToken());
        connection.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MILLISECONDS);
        connection.setReadTimeout(HTTP_READ_TIMEOUT_MILLISECONDS);
        try {
            int statusCode = connection.getResponseCode();
            InputStream stream = statusCode >= 400
                    ? connection.getErrorStream() : connection.getInputStream();
            String body = stream == null ? "" : readAll(stream);
            return new GraphResponse(statusCode, body,
                    connection.getHeaderField("WWW-Authenticate"));
        } finally {
            connection.disconnect();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> jsonObject(
            String json,
            String field) throws Exception {
        Object parsed = com.nimbusds.jose.util.JSONObjectUtils.parse(json).get(field);
        assertTrue(parsed instanceof Map,
                "Response JSON should contain an object named '" + field + "'.");
        return (Map<String, Object>) parsed;
    }

    private static String readAll(InputStream input) throws Exception {
        try (InputStream stream = input;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02X", value));
        }
        return result.toString();
    }

    static final class CertificateCredential {
        final PrivateKey privateKey;
        final List<X509Certificate> chain;
        final X509Certificate leaf;

        private CertificateCredential(
                PrivateKey privateKey,
                List<X509Certificate> chain) {
            this.privateKey = privateKey;
            this.chain = chain;
            this.leaf = chain.get(0);
        }
    }

    private static final class GraphResponse {
        private final int statusCode;
        private final String body;
        private final String wwwAuthenticate;

        private GraphResponse(
                int statusCode,
                String body,
                String wwwAuthenticate) {
            this.statusCode = statusCode;
            this.body = body;
            this.wwwAuthenticate = wwwAuthenticate;
        }
    }
}
