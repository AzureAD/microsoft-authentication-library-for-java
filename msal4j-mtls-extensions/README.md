# MSAL4J Key Attestation

`com.microsoft.azure:msal4j-key-attestation` adds Managed Identity v2 mTLS
Proof-of-Possession (PoP) and bearer-over-mTLS to
`ManagedIdentityApplication`.

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
`ManagedIdentityAttestationExtensions.withAttestationSupport(builder)` is used,
bundled DLL extraction/loading, KeyGuard attestation, or invalid attestation
evidence fails closed. Platform support,
credential issuance, certificate validation, and an explicit
`token_type=mtls_pop` response are always required.

The attestation convenience API is intentionally declared in this optional
artifact, matching the MSAL.NET `Microsoft.Identity.Client.KeyAttestation`
package boundary. Core MSAL contains only the provider integration contract.

For bearer-over-mTLS, replace `withMtlsProofOfPossession(...)` with
`withRequestOverMtls()`. The KeyGuard certificate authenticates the ESTS
connection, while the returned token remains an ordinary bearer token and has
no binding context.

The bundled native library is verified with both a pinned SHA-256 digest and
Windows `WinVerifyTrust` Authenticode validation before loading. Its extraction
directory is restricted to the current Windows user.

## Dependency

```xml
<dependency>
  <groupId>com.microsoft.azure</groupId>
  <artifactId>msal4j-key-attestation</artifactId>
  <version>${msal4j.version}</version>
</dependency>
```

## Token acquisition

```java
ManagedIdentityApplication application = ManagedIdentityApplication
        .builder(ManagedIdentityId.systemAssigned())
        .build();

ManagedIdentityParameters.ManagedIdentityParametersBuilder builder =
        ManagedIdentityParameters
        .builder("https://vault.azure.net")
        .withMtlsProofOfPossession(
                MtlsPopOptions.builder()
                        .minimumBindingStrength(
                                MtlsBindingStrength.KEY_GUARD)
                        .build());

ManagedIdentityParameters parameters =
        ManagedIdentityAttestationExtensions
                .withAttestationSupport(builder)
                .build();

IAuthenticationResult result = application
        .acquireTokenForManagedIdentity(parameters)
        .get();
```

Before acquisition, credential chains can call
`application.getManagedIdentityCapabilities()` and inspect
`maxSupportedBindingStrength()` and `isMtlsPopSupportedByHost()`. Discovery verifies
the IMDS v2 and KeyGuard capability without acquiring an access token.

For a user-assigned identity:

```java
ManagedIdentityId identity =
        ManagedIdentityId.userAssignedClientId("<managed-identity-client-id>");
```

The result exposes:

- `tokenType()` - must be `mtls_pop`
- `bindingCertificate()` - the IMDS-issued leaf certificate
- `mtlsBindingContext()` - the live, process-local binding capability
- `mtlsBindingStrength()` - the actual binding strength used by the result
- `mtlsBindingContext().sslContext()` - a reusable Java JSSE `SSLContext`
- `mtlsBindingContext().keyManager()` - the non-exportable-key
  `X509ExtendedKeyManager` for custom TLS contexts
- `mtlsBindingContext().keyId()` - Base64URL SHA-256 of the complete leaf DER

Private key bytes and native handles are never exposed. Binding contexts are
transient and are not serialized into persistent token caches.

The downstream resource call must use the same live binding context and leaf
certificate returned with the token. A token acquired for one binding generation
must not be sent over a TLS connection using another certificate.

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
        "MTLS_POP " + result.accessToken());
connection.setRequestProperty("x-ms-tokenboundauth", "true");

if (connection.getResponseCode() != 200) {
    throw new IllegalStateException("Token-bound Key Vault call failed.");
}
```

The application owns this downstream call. The extension does not expose a
native HTTP helper or a downstream request API.

`MTLS_POP` is the downstream HTTP authorization scheme. The token endpoint
returns `token_type=mtls_pop`; authorization-scheme matching is case-insensitive,
but the sample uses the conventional uppercase form explicitly.

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
the request-specific `SSLContext` or `SSLSocketFactory`, and disable redirects
for mTLS token requests. The supplied `SSLContext` uses JVM default trust
managers and is safe to reuse for ordinary JSSE resource calls. Applications
that need custom trust anchors or a non-JSSE transport should build their own
TLS context from the exposed key manager.

The current binding context uses TLS 1.2. TLS 1.3 support is being investigated
with the service team because the service does not yet request the required
client certificate during TLS 1.3 negotiation.

The initial native package is Windows x64 only. Windows ARM64 callers receive a
typed unsupported-architecture failure before native loading.

Set `MSAL_MI_DISABLE_IMDS_V2=true` (or `1`) and restart the process to disable
Managed Identity v2. Explicit PoP and bearer-over-mTLS requests then fail closed,
while capability discovery reports no available mTLS binding.

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

To validate bearer-over-mTLS token acquisition and its cache partition:

```powershell
$env:MSAL_JAVA_MTLS_TOKEN_ONLY = "true"
$env:MSAL_JAVA_MTLS_REQUEST_OVER_MTLS = "true"
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
See the
[DevEx guide](../msal4j-sdk/docs/managed-identity-v2-devex.md)
for a concise before/after API migration.
