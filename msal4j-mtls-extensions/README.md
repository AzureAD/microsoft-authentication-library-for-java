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
- For attestation, an application-supplied Windows x64 `AttestationClientLib.dll`
  from `Microsoft.Azure.Security.KeyGuardAttestation`, deployed separately
- A resource and tenant enrolled for Managed Identity mTLS PoP

Attestation is optional at the public API. When
`ManagedIdentityAttestationExtensions.withAttestationSupport(builder)` is used,
external DLL configuration/loading, KeyGuard attestation, or invalid attestation
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

The application owns obtaining a trusted native package, selecting a compatible
version, verifying its provenance, deploying its dependencies, and servicing it.
MSAL verifies Windows `WinVerifyTrust` Authenticode trust before loading the
configured DLL. This trust check is not a package-version or publisher pin.
The application must protect the DLL and its directory against modification by
untrusted users.

## Bring the native package

The Java Maven dependency contains the integration code, **not the native DLL or
native project**. MSAL does not download, extract, or search `PATH` or
`java.library.path` for the attestation DLL.

Obtain the native
[`Microsoft.Azure.Security.KeyGuardAttestation` package](https://www.nuget.org/packages/Microsoft.Azure.Security.KeyGuardAttestation)
through your approved package-management process. Deploy its Windows x64 native
payload and required dependencies outside the application JAR, then configure
the exact DLL before starting Java:

```powershell
$env:MSAL_KEY_ATTESTATION_LIBRARY_PATH = "C:\my-app\native\AttestationClientLib.dll"
java -jar my-app.jar
```

An absolute, readable file path is required. The library is loaded lazily on the
first attestation request and reused for the process lifetime. Restart the
application after changing its native package or configuration. Missing
configuration, invalid signatures and load failures produce errors; there is no
fallback to an unverified DLL or to unattested authentication. Flows that do not
request attestation do not require this DLL.

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
An HTTP client that does not implement `IMtlsCapableHttpClient` reports no
mTLS support without probing IMDS or KeyGuard.

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
- `mtlsBindingContext().rotatingSslContext()` / `rotatingKeyManager()` -
  stable TLS capabilities that follow the provider's active binding generation
- `mtlsBindingContext().keyId()` - Base64URL SHA-256 of the complete leaf DER

Private key bytes and native handles are never exposed. Binding contexts are
transient and are not serialized into persistent token caches.

The downstream resource call must use the same live binding context and leaf
certificate returned with the token. A token acquired for one binding generation
must not be sent over a TLS connection using another certificate.

Long-lived HTTP clients can retain `rotatingSslContext()` or build a custom
context once with `rotatingKeyManager()`. These follow certificate renewal and
reminting for the same identity and attestation mode. Acquire a current token
before resource calls and retire pooled connections when the returned binding
key ID changes: an existing connection still uses its original certificate.
Custom TLS contexts must also invalidate cached TLS sessions on that change.
Generation-specific `sslContext()` and `keyManager()` remain available for
requests using an older, still-valid token. Aliases selected before rotation
continue to resolve to that generation until its certificate expires.

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
that need custom trust anchors or a JSSE-backed custom transport should build their own
TLS context from the exposed key manager.

Custom TLS contexts must enable only TLS 1.2 and use a JSSE-backed provider
(for example, Netty `SslProvider.JDK`, not `SslProvider.OPENSSL`). The
non-exportable CNG key signs through JCA and cannot be exported to OpenSSL.
The supplied binding contexts use TLS 1.2. TLS 1.3 support is being investigated
with the service team because the service does not yet request the required
client certificate during TLS 1.3 negotiation.

The current Java native integration supports Windows x64 only. Windows ARM64 callers receive a
typed unsupported-architecture failure before native loading.

Set `MSAL_MI_DISABLE_IMDS_V2=true` (or `1`) and restart the process to disable
Managed Identity v2. Explicit PoP and bearer-over-mTLS requests then fail closed,
while capability discovery reports no available mTLS binding.

## Manual validation

From the repository root:

```powershell
.\build\run-java-msi-v2-mtls-devapp.ps1
```

This **test-only** wrapper downloads and verifies a pinned native test package
into `msal4j-mtls-extensions\target\key-attestation-test` when
`MSAL_KEY_ATTESTATION_LIBRARY_PATH` is unset. The PR E2E job provisions the same
dependency separately. Neither the production JAR nor the shaded E2E JAR
contains `AttestationClientLib.dll`.

For offline test setup, supply a DLL already obtained from the approved native
package (the helper enforces its test-version hash and Authenticode signature):

```powershell
$env:MSAL_KEY_ATTESTATION_LIBRARY_PATH = .\build\prepare-key-attestation-test-dependency.ps1 `
    -LibraryPath "C:\approved-packages\keyguard-1.1.5\AttestationClientLib.dll"
```

Ordinary unit tests do not need the package. The external-DLL signature test
runs only on Windows when the path is configured; configured errors fail rather
than skipping. Live attestation E2E tests require the DLL and fail if provisioning
or loading fails.

To validate only token acquisition and certificate binding without calling a
downstream resource:

```powershell
$env:MSAL_JAVA_MTLS_TOKEN_ONLY = "true"
.\build\run-java-msi-v2-mtls-devapp.ps1
```

To validate bearer-over-mTLS token acquisition and its cache partition:

```powershell
$env:MSAL_JAVA_MTLS_TOKEN_ONLY = "true"
$env:MSAL_JAVA_MTLS_REQUEST_OVER_MTLS = "true"
.\build\run-java-msi-v2-mtls-devapp.ps1
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
