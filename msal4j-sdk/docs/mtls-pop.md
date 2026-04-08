# mTLS Proof-of-Possession (mTLS PoP) in MSAL4J

## Overview

mTLS Proof-of-Possession is a token-binding mechanism that cryptographically ties an access token to a specific client certificate. Unlike Bearer tokens, an mTLS PoP token can only be used by the party that holds the private key of the binding certificate — even if the token is intercepted, it is useless without the private key.

The token contains a `cnf` (confirmation) claim with an `x5t#S256` field: the SHA-256 thumbprint of the binding certificate. The resource server validates the mTLS connection and checks that the connecting client's certificate matches the token's `cnf` claim.

MSAL4J supports two mTLS PoP acquisition paths:

| Path | Application Type | Certificate Source | Attestation |
|------|-----------------|-------------------|-------------|
| **SNI (Subject Name Indication)** | `ConfidentialClientApplication` | Any PKCS12/PEM cert or hardware token (PKCS11) | Not required |
| **Managed Identity** | `ManagedIdentityApplication` | IMDS-issued KeyGuard-backed certificate | Optional (IMDS-attested) |

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
// 1. Load your certificate
InputStream certStream = new FileInputStream("/path/to/cert.p12");
ClientCertificate cert = ClientCertificate.create(certStream, "password");

// 2. Build the app (tenanted authority + region required)
ConfidentialClientApplication app = ConfidentialClientApplication
    .builder("your-client-id", cert)
    .authority("https://login.microsoftonline.com/your-tenant-id")
    .azureRegion("eastus")     // or AzureRegion.AUTO_DISCOVER_REGION
    .build();

// 3. Request an mTLS PoP token
Set<String> scopes = Collections.singleton("https://graph.microsoft.com/.default");
ClientCredentialParameters params = ClientCredentialParameters
    .builder(scopes)
    .withMtlsProofOfPossession(true)
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
X509Certificate[] certChain = ...;  // from ks.getCertificateChain()

ClientCertificate cert = ClientCertificate.create(privateKey, certChain);
```

The `MtlsSslContextHelper` handles the PKCS12 in-memory KeyStore setup transparently.

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

Managed Identity mTLS PoP uses a KeyGuard-backed certificate issued by IMDS. KeyGuard keys are hardware-isolated keys created with CNG (`NCryptCreatePersistedKey` with `NCRYPT_VBS_KEYISOLATION_FLAG`). Java's JSSE and `SunMSCAPI` use the legacy Windows CryptoAPI (CAPI) and cannot create or use KeyGuard keys — the same limitation that exists in Node.js/OpenSSL. See [keyguard-jvm-analysis.md](keyguard-jvm-analysis.md) for a detailed technical analysis.

The implementation uses a subprocess approach: `MsalMtlsMsiHelper.exe`, a .NET 8 binary bundled in the `msal4j-mtls-extensions` module. The helper handles:
1. IMDS `getplatformmetadata` call
2. KeyGuard key creation via CNG
3. CSR generation
4. Optional MAA attestation
5. IMDS `/issuecredential` (get cert from IMDS)
6. mTLS token request to AAD

MSAL4J orchestrates the subprocess and caches the result.

### Requirements

- Azure VM, App Service, Azure Functions, or other managed identity-enabled resource
- `msal4j-mtls-extensions` artifact on the classpath (add as Maven dependency)
- .NET 8 runtime on the target host (pre-installed on most Azure Windows VMs)
- System-assigned or user-assigned managed identity enabled

See [msal4j-mtls-extensions/README.md](../../msal4j-mtls-extensions/README.md) for setup details.

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
| `.withMtlsProofOfPossession(boolean)` | When `true`, acquires an `mtls_pop` token instead of Bearer |

### `ManagedIdentityParameters`

| Method | Description |
|--------|-------------|
| `.withMtlsProofOfPossession(boolean)` | When `true`, delegates to `MsalMtlsMsiHelper.exe` for KeyGuard-backed cert |

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
- **Managed Identity path** requires Windows with .NET 8 runtime (the helper is a Windows-only binary).
- **Java cannot natively use KeyGuard keys** — see [keyguard-jvm-analysis.md](keyguard-jvm-analysis.md).
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

## Why Java's MI Path Cannot Use CNG

Java's TLS stack (JSSE) on Windows uses `SunMSCAPI` for Windows certificate store integration. `SunMSCAPI` bridges through the legacy **CryptoAPI (CAPI)** — not the modern **CNG (Cryptography API: Next Generation)**. KeyGuard keys are CNG-only; they require `NCryptCreatePersistedKey` with `NCRYPT_VBS_KEYISOLATION_FLAG`. CAPI cannot create or export these keys, and JSSE's `SunX509KeyManager` ultimately calls `java.security.Signature` which requires access to raw key material — a path that doesn't exist for hardware-isolated KeyGuard keys.

.NET's `HttpClientHandler` integrates with Schannel, which delegates TLS operations directly to `NCRYPT_KEY_HANDLE`, bypassing any need to export the private key. Java has no equivalent path.

See [keyguard-jvm-analysis.md](keyguard-jvm-analysis.md) for the full analysis including a JNI feasibility study.

---

## References

- [mTLS PoP Manual Testing Guide](mtls-pop-manual-testing.md)
- [msal4j-mtls-extensions README](../../msal4j-mtls-extensions/README.md)
- [KeyGuard JVM Analysis](keyguard-jvm-analysis.md)
- [MSAL.js mTLS PoP](https://github.com/AzureAD/microsoft-authentication-library-for-js/pull/8476)
- [MSAL.NET mTLS PoP](https://github.com/AzureAD/microsoft-authentication-library-for-dotnet/tree/main/docs)
- [RFC 8705 - OAuth 2.0 Mutual-TLS Client Authentication and Certificate-Bound Access Tokens](https://www.rfc-editor.org/rfc/rfc8705)
