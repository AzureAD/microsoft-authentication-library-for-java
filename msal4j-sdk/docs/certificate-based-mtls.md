# Certificate-based mTLS

`ConfidentialClientApplication.Builder.sendCertificateOverMtls(true)` presents the
effective `IClientCertificate` to the token endpoint while retaining normal Bearer token
and cache semantics. For client-credential requests, a credential supplied through
`ClientCredentialParameters.clientCredential()` overrides the application credential and
the same resolved certificate generation drives both TLS and assertion signing. The
feature requires a concrete tenanted AAD authority in a supported cloud, routes to the
validated `mtlsauth` endpoint without redirects, and forces the certificate chain into
the assertion's `x5c` header for SN/I matching.

When an Azure region is configured, client credentials, OBO, authorization-code
redemption, and refresh-token redemption use the corresponding regional
`{region}.mtlsauth` endpoint. Without this option, existing regional behavior is
unchanged; ordinary user flows continue to use the global token endpoint.

The result is an ordinary Bearer token and does not expose a binding certificate or
`IMtlsBindingContext`. Per-request `mtlsProofOfPossession()` takes precedence and instead
requests a certificate-bound `mtls_pop` token.

Custom HTTP clients must implement `IMtlsCapableHttpClient` and honor request-specific TLS
and redirect settings. A configured application `SSLSocketFactory` cannot be combined
safely with this option; unsupported clients, authorities, clouds, endpoints, redirects,
token types, and protocol parameter overrides fail closed.

## Certificate-bound federated client assertions

`ClientCredentialFactory.createFromSignedAssertionCallback(IClientSignedAssertionProvider)`
supports the second leg of a certificate-bound federated identity credential flow. The
callback returns a `ClientSignedAssertion` containing the already-signed assertion and,
optionally, the `IClientCertificate` that binds it to the token endpoint TLS connection. MSAL forwards the
assertion unchanged and does not perform leg-1 acquisition or re-signing.

| Binding certificate | `mtlsProofOfPossession()` | Assertion type | Access token |
|---|---|---|---|
| absent | no | `jwt-bearer` | `Bearer` over the regular endpoint |
| absent | yes | local `mtls_certificate_not_provided` failure | none |
| present | no | `jwt-pop` | `Bearer` over mTLS, with no result binding context |
| present | yes | `jwt-pop` | `mtls_pop` over mTLS, cache-partitioned by certificate DER |

As in MSAL.NET, the callback is invoked during preflight before cache lookup so MSAL can
discover the optional certificate. A cache hit invokes it once. A network request invokes
it again to obtain the assertion sent on the wire. Providers should therefore return
pre-generated or cached material.

Certificate-bound results are supported only for client-credentials requests. OBO,
authorization-code redemption, and refresh-token redemption may use this provider without
a binding certificate; returning a certificate for these grants fails locally with
`mtls_pop_error` before the token request is sent. These grants invoke the provider only
when a network request needs credential material, without client-credentials preflight.

Preflight fixes the TLS certificate and, for PoP, the certificate-specific cache partition.
The network callback supplies the assertion actually sent and selects its assertion type:

| Preflight certificate | Network callback certificate | Without PoP | With PoP |
|---|---|---|---|
| absent | absent | `jwt-bearer` over the regular endpoint | preflight fails with `mtls_certificate_not_provided` |
| absent | present | local `mtls_pop_error`; mTLS cannot be introduced after preflight | preflight fails with `mtls_certificate_not_provided` |
| present | absent | `jwt-bearer` over preflight's mTLS transport | local `mtls_certificate_not_provided` |
| present | present | `jwt-pop` over preflight's mTLS transport | `jwt-pop` over preflight's mTLS transport |

Providers must keep runtime assertions consistent with the preflight certificate. MSAL
does not compare certificate identities across invocations or guarantee exactly-once
callback execution. Rotate certificate/assertion material between acquisitions, not
between an acquisition's preflight and network callback.

`AssertionRequestOptions.tokenEndpoint()` identifies the configured authority endpoint
(with any tenant override) during preflight and the actual selected transport endpoint
during the network callback, including regional and `mtlsauth` routing. It does not
specify or rewrite the signed assertion's audience claim.

Without PoP, certificate-backed requests reject `token_type` and `req_cnf` overrides and
require a Bearer response. Unexpected bound tokens fail before being cached. With PoP,
a missing preflight certificate is reported before validating the mTLS authority, including
when a tenantless authority such as `common` is configured.

Java uses `IClientCertificate` instead of .NET's `X509Certificate2` so provider-backed and
non-exportable private keys remain usable through the existing Java certificate abstraction.
The Java callback is synchronous, matching the existing MSAL Java assertion-provider model.
The factory method has a distinct name because Java cannot safely overload the existing
`createFromCallback(Function<AssertionRequestOptions, String>)` with another single-argument
functional-interface callback without making existing lambdas ambiguous.
