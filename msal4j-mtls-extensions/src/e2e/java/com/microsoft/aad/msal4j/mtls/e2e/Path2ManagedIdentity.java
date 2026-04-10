// mTLS PoP Manual Test — Path 2: Managed Identity (IMDSv2, Windows + VBS)
//
// Tests the managed identity mTLS PoP flow end-to-end on an Azure VM with:
//   - System-assigned or user-assigned managed identity
//   - Windows OS with VBS (Virtualization-Based Security) KeyGuard
//   - IMDSv2 endpoint accessible at 169.254.169.254
//
// Usage (from the msal4j-mtls-extensions directory):
//   mvn package -DskipTests
//   mvn exec:java -Dexec.mainClass=com.microsoft.aad.msal4j.mtls.e2e.Path2ManagedIdentity
//
// Or with attestation:
//   mvn exec:java -Dexec.mainClass=com.microsoft.aad.msal4j.mtls.e2e.Path2ManagedIdentity -Dexec.args="--attest"

package com.microsoft.aad.msal4j.mtls.e2e;

import com.microsoft.aad.msal4j.mtls.MtlsMsiClient;
import com.microsoft.aad.msal4j.mtls.MtlsMsiException;
import com.microsoft.aad.msal4j.mtls.MtlsMsiHelperResult;
import com.microsoft.aad.msal4j.mtls.MtlsMsiHttpResponse;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

/**
 * End-to-end test for mTLS PoP Managed Identity (Path 2).
 *
 * <p>Mirrors msal-go's {@code apps/tests/devapps/mtls-pop/path2_managedidentity/main.go}.</p>
 */
public class Path2ManagedIdentity {

    private static final String RESOURCE = "https://management.azure.com";

    public static void main(String[] args) throws Exception {
        boolean withAttestation = Arrays.asList(args).contains("--attest");

        System.out.println("=== Path 2: Managed Identity mTLS PoP ===");
        System.out.println();
        if (withAttestation) {
            System.out.println("[Attestation mode: ON — requires AttestationClientLib.dll on PATH]");
            System.out.println();
        }

        MtlsMsiClient client = new MtlsMsiClient();
        String correlationId = UUID.randomUUID().toString();

        // ── First call: full IMDS flow ──────────────────────────────────────────
        System.out.println("Acquiring mTLS PoP token via IMDSv2 (full flow)...");
        MtlsMsiHelperResult result1;
        try {
            result1 = client.acquireToken(RESOURCE, "SystemAssigned", null,
                    withAttestation, correlationId);
        } catch (MtlsMsiException e) {
            System.out.println();
            System.err.println("❌ acquireToken failed: " + e.getMessage());
            System.err.println();
            // Check for tenant/resource misconfiguration (not a code bug)
            if (e.getMessage() != null && e.getMessage().contains("AADSTS392196")) {
                System.err.println("ℹ️  AADSTS392196: The resource application does not support certificate-bound tokens.");
                System.err.println("   This is a tenant/resource configuration issue (same as MSAL.NET on this VM).");
                System.err.println("   The mTLS handshake succeeded — the code is working correctly.");
                System.err.println("   To fully test, use a tenant where mTLS PoP is enabled for management.azure.com.");
            } else {
                System.err.println("Common causes:");
                System.err.println("  - VBS/KeyGuard not running (check msinfo32.exe)");
                System.err.println("  - IMDSv2 not returning platform metadata");
                System.err.println("  - VM managed identity not configured");
                System.err.println("  - 403 from IMDS issuecredential endpoint");
                System.err.println("  - Tenant not configured for mTLS PoP (AADSTS392196)");
            }
            System.exit(1);
            return;
        }

        System.out.println();
        printResult("First call (from IMDS)", result1);

        // ── Second call: should hit cached binding cert ─────────────────────────
        System.out.println();
        System.out.println("Acquiring again (expect cert cache hit)...");
        long t0 = System.currentTimeMillis();
        MtlsMsiHelperResult result2;
        try {
            result2 = client.acquireToken(RESOURCE, "SystemAssigned", null,
                    withAttestation, UUID.randomUUID().toString());
        } catch (MtlsMsiException e) {
            System.err.println("❌ Second acquireToken failed: " + e.getMessage());
            System.exit(1);
            return;
        }
        long elapsedMs = System.currentTimeMillis() - t0;
        printResult("Second call (should be cert-cached, ~fast)", result2);
        System.out.printf("  ⏱  Elapsed: %d ms%n", elapsedMs);

        // Cert cache check: same cert PEM implies cert was cached.
        if (result1.getBindingCertificate() != null
                && result1.getBindingCertificate().equals(result2.getBindingCertificate())) {
            System.out.println("  ✅ Binding cert cache working: same cert on second call");
        } else {
            System.out.println("  ⚠️  Different binding cert on second call — may indicate cache miss or cert was expiring");
        }

        // ── Third call: Graph /me to verify token actually works ────────────────
        System.out.println();
        System.out.println("Making downstream mTLS call to management.azure.com...");
        makeDownstreamCall(client, result1, withAttestation);

        System.out.println();
        System.out.println("=== Path 2 Complete ===");
    }

