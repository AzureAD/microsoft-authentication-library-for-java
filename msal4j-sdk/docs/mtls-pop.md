# mTLS Proof-of-Possession (mTLS PoP) in MSAL4J

## Overview

mTLS Proof-of-Possession is a token-binding mechanism that cryptographically ties an access token to a specific client certificate. Unlike Bearer tokens, an mTLS PoP token can only be used by the party that holds the private key of the binding certificate — even if the token is intercepted, it is useless without the private key.

The token contains a `cnf` (confirmation) claim with an `x5t#S256` field: the SHA-256 thumbprint of the binding certificate. The resource server validates the mTLS connection and checks that the connecting client's certificate matches the token's `cnf` claim.

MSAL4J supports two mTLS PoP acquisition paths:

| Path | Application Type | Certificate Source | Attestation |
|------|-----------------|-------------------|-------------|
| **SNI (Subject Name Indication)** | `ConfidentialClientApplication` | Any PKCS12/PEM cert or hardware token (PKCS11) | Not required |
| **Managed Identity** | `MtlsMsiClient` (via `msal4j-mtls-extensions`) | IMDS-issued KeyGuard-backed certificate | Optional (Trusted Launch VMs) |

---

## Cross-SDK Implementation Comparison

| Library | TLS Stack | CNG Support | Approach |
|---------|-----------|-------------|----------|
| **msal-java** | JSSE + custom `SSLSocketFactory` (Path 1); JNA → `ncrypt.dll` (Path 2) | ✅ Via JNA | In-process |
| **msal-dotnet** | Schannel (.NET) | ✅ Native | In-process |
| **msal-go** | `crypto/tls` (pure Go) | ✅ Via `crypto.Signer` | In-process |
| **msal-node** | OpenSSL (Node.js) | ❌ None | .NET subprocess (`MsalMtlsMsiHelper.exe`) |

No subprocess is needed in msal-java.

---

## Why mTLS PoP?

Standard Bearer tokens are vulnerable to:
- Token theft via XSS or compromised intermediaries
- Confused-deputy attacks where a stolen token is replayed from a different client

With mTLS PoP:
- The token is bound to the TLS client certificate at issuance time
- Resource servers enforce that the connecting client certificate matches the token's `cnf` claim
- An attacker who steals the token cannot use it without also stealing the private key

This makes mTLS PoP suitable for high-value API access from server-side applications running in trusted Azure environments.

---

## Path 1: SNI / ConfidentialClientApplication

### How It Works

1. Your app creates a `ConfidentialClientApplication` with a certificate credential.
2. Calls `acquireToken` with `ClientCredentialParameters.withMtlsProofOfPossession()`.
3. MSAL4J builds a custom `SSLSocketFactory` from the cert and private key.
4. The token request goes to the mTLS-specific endpoint (`mtlsauth.microsoft.com` for public cloud).
5. The request body contains `grant_type=client_credentials`, `token_type=mtls_pop`, and `scope`. There is **no `client_assertion`** — authentication happens at the TLS layer.
6. The response access token contains a `cnf.x5t#S256` binding.

### Requirements

- Certificate credential (`ClientCertificate`) — PKCS12, PEM, or hardware-backed (PKCS11)
- Tenanted authority (must specify a tenant ID or tenant FQDN — common/organizations endpoints not supported)
- Azure region configured or auto-detect enabled
- AAD authority only — B2C is not supported
- Public cloud or sovereign clouds only (US Gov and China clouds are not supported)

### Quick Start

```java
import com.microsoft.aad.msal4j.*;
import java.io.FileInputStream;
import java.util.*;

// 1. Load your certificate (PKCS12)
IClientCertificate cert = ClientCredentialFactory.createFromCertificate(
    new FileInputStream("/path/to/cert.p12"), "password");

// 2. Build the app (tenanted authority + region required)
ConfidentialClientApplication app = ConfidentialClientApplication
    .builder("your-client-id", cert)
    .authority("https://login.microsoftonline.com/your-tenant-id")
    .azureRegion("eastus")     // or autoDetectRegion(true)
    .build();

// 3. Request an mTLS PoP token
Set<String> scopes = Collections.singleton("https://graph.microsoft.com/.default");
ClientCredentialParameters params = ClientCredentialParameters
    .builder(scopes)
    .withMtlsProofOfPossession()
    .build();

IAuthenticationResult result = app.acquireToken(params).get();

System.out.println("Token type: " + result.tokenType());       // "mtls_pop"
System.out.println("Binding cert: " + result.bindingCertificate().getSubjectX500Principal());
System.out.println("Access token: " + result.accessToken());
```

### Hardware-Backed Certificates (PKCS11)

For hardware security modules or smart cards, load the private key and certificate chain from a PKCS11 provider:

```java
Provider pkcs11Provider = Security.getProvider("SunPKCS11");
KeyStore ks = KeyStore.getInstance("PKCS11", pkcs11Provider);
ks.load(null, "pin".toCharArray());

PrivateKey privateKey = (PrivateKey) ks.getKey("my-key-alias", null);
X509Certificate cert = (X509Certificate) ks.getCertificate("my-key-alias");

IClientCertificate clientCert = ClientCredentialFactory.createFromCertificate(privateKey, cert);
```

### Token Endpoint

For public cloud, MSAL4J constructs:
```
https://{region}.mtlsauth.microsoft.com/{tenantId}/oauth2/v2.0/token
```

For sovereign clouds, the `login.` prefix is replaced with `mtlsauth.`:
- `login.microsoftonline.us` → `mtlsauth.microsoftonline.us`
- `login.microsoftonline.de` → `mtlsauth.microsoftonline.de`

