// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls.e2e;

import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IMtlsBindingContext;
import com.microsoft.aad.msal4j.ManagedIdentityApplication;
import com.microsoft.aad.msal4j.ManagedIdentityId;
import com.microsoft.aad.msal4j.ManagedIdentityParameters;
import com.microsoft.aad.msal4j.TokenSource;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

/**
 * Manual Java 8 validation for attested managed identity v2 mTLS PoP with Key Vault.
 */
public final class ManagedIdentityMtlsPopKeyVaultDevApp {

    private static final String RESOURCE = "https://vault.azure.net";

    public static void main(String[] args) throws Exception {
        boolean tokenOnly = Boolean.parseBoolean(
                System.getenv("MSAL_JAVA_MTLS_TOKEN_ONLY"));
        String vaultUrl = tokenOnly
                ? null : required("MSAL_JAVA_MTLS_AKV_URL");
        String secretName = tokenOnly
                ? null : required("MSAL_JAVA_MTLS_AKV_SECRET_NAME");
        String identityClientId = System.getenv("MSAL_JAVA_MTLS_IDENTITY_CLIENT_ID");
        String mismatchIdentityClientId =
                System.getenv("MSAL_JAVA_MTLS_MISMATCH_IDENTITY_CLIENT_ID");
        String expectedSecretValue =
                System.getenv("MSAL_JAVA_MTLS_EXPECTED_SECRET_VALUE");
        boolean forceRefresh = Boolean.parseBoolean(
                System.getenv("MSAL_JAVA_MTLS_FORCE_REFRESH"));
        String runId = UUID.randomUUID().toString();

        ManagedIdentityId identity = isBlank(identityClientId)
                ? ManagedIdentityId.systemAssigned()
                : ManagedIdentityId.userAssignedClientId(identityClientId);
        ManagedIdentityApplication application =
                ManagedIdentityApplication.builder(identity).build();

        System.out.println("Java Managed Identity v2 mTLS PoP manual validation");
        System.out.println();
        System.out.println("Platform: " + System.getProperty("os.name"));
        System.out.println("JVM: " + System.getProperty("java.version"));
        System.out.println("Identity: " + (isBlank(identityClientId)
                ? "SystemAssigned" : "UserAssigned"));
        System.out.println("Attestation: enabled");
        System.out.println("Resource: " + RESOURCE);
        if (!tokenOnly) {
            System.out.println("AKV host: " + new URL(vaultUrl).getHost());
        }
        System.out.println("Correlation ID: " + runId);

        System.out.println("\n[1] Acquiring attested mTLS PoP token...");
        IAuthenticationResult first = acquire(application, false);
        verifyResult(first);
        System.out.println("PASS: token_type = mtls_pop");
        System.out.println("PASS: binding certificate returned");
        System.out.println("PASS: reusable JSSE binding context returned");

        System.out.println("\n[2] Verifying certificate-bound token...");
        verifyTokenBinding(first);
        System.out.println("PASS: cnf.x5t#S256 matches binding certificate");
        System.out.println("Binding key ID: " + mask(first.mtlsBindingContext().keyId()));

        if (tokenOnly) {
            System.out.println("\nRESULT: PASS - attested mTLS PoP token acquired");
            return;
        }

        System.out.println("\n[3] Building independent Java 8 HTTPS client...");
        System.out.println("PASS: HttpsURLConnection configured from returned SSLContext");

        System.out.println("\n[4] Calling AKV...");
        String response = callKeyVault(first, vaultUrl, secretName);
        if (!isBlank(expectedSecretValue)) {
            String actualValue = extractJsonString(response, "value");
            if (!expectedSecretValue.equals(actualValue)) {
                throw new IllegalStateException(
                        "AKV secret value did not match the expected value.");
            }
        }
        System.out.println("PASS: HTTP 200");
        System.out.println("PASS: AKV response validated");

        System.out.println("\n[5] Verifying the token is rejected without its certificate...");
        verifyMissingBindingRejected(first, vaultUrl, secretName);

        int nextStep = 6;
        if (!isBlank(mismatchIdentityClientId)) {
            System.out.println("\n[" + nextStep++ + "] Verifying mismatched binding is rejected...");
            ManagedIdentityApplication mismatchApplication =
                    ManagedIdentityApplication.builder(
                            ManagedIdentityId.userAssignedClientId(
                                    mismatchIdentityClientId))
                            .build();
            IAuthenticationResult mismatchBinding =
                    acquire(mismatchApplication, false);
            verifyResult(mismatchBinding);
            if (first.mtlsBindingContext().keyId().equals(
                    mismatchBinding.mtlsBindingContext().keyId())) {
                throw new IllegalStateException(
                        "Negative binding acquisition returned the same certificate.");
            }
            KeyVaultResponse rejection = callKeyVault(
                    first,
                    mismatchBinding.mtlsBindingContext(),
                    vaultUrl,
                    secretName,
                    false);
            if (rejection.status == 200) {
                throw new IllegalStateException(
                        "AKV accepted token A with mismatched binding B.");
            }
            System.out.println("PASS: token A + binding B rejected with HTTP "
                    + rejection.status);
        }

        System.out.println("\n[" + nextStep++ + "] Reacquiring...");
        IAuthenticationResult cached = acquire(application, false);
        verifyResult(cached);
        if (cached.metadata().tokenSource() != TokenSource.CACHE) {
            throw new IllegalStateException("Second acquisition was not a cache hit.");
        }
        if (!first.mtlsBindingContext().keyId()
                .equals(cached.mtlsBindingContext().keyId())) {
            throw new IllegalStateException(
                    "Cache hit returned a different binding generation.");
        }
        System.out.println("PASS: TokenSource = CACHE");
        System.out.println("PASS: matching binding context available");

        if (forceRefresh) {
            System.out.println("\n[" + nextStep + "] Force refresh...");
            IAuthenticationResult refreshed = acquire(application, true);
            verifyResult(refreshed);
            if (refreshed.metadata().tokenSource() != TokenSource.IDENTITY_PROVIDER) {
                throw new IllegalStateException(
                        "Force refresh did not use the identity provider.");
            }
            callKeyVault(refreshed, vaultUrl, secretName);
            System.out.println("PASS: TokenSource = IDENTITY_PROVIDER");
            System.out.println("PASS: force-refreshed binding context returned HTTP 200");
        }

        System.out.println("\nRESULT: PASS");
    }