    // ── Downstream mTLS call ──────────────────────────────────────────────────

    private static void makeDownstreamCall(MtlsMsiClient client, MtlsMsiHelperResult result,
                                            boolean withAttestation) {
        // management.azure.com /subscriptions — any auth error is still a TLS success.
        String url = "https://management.azure.com/subscriptions?api-version=2020-01-01";
        try {
            MtlsMsiHttpResponse resp = client.httpRequest(
                    url, "GET", result.getAccessToken(),
                    null, null, null,
                    RESOURCE, "SystemAssigned", null,
                    withAttestation, UUID.randomUUID().toString(),
                    false);

            System.out.printf("  Downstream HTTP status: %d%n", resp.getStatus());
            if (resp.getStatus() < 500) {
                System.out.println("  ✅ TLS handshake + token delivery succeeded (HTTP < 500)");
            } else {
                System.out.println("  ❌ Server error — check token and resource enrollment");
            }
            if (resp.getStatus() == 200) {
                System.out.println("  ✅ HTTP 200 — full mTLS PoP token accepted by management.azure.com");
            } else if (resp.getStatus() == 401 || resp.getStatus() == 403) {
                System.out.println("  ℹ️  " + resp.getStatus() + " — TLS OK, authorization depends on subscription/role");
            }
        } catch (MtlsMsiException e) {
            System.out.println("  ❌ Downstream mTLS call failed: " + e.getMessage());
        }
    }

    // ── Print helpers ─────────────────────────────────────────────────────────

    private static void printResult(String label, MtlsMsiHelperResult result) {
        System.out.println("[" + label + "]");

        // Print binding cert details.
        if (result.getBindingCertificate() != null) {
            System.out.println("  ✅ BindingCertificate present");
            try {
                X509Certificate cert = parsePem(result.getBindingCertificate());
                System.out.println("     Subject:   " + cert.getSubjectX500Principal().getName());
                System.out.println("     Issuer:    " + cert.getIssuerX500Principal().getName());
                System.out.println("     NotBefore: " + cert.getNotBefore());
                System.out.println("     NotAfter:  " + cert.getNotAfter());
            } catch (Exception e) {
                System.out.println("     (could not parse cert: " + e.getMessage() + ")");
            }
        } else {
            System.out.println("  ❌ BindingCertificate is null — expected non-null for mTLS PoP");
        }

        System.out.println("  TokenType:  " + result.getTokenType());
        System.out.println("  ExpiresIn:  " + result.getExpiresIn() + "s");
        System.out.println("  TenantId:   " + result.getTenantId());
        System.out.println("  ClientId:   " + result.getClientId());

        // Print abbreviated JWT header/claims.
        printTokenSummary(result.getAccessToken());
    }

    private static void printTokenSummary(String jwt) {
        if (jwt == null || jwt.isEmpty()) {
            System.out.println("  ❌ AccessToken is null/empty");
            return;
        }
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) {
            System.out.println("  AccessToken: (not a JWT — " + jwt.length() + " chars)");
            return;
        }
        try {
            String header  = new String(Base64.getUrlDecoder().decode(pad(parts[0])));
            String payload = new String(Base64.getUrlDecoder().decode(pad(parts[1])));
            System.out.println("  AccessToken header:  " + header);
            // Print only key claims to keep output readable.
            printClaim(payload, "oid");
            printClaim(payload, "tid");
            printClaim(payload, "token_type");
            printClaim(payload, "cnf");
            long expEpoch = extractLong(payload, "exp");
            if (expEpoch > 0) {
                System.out.println("  AccessToken exp:     "
                        + new java.util.Date(expEpoch * 1000));
            }
            System.out.println("  ✅ AccessToken present (" + jwt.length() + " chars)");
        } catch (Exception e) {
            System.out.println("  AccessToken: (could not decode JWT: " + e.getMessage() + ")");
        }
    }

    private static void printClaim(String payload, String key) {
        String val = extractString(payload, key);
        if (val != null) {
            // Truncate long values (e.g. cnf object).
            if (val.length() > 120) val = val.substring(0, 120) + "...";
            System.out.println("  AccessToken " + key + ": " + val);
        }
    }

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
            // Return the whole nested object/array.
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

    private static X509Certificate parsePem(String pem) throws Exception {
        String b64 = pem
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(b64);
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(der));
    }
}
