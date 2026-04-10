# mTLS PoP Manual Testing Guide

This guide walks through manual verification of both mTLS PoP paths in MSAL4J.

---

## Prerequisites

- Java 8+ and Maven installed
- For SNI path: a valid test certificate (PKCS12)
- For Managed Identity path:
  - An Azure VM with managed identity enabled
  - Windows x64 OS with VBS (Virtualization-Based Security) KeyGuard
  - `msal4j-mtls-extensions` on classpath (add dependency)
  - On Trusted Launch VMs: `AttestationClientLib.dll` on `PATH`
- An AAD tenant with a registered app (client credentials configured)

---

## Path 1: SNI / ConfidentialClientApplication

### 1. Generate a test certificate

If you don't have a certificate, generate one with keytool:

```bash
keytool -genkeypair \
  -alias mtls-test \
  -keyalg RSA \
  -keysize 2048 \
  -validity 365 \
  -storetype PKCS12 \
  -keystore test-cert.p12 \
  -storepass changeit \
  -dname "CN=MyApp, O=MyOrg, C=US"
```

Export the public certificate for registration with Azure AD:

```bash
keytool -exportcert -alias mtls-test -keystore test-cert.p12 -storepass changeit -rfc -file test-cert.pem
```

Upload `test-cert.pem` to your app registration → **Certificates & secrets → Certificates**.

### 2. Verify the mTLS endpoint is reachable

```bash
curl -v --cert test-cert.pem --key test-key.pem \
  "https://eastus.mtlsauth.microsoft.com/your-tenant-id/oauth2/v2.0/token" \
  -d "grant_type=client_credentials&client_id=your-client-id&scope=https://graph.microsoft.com/.default&token_type=mtls_pop"
```

Expected: HTTP 200 with `"token_type":"mtls_pop"` in the JSON response.

### 3. Java test program

Create `TestMtlsPop.java`:

```java
import com.microsoft.aad.msal4j.*;
import java.io.*;
import java.util.*;

public class TestMtlsPop {
    public static void main(String[] args) throws Exception {
        // Load certificate
        InputStream certStream = new FileInputStream("test-cert.p12");
        ClientCertificate cert = ClientCertificate.create(certStream, "changeit");

        // Build app
        ConfidentialClientApplication app = ConfidentialClientApplication
            .builder("your-client-id", cert)
            .authority("https://login.microsoftonline.com/your-tenant-id")
            .azureRegion("eastus")
            .build();

        // Acquire mTLS PoP token
        Set<String> scopes = Collections.singleton("https://graph.microsoft.com/.default");
        ClientCredentialParameters params = ClientCredentialParameters
            .builder(scopes)
            .withMtlsProofOfPossession(true)
            .build();

        IAuthenticationResult result = app.acquireToken(params).get();

        System.out.println("=== SUCCESS ===");
        System.out.println("Token type:       " + result.tokenType());
        System.out.println("Expires:          " + result.expiresOnDate());
        System.out.println("Binding cert CN:  " + result.bindingCertificate().getSubjectX500Principal());
        System.out.println("Access token:     " + result.accessToken().substring(0, 40) + "...");
    }
}
```

### 4. Expected output

```
=== SUCCESS ===
Token type:       mtls_pop
Expires:          <date ~1hr from now>
Binding cert CN:  CN=MyApp, O=MyOrg, C=US
Access token:     eyJ0eXAiOiJKV1QiLCJub25jZSI6...
```

### 5. Verify cache hit (silent re-acquisition)

Call `acquireToken` again immediately — the second call should not make a network request:

```java
long t0 = System.currentTimeMillis();
IAuthenticationResult r1 = app.acquireToken(params).get();
IAuthenticationResult r2 = app.acquireToken(params).get();
System.out.println("Same token: " + r1.accessToken().equals(r2.accessToken())); // true
System.out.println("Elapsed: " + (System.currentTimeMillis() - t0) + "ms");    // should be <50ms
```

### 6. Verify token binding

Decode the access token (base64url decode the middle JWT segment) and verify:

```json
{
  "cnf": {
    "x5t#S256": "<base64url SHA-256 of your cert's DER encoding>"
  }
}
```

### Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `invalid_request` - authority must be tenanted | Common/organizations authority | Use `https://login.microsoftonline.com/{tenantId}` |
| `invalid_request` - certificate credential required | Using client secret | Switch to `ClientCertificate` credential |
| `AADSTS700016` - application not found | Wrong tenant or client ID | Verify app registration |
| `AADSTS7000215` - invalid client secret | Certificate not registered | Upload PEM cert to Azure Portal |
| `AADSTS90002` - tenant not found | Typo in tenant ID | Check tenant GUID |
| SSL handshake failure | Wrong cert/key | Verify cert and private key are paired |
| `Connection refused` on mtlsauth.microsoft.com | Network/firewall | Check outbound HTTPS access on port 443 |

---

## Path 2: Managed Identity

### Prerequisites

- Azure VM with managed identity enabled (System-assigned or User-assigned)
- Windows x64 OS with VBS (Virtualization-Based Security) KeyGuard
- `msal4j-mtls-extensions` JAR on classpath (or use the pre-built fat JAR)
- On Trusted Launch VMs: `AttestationClientLib.dll` on `PATH` or application directory
- No .NET runtime required — the extension calls CNG directly via JNA

