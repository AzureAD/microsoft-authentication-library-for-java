// mTLS PoP Manual Test — Path 1: Confidential Client (SNI Certificate)
//
// Tests both the happy path (requires Azure AD app registration + cert upload)
// and all error cases (no Azure credentials required).
//
// Usage (from the msal4j-mtls-extensions directory):
//   mvn package -DskipTests
//
//   # Error cases only (no Azure credentials needed):
//   java -jar target/msal4j-mtls-extensions-1.0.0-e2e.jar path1 --errors-only
//
//   # Full test (requires Azure app registration):
//   java -jar target/msal4j-mtls-extensions-1.0.0-e2e.jar path1 \
//       --tenant <tenantID> --client <clientID> --region <region>
//
// Cert files (test-cert.pem, test-key.pem) must be in the parent directory (mtls-pop/).
// test-cert.pem is committed to the repo; test-key.pem is gitignored.
//
// Generate cert + key:
//   openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out ../test-key.pem
//   openssl req -new -x509 -key ../test-key.pem -out ../test-cert.pem \
//       -days 365 -subj "/CN=msal-java-mtls-test"
//
// Then upload test-cert.pem to your Azure AD app registration under
// "Certificates & secrets" > "Certificates".

package com.microsoft.aad.msal4j.mtls.e2e;

import com.microsoft.aad.msal4j.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import javax.net.ssl.SSLContext;

/**
 * End-to-end test for mTLS PoP Confidential Client (Path 1).
 *
 * <p>Mirrors msal-go's
 * {@code apps/tests/devapps/mtls-pop/path1_confidential/main.go}.</p>
 */
public class Path1ConfidentialClient {

