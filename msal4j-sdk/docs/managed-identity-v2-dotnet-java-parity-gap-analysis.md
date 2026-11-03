# Managed Identity v2: MSAL.NET and MSAL Java parity gap analysis

This document compares the Managed Identity v2 implementation in MSAL.NET
`main` at `4392f63e5` (October 28, 2026) with the implementation proposed by MSAL Java PR
[#1059](https://github.com/AzureAD/microsoft-authentication-library-for-java/pull/1059)
including the October 2026 follow-up fixes described below.

The comparison is source-based. The Java implementation remains an open pull
request and is not a released Maven artifact. References to MSAL.NET describe
the current repository implementation and do not assert that every feature is
present in a particular released NuGet version.

## Executive conclusion

The Java implementation covers the primary Managed Identity v2 KeyGuard
protocol, with remaining lifecycle and capability-discovery differences:

```text
IMDS platform metadata
  -> non-exportable VBS KeyGuard key
  -> optional MAA attestation
  -> IMDS issuecredential
  -> ESTS request over mTLS
  -> mtls_pop or bearer token
```

The material remaining differences include:

1. MSAL.NET can persist an issued binding certificate in `CurrentUser\My` and
   reuse it across processes. Java intentionally keeps issued certificates and
   binding contexts in process memory.
2. MSAL.NET implements an optional total capability-discovery timeout after
   [#6181](https://github.com/AzureAD/microsoft-authentication-library-for-dotnet/pull/6181).
   Java does not yet propagate a discovery deadline through lock waits,
   HTTP calls, retries, and native key probing.

Java now matches .NET's rejected-binding recovery shape: it invalidates the
current generation, remints, and retries the token request once after
`invalid_client`, a TLS failure, or a recognized connection reset. It does not
downgrade to an ordinary bearer flow.

Explicit force refresh also remints the binding certificate, reusing the
current KeyGuard key and a still-valid attestation JWT. Attested minting selects
the regional MAA endpoint and sends tenant ID as MAA `client_id` telemetry.
These follow .NET
[#6215](https://github.com/AzureAD/microsoft-authentication-library-for-dotnet/pull/6215),
[#6238](https://github.com/AzureAD/microsoft-authentication-library-for-dotnet/pull/6238),
and [#6200](https://github.com/AzureAD/microsoft-authentication-library-for-dotnet/pull/6200).

This comparison does not imply parity with every confidential-client or FIC
mTLS API. This Java PR covers managed-identity acquisition and downstream JSSE
binding contexts, not the broader signed-assertion/mTLS transport additions
included in the Go integration PR.

Java additionally exposes a reusable `IMtlsBindingContext` containing standard
JSSE material for independent downstream Java HTTP calls. This is a
Java-specific integration advantage rather than a parity gap.

## Status definitions

| Status | Meaning |
| --- | --- |
| Full parity | Equivalent externally observable behavior |
| Java-specific parity | Equivalent security or protocol result using Java runtime mechanisms |
| Java advantage | Additional Java integration capability |
| Intentional difference | Deliberate platform-specific lifecycle choice |
| Partial parity | Main behavior exists, but lifecycle or resilience differs |
| Missing | Meaningful current MSAL.NET capability is absent |
| Future proposal | Not implemented in current MSAL.NET or Java |

## Public API and packaging

| Capability | MSAL.NET | MSAL Java PR #1059 | Status |
| --- | --- | --- | --- |
| mTLS PoP opt-in | `WithMtlsProofOfPossession()` | `withMtlsProofOfPossession()` | Full parity |
| Minimum binding strength | `PoPOptions.MinStrength` | `MtlsPopOptions.minimumBindingStrength(...)` | Full parity |
| Bearer token over attested mTLS | `WithRequestOverMtls()` | `withRequestOverMtls()` | Full parity |
| Request-mode exclusivity | PoP and bearer-over-mTLS are mutually exclusive | Same | Full parity |
| Attestation opt-in ownership | `Microsoft.Identity.Client.KeyAttestation` | `com.microsoft.azure:msal4j-key-attestation` | Full parity |
| Attestation API | `WithAttestationSupport()` extension | `ManagedIdentityAttestationExtensions.withAttestationSupport(builder)` | Java-specific parity |
| Platform provider extensibility | Internal attestation-provider delegate | Public `IManagedIdentityMtlsProvider` SPI | Java advantage |

The Java optional package has a broader responsibility than the .NET optional
package. .NET has built-in CNG and certificate integration, so its optional
package only needs to supply the attestation bridge. The JVM has no native CNG
key type, so the Java package owns JNA CNG interop, the JCA provider, CSR
generation, JSSE key management, binding lifecycle, and attestation.

## Protocol and cryptography

| Capability | MSAL.NET | MSAL Java PR #1059 | Status |
| --- | --- | --- | --- |
| Credential API version | `cred-api-version=2.0` | Same | Full parity |
| Platform metadata path | `/metadata/identity/getplatformmetadata` | Same | Full parity |
| Credential issuance path | `/metadata/identity/issuecredential` | Same | Full parity |
| CSR subject | `CN={clientId}, DC={tenantId}` | Same | Full parity |
| CUID attribute | OID `1.3.6.1.4.1.311.90.2.10`, DER UTF8 JSON | Same | Full parity |
| CSR signature | RSA-PSS SHA-256 with 32-byte salt | Same | Full parity |
| Key protection | VBS KeyGuard CNG key | Same through JNA and NCrypt | Java-specific parity |
| Private-key export | Not required | Private-key bytes never enter Java memory | Full parity |
| PoP token type | `mtls_pop` | `mtls_pop` | Full parity |
| Bearer-over-mTLS token type | `bearer` | `bearer` | Full parity |
| Claims and client capabilities | Merged through `TokenClient` | Merged through `TokenRequestExecutor` | Full parity |
| Correlation ID | IMDS, ESTS, and attestation diagnostics | Same | Full parity |

Java performs TLS through the standard JCA/JSSE path:

```text
JSSE
  -> X509ExtendedKeyManager
  -> CngRsaPrivateKey
  -> CngSignatureSpi
  -> NCryptSignHash
  -> VBS KeyGuard
```

Native code is limited to KeyGuard/CNG operations and
`AttestationClientLib.dll`. Java does not use WinHTTP or SCHANNEL as its HTTP
stack.

## Binding strength and capability discovery

| Capability | MSAL.NET | MSAL Java PR #1059 | Status |
| --- | --- | --- | --- |
| Strength taxonomy | `None=0`, `Software=1`, `KeyGuard=3`; value 2 reserved | `NONE=0`, `SOFTWARE=1`, `KEY_GUARD=3` | Full parity |
| Strength meaning | Host maximum; KeyGuard is the attested tier | Same | Full parity |
| Minimum-strength behavior | A floor assertion, not a downgrade selector | Same | Full parity |
| Capability result | Source, error reason, maximum strength, convenience Boolean | Same | Full parity |
| Concurrent discovery | Process-wide single-flight | Per-application synchronized future | Java-specific parity |
| Cancellation | Explicit `CancellationToken` | `CompletableFuture` cancellation surface | Partial parity |
| Negative-result caching | Explicit discovery result, including none, is cached | Transient IMDS/none results can be retried | Intentional difference |
| Total discovery timeout | Optional budget implemented by #6181 | Not implemented | Missing |

[MSAL.NET PR #6181](https://github.com/AzureAD/microsoft-authentication-library-for-dotnet/pull/6181)
implements the optional total capability-discovery budget proposed by issue
#6180. Java still needs a cancellation and transport-budget contract covering
lock wait, probes, retry delays, and fallback. Merely timing out the returned
future would leave the underlying operation running and would not close this gap.

## Token cache behavior

| Capability | MSAL.NET | MSAL Java PR #1059 | Status |
| --- | --- | --- | --- |
| PoP binding identity | Complete certificate DER SHA-256 (`x5t#S256`) | Same | Full parity |
| Attested/unattested partition | Separate | Separate | Full parity |
| Minimum-strength partition | Explicit strength floor participates in the key | Same | Full parity |
| Bearer-over-mTLS partition | Separate from ordinary bearer and PoP | Same | Full parity |
| Certificate renewal | New certificate creates a new PoP cache generation | Same | Full parity |
| Live proof serialization | Native proof object is not serialized | Same | Full parity |
| Cache-hit result | Binding certificate is restored through the authentication scheme | Matching live `IMtlsBindingContext` is reattached | Java-specific parity |
| Force refresh | Bypasses token and certificate caches; reuses current key and fresh MAA JWT | Same | Full parity |
| Claims refresh | Claims participate in token refresh and token request | Same | Full parity |
| Credential `bypass_cache` | Current source contains a TODO | Intentionally not sent | No current gap |

Java uses the complete leaf certificate DER as the binding key ID rather than
only the RSA public key. A renewed certificate therefore cannot accidentally
reuse a token minted for an older certificate, even when the underlying CNG key
container remains the same.

## Attestation

| Capability | MSAL.NET | MSAL Java PR #1059 | Status |
| --- | --- | --- | --- |
| Cache key | Normalized endpoint and key ID | Same | Full parity |
| Freshness buffer | Five minutes | Five minutes | Full parity |
| Concurrent calls | Per-key single-flight | Per-key single-flight | Full parity |
| Cache admission | Successful fresh token only | Same | Full parity |
| Configured attestation failure | Fails closed | Fails closed | Full parity |
| Regional endpoint | Compute environment/location selects Public, US Government, China, or Bleu endpoint | Same, with logged platform-endpoint fallback | Full parity |
| MAA `client_id` metadata | Tenant ID, without changing CSR/OAuth identity | Same | Full parity |
| Native test dependency | KeyGuardAttestation 1.1.8 | Test harness pins verified 1.1.8; application package remains separately supplied | Aligned test baseline |
| Native logging | MSAL logger bridge | SLF4J bridge with correlation ID | Java-specific parity |
| Raw debug payloads | May use the protected logger channel | Suppressed by Java | Intentional, more conservative |

The Java artifact does not bundle or download the native attestation package.
The application developer obtains, verifies, deploys and services it separately,
configuring its absolute DLL path with `MSAL_KEY_ATTESTATION_LIBRARY_PATH`.
MSAL validates Authenticode trust before loading that exact file; it does not
pin the application to a package version or publisher. Only the test harness
provisions a pinned, hash-verified native package outside the JAR.

## Certificate and key lifecycle

| Capability | MSAL.NET | MSAL Java PR #1059 | Status |
| --- | --- | --- | --- |
| Rotation buffer | 24 hours | 24 hours | Full parity |
| Process-memory cache | Yes | Yes | Full parity |
| CNG key container | Persisted KeyGuard container | Persisted KeyGuard container | Full parity |
| Issued certificate persistence | Best-effort `CurrentUser\My` | Process memory only | Intentional difference |
| Cross-process reuse | Supported | Not supported | Missing |
| Interprocess coordination | Alias-scoped short-timeout lock | Process-local keyed synchronization | Missing cross-process capability |
| Post-reboot stale key detection | Signing liveness plus cert/key orphan checks | Native signing liveness | Core parity |
| Old generation lifetime | Persistent store selection and pruning | Retired in-memory generations survive until certificate expiry | Java-specific parity |
| Rejected generation | Evicted before one remint retry | Invalidated and retired before one remint retry | Full parity |

Java does not need a Windows certificate-store entry for TLS. The binding
certificate is supplied directly to JSSE by `X509ExtendedKeyManager`, while the
corresponding private-key operation remains in KeyGuard.

## Rejected-certificate recovery

MSAL.NET recognizes:

- ESTS `invalid_client`;
- socket reset error 10054; and
- TLS authentication failures.

Both implementations then:

1. Remove the certificate from the memory cache.
2. Remove any reusable current entry for the rejected generation.
3. Forces a new CSR and `/issuecredential` call.
4. Retries the token request once.

Java classifies `invalid_client`, JSSE `SSLException` failures, and Java socket
reset messages corresponding to connection reset/Windows 10054. The provider
SPI invalidates the rejected generation, the KeyGuard provider retires it, and
core resolves a new binding and retries exactly once. The replacement
certificate identity is applied to subsequent PoP cache storage.

`bypass_cache=true` is not sent to `/issuecredential`; current MSAL.NET source
also retains this as a TODO, so it is not a current parity gap.

## HTTP and downstream integration

| Capability | MSAL.NET | MSAL Java PR #1059 | Status |
| --- | --- | --- | --- |
| Token-endpoint TLS | Platform HTTP/TLS with binding certificate | JSSE with KeyGuard-backed key manager | Java-specific parity |
| Custom transport guard | .NET HTTP abstractions | `IMtlsCapableHttpClient` checked before metadata or native work | Java-specific parity |
| Engine-based clients | Platform behavior | `chooseEngineClientAlias(...)` supports `SSLEngine` clients | Full Java requirement |
| Downstream binding material | `BindingCertificate` | `SSLContext`, key manager, certificate, key ID, strength | Java advantage |
| Credential-bound redirects | Transport behavior | Explicitly disabled | Full security parity |
| TLS protocol selection | Platform negotiation; no explicit TLS 1.2 restriction found | Explicit `TLSv1.2` context | Partial parity |

The Java result enables an application to use the exact binding generation with
`HttpsURLConnection`, JDK `HttpClient`, or another JSSE-aware transport without
exposing or copying the private key.

## Kill switch

[MSAL.NET PR #6178](https://github.com/AzureAD/microsoft-authentication-library-for-dotnet/pull/6178)
introduced `MSAL_MI_DISABLE_IMDS_V2`. Java implements the same behavior.

| Behavior | MSAL.NET | MSAL Java PR #1059 |
| --- | --- | --- |
| Accepted disable values | `true`, case-insensitive, or `1` | Same |
| Other values | Ignored | Same |
| Ordinary bearer | Continues through IMDS v1 | Same |
| Explicit PoP | Fails instead of downgrading | Same |
| Bearer-over-mTLS | Fails instead of downgrading | Same |
| Capability discovery | Reports no available binding strength | Same |
| External environment update | Requires process restart or recycle | Same |

## Platform and release maturity

| Area | MSAL.NET | MSAL Java PR #1059 | Assessment |
| --- | --- | --- | --- |
| Operating system | Windows for the KeyGuard flow | Windows | Full parity |
| Runtime architecture | Platform/package dependent | Application-supplied attestation library integration is Windows x64 only | Java gap for ARM64 attestation |
| Java compatibility | Not applicable | Java 8 source and target compatibility | Meets repository requirement |
| Release status | Compared against current `main` | Open PR, not published to Maven Central | Not released |
| PR checks at comparison time | Not applicable | Local full reactor tests and E2E packaging pass; a dedicated IMDS v2 job is wired into the PR pipeline | Pending hosted VM validation |
| Review state at comparison time | Not applicable | Review fixes implemented; maintainer approval remains | Pending approval |

## Historical live Java validation

The earlier implementation was run on a Windows Server 2025 Trusted Launch Azure
VM with Secure Boot, vTPM, VBS/KeyGuard, a system-assigned identity, and an
attached user-assigned identity. These results predate the external-DLL loading
change and the October follow-up fixes; they are not evidence of a current-head
cloud rerun.

The PR pipeline has an IMDS v2 job on the `MISEManagedIdentity` VM pool. It
acquires an attested `mtls_pop` token, verifies the certificate
  thumbprint binding, calls the token-bound Key Vault with the returned JSSE
  binding context, verifies rejection without the certificate, and proves cache
  reuse preserves the binding generation.

Managed Identity v1 and Azure Arc E2E coverage are tracked separately so this
PR remains scoped to Managed Identity v2.

| Scenario | Result |
| --- | --- |
| KeyGuard creation and MAA attestation | Passed |
| IMDS v2 certificate issuance | Passed |
| Attested `mtls_pop` acquisition | Passed |
| JWT `cnf.x5t#S256` matched the leaf certificate DER hash | Passed |
| Binding-context key ID matched the certificate identity | Passed |
| Independent `HttpsURLConnection` call to token-bound Key Vault | HTTP 200 |
| Same token without the binding certificate | HTTP 401 |
| Token-cache hit with live binding reattachment | Passed |
| Force refresh followed by another Key Vault call | Identity provider, HTTP 200 |
| Bearer-over-mTLS | Bearer token, no downstream binding context |
| Bearer-over-mTLS second acquisition | Cache hit |
| IMDS v2 kill switch | Failed before native attestation work |

That historical run exercised the JSSE-to-KeyGuard signing path, not only the
IMDS and token protocol. The new local regressions cover force-remint cache
eviction (including failed issuance), key/JWT reuse, regional endpoint and
tenant-metadata selection, early transport rejection, and ordinary RSA-PSS
signing/verification after the CNG provider is installed. They do not replace
live IMDS/MAA/ESTS/Key Vault validation.

## Remaining work before release

| Priority | Recommendation |
| --- | --- |
| P0 | Complete required CI and maintainer review |
| P0 | Re-run the manual KeyGuard VM flow and exercise rejected-binding recovery against a real token-leg failure |
| P1 | Validate TLS 1.3 service behavior and remove the TLS 1.2 restriction if safe |
| P1 | Add Windows ARM64 packaging if required by the supported-host matrix |
| P2 | Close the current .NET #6181 capability-discovery timeout gap with a Java cancellation/transport-budget design |
| Intentional | Retain process-memory-only certificate handling unless operational data justifies cross-process persistence |

## Key source locations

### MSAL.NET

- `src/client/Microsoft.Identity.Client/ManagedIdentity/ManagedIdentityPopExtensions.cs`
- `src/client/Microsoft.Identity.Client/Internal/Requests/ManagedIdentityAuthRequest.cs`
- `src/client/Microsoft.Identity.Client/ManagedIdentity/ManagedIdentityClient.cs`
- `src/client/Microsoft.Identity.Client/ManagedIdentity/V2/ImdsV2ManagedIdentitySource.cs`
- `src/client/Microsoft.Identity.Client/ManagedIdentity/V2/MtlsCertificateCache.cs`
- `src/client/Microsoft.Identity.Client/ManagedIdentity/V2/WindowsPersistentCertificateCache.cs`
- `src/client/Microsoft.Identity.Client.KeyAttestation/ManagedIdentityAttestationExtensions.cs`
- `src/client/Microsoft.Identity.Client.KeyAttestation/PopKeyAttestor.cs`

### MSAL Java PR #1059

- `msal4j-sdk/src/main/java/com/microsoft/aad/msal4j/ManagedIdentityParameters.java`
- `msal4j-sdk/src/main/java/com/microsoft/aad/msal4j/AcquireTokenByManagedIdentitySupplier.java`
- `msal4j-sdk/src/main/java/com/microsoft/aad/msal4j/ManagedIdentityApplication.java`
- `msal4j-sdk/src/main/java/com/microsoft/aad/msal4j/IMtlsBindingContext.java`
- `msal4j-mtls-extensions/src/main/java/com/microsoft/aad/msal4j/mtls/ManagedIdentityAttestationExtensions.java`
- `msal4j-mtls-extensions/src/main/java/com/microsoft/aad/msal4j/mtls/KeyGuardManagedIdentityMtlsProvider.java`
- `msal4j-mtls-extensions/src/main/java/com/microsoft/aad/msal4j/mtls/CngKeyGuard.java`
- `msal4j-mtls-extensions/src/main/java/com/microsoft/aad/msal4j/mtls/CngSignatureSpi.java`
- `msal4j-mtls-extensions/src/main/java/com/microsoft/aad/msal4j/mtls/AttestationTokenCache.java`