### 1. Build the e2e fat JAR

```bash
cd msal4j-mtls-extensions
mvn package -DskipTests
# Produces: target/msal4j-mtls-extensions-1.0.0-e2e.jar
```

### 2. Run Path 2 (Managed Identity)

```powershell
# Basic (no attestation — works on standard VMs)
java -jar target\msal4j-mtls-extensions-1.0.0-e2e.jar path2

# With attestation (Trusted Launch VMs with AttestationClientLib.dll)
java -Djava.library.path=C:\msiv2 -jar target\msal4j-mtls-extensions-1.0.0-e2e.jar path2 --attest
```

### 3. Expected output

```
=== Path 2: Managed Identity mTLS PoP ===

Acquiring mTLS PoP token via IMDSv2 (full flow)...

[First call (from IMDS)]
  ✅ BindingCertificate present
     Subject:   CN=<client-id>,DC=<tenant-id>
     Issuer:    CN=managedidentitysnissuer.login.microsoft.com
     NotBefore: ...
     NotAfter:  ... (14 days)
  TokenType:  mtls_pop
  ExpiresIn:  86399s
  AccessToken cnf: {"x5t#S256":"<thumbprint>"}
  ✅ AccessToken present

Acquiring again (expect cert cache hit)...
[Second call (should be cert-cached, ~fast)]
  ✅ Binding cert cache working: same cert on second call
  ⏱  Elapsed: ~60ms

Making downstream mTLS call to graph.microsoft.com...
  Downstream HTTP status: 401
  ✅ TLS handshake + token delivery succeeded (HTTP < 500)
  ℹ️  401 — TLS OK, authorization depends on permissions

=== Path 2 Complete ===
```

> **Expected HTTP 401 from graph.microsoft.com:** This is correct behavior. The TLS handshake and token were accepted — the managed identity simply has no Graph role assigned. HTTP 401 confirms the mTLS PoP flow succeeded end-to-end.

### 4. Java API

```java
import com.microsoft.aad.msal4j.mtls.*;

MtlsMsiClient client = new MtlsMsiClient();
MtlsMsiHelperResult result = client.acquireToken(
    "https://graph.microsoft.com",   // resource (graph.microsoft.com confirmed enrolled)
    "SystemAssigned",                 // identity type
    null,                             // identity id (null for system-assigned)
    false,                            // withAttestation — set true on Trusted Launch VMs
    null                              // correlationId (optional)
);

String accessToken = result.getAccessToken();
String certPem     = result.getBindingCertificate();
```

> **Resource note:** Use `https://graph.microsoft.com` or `https://storage.azure.com` for testing.
> `https://management.azure.com` may return `AADSTS392196` if the resource is not enrolled for mTLS PoP in your tenant.

### 5. Verify token claims

Decode the JWT payload and confirm:

```powershell
$token = "<access-token>"
$parts = $token -split "\."
[System.Text.Encoding]::UTF8.GetString(
    [System.Convert]::FromBase64String(
        $parts[1].PadRight($parts[1].Length + (4 - $parts[1].Length % 4) % 4, '='))) |
    ConvertFrom-Json
```

Expected claims:
```json
{
  "cnf":            { "x5t#S256": "<thumbprint matching binding cert>" },
  "xms_tbflags":    2,
  "appidacr":       "2",
  "aud":            "https://graph.microsoft.com",
  "idtyp":          "app",
  "app_displayname": "<your VM's managed identity name>"
}
```

The `cnf.x5t#S256` thumbprint must match the binding certificate returned by `result.getBindingCertificate()`.

### Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `VBS KeyGuard not available` | Credential Guard not enabled | Enable VBS/Credential Guard and reboot |
| `AttestationClientLib.dll not found` | DLL not on PATH | Copy DLL from NuGet package to application directory |
| `HTTP 400 from IMDS issuecredential` | Attestation token empty | Check DLL is present; VM must be Trusted Launch |
| `AADSTS392196` | Resource not enrolled for mTLS PoP | Use `https://graph.microsoft.com` instead |
| `IMDS not accessible` | Not running on Azure VM | This path only works in Azure managed identity environments |
| `NCryptFinalizeKey NTE_BAD_FLAGS` | VBS not running | Check `msinfo32.exe` → Virtualization-based security must show "Running" |

---

## Validating Cache Isolation

To verify mTLS PoP and Bearer tokens don't share cache entries:

```java
// Acquire Bearer token
ClientCredentialParameters bearerParams = ClientCredentialParameters
    .builder(scopes)
    .build();
IAuthenticationResult bearerResult = app.acquireToken(bearerParams).get();

// Acquire mTLS PoP token
ClientCredentialParameters mtlsParams = ClientCredentialParameters
    .builder(scopes)
    .withMtlsProofOfPossession(true)
    .build();
IAuthenticationResult mtlsResult = app.acquireToken(mtlsParams).get();

// Tokens must be different
assert !bearerResult.accessToken().equals(mtlsResult.accessToken());
assert "Bearer".equalsIgnoreCase(bearerResult.tokenType());
assert "mtls_pop".equalsIgnoreCase(mtlsResult.tokenType());
```