    static void run(String[] args) throws Exception {
        String tenantId   = argValue(args, "--tenant",   null);
        String clientId   = argValue(args, "--client",   null);
        String region     = argValue(args, "--region",   "centraluseuap");
        String resource   = argValue(args, "--resource", "https://graph.microsoft.com");
        boolean errorsOnly = Arrays.asList(args).contains("--errors-only");

        // Load cert + key from PEM files (parent directory, same layout as msal-go).
        X509Certificate cert = loadCert();
        PrivateKey      key  = loadKey();

        IClientCertificate certCred = ClientCredentialFactory.createFromCertificate(key, cert);

        System.out.println("=== Path 1: Error-Case Validation ===");
        System.out.println();
        int[] counts = testErrorCases(certCred, tenantId, region);
        System.out.printf("%n  Error cases: %d passed, %d failed%n", counts[0], counts[1]);

        if (errorsOnly) {
            System.out.println();
            System.out.println("[Skipping happy-path test: --errors-only flag set]");
            System.out.println("To run the happy path, register an Azure AD app and upload the certificate at");
            System.out.println("  ../test-cert.pem");
            System.out.println("then run:");
            System.out.printf("  java -jar <e2e.jar> path1 --tenant <tenantID> --client <clientID> --region %s%n", region);
            return;
        }

        if (tenantId == null || clientId == null) {
            System.out.println();
            System.out.println("[Skipping happy-path test: --tenant and --client flags required]");
            System.out.println("Run with --errors-only to test only error cases, or provide --tenant/--client for the full test.");
            System.exit(1);
            return;
        }

        System.out.println();
        System.out.println("=== Path 1: Happy Path ===");
        System.out.println();
        testHappyPath(certCred, cert, key, tenantId, clientId, region, resource);
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    private static int[] testErrorCases(IClientCertificate certCred,
                                        String tenantId, String region) {
        String errorTenant = tenantId != null ? tenantId
                : System.getenv("AZURE_TENANT_ID") != null ? System.getenv("AZURE_TENANT_ID")
                : "00000000-0000-0000-0000-000000000000";
        String authority = "https://login.microsoftonline.com/" + errorTenant;
        String placeholderId = "00000000-0000-0000-0000-000000000000";
        String scope = "https://graph.microsoft.com/.default";

        int pass = 0, fail = 0;

        // Error case 1: missing region
        try {
            ConfidentialClientApplication app = ConfidentialClientApplication
                    .builder(placeholderId, certCred)
                    .authority(authority)
                    .build();
            app.acquireToken(ClientCredentialParameters
                    .builder(Collections.singleton(scope))
                    .withMtlsProofOfPossession()
                    .build()).get();
            System.out.println("  ❌ FAIL [missing-region]: expected error, got success");
            fail++;
        } catch (Exception e) {
            String msg = rootCause(e).getMessage();
            if (msg != null && msg.contains("Azure region")) {
                System.out.println("  ✅ PASS [missing-region]: " + msg);
                pass++;
            } else {
                System.out.println("  ❌ FAIL [missing-region]: unexpected error: " + msg);
                fail++;
            }
        }

        // Error case 2: non-tenanted authority (/common)
        try {
            ConfidentialClientApplication app = ConfidentialClientApplication
                    .builder(placeholderId, certCred)
                    .authority("https://login.microsoftonline.com/common")
                    .azureRegion(region)
                    .build();
            app.acquireToken(ClientCredentialParameters
                    .builder(Collections.singleton(scope))
                    .withMtlsProofOfPossession()
                    .build()).get();
            System.out.println("  ❌ FAIL [non-tenanted(/common)]: expected error, got success");
            fail++;
        } catch (Exception e) {
            String msg = rootCause(e).getMessage();
            if (msg != null && (msg.contains("/common") || msg.contains("/organizations") || msg.contains("tenanted"))) {
                System.out.println("  ✅ PASS [non-tenanted(/common)]: " + msg);
                pass++;
            } else {
                System.out.println("  ❌ FAIL [non-tenanted(/common)]: unexpected error: " + msg);
                fail++;
            }
        }

        // Error case 3: non-tenanted authority (/organizations)
        try {
            ConfidentialClientApplication app = ConfidentialClientApplication
                    .builder(placeholderId, certCred)
                    .authority("https://login.microsoftonline.com/organizations")
                    .azureRegion(region)
                    .build();
            app.acquireToken(ClientCredentialParameters
                    .builder(Collections.singleton(scope))
                    .withMtlsProofOfPossession()
                    .build()).get();
            System.out.println("  ❌ FAIL [non-tenanted(/organizations)]: expected error, got success");
            fail++;
        } catch (Exception e) {
            String msg = rootCause(e).getMessage();
            if (msg != null && (msg.contains("/organizations") || msg.contains("tenanted"))) {
                System.out.println("  ✅ PASS [non-tenanted(/organizations)]: " + msg);
                pass++;
            } else {
                System.out.println("  ❌ FAIL [non-tenanted(/organizations)]: unexpected error: " + msg);
                fail++;
            }
        }

        // Error case 4: secret credential (not cert-based)
        try {
            IClientSecret secretCred = ClientCredentialFactory.createFromSecret("dummy-secret");
            ConfidentialClientApplication app = ConfidentialClientApplication
                    .builder(placeholderId, secretCred)
                    .authority(authority)
                    .azureRegion(region)
                    .build();
            app.acquireToken(ClientCredentialParameters
                    .builder(Collections.singleton(scope))
                    .withMtlsProofOfPossession()
                    .build()).get();
            System.out.println("  ❌ FAIL [secret-credential]: expected error, got success");
            fail++;
        } catch (Exception e) {
            String msg = rootCause(e).getMessage();
            if (msg != null && msg.contains("ClientCertificate")) {
                System.out.println("  ✅ PASS [secret-credential]: " + msg);
                pass++;
            } else {
                System.out.println("  ❌ FAIL [secret-credential]: unexpected error: " + msg);
                fail++;
            }
        }

        return new int[]{pass, fail};
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    private static void testHappyPath(IClientCertificate certCred,
                                      X509Certificate cert, PrivateKey key,
                                      String tenantId, String clientId,
                                      String region, String resource) throws Exception {
        String authority = "https://login.microsoftonline.com/" + tenantId;
        String scope     = resource.replaceAll("/$", "") + "/.default";

        ConfidentialClientApplication app = ConfidentialClientApplication
                .builder(clientId, certCred)
                .authority(authority)
                .azureRegion(region)
                .build();

        System.out.printf("  Acquiring mTLS PoP token (region=%s, scope=%s)...%n", region, scope);
        IAuthenticationResult result1 = app.acquireToken(
                ClientCredentialParameters.builder(Collections.singleton(scope))
                        .withMtlsProofOfPossession()
                        .build()).get();

        System.out.println();
        printResult("First call (from AAD)", result1);

        // Second call — should hit cache
        System.out.println();
        System.out.println("  Acquiring again (expect cache hit)...");
        IAuthenticationResult result2 = app.acquireToken(
                ClientCredentialParameters.builder(Collections.singleton(scope))
                        .withMtlsProofOfPossession()
                        .build()).get();

        if (result2.metadata().tokenSource() == TokenSource.CACHE) {
            System.out.println("  ✅ Second call returned cached token");
        } else {
            System.out.println("  ⚠️  Second call did NOT return cached token (source: "
                    + result2.metadata().tokenSource() + ")");
        }
        if (result2.accessToken() != null && result2.accessToken().equals(result1.accessToken())) {
            System.out.println("  ✅ Same access token returned from cache");
        }

        // Downstream call — present the binding cert over mTLS
        System.out.println();
        System.out.printf("  Making downstream call to %s...%n", resource);
        makeDownstreamCall(result1.accessToken(), cert, key, resource);

        System.out.println();
        System.out.println("  Happy path complete ✅");
    }

    // ── Downstream mTLS call ──────────────────────────────────────────────────

    private static void makeDownstreamCall(String token,
                                           X509Certificate cert, PrivateKey key,
                                           String resource) {
        // For Graph, append /v1.0/organization; any 4xx is still a TLS success.
        String url = resource.replaceAll("/$", "");
        if (url.contains("graph.microsoft.com")) {
            url += "/v1.0/organization";
        }

        try {
            // Build an SSLSocketFactory that presents our binding cert as the client cert.
            javax.net.ssl.KeyManagerFactory kmf = javax.net.ssl.KeyManagerFactory.getInstance(
                    javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
            java.security.KeyStore ks = java.security.KeyStore.getInstance("PKCS12");
            ks.load(null, null);
            ks.setKeyEntry("mtls", key, new char[0], new X509Certificate[]{cert});
            kmf.init(ks, new char[0]);

            SSLContext sslCtx = SSLContext.getInstance("TLS");
            sslCtx.init(kmf.getKeyManagers(), null, null);

            URL reqUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) reqUrl.openConnection();
            if (conn instanceof javax.net.ssl.HttpsURLConnection) {
                ((javax.net.ssl.HttpsURLConnection) conn).setSSLSocketFactory(sslCtx.getSocketFactory());
            }
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.connect();

            int status = conn.getResponseCode();
            switch (status) {
                case 200:
                    System.out.printf("  ✅ Downstream call succeeded: HTTP %d%n", status);
                    break;
                case 401:
                    System.out.println("  ❌ HTTP 401 — token or mTLS cert rejected");
                    break;
                case 403:
                    System.out.println("  ⚠️  HTTP 403 — TLS handshake OK, token accepted, missing permissions");
                    break;
                default:
                    System.out.printf("  ⚠️  HTTP %d%n", status);
            }
        } catch (Exception e) {
            System.out.println("  ❌ Downstream call failed: " + e.getMessage());
        }
    }

    // ── PEM loading ───────────────────────────────────────────────────────────

    /**
     * Loads the test X.509 certificate.  First tries PEM files in the standard locations
     * (parent directory or current directory, same layout as msal-go); if those aren't
     * present falls back to the bundled {@code mtls-test-cert.p12} so that error-case
     * tests work without any manual setup.
     */
    private static X509Certificate loadCert() throws Exception {
        if (pemFileExists("test-cert.pem")) {
            byte[] pem = readPemFile("test-cert.pem");
            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(pem));
        }
        return loadCertFromBundledP12();
    }

    /**
     * Loads the test RSA private key.  Same fallback logic as {@link #loadCert()}.
     */
    private static PrivateKey loadKey() throws Exception {
        if (pemFileExists("test-key.pem")) {
            String raw = readPemFileString("test-key.pem");
            boolean isPkcs8 = raw.contains("BEGIN PRIVATE KEY");
            String b64 = raw
                    .replaceAll("-----[^-]+-----", "")
                    .replaceAll("\\s", "");
            byte[] derBytes = Base64.getDecoder().decode(b64);
            if (!isPkcs8) {
                derBytes = wrapPkcs1InPkcs8(derBytes);
            }
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(derBytes));
        }
        return loadKeyFromBundledP12();
    }

