# KeyGuard Key Creation — JVM Feasibility Analysis

## Summary

**Java cannot natively create or use KeyGuard keys on Windows.** The root cause is the same as for Node.js/OpenSSL: Java's Windows TLS/crypto integration (`SunMSCAPI`) uses the legacy **CryptoAPI (CAPI)** rather than the modern **CNG (Cryptography API: Next Generation)**. KeyGuard keys are CNG-only.

The only viable architecture for Managed Identity mTLS PoP in Java is the same subprocess approach used by msal-node: spawning `MsalMtlsMsiHelper.exe` (a .NET 8 binary) which uses Schannel and CNG natively.

This document records the investigation into whether a JNI native addon could replace the subprocess, including a detailed breakdown of exactly where the pipeline breaks.

---

## Background: What KeyGuard Keys Are

KeyGuard keys are **hardware-isolated private keys** stored in a Windows CNG key storage provider (KSP) with Virtualization Based Security (VBS) isolation. They are created via:

```c
NCryptCreatePersistedKey(
    hProvider,
    &hKey,
    BCRYPT_RSA_ALGORITHM,
    keyName,
    0,
    NCRYPT_VBS_KEYISOLATION_FLAG  // <- CNG-only flag
);
```

Once created, the key material never leaves the VBS enclave. Applications reference the key via an `NCRYPT_KEY_HANDLE`. The TLS stack (Schannel) accepts an `NCRYPT_KEY_HANDLE` directly and uses it for signing operations without needing raw key bytes.

---

## Java's Windows Crypto Stack

### Provider Inventory (JDK 21)

On a standard Windows system running JDK 21:

| Provider | Algorithm Coverage | Backend |
|---|---|---|
| `SUN` | JCA core | Pure Java |
| `SunRsaSign` | RSA signature | Pure Java |
| `SunEC` | EC key/signature | Pure Java |
| `SunJSSE` | TLS/SSL | Pure Java + OS TLS via `SunMSCAPI` delegation for Windows cert store only |
| `SunMSCAPI` | `KeyStore.Windows-MY`, `KeyPairGenerator.RSA` | **Windows CAPI (`CryptAcquireContext`, `CryptGenKey`, `CryptSignMessage`)** |
| `SunJCE` | AES, HMAC, etc. | Pure Java |

Notably absent: **No `SunCNG` or equivalent CNG-aware provider exists in any JDK distribution** (Oracle, OpenJDK, Microsoft Build of OpenJDK, Azul, etc.).

### `SunMSCAPI` Internal Implementation

`SunMSCAPI` in JDK 21 (source: `jdk/src/windows/native/sun/security/mscapi/security.cpp`) calls:

```c
// Key generation
CryptAcquireContext(&hCryptProv, keyName, NULL, PROV_RSA_AES, 0);
CryptGenKey(hCryptProv, AT_KEYEXCHANGE, CRYPT_EXPORTABLE | keySize, &hCryptKey);
```

This is the **CAPI path**. There is no call to `NCryptOpenStorageProvider`, `NCryptCreatePersistedKey`, or any other `NCrypt*` function.

The `CryptAcquireContext` with `PROV_RSA_AES` provider type specifically targets CAPI's RSA/AES provider, not a CNG KSP. CAPI and CNG are separate subsystems; CAPI has no mechanism to create keys with `NCRYPT_VBS_KEYISOLATION_FLAG`.

---

## Why a JNI Addon Cannot Fully Replace the Subprocess

A JNI C++ addon could theoretically handle the CNG-specific operations. Here is a step-by-step breakdown of the Managed Identity mTLS PoP pipeline and where JNI can and cannot help:

### MI mTLS PoP Pipeline

| Step | Operation | JNI Possible? | Notes |
|------|-----------|--------------|-------|
| 1 | IMDS `getplatformmetadata` | ✅ Java HTTP | Standard HTTP, no JNI needed |
| 2 | Create KeyGuard key | ✅ JNI (NCrypt) | `NCryptCreatePersistedKey` + `NCRYPT_VBS_KEYISOLATION_FLAG` |
| 3 | Generate CSR | ✅ JNI (NCrypt + CertEnroll) | `IX509CertificateRequestPkcs10` via `CertEnroll.dll` |
| 4 | MAA attestation (optional) | ✅ JNI | Calls `AttestationClientLib.dll` |
| 5 | IMDS `/issuecredential` | ✅ Java HTTP | Standard HTTP with CSR in body |
| 6 | Parse issued cert from response | ✅ Java | Standard X.509 parsing |
| 7 | **mTLS token request (TLS with KeyGuard key)** | ❌ **BLOCKED** | See below |

### Step 7 Breakdown: Why TLS Is Blocked

The mTLS token request to `mtlsauth.microsoft.com` must use the KeyGuard-backed private key for the TLS client certificate. This is where the JNI path fails:

**Option A: Use Java's `JSSE`**

```
JSSE SSLContext → KeyManager → KeyManagerFactory → JCE Signature engine
                                                    → needs raw PrivateKey object
                                                    → calls key.getEncoded() or sign() via JCE
```