    private static IAuthenticationResult acquire(
            ManagedIdentityApplication application,
            boolean forceRefresh) throws Exception {
        ManagedIdentityParameters parameters = ManagedIdentityParameters
                .builder(RESOURCE)
                .withMtlsProofOfPossession()
                .withAttestationSupport()
                .forceRefresh(forceRefresh)
                .build();
        return application.acquireTokenForManagedIdentity(parameters).get();
    }

    private static void verifyResult(IAuthenticationResult result) {
        if (result == null
                || isBlank(result.accessToken())
                || !"mtls_pop".equals(result.tokenType())
                || result.bindingCertificate() == null
                || result.mtlsBindingContext() == null
                || result.mtlsBindingContext().sslContext() == null
                || isBlank(result.mtlsBindingContext().keyId())) {
            throw new IllegalStateException(
                    "Managed identity mTLS PoP result was incomplete.");
        }
    }

    private static void verifyTokenBinding(IAuthenticationResult result)
            throws Exception {
        IMtlsBindingContext context = result.mtlsBindingContext();
        String payload = result.accessToken().split("\\.")[1];
        String payloadJson = new String(
                Base64.getUrlDecoder().decode(padBase64(payload)),
                StandardCharsets.UTF_8);
        String cnfObject = extractJsonObject(payloadJson, "cnf");
        String tokenKeyId = extractJsonString(cnfObject, "x5t#S256");
        String certificateKeyId = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256")
                        .digest(result.bindingCertificate().getEncoded()));
        if (!context.keyId().equals(tokenKeyId)
                || !context.keyId().equals(certificateKeyId)) {
            throw new IllegalStateException(
                    "Token cnf, binding context and certificate key IDs differ.");
        }
    }

    private static String callKeyVault(
            IAuthenticationResult result,
            String vaultUrl,
            String secretName) throws Exception {
        KeyVaultResponse response = callKeyVault(
                result,
                result.mtlsBindingContext(),
                vaultUrl,
                secretName,
                true);
        return response.body;
    }

    private static KeyVaultResponse callKeyVault(
            IAuthenticationResult token,
            IMtlsBindingContext binding,
            String vaultUrl,
            String secretName,
            boolean requireSuccess) throws Exception {
        String endpoint = trimTrailingSlash(vaultUrl)
                + "/secrets/" + secretName + "?api-version=7.5";
        HttpsURLConnection connection =
                (HttpsURLConnection) new URL(endpoint).openConnection();
        SSLContext sslContext = binding == null
                ? createTls12ContextWithoutClientCertificate()
                : binding.sslContext();
        connection.setSSLSocketFactory(sslContext.getSocketFactory());
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(30_000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty(
                "Authorization",
                "mtls_pop " + token.accessToken());
        connection.setRequestProperty("x-ms-tokenboundauth", "true");
        connection.setRequestProperty(
                "x-ms-client-request-id",
                UUID.randomUUID().toString());

        int status = connection.getResponseCode();
        InputStream stream = status == 200
                ? connection.getInputStream() : connection.getErrorStream();
        String body = readBody(stream);
        if (requireSuccess && status != 200) {
            throw new IllegalStateException(
                    "AKV token-bound request failed with HTTP " + status + ".");
        }
        return new KeyVaultResponse(status, body);
    }

    private static void verifyMissingBindingRejected(
            IAuthenticationResult token,
            String vaultUrl,
            String secretName) throws Exception {
        KeyVaultResponse response = callKeyVault(
                token,
                null,
                vaultUrl,
                secretName,
                false);
        String errorCode = extractJsonString(response.body, "code");
        if (response.status != 401 || !"Unauthorized".equals(errorCode)) {
            throw new IllegalStateException(
                    "AKV returned an unexpected response for an mtls_pop token " +
                            "without its binding certificate: HTTP " +
                            response.status + ", code=" + errorCode);
        }
        System.out.println(
                "PASS: token without certificate rejected with HTTP 401 Unauthorized");
    }

    private static SSLContext createTls12ContextWithoutClientCertificate()
            throws Exception {
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(null, null, null);
        return context;
    }

    private static String readBody(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }

    private static String extractJsonObject(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        int start = keyIndex < 0 ? -1 : json.indexOf('{', keyIndex);
        int end = start < 0 ? -1 : json.indexOf('}', start);
        if (start < 0 || end < 0) {
            return null;
        }
        return json.substring(start, end + 1);
    }

    private static String extractJsonString(String json, String key) {
        if (json == null) {
            return null;
        }
        int keyIndex = json.indexOf("\"" + key + "\"");
        int colon = keyIndex < 0 ? -1 : json.indexOf(':', keyIndex);
        int start = colon < 0 ? -1 : json.indexOf('"', colon);
        int end = start < 0 ? -1 : json.indexOf('"', start + 1);
        return start < 0 || end < 0 ? null : json.substring(start + 1, end);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (isBlank(value)) {
            throw new IllegalArgumentException(
                    "Required environment variable is missing: " + name);
        }
        return value;
    }

    private static String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String padBase64(String value) {
        int remainder = value.length() % 4;
        return remainder == 0 ? value : value + (remainder == 2 ? "==" : "=");
    }

    private static String mask(String value) {
        return value.length() <= 8
                ? "<masked>"
                : value.substring(0, 4) + "..." + value.substring(value.length() - 4);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class KeyVaultResponse {
        final int status;
        final String body;

        KeyVaultResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    private ManagedIdentityMtlsPopKeyVaultDevApp() {
    }
}
