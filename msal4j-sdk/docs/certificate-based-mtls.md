# Certificate-based mTLS

`ConfidentialClientApplication.Builder.sendCertificateOverMtls(true)` presents the
effective `IClientCertificate` to the token endpoint while retaining normal Bearer token
and cache semantics. For client-credential requests, a credential supplied through
`ClientCredentialParameters.clientCredential()` overrides the application credential and
the same resolved certificate generation drives both TLS and assertion signing. The
feature requires a concrete tenanted AAD authority in a supported cloud, routes to the
validated `mtlsauth` endpoint without redirects, and forces the certificate chain into
the assertion's `x5c` header for SN/I matching.

The result is an ordinary Bearer token and does not expose a binding certificate or
`IMtlsBindingContext`. Per-request `mtlsProofOfPossession()` takes precedence and instead
requests a certificate-bound `mtls_pop` token.

Custom HTTP clients must implement `IMtlsCapableHttpClient` and honor request-specific TLS
and redirect settings. A configured application `SSLSocketFactory` cannot be combined
safely with this option; unsupported clients, authorities, clouds, endpoints, redirects,
token types, and protocol parameter overrides fail closed.