JSSE's `SunX509KeyManager` requires a `PrivateKey` object from the Java security API. Even if a JNI addon wraps the `NCRYPT_KEY_HANDLE` in a `PrivateKey` implementation, the underlying `java.security.Signature` provider (`SunRsaSign` or `SunEC`) calls `engineInitSign(PrivateKey)` and expects either:
- A `RSAPrivateCrtKeyImpl` (extracts raw key bytes via `key.getEncoded()`)
- Or a `PKCS11Key` from `SunPKCS11` (calls PKCS11 `C_Sign`)

There is no standard SPI to "plug in" an `NCRYPT_KEY_HANDLE` as a signing backend. A custom `Provider` + `KeySpi` + `SignatureSpi` could be written, but:
- It would need to JNI into `NCryptSignHash` for each TLS handshake
- JSSE does not expose a hook to inject a custom `SSLEngine` signing path
- The `SSLSocketFactory` → `SSLSocket` → `SSLEngine` chain calls `KeyManager.chooseClientAlias` and then uses the returned `PrivateKey` through JCE — the same dead end

**Option B: Use a JNI-wrapped `WinHTTP` or Schannel**

A JNI addon could use `WinHttpSetOption(WINHTTP_OPTION_CLIENT_CERT_CONTEXT, ...)` with a cert context that references the `NCRYPT_KEY_HANDLE`. This would work at the Win32 level, but:
- It bypasses `URLConnection`, `HttpsURLConnection`, and all JSSE abstractions
- The response would have to be parsed from raw Win32 API output
- Building a compliant HTTP/1.1 client (redirects, connection pooling, header parsing) on top of raw `WinHTTP` is effectively reimplementing a full HTTP stack in JNI — months of work, not weeks

**Option C: Use the Existing JNI Path in `SunMSCAPI`**

`SunMSCAPI` can import certificates from the Windows `MY` store and return a `PrivateKey` that delegates signing to CAPI. But CAPI keys stored via `CryptImportKey` or `CryptGenKey` are not CNG keys. There is no CAPI API to import a key identified only by a `NCRYPT_KEY_HANDLE` — these are different object types in different subsystems.

### What .NET Does Differently

.NET's `HttpClientHandler` with `ClientCertificates`:

```
HttpClientHandler.ClientCertificates → SslStream → Schannel
                                                    → NCRYPT_KEY_HANDLE (from X509Certificate2)
                                                    → NCryptSignHash (called by Schannel internally)
```

Schannel accepts an `NCRYPT_KEY_HANDLE` directly via `SCHANNEL_CRED` or `SCH_CREDENTIALS`. The key material never leaves the VBS enclave — Schannel calls `NCryptSignHash` with the opaque handle, and the actual signing happens inside the enclave.

Java's JSSE has no equivalent of `SCHANNEL_CRED`. JSSE runs TLS entirely in the JVM's managed memory, which means key material must flow into Java objects where the JVM GC can observe it. This is architecturally incompatible with hardware-isolated keys.

---

## Conclusion

| Approach | Feasibility | Notes |
|---|---|---|
| Pure Java (JSSE + SunMSCAPI) | ❌ Impossible | SunMSCAPI uses CAPI, not CNG |
| JNI addon for steps 1–6 only | ✅ Possible but incomplete | Cannot solve step 7 (TLS with KeyGuard key) |
| JNI + custom Schannel HTTP client | 🟡 Theoretically possible | ~3-6 months, enormous scope, maintenance burden |
| Subprocess (`MsalMtlsMsiHelper.exe`) | ✅ **Implemented** | Same approach as msal-node; .NET 8 handles all CNG/Schannel steps |

The subprocess approach is the **only practical architecture** that:
- Works today
- Requires minimal Java code
- Correctly handles KeyGuard key creation, MAA attestation, and CNG-backed TLS
- Is consistent with the approach taken by msal-node

A future JNI addon would be worthwhile only if the goal is to eliminate the .NET runtime dependency on Azure VMs, and only if a maintainer is willing to own a full Schannel-based HTTP client implementation in C++.

---

## References

- [Windows CNG Key Storage Provider](https://learn.microsoft.com/en-us/windows/win32/seccng/key-storage-and-retrieval)
- [NCRYPT_VBS_KEYISOLATION_FLAG](https://learn.microsoft.com/en-us/windows/win32/api/ncrypt/nf-ncrypt-ncryptcreatepersistedkey)
- [SunMSCAPI source (OpenJDK)](https://github.com/openjdk/jdk/blob/master/src/jdk.crypto.mscapi/windows/native/libsunmscapi/security.cpp)
- [msal-node KeyGuard NAPI Analysis](https://github.com/AzureAD/microsoft-authentication-library-for-js/tree/dev/extensions/msal-node-mtls-extensions)
- [Schannel CNG key handle usage](https://learn.microsoft.com/en-us/windows/win32/secauthn/tls-handshake-protocol)
