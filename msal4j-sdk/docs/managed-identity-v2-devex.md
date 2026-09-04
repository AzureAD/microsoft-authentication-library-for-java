# Managed Identity v2 Java DevEx

Managed Identity v2 keeps protocol, HTTP, TLS, and token caching in MSAL core.
Windows KeyGuard and Microsoft Azure Attestation support is supplied by the
optional `com.microsoft.azure:msal4j-key-attestation` artifact.

## Dependency

```xml
<dependency>
  <groupId>com.microsoft.azure</groupId>
  <artifactId>msal4j</artifactId>
  <version>${msal4j.version}</version>
</dependency>
<dependency>
  <groupId>com.microsoft.azure</groupId>
  <artifactId>msal4j-key-attestation</artifactId>
  <version>${msal4j.version}</version>
</dependency>
```

## Before and after

**Before:** attestation opt-in was incorrectly exposed from the core request
builder.

```java
ManagedIdentityParameters parameters =
        ManagedIdentityParameters.builder(resource)
                .withMtlsProofOfPossession()
                .withAttestationSupport()
                .build();
```

**After:** core selects the mTLS request mode; the optional package enables
attestation, matching the MSAL.NET package boundary.

```java
ManagedIdentityParameters.ManagedIdentityParametersBuilder builder =
        ManagedIdentityParameters.builder(resource)
                .withMtlsProofOfPossession();

ManagedIdentityParameters parameters =
        ManagedIdentityAttestationExtensions
                .withAttestationSupport(builder)
                .build();
```

## Choose the request mode

Use `withMtlsProofOfPossession()` for a certificate-bound access token. The
result includes a reusable `IMtlsBindingContext` for the downstream Java TLS
connection.

Use `withRequestOverMtls()` for an ordinary bearer token acquired over the
KeyGuard-authenticated TLS channel. The result intentionally has no binding
context.

The two modes are mutually exclusive.

## Check the host first

```java
ManagedIdentityCapabilities capabilities =
        application.getManagedIdentityCapabilities().get();

if (capabilities.maxSupportedBindingStrength()
        != MtlsBindingStrength.KEY_GUARD) {
    // Continue to the next credential in the application credential chain.
}
```

Use `MtlsPopOptions.minimumBindingStrength(...)` when token acquisition itself
must fail closed below a required binding strength.

## Disable Managed Identity v2

Set `MSAL_MI_DISABLE_IMDS_V2=true` (or `1`) and restart the process. Ordinary
managed identity bearer requests continue, but explicit PoP and
bearer-over-mTLS requests fail closed. Capability discovery reports
`MtlsBindingStrength.NONE`.

Certificates and binding contexts remain process-memory only. The KeyGuard key
container is persistent; no certificate is written to the Windows certificate
store.