US Government (`login.usgovcloudapi.net`) and China (`login.chinacloudapi.cn`) clouds are **not supported** — these clouds do not have an mTLS auth endpoint.

### Token Cache

mTLS PoP tokens are cached separately from Bearer tokens using:
- Credential type: `AccessToken_With_AuthScheme` (instead of `AccessToken`)
- Cache key suffix: `x5t#S256` thumbprint of the binding certificate

This prevents a Bearer token cache hit from returning an mTLS PoP token and vice versa.

---

## Path 2: Managed Identity

### How It Works

Managed Identity mTLS PoP uses a KeyGuard-backed certificate issued by IMDS. KeyGuard keys are hardware-isolated keys created with CNG (`NCryptCreatePersistedKey` with `NCRYPT_VBS_KEYISOLATION_FLAG`). The `msal4j-mtls-extensions` module calls Windows CNG directly via JNA (Java Native Access), so no .NET runtime or external subprocess is needed.

The extension handles:
1. IMDS `getplatformmetadata` call
2. KeyGuard key creation via CNG (`ncrypt.dll`)
3. CSR generation
4. Optional MAA attestation via `AttestationClientLib.dll`
5. IMDS `/issuecredential` (get cert from IMDS)
6. mTLS token request to AAD

MSAL4J orchestrates the extension via reflection and caches the result.

### Requirements

- Azure VM with system-assigned or user-assigned managed identity enabled
- Windows x64 OS with VBS (Virtualization-Based Security) KeyGuard available
- `msal4j-mtls-extensions` artifact on the classpath (add as Maven dependency)
- On Trusted Launch VMs: `AttestationClientLib.dll` on `PATH` or in the application directory (see [msal4j-mtls-extensions README](../../msal4j-mtls-extensions/README.md))
- No .NET runtime required

### Quick Start

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>com.microsoft.azure</groupId>
    <artifactId>msal4j-mtls-extensions</artifactId>
    <version>1.24.0</version>
</dependency>
```

```java
// System-assigned managed identity
ManagedIdentityApplication app = ManagedIdentityApplication
    .builder(ManagedIdentityId.systemAssigned())
    .build();

ManagedIdentityParameters params = ManagedIdentityParameters
    .builder("https://graph.microsoft.com/.default")
    .withMtlsProofOfPossession(true)
    // .withAttestation(true)  // optional: enable MAA attestation
    .build();

IAuthenticationResult result = app.acquireTokenForManagedIdentity(params).get();
System.out.println("Token type: " + result.tokenType()); // "mtls_pop"
```

For user-assigned managed identity:
```java
ManagedIdentityApplication app = ManagedIdentityApplication
    .builder(ManagedIdentityId.userAssignedClientId("your-client-id"))
    .build();
```

---

## API Reference

### `ClientCredentialParameters`

| Method | Description |
|--------|-------------|
| `.withMtlsProofOfPossession()` | Acquires an `mtls_pop` token instead of Bearer |

### `ManagedIdentityParameters`

| Method | Description |
|--------|-------------|
| `.withMtlsProofOfPossession(boolean)` | When `true`, uses the JNA-backed KeyGuard extension to acquire a hardware-bound `mtls_pop` token |

### `IAuthenticationResult`

| Method | Returns |
|--------|---------|
| `.tokenType()` | `"mtls_pop"` or `"Bearer"` |
| `.bindingCertificate()` | `X509Certificate` used for mTLS binding, or `null` for Bearer |

---

## Caching Behavior

mTLS PoP tokens use a 7-segment cache key:
```
{homeAccountId}-{environment}-{credentialType}-{clientId}-{realm}-{target}-{keyId}
```
where `credentialType = AccessToken_With_AuthScheme` and `keyId` = Base64URL(SHA-256(DER cert)).

Standard Bearer tokens use a 6-segment key (no `keyId`). The two token types never collide in cache.

---

## Known Limitations

- **US Government and China clouds** are not supported for SNI path (no mTLS auth endpoint).
- **Managed Identity path** requires Windows x64 with VBS KeyGuard (the JNA native binding is Windows-only).
- **No refresh token** — mTLS PoP tokens cannot be silently refreshed via a refresh token. They are re-acquired via client credentials or re-issued by IMDS. The in-memory cache covers the token lifetime.
- **Sovereign cloud attestation** — MAA attestation is only available in public cloud regions.

---

## Error Reference

| Error Code | Meaning | Fix |
|---|---|---|
| `invalid_request` | Authority is not tenanted | Use `https://login.microsoftonline.com/{tenantId}` |
| `invalid_request` | Credential is not a certificate | mTLS PoP requires a `ClientCertificate` credential |
| `invalid_request` | Unsupported cloud | Use public cloud or a supported sovereign cloud |
| `invalid_request` | Region required | Set `.azureRegion(...)` on the app builder |
| `invalid_request` | mTLS extensions not on classpath | Add `msal4j-mtls-extensions` dependency |

---

## References

- [mTLS PoP Manual Testing Guide](mtls-pop-manual-testing.md)
- [mTLS PoP Architecture](mtls-pop-architecture.md)
- [msal4j-mtls-extensions README](../../msal4j-mtls-extensions/README.md)
- [MSAL.js mTLS PoP](https://github.com/AzureAD/microsoft-authentication-library-for-js/pull/8476)
- [MSAL.NET mTLS PoP](https://github.com/AzureAD/microsoft-authentication-library-for-dotnet/tree/main/docs)
- [RFC 8705 - OAuth 2.0 Mutual-TLS Client Authentication and Certificate-Bound Access Tokens](https://www.rfc-editor.org/rfc/rfc8705)
