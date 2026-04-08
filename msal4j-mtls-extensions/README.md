# msal4j-mtls-extensions

Provides Managed Identity mTLS Proof-of-Possession (mTLS PoP) support for `msal4j`. This module handles the acquisition of `mtls_pop` tokens using IMDS-issued, KeyGuard-backed certificates on Azure VMs.

> **Platform**: Windows only (requires .NET 8 runtime for the bundled `MsalMtlsMsiHelper.exe`)

---

## Why a Separate Module?

KeyGuard keys — hardware-isolated private keys used by Managed Identity mTLS PoP — are created via Windows CNG (`NCryptCreatePersistedKey` with `NCRYPT_VBS_KEYISOLATION_FLAG`). Java's Windows TLS integration (`SunMSCAPI`) uses the legacy CAPI subsystem and has no path to CNG. This is the same fundamental limitation as Node.js/OpenSSL.

The solution is a subprocess model: this module bundles `MsalMtlsMsiHelper.exe`, a .NET 8 binary that uses Schannel and CNG natively to handle the full KeyGuard certificate lifecycle. `msal4j-sdk` delegates to this binary when `withMtlsProofOfPossession(true)` is set on `ManagedIdentityParameters`.

For a detailed technical analysis, see [keyguard-jvm-analysis.md](../msal4j-sdk/docs/keyguard-jvm-analysis.md).

---

## Installation

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.microsoft.azure</groupId>
    <artifactId>msal4j-mtls-extensions</artifactId>
    <version>1.24.0</version>
</dependency>
```

The extension is a standalone artifact — you do **not** need to depend on `msal4j` separately; `msal4j-mtls-extensions` already depends on it transitively.

---

## Requirements

| Requirement | Details |
|-------------|---------|
| OS | Windows (the bundled binary is Windows x64) |
| .NET runtime | .NET 8.x (`dotnet --version` must print `8.x.x`) |
| Azure environment | VM, App Service, Azure Functions, or any managed-identity-enabled resource |
| Managed identity | System-assigned or user-assigned identity must be enabled |

---

## Usage

### System-Assigned Managed Identity

```java
import com.microsoft.aad.msal4j.*;

ManagedIdentityApplication app = ManagedIdentityApplication
    .builder(ManagedIdentityId.systemAssigned())
    .build();

ManagedIdentityParameters params = ManagedIdentityParameters
    .builder("https://management.azure.com/")
    .withMtlsProofOfPossession(true)
    .build();

IAuthenticationResult result = app.acquireTokenForManagedIdentity(params).get();
String token = result.accessToken();   // mtls_pop token
```

### User-Assigned Managed Identity

```java
// By client ID
ManagedIdentityApplication app = ManagedIdentityApplication
    .builder(ManagedIdentityId.userAssignedClientId("your-client-id"))
    .build();

// By object ID
ManagedIdentityApplication app2 = ManagedIdentityApplication
    .builder(ManagedIdentityId.userAssignedObjectId("your-object-id"))
    .build();
```

### With MAA Attestation

MAA (Microsoft Azure Attestation) attestation provides cryptographic proof that the key was created in a VBS-isolated enclave. Requires Trusted Launch or Confidential VM.

```java
ManagedIdentityParameters params = ManagedIdentityParameters
    .builder("https://graph.microsoft.com/")
    .withMtlsProofOfPossession(true)
    .withAttestation(true)
    .build();
```

---

## API Reference

### `MtlsMsiClient`

Main entry point for the subprocess wrapper (used internally by `msal4j-sdk` via reflection).

```java
package com.microsoft.aad.msal4j.mtls;

public class MtlsMsiClient {
    // Acquire an mtls_pop token via MsalMtlsMsiHelper.exe
    public MtlsMsiHelperResult acquireToken(
        String resource,
        String identityType,   // "SystemAssigned" | "UserAssignedClientId" | "UserAssignedObjectId" | "UserAssignedResourceId"
        String identityId,     // null for SystemAssigned
        boolean withAttestation,
        String correlationId
    ) throws MtlsMsiException;

