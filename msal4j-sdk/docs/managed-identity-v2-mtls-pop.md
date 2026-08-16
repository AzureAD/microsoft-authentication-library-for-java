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
ManagedIdentityParameters.builder("https://vault.azure.net")
        .withMtlsProofOfPossession()
        .withAttestationSupport()
        .build();
```

`withMtlsProofOfPossession()` can be used without attestation.
`withAttestationSupport()` requires mTLS PoP and makes any attestation failure
fail closed.

The returned `IMtlsBindingContext` contains an `SSLContext`, leaf certificate,
and complete-certificate key ID. It contains no token-acquisition helper and no
native HTTP surface.

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

## Failure behavior

The flow fails closed when:

- the optional extension is absent or ambiguous;
- the source is not IMDS VM/VMSS;
- platform metadata or credential responses are incomplete;
- identity selection does not match IMDS metadata;
- KeyGuard or CNG operations fail;
- the bundled `Microsoft.Azure.Security.KeyGuardAttestation` 1.1.5 native
  library is missing, corrupt, or cannot be loaded;
- attestation is empty, malformed, expired, or insufficiently fresh;
- the issued certificate does not match the KeyGuard public key;
- the token endpoint does not explicitly return `token_type=mtls_pop`;
- a configured custom HTTP client does not implement
  `IMtlsCapableHttpClient`.

Credential-bound token requests do not follow redirects. A custom HTTP client
must honor `HttpRequest.sslSocketFactory()` and preserve that no-redirect
behavior.

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
