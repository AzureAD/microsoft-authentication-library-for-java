# Managed Identity v2 attested mTLS PoP

## Architecture

MSAL core owns request validation, identity selection, correlation IDs, HTTP
policy, proxy behavior, retries, telemetry, OAuth form construction, response
parsing, and token caching. The optional Windows extension owns only:

1. KeyGuard RSA key creation and NCrypt signing.
2. CSR construction.
3. `AttestationClientLib.dll` invocation and MAA JWT caching.
4. IMDS v2 binding-certificate lifecycle.
5. A JCA `PrivateKey`, key-selective `Provider`, `SignatureSpi`, and
   `X509ExtendedKeyManager`.

JSSE performs the token-endpoint and downstream mTLS handshakes:

```text
JSSE
  -> X509ExtendedKeyManager
  -> CngRsaPrivateKey
  -> CngSignatureSpi
  -> NCryptSignHash
  -> VBS KeyGuard
```

The provider advertises the required RSA signature algorithms, but its services
accept only `CngRsaPrivateKey`. Ordinary Java RSA keys continue to use the
platform's normal providers.

The current flow uses TLS 1.2 because the service does not yet request the
required client certificate during TLS 1.3 negotiation. TLS 1.3 support is
being investigated with the service team and can be enabled after the
end-to-end client-certificate behavior is supported and validated.

## Current protocol contract

Current MSAL.NET product behavior takes precedence over older design and
prototype material where they differ:

| Area | Current behavior |
| --- | --- |
| IMDS API | `cred-api-version=2.0` |
| Metadata path | `/metadata/identity/getplatformmetadata` |
| Credential path | `/metadata/identity/issuecredential` |
| Metadata fields | `clientId`, `tenantId`, `cuId`, `attestationEndpoint` |
| Credential fields | `certificate`, `client_id`, `tenant_id`, `identity_type`, `mtls_authentication_endpoint` |
| CSR subject | `CN={clientId}, DC={tenantId}` |
| CUID attribute | OID `1.3.6.1.4.1.311.90.2.10`, DER UTF8 JSON |
| CSR signature | RSA-PSS with SHA-256 and 32-byte salt |
| Certificate rotation | 24 hours before expiry |
| Token request | client credentials with `token_type=mtls_pop` over JSSE mTLS |

The older design's `api-version=2025-05-01`, challenge-password CUID encoding,
and three-day rotation window are not used.

## Public API and Java 8 compatibility

Request mTLS PoP and opt into attestation separately:

```java
ManagedIdentityParameters.ManagedIdentityParametersBuilder builder =
        ManagedIdentityParameters.builder("https://vault.azure.net")
                .withMtlsProofOfPossession();

ManagedIdentityParameters parameters =
        ManagedIdentityAttestationExtensions
                .withAttestationSupport(builder)
                .build();
```

`ManagedIdentityAttestationExtensions` is supplied only by the optional
`com.microsoft.azure:msal4j-key-attestation` artifact. Core MSAL does not expose
the attestation opt-in API.

Credential chains can probe the host before acquiring a token:

```java
ManagedIdentityCapabilities capabilities = application
        .getManagedIdentityCapabilities()
        .get();

if (capabilities.maxSupportedBindingStrength()
        == MtlsBindingStrength.KEY_GUARD) {
    // The host and optional extension can produce a KeyGuard binding.
}
```

Successful capability discovery is cached per application instance and checks the selected managed identity source, optional
provider availability, IMDS v2 platform metadata, and local KeyGuard
availability. It may create or reopen a persisted KeyGuard probe key, but it does
not issue a credential or acquire an access token.
Failed IMDS/KeyGuard probes are not cached permanently and can be retried.

Callers can also enforce a fail-closed minimum:

```java
MtlsPopOptions options = MtlsPopOptions.builder()
        .minimumBindingStrength(MtlsBindingStrength.KEY_GUARD)
        .build();

ManagedIdentityParameters.ManagedIdentityParametersBuilder builder =
        ManagedIdentityParameters.builder("https://vault.azure.net")
                .withMtlsProofOfPossession(options);

ManagedIdentityParameters parameters =
        ManagedIdentityAttestationExtensions
                .withAttestationSupport(builder)
                .build();
```

The tiers are `NONE`, `SOFTWARE`, and `KEY_GUARD`. The current Java extension
produces only `KEY_GUARD`; `SOFTWARE` is reserved for compatible future
providers. A successful request with a minimum strength guarantees that the
returned binding met that floor.

`withMtlsProofOfPossession()` can be used without attestation.
`ManagedIdentityAttestationExtensions.withAttestationSupport(builder)` requires
an mTLS flow and makes any attestation failure fail closed.

