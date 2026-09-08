# SN/I certificate over mTLS Proof-of-Possession (PoP)

MSAL4J lets a confidential-client application present its **Subject-Name/Issuer (SN/I) certificate as
the client TLS certificate** during the mutual-TLS handshake to the token endpoint. Entra ID (ESTS)
then returns an access token that is **cryptographically bound to that certificate**
(`token_type=mtls_pop`, bound via `cnf` / `x5t#S256`), instead of a plain Bearer token.

The credential is exactly the same certificate you already use for SN/I authentication — only the
mechanism changes: instead of signing a `private_key_jwt` (x5c) client assertion, the certificate is
presented on the TLS handshake.

> mTLS PoP is a **confidential-client** feature. It is distinct from the broker-based Signed-HTTP-Request
> (SHR) PoP used by public clients (`token_type=pop`), which is unaffected.

## Enabling it

Opt in per request with `ClientCredentialParameters.Builder.mtlsProofOfPossession()`:

```java
ConfidentialClientApplication cca =
        ConfidentialClientApplication.builder(CLIENT_ID, sniCertificate)
                .authority("https://login.microsoftonline.com/<tenant>/") // tenanted authority required
                // .azureRegion("westus")   // OPTIONAL — omit for the global mtlsauth.microsoft.com endpoint
                .build();

ClientCredentialParameters parameters =
        ClientCredentialParameters
                .builder(Collections.singleton("https://graph.microsoft.com/.default"))
                .mtlsProofOfPossession()     // request an mTLS-bound PoP token
                .build();

IAuthenticationResult result = cca.acquireToken(parameters).join();

result.metadata().tokenType();            // TokenType.MTLS_POP
result.metadata().bindingCertificate();   // public material only: x5c chain + x5t#S256 thumbprint
```

The resulting token is bound to the certificate. To call a protected resource, present the **same
certificate** on the TLS handshake and send the token with the `mtls_pop` authorization scheme:

```
Authorization: mtls_pop <access_token>
```

`BindingCertificate` exposes public material only (the `x5c` chain and the SHA-256 thumbprint); the
private key is never exported — it is used in place through its own provider.

## Bearer vs. mTLS PoP

Without `mtlsProofOfPossession()`, the SN/I certificate behaves exactly as before: it **signs and sends
a `private_key_jwt` client assertion**, and the result is a `TokenType.BEARER` token with no binding
certificate. Omitting the client assertion (and presenting the certificate on the TLS handshake
instead) is **specific to the mTLS PoP path**; the legacy SNI + Bearer flow is unchanged.

Bearer and mTLS-PoP tokens for the same scope are cached separately (keyed on
`{token_type + certificate key id}`), so enabling PoP never aliases an existing Bearer entry.

## Requirements and notes

- **Tenanted authority is required.** `/common` and `/organizations` are rejected on the mTLS PoP path.
- **Region is optional.** Omit `azureRegion(...)` to use the global `mtlsauth.microsoft.com` endpoint
  (production-ready via ESTS-R regional failover), or set a region to target
  `<region>.mtlsauth.microsoft.com` (recommended when you know your region).
- **Resource audience must be allow-listed.** ESTS gates mTLS PoP on the **final resource audience**,
  which must be an ESTS mTLS-PoP allow-listed resource (for example Azure Key Vault or Microsoft Graph),
  not the client app itself.
- **Cloud support.** Azure Public, Azure Government, and the current national clouds are supported (the
  `login.*` authority host is rewritten to the matching `mtlsauth.*` host). Two legacy sovereign hosts
  that have no `mtlsauth.*` endpoint — `login.usgovcloudapi.net` and `login.chinacloudapi.cn` — are not
  supported and fail fast.

See also the runnable sample:
[`src/samples/confidential-client/ClientCredentialMtlsProofOfPossession.java`](../src/samples/confidential-client/ClientCredentialMtlsProofOfPossession.java),
and https://aka.ms/msal4j-pop.