    // Make an mTLS-authenticated HTTP request via the helper
    public MtlsMsiHttpResponse httpRequest(
        String url,
        String method,
        String token,
        Map<String, String> headers,
        String body
    ) throws MtlsMsiException;
}
```

### `MtlsMsiHelperResult`

Result of a successful `acquireToken` call.

| Field | Type | Description |
|-------|------|-------------|
| `accessToken` | `String` | The `mtls_pop` access token |
| `tokenType` | `String` | Always `"mtls_pop"` |
| `expiresOn` | `long` | Expiry as Unix timestamp (seconds) |
| `thumbprint` | `String` | x5t#S256 Base64URL thumbprint of binding cert |

### `MtlsMsiException`

Thrown when the helper subprocess fails. Wraps the error JSON from the helper's stderr:

```json
{ "error": "...", "error_description": "..." }
```

---

## How It Works

```
Java App
  │
  ▼ acquireTokenForManagedIdentity(params) [withMtlsProofOfPossession=true]
ManagedIdentityApplication (msal4j-sdk)
  │
  ▼ reflection → MtlsMsiClient (msal4j-mtls-extensions)
MtlsMsiClient
  │
  ▼ spawn subprocess
MsalMtlsMsiHelper.exe (.NET 8)
  ├── IMDS getplatformmetadata
  ├── NCryptCreatePersistedKey (CNG + NCRYPT_VBS_KEYISOLATION_FLAG)
  ├── Generate CSR
  ├── [optional] MAA attestation
  ├── IMDS /issuecredential → receives KeyGuard-backed X509 cert
  ├── mTLS token request to mtlsauth.microsoft.com (Schannel + NCRYPT_KEY_HANDLE)
  └── stdout: JSON {access_token, token_type, expires_on, thumbprint}
  │
  ▼ parse JSON, build AuthenticationResult
Java App receives IAuthenticationResult
```

---

## Helper Binary Location

The `MsalMtlsMsiHelper.exe` binary is bundled in the JAR at `resources/MsalMtlsMsiHelper.exe`. At runtime, `MtlsMsiHelperLocator` extracts it to a temp directory on first use.

**Override**: Set the `MSAL_MTLS_HELPER_PATH` environment variable to an absolute path to use a custom or pre-extracted binary:

```bash
set MSAL_MTLS_HELPER_PATH=C:\custom\path\MsalMtlsMsiHelper.exe
```

This is useful for:
- Air-gapped environments where JAR extraction is restricted
- Testing with a debug build of the helper
- Pre-extracting the binary to a known location as part of VM provisioning

---

## Building `MsalMtlsMsiHelper.exe` from Source

The binary is built from the msaljs `msal-node-mtls-extensions` project:

```
C:\Projects\msaljs\extensions\msal-node-mtls-extensions\native\MsalMtlsMsiHelper\
```

Build (framework-dependent, requires .NET 8 SDK):

```bash
cd MsalMtlsMsiHelper
dotnet publish -r win-x64 --self-contained false -o publish/
# Output: publish/MsalMtlsMsiHelper.exe (≈1.4 MB)
```

For self-contained (no .NET runtime required on target):

```bash
dotnet publish -r win-x64 --self-contained true -p:PublishSingleFile=true -o publish/
# Output: publish/MsalMtlsMsiHelper.exe (≈65 MB)
```

---

## Token Caching

mTLS PoP tokens are cached in the in-memory token cache with credential type `AccessToken_With_AuthScheme` and a `keyId` segment (the x5t#S256 thumbprint). Cache entries do not conflict with Bearer tokens for the same scope.

Cache TTL matches the `expires_in` returned by AAD (typically 1 hour). Expired tokens trigger a new subprocess invocation.

---

## Support and Servicing

This module follows the same support lifecycle as `msal4j`. File issues at [GitHub Issues](https://github.com/AzureAD/microsoft-authentication-library-for-java/issues).

**Windows only**: The bundled `MsalMtlsMsiHelper.exe` is a Windows x64 binary. Linux/macOS Azure environments that support Managed Identity can use the standard Bearer token flow via `ManagedIdentityApplication` without this extension.