Use `withRequestOverMtls()` instead of `withMtlsProofOfPossession()` when the
KeyGuard certificate should authenticate only the ESTS connection. That mode
requests `token_type=bearer`; its result has no binding context because the
access token itself is not certificate-bound.

The returned `IMtlsBindingContext` contains an `SSLContext`,
`X509ExtendedKeyManager`, leaf certificate, and complete-certificate key ID.
It also reports the actual `bindingStrength()`;
`IAuthenticationResult.mtlsBindingStrength()` exposes the same value.
JSSE clients can consume the ready-to-use `SSLContext`. Async or engine-based
transports can use the context directly, while applications that require custom
trust anchors can combine the exposed key manager with their own trust
configuration. It contains no token-acquisition helper and no native HTTP
surface.

Production source remains Java 8 compatible. Use `HttpsURLConnection` for the
primary compatibility proof. Java 11 `java.net.http.HttpClient` may use the same
`SSLContext` in an application compiled separately for Java 11 or later.

## Cache safety

MSAL resolves the current binding generation before cache lookup. The extended
access-token cache key includes:

```text
token_type = mtls_pop
key_id     = Base64UrlNoPadding(SHA256(leafCertificate.getEncoded()))
attestation = att0 | att1
```

This prevents:

- Bearer and mTLS PoP token collisions.
- Token reuse across certificate renewal.
- Returning an mTLS token without a matching live binding context.

Native key state and `SSLContext` are process-local and excluded from serialized
cache data.

On every acquisition, MSAL resolves the current binding generation before the
cache lookup. A persisted `mtls_pop` cache entry is returned only after the
matching live binding context has been recreated and attached to the result.

## Downstream resource contract

The resource request must:

- perform TLS client authentication with the same leaf certificate and
  non-exportable key represented by `result.mtlsBindingContext()`;
- send `Authorization: MTLS_POP <access-token>`;
- avoid reusing the token after certificate rotation changes
  `result.mtlsBindingContext().keyId()`.

The supplied `sslContext()` is stable for the binding generation, uses JVM
default trust, and is safe to reuse for ordinary JSSE resource calls.
Applications that require custom trust anchors or a non-JSSE transport should
construct their own TLS context from `keyManager()`.

## Failure behavior

The flow fails closed when:

- the optional extension is absent or ambiguous;
- the source is not IMDS VM/VMSS;
- platform metadata or credential responses are incomplete;
- identity selection does not match IMDS metadata;
- KeyGuard or CNG operations fail;
- the bundled `Microsoft.Azure.Security.KeyGuardAttestation` 1.1.5 native
  library is missing, corrupt, lacks a valid Windows Authenticode signature,
  or cannot be loaded;
- attestation is empty, malformed, expired, or insufficiently fresh;
- the issued certificate does not match the KeyGuard public key;
- the token endpoint does not explicitly return `token_type=mtls_pop`;
- a configured custom HTTP client does not implement
  `IMtlsCapableHttpClient`.

Credential-bound token requests do not follow redirects. A custom HTTP client
must honor `HttpRequest.sslContext()` or `HttpRequest.sslSocketFactory()` and
preserve that no-redirect behavior.

The initial native package supports Windows x64. Windows ARM64 is not supported
by this release and fails before native loading with an architecture-specific
error.

Errors and logs must not contain access tokens, attestation JWTs, private-key
material, or native handles.

## Manual Key Vault validation

Set:

```powershell
$env:MSAL_JAVA_MTLS_AKV_URL = "https://<vault>.vault.azure.net"
$env:MSAL_JAVA_MTLS_AKV_SECRET_NAME = "<secret-name>"
$env:MSAL_JAVA_MTLS_IDENTITY_CLIENT_ID = "<optional-uami-client-id>"
$env:MSAL_JAVA_MTLS_MISMATCH_IDENTITY_CLIENT_ID = "<optional-second-uami-client-id>"
$env:MSAL_JAVA_MTLS_EXPECTED_SECRET_VALUE = "<optional-expected-value>"
$env:MSAL_JAVA_MTLS_FORCE_REFRESH = "true" # optional
$env:MSAL_JAVA_MTLS_TOKEN_ONLY = "true"    # optional: skip Key Vault call
```

Run:

```powershell
.\run-java-msi-v2-mtls-devapp.ps1
```

The app verifies:

- explicit `mtls_pop`;
- returned certificate and binding context;
- JWT `cnf.x5t#S256` equals the complete-certificate key ID;
- independent Java 8 `HttpsURLConnection` receives Key Vault HTTP 200;
- the same valid `mtls_pop` token without its binding certificate receives
  HTTP 401 `Unauthorized`;
- token A with binding B is rejected when a distinct second managed identity
  client ID is supplied;
- second acquisition is `TokenSource.CACHE` with the same binding generation;
- optional force refresh is `TokenSource.IDENTITY_PROVIDER` and still receives
  HTTP 200.
