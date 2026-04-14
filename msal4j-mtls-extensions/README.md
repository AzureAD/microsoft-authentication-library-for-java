# msal4j-mtls-extensions

This extension enables mTLS Proof-of-Possession (mTLS PoP) token acquisition for Azure Managed Identity in Java applications. It uses [JNA](https://github.com/java-native-access/jna) to call Windows CNG (`ncrypt.dll`) directly, creating and using KeyGuard-isolated private keys in-process — no .NET runtime or subprocess required.

The latest code resides in the `dev` branch.

Quick links:

| [Docs](../../msal4j-sdk/docs/mtls-pop.md) | [Manual Testing](../../msal4j-sdk/docs/mtls-pop-manual-testing.md) | [Architecture](../../msal4j-sdk/docs/mtls-pop-architecture.md) | [Support](README.md#community-help-and-support) |
| --- | --- | --- | --- |

## Installation

### Requirements

- Windows x64 Azure VM with Managed Identity enabled
- Java 8 or higher
- `AttestationClientLib.dll` on `PATH`, when using Trusted Launch VMs with attestation (see [Attestation DLL](#attestationclientlibdll) below)

### Adding the dependency

```xml
<dependency>
    <groupId>com.microsoft.azure</groupId>
    <artifactId>msal4j-mtls-extensions</artifactId>
    <version>1.0.0</version>
</dependency>
```

`msal4j-mtls-extensions` depends on `msal4j` transitively — you do not need to declare `msal4j` separately.

### AttestationClientLib.dll

On Trusted Launch VMs, the IMDS `/issuecredential` endpoint requires a MAA attestation JWT proving the key was created in a VBS-isolated enclave. This JWT is produced by `AttestationClientLib.dll`, distributed via the `Microsoft.Azure.Security.KeyGuardAttestation` NuGet package.

To obtain the DLL:

```powershell
# Download the NuGet package
dotnet add package Microsoft.Azure.Security.KeyGuardAttestation --package-directory C:\nuget

# Copy the DLL next to your application or to a directory on PATH
$dll = Get-ChildItem C:\nuget\microsoft.azure.security.keyguardattestation -Recurse -Filter "AttestationClientLib.dll" | Select-Object -First 1
Copy-Item $dll.FullName C:\your-app\
```

Unlike msal-dotnet, which receives this DLL automatically via NuGet, Java applications must place it on `PATH` or in the application directory. If your VM does not use Trusted Launch, pass `withAttestation: false` and no DLL is needed.

## Usage

Before using this extension, ensure Managed Identity is enabled on your Azure VM.

### Path 2 — Managed Identity: Acquiring an mTLS PoP Token

Acquiring a token follows this general pattern:

1. Create a client and call `acquireToken()`.

   * System-assigned Managed Identity:

     ```java
     import com.microsoft.aad.msal4j.mtls.*;

     MtlsMsiClient client = new MtlsMsiClient();
     MtlsMsiHelperResult result = client.acquireToken(
         "https://graph.microsoft.com",    // resource (confirmed enrolled for mTLS PoP)
         "SystemAssigned",                  // identity type
         null,                              // identity id (null for system-assigned)
         false,                             // withAttestation — set true on Trusted Launch VMs
         null                               // correlationId (optional)
     );
     String accessToken = result.getAccessToken();
     ```

   * User-assigned Managed Identity:

     ```java
     MtlsMsiHelperResult result = client.acquireToken(
         "https://graph.microsoft.com",
         "UserAssigned",
         "your-client-id",
         false,
         null
     );
     String accessToken = result.getAccessToken();
     ```

   > **Resource note:** Use `https://graph.microsoft.com` or `https://storage.azure.com`. `https://management.azure.com` may return `AADSTS392196` if that resource is not enrolled for mTLS PoP in your tenant.

2. The binding certificate is cached in-process for the lifetime of the IMDS-issued certificate (minus a 5-minute safety margin). Subsequent calls return the cached token until it nears expiry.

### Path 2 — Making Downstream mTLS Calls

Once you have a token, use `httpRequest()` to make downstream calls over the same KeyGuard-backed mTLS channel:

```java
MtlsMsiHttpResponse response = client.httpRequest(
    "https://myservice.example.com/api",  // URL
    "GET",                                 // method
    result.getAccessToken(),               // bearer token
    null,                                  // body
    null,                                  // contentType
    null,                                  // extra headers
    "https://graph.microsoft.com",         // resource (for cert refresh)
    "SystemAssigned", null,                // identity type, identity id
    false,                                 // withAttestation
    null,                                  // correlationId
    false                                  // allowInsecureTls
);
System.out.println(response.getStatus()); // e.g. 200
System.out.println(response.getBody());
```

The downstream server must be configured to *require* mutual TLS — it must send a TLS `CertificateRequest` during the handshake.

### Path 1 — Confidential Client (SNI Certificate)

For applications with an SNI certificate (e.g., from OneCert/DSMS), use `ConfidentialClientApplication` from the core `msal4j` library:

```java
import com.microsoft.aad.msal4j.*;
import java.io.FileInputStream;

// 1. Load your certificate (PKCS12)
IClientCertificate cert = ClientCredentialFactory.createFromCertificate(
    new FileInputStream("cert.p12"), "password");

// 2. Build the app — tenanted authority and region required
ConfidentialClientApplication app = ConfidentialClientApplication
    .builder("your-client-id", cert)
    .authority("https://login.microsoftonline.com/your-tenant-id")
    .azureRegion("centraluseuap")
    .build();

// 3. Acquire an mTLS PoP token
IAuthenticationResult result = app.acquireToken(
    ClientCredentialParameters
        .builder(Collections.singleton("https://graph.microsoft.com/.default"))
        .withMtlsProofOfPossession()
        .build()
).get();

System.out.println("Token type:    " + result.tokenType());       // "mtls_pop"
System.out.println("Binding cert:  " + result.bindingCertificate().getSubjectX500Principal());
System.out.println("Access token:  " + result.accessToken());
```

**Requirements:** Certificate credential, tenanted authority (not `/common` or `/organizations`), Azure region.

---

## End-to-End Test Driver

The `msal4j-mtls-extensions` module ships an e2e fat JAR for manual testing:

```powershell
# Build
mvn package -DskipTests

# Path 1 — error-case validation (no Azure credentials required)
java -jar target\msal4j-mtls-extensions-1.0.0-e2e.jar path1 --errors-only

# Path 1 — full happy path
java -jar target\msal4j-mtls-extensions-1.0.0-e2e.jar path1 `
    --tenant <tenantID> --client <clientID> --region centraluseuap

# Path 2 — Managed Identity (with attestation)
java -Djava.library.path=C:\msiv2 `
    -jar target\msal4j-mtls-extensions-1.0.0-e2e.jar path2 --attest
```

See [Manual Testing Guide](../../msal4j-sdk/docs/mtls-pop-manual-testing.md) for full instructions.

## Community Help and Support

We use [Stack Overflow](http://stackoverflow.com/questions/tagged/msal) to work with the community on supporting Azure Active Directory and its SDKs, including this one! We highly recommend you ask your questions on Stack Overflow (we're all on there!) Also browse existing issues to see if someone has had your question before. Please use the "msal" tag when asking your questions.

If you find a bug or have a feature request, please raise the issue on [GitHub Issues](https://github.com/AzureAD/microsoft-authentication-library-for-java/issues).

## Submit Feedback

We'd like your thoughts on this library. Please complete [this short survey.](https://forms.office.com/r/6AhHwQp3pe)

## Contributing

This project welcomes contributions and suggestions. Most contributions require you to agree to a Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us the rights to use your contribution. For details, visit https://cla.opensource.microsoft.com.

When you submit a pull request, a CLA bot will automatically determine whether you need to provide a CLA and decorate the PR appropriately (e.g., status check, comment). Simply follow the instructions provided by the bot. You will only need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

## Security Library

This library controls how users sign in and access services. We recommend you always take the latest version of our library in your app when possible. We use [semantic versioning](http://semver.org) so you can control the risk associated with updating your app. As an example, always downloading the latest minor version number (e.g. x.*y*.x) ensures you get the latest security and feature enhancements but our API surface remains the same. You can always see the latest version and release notes under the Releases tab of GitHub.

## Security Reporting

If you find a security issue with our libraries or services please report it to [secure@microsoft.com](mailto:secure@microsoft.com) with as much detail as possible. Your submission may be eligible for a bounty through the [Microsoft Bounty](http://aka.ms/bugbounty) program. Please do not post security issues to GitHub Issues or any other public site. We will contact you shortly upon receiving the information. We encourage you to get notifications of when security incidents occur by visiting [this page](https://technet.microsoft.com/en-us/security/dd252948) and subscribing to Security Advisory Alerts.

Copyright (c) Microsoft Corporation. All rights reserved. Licensed under the MIT License (the "License").
