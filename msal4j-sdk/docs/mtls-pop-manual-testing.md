# mTLS PoP Manual Testing Guide

This guide walks through manual verification of both mTLS PoP paths in MSAL4J.

---

## Prerequisites

- Java 8+ and Maven installed
- For SNI path: a valid test certificate (PKCS12)
- For Managed Identity path:
  - An Azure VM with managed identity enabled
  - `msal4j-mtls-extensions` on classpath (add dependency)
  - .NET 8 runtime installed on the VM
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
- `msal4j-mtls-extensions` JAR on classpath
- .NET 8 runtime: `dotnet --version` should print `8.x.x`
- IMDS accessible: `curl http://169.254.169.254/metadata/identity/oauth2/token?api-version=2018-02-01&resource=...`

### 1. Smoke-test the .NET helper binary

Locate the helper (bundled in the `msal4j-mtls-extensions` JAR or at `MSAL_MTLS_HELPER_PATH`):

```bash
# If using env override:
export MSAL_MTLS_HELPER_PATH=/path/to/MsalMtlsMsiHelper.exe

# Smoke test - acquire token for ARM resource
./MsalMtlsMsiHelper.exe \
  --mode acquire-token \
  --resource https://management.azure.com/ \
  --identity-type SystemAssigned
```

Expected stdout (JSON):
```json
{
  "access_token": "eyJ0...",
  "token_type": "mtls_pop",
  "expires_on": 1234567890,
  "thumbprint": "abc123..."
}
```

### 2. Java test program

```java
import com.microsoft.aad.msal4j.*;
import java.util.*;

public class TestMtlsMsi {
    public static void main(String[] args) throws Exception {
        ManagedIdentityApplication app = ManagedIdentityApplication
            .builder(ManagedIdentityId.systemAssigned())
            .build();

        ManagedIdentityParameters params = ManagedIdentityParameters
            .builder("https://management.azure.com/")
            .withMtlsProofOfPossession(true)
            .build();

        IAuthenticationResult result = app.acquireTokenForManagedIdentity(params).get();

        System.out.println("=== SUCCESS ===");
        System.out.println("Token type: " + result.tokenType());
        System.out.println("Expires:    " + result.expiresOnDate());
        System.out.println("Token:      " + result.accessToken().substring(0, 40) + "...");
    }
}
```

### 3. With attestation

```java
ManagedIdentityParameters params = ManagedIdentityParameters
    .builder("https://management.azure.com/")
    .withMtlsProofOfPossession(true)
    .withAttestation(true)
    .build();
```

Attestation requires:
- Azure VM with vTPM or Trusted Launch enabled
- MAA (Microsoft Azure Attestation) service accessible from the VM

### 4. User-assigned managed identity

```java
// By client ID
ManagedIdentityApplication app = ManagedIdentityApplication
    .builder(ManagedIdentityId.userAssignedClientId("your-client-id"))
    .build();

// By object ID  
ManagedIdentityApplication app2 = ManagedIdentityApplication
    .builder(ManagedIdentityId.userAssignedObjectId("your-object-id"))
    .build();

// By resource ID
ManagedIdentityApplication app3 = ManagedIdentityApplication
    .builder(ManagedIdentityId.userAssignedResourceId("/subscriptions/.../resourceGroups/.../providers/..."))
    .build();
```

### 5. End-to-end test: making an mTLS-required HTTP request

Use the result from step 2 to make a request to a resource server that enforces mTLS:

```java
// After acquiring the token, use MtlsMsiClient directly for mTLS-authenticated HTTP calls
// (this requires msal4j-mtls-extensions on classpath)
import com.microsoft.aad.msal4j.mtls.MtlsMsiClient;

MtlsMsiClient client = new MtlsMsiClient();
// Use the token to call a downstream API via mTLS
// The helper binary handles the mTLS transport
```

### Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `msal4j-mtls-extensions not on classpath` | Missing dependency | Add `msal4j-mtls-extensions` to pom.xml |
| Helper not found | No exe or env var not set | Set `MSAL_MTLS_HELPER_PATH` or include extensions JAR |
| `.NET runtime not found` | .NET 8 not installed | `sudo apt install dotnet-runtime-8.0` or Windows installer |
| `IMDS not accessible` | Not running on Azure VM | This path only works in Azure managed identity environments |
| Helper exits with non-zero | See stderr JSON `error_description` | Check IMDS logs, managed identity config, network rules |
| Attestation failure | VM doesn't support vTPM | Use `withAttestation(false)` or enable Trusted Launch |

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