    private static boolean pemFileExists(String name) {
        return Files.exists(Paths.get("../" + name)) || Files.exists(Paths.get(name));
    }

    /** Loads the X.509 certificate from the bundled test PKCS#12 store. */
    private static X509Certificate loadCertFromBundledP12() throws Exception {
        KeyStore ks = loadBundledKeyStore();
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (ks.isCertificateEntry(alias) || ks.isKeyEntry(alias)) {
                return (X509Certificate) ks.getCertificate(alias);
            }
        }
        throw new IOException("No certificate found in bundled mtls-test-cert.p12");
    }

    /** Loads the private key from the bundled test PKCS#12 store. */
    private static PrivateKey loadKeyFromBundledP12() throws Exception {
        KeyStore ks = loadBundledKeyStore();
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (ks.isKeyEntry(alias)) {
                return (PrivateKey) ks.getKey(alias, "changeit".toCharArray());
            }
        }
        throw new IOException("No private key found in bundled mtls-test-cert.p12");
    }

    private static KeyStore loadBundledKeyStore() throws Exception {
        InputStream is = Path1ConfidentialClient.class.getResourceAsStream("/mtls-test-cert.p12");
        if (is == null) {
            throw new IOException(
                    "Cannot find test-cert.pem or the bundled mtls-test-cert.p12.\n"
                    + "Generate cert + key with:\n"
                    + "  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out ../test-key.pem\n"
                    + "  openssl req -new -x509 -key ../test-key.pem -out ../test-cert.pem"
                    + " -days 365 -subj \"/CN=msal-java-mtls-test\"");
        }
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(is, "changeit".toCharArray());
        return ks;
    }

    private static byte[] readPemFile(String name) throws IOException {
        return readPemFileString(name).getBytes();
    }

    private static String readPemFileString(String name) throws IOException {
        // Look in ../  (same as msal-go: cert files live in mtls-pop/, one level up)
        String[] candidates = {"../" + name, name};
        for (String path : candidates) {
            if (Files.exists(Paths.get(path))) {
                return new String(Files.readAllBytes(Paths.get(path)));
            }
        }
        throw new IOException("Cannot find " + name + " (tried ../" + name + " and " + name
                + ").\nGenerate with:\n"
                + "  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out ../test-key.pem\n"
                + "  openssl req -new -x509 -key ../test-key.pem -out ../test-cert.pem -days 365 -subj \"/CN=msal-java-mtls-test\"");
    }

    /**
     * Wraps a PKCS#1 RSA private key DER blob in a PKCS#8 PrivateKeyInfo envelope so that
     * Java's {@link KeyFactory} can parse it without Bouncy Castle.
     */
    private static byte[] wrapPkcs1InPkcs8(byte[] pkcs1) {
        // rsaEncryption OID: 1.2.840.113549.1.1.1
        byte[] oidBytes = {0x2a, (byte)0x86, 0x48, (byte)0x86, (byte)0xf7, 0x0d, 0x01, 0x01, 0x01};
        byte[] algId    = derSeq(concat(derTlv(0x06, oidBytes), new byte[]{0x05, 0x00}));
        byte[] version  = {0x02, 0x01, 0x00};
        byte[] privKey  = derTlv(0x04, pkcs1);
        return derSeq(concat(version, algId, privKey));
    }

    private static byte[] derSeq(byte[] content) {
        return derTlv(0x30, content);
    }

    private static byte[] derTlv(int tag, byte[] value) {
        byte[] len = derLen(value.length);
        byte[] out = new byte[1 + len.length + value.length];
        out[0] = (byte) tag;
        System.arraycopy(len, 0, out, 1, len.length);
        System.arraycopy(value, 0, out, 1 + len.length, value.length);
        return out;
    }

    private static byte[] derLen(int n) {
        if (n < 128)   return new byte[]{(byte) n};
        if (n < 256)   return new byte[]{(byte) 0x81, (byte) n};
        return new byte[]{(byte) 0x82, (byte)(n >> 8), (byte)(n & 0xff)};
    }

    private static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) total += p.length;
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] p : parts) { System.arraycopy(p, 0, out, pos, p.length); pos += p.length; }
        return out;
    }

    // ── Print helpers ─────────────────────────────────────────────────────────

    private static void printResult(String label, IAuthenticationResult result) {
        System.out.println("[" + label + "]");

        X509Certificate binding = result.bindingCertificate();
        if (binding != null) {
            System.out.println("  ✅ BindingCertificate: subject=" + binding.getSubjectX500Principal().getName()
                    + ", expires=" + binding.getNotAfter());
        } else {
            System.out.println("  ❌ BindingCertificate is null — expected non-null for mTLS PoP");
        }

        System.out.println("  TokenType:  " + result.tokenType());
        System.out.println("  Scopes:     " + result.scopes());
        System.out.println("  ExpiresOn:  " + result.expiresOnDate());

        printTokenSummary(result.accessToken());
    }

    private static void printTokenSummary(String jwt) {
        if (jwt == null || jwt.isEmpty()) {
            System.out.println("  ❌ AccessToken is null/empty");
            return;
        }
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) {
            System.out.printf("  AccessToken: (opaque, %d chars)%n", jwt.length());
            return;
        }
        try {
            String header  = new String(Base64.getUrlDecoder().decode(pad(parts[0])));
            String payload = new String(Base64.getUrlDecoder().decode(pad(parts[1])));
            System.out.println("  AccessToken header:  " + header);
            printClaim(payload, "oid");
            printClaim(payload, "tid");
            printClaim(payload, "token_type");
            printClaim(payload, "cnf");
            long expEpoch = extractLong(payload, "exp");
            if (expEpoch > 0) {
                System.out.println("  AccessToken exp:     " + new Date(expEpoch * 1000));
            }
            System.out.printf("  ✅ AccessToken present (%d chars)%n", jwt.length());
        } catch (Exception e) {
            System.out.println("  AccessToken: (could not decode JWT: " + e.getMessage() + ")");
        }
    }

    private static void printClaim(String payload, String key) {
        String val = extractString(payload, key);
        if (val != null) {
            if (val.length() > 120) val = val.substring(0, 120) + "...";
            System.out.println("  AccessToken " + key + ": " + val);
        }
    }

    // ── Minimal JSON extract (no external deps) ───────────────────────────────

    private static String extractString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int vs = colon + 1;
        while (vs < json.length() && Character.isWhitespace(json.charAt(vs))) vs++;
        if (vs >= json.length()) return null;
        char first = json.charAt(vs);
        if (first == '"') {
            int end = vs + 1;
            while (end < json.length() && json.charAt(end) != '"') end++;
            return json.substring(vs + 1, end);
        } else if (first == '{' || first == '[') {
            char close = first == '{' ? '}' : ']';
            int depth = 0, end = vs;
            while (end < json.length()) {
                char c = json.charAt(end);
                if (c == first) depth++;
                else if (c == close) { if (--depth == 0) { end++; break; } }
                end++;
            }
            return json.substring(vs, end);
        }
        return null;
    }

    private static long extractLong(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return 0;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return 0;
        int vs = colon + 1;
        while (vs < json.length() && Character.isWhitespace(json.charAt(vs))) vs++;
        int ve = vs;
        while (ve < json.length() && (Character.isDigit(json.charAt(ve)) || json.charAt(ve) == '-')) ve++;
        try { return Long.parseLong(json.substring(vs, ve)); } catch (Exception e) { return 0; }
    }

    private static String pad(String s) {
        return s + "==".substring(0, (4 - s.length() % 4) % 4);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static String argValue(String[] args, String flag, String defaultVal) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) return args[i + 1];
        }
        return defaultVal;
    }

    private static Throwable rootCause(Throwable t) {
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return t;
    }
}
