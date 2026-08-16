# MSAL4J Managed Identity v2 mTLS extension

`msal4j-mtls-extensions` adds Managed Identity v2 mTLS
Proof-of-Possession (PoP) to `ManagedIdentityApplication`.

The extension keeps native interop limited to Windows KeyGuard/CNG signing and
Microsoft Azure Attestation. OAuth, HTTP policy, token caching, TLS, and
downstream resource calls remain Java-owned:

```text
ManagedIdentityApplication
  -> IMDS v2 platform metadata
  -> KeyGuard non-exportable RSA key
  -> MAA attestation
  -> IMDS v2 binding certificate
  -> JSSE mTLS token request
  -> IAuthenticationResult + reusable IMtlsBindingContext
```

## Requirements

- Java 8 or later
- Windows x64 Trusted Launch Azure VM or VMSS instance
- Secure Boot, vTPM, VBS KeyGuard, and Managed Identity enabled
- The extension bundles the Microsoft-signed x64 `AttestationClientLib.dll` from
  `Microsoft.Azure.Security.KeyGuardAttestation` 1.1.5, matching MSAL.NET
- A resource and tenant enrolled for Managed Identity mTLS PoP

Attestation is optional at the public API. When
`withAttestationSupport()` is enabled, bundled DLL extraction/loading, KeyGuard
attestation, or invalid attestation evidence fails closed. Platform support,
credential issuance, certificate validation, and an explicit
`token_type=mtls_pop` response are always required.

## Token acquisition

```java
ManagedIdentityApplication application = ManagedIdentityApplication
        .builder(ManagedIdentityId.systemAssigned())
        .build();

ManagedIdentityParameters parameters = ManagedIdentityParameters
        .builder("https://vault.azure.net")
        .withMtlsProofOfPossession()
        .withAttestationSupport()
        .build();

IAuthenticationResult result = application
        .acquireTokenForManagedIdentity(parameters)
        .get();
```

For a user-assigned identity:

```java
ManagedIdentityId identity =
        ManagedIdentityId.userAssignedClientId("<managed-identity-client-id>");
```

The result exposes:

- `tokenType()` - must be `mtls_pop`
- `bindingCertificate()` - the IMDS-issued leaf certificate
- `mtlsBindingContext()` - the live, process-local binding capability
- `mtlsBindingContext().sslContext()` - a reusable Java JSSE `SSLContext`
- `mtlsBindingContext().keyId()` - Base64URL SHA-256 of the complete leaf DER

Private key bytes and native handles are never exposed. Binding contexts are
transient and are not serialized into persistent token caches.

## Independent Java 8 Key Vault call

```java
URL url = new URL(
        "https://<vault>.vault.azure.net/secrets/<name>?api-version=7.5");
HttpsURLConnection connection =
        (HttpsURLConnection) url.openConnection();

connection.setSSLSocketFactory(
        result.mtlsBindingContext().sslContext().getSocketFactory());
connection.setInstanceFollowRedirects(false);
connection.setRequestMethod("GET");
connection.setRequestProperty(
        "Authorization",
        result.tokenType() + " " + result.accessToken());
connection.setRequestProperty("x-ms-tokenboundauth", "true");

if (connection.getResponseCode() != 200) {
    throw new IllegalStateException("Token-bound Key Vault call failed.");
}
```

The application owns this downstream call. The extension does not expose a
native HTTP helper or a downstream request API.

## Caching and rotation

- Bearer and mTLS PoP access tokens use distinct cache partitions.
- The mTLS partition includes the complete-certificate key ID.
- Attested and unattested requests use distinct cache partitions.
- A renewed certificate creates a new token-cache partition even when it uses
  the same underlying RSA key.
- Cache hits reacquire the matching live binding context before returning.
- Certificates enter rotation 24 hours before expiry.
- Attestation JWTs are cached by normalized attestation endpoint and key ID,
  with a five-minute freshness buffer and per-key single-flight behavior.

Custom application HTTP clients must implement `IMtlsCapableHttpClient`, honor
the request-specific `SSLSocketFactory`, and disable redirects for mTLS token
requests.

## Manual validation

From the repository root:

```powershell
.\run-java-msi-v2-mtls-devapp.ps1
```

To validate only token acquisition and certificate binding without calling a
downstream resource:

```powershell
$env:MSAL_JAVA_MTLS_TOKEN_ONLY = "true"
.\run-java-msi-v2-mtls-devapp.ps1
```

For the negative certificate-binding proof, attach a distinct user-assigned
managed identity to the VM and set:

```powershell
$env:MSAL_JAVA_MTLS_MISMATCH_IDENTITY_CLIENT_ID = "<second-uami-client-id>"
```

The app first proves token A with binding A returns HTTP 200, then requires
token A with binding B to be rejected.

See
[`msal4j-sdk/docs/managed-identity-v2-mtls-pop.md`](../msal4j-sdk/docs/managed-identity-v2-mtls-pop.md)
for architecture, protocol reconciliation, and troubleshooting.
