# MSAL Java - Copilot Instructions

## Quick Reference

- **Main Module**: `msal4j-sdk/` (focus here for most work)
- **Main Package**: `com.microsoft.aad.msal4j`
- **Three Application Types**: `PublicClientApplication`, `ConfidentialClientApplication`, `ManagedIdentityApplication`
- **Pattern**: Each auth flow has `*Parameters` (public API), `*Request` (internal), `*Supplier` (executor)
- **Current Version**: 1.25.0

---

## Project Overview

**Microsoft Authentication Library for Java (MSAL4J)** is a library that enables applications to integrate with the Microsoft identity platform. It provides APIs for acquiring security tokens from Azure Active Directory (Azure AD), Microsoft Accounts, and Azure AD B2C.

- **Language**: Java 8+
- **Build Tool**: Maven
- **Current Version**: 1.25.0
- **Artifact**: `com.microsoft.azure:msal4j`
- **Key Protocols**: OAuth2, OpenID Connect

### Core Capabilities
- Sign in users with Microsoft identities
- Acquire access tokens for protected APIs (Microsoft Graph, custom APIs)
- Token caching and automatic refresh
- Support for various authentication flows (interactive, silent, client credentials, on-behalf-of, device code, managed identity)
- mTLS Proof-of-Possession (PoP) tokens for confidential clients using an SN/I certificate (see Client Credentials)
- Multi-cloud and B2C support

### Repository Structure
This repository contains three Maven modules:
- **`msal4j-sdk/`** - The main MSAL Java library (focus of development)
- **`msal4j-brokers/`** - Broker integration for native authentication (Windows WAM)
- **`msal4j-persistence-extension/`** - Cross-platform token cache persistence helpers

For most work, focus on **`msal4j-sdk/`**.

---

## Architecture & Structure

### Application Hierarchy

MSAL4J provides three main application types, all extending from a common base:

```
AbstractApplicationBase (base for all applications)
├── AbstractClientApplicationBase (base for client applications)
│   ├── PublicClientApplication (desktop, mobile apps)
│   └── ConfidentialClientApplication (web apps, web APIs, daemons)
└── ManagedIdentityApplication (Azure managed identity)
```

**Key Classes:**
- `PublicClientApplication` - For apps that cannot securely store secrets (desktop/mobile). Supports interactive, device code, integrated Windows auth flows.
- `ConfidentialClientApplication` - For apps that can securely store secrets (web apps, APIs, daemons). Supports client credentials and on-behalf-of flows.
- `ManagedIdentityApplication` - For Azure resources using managed identities (VMs, App Service, Functions, etc.)

### Important Directories

**Main source code:**
- `msal4j-sdk/src/main/java/com/microsoft/aad/msal4j/` - All library code
  - Core application classes: `PublicClientApplication`, `ConfidentialClientApplication`, `ManagedIdentityApplication`
  - Token cache: `TokenCache`, `TokenCacheAccessContext`, cache entity classes
  - Authentication flows: `*Request.java`, `*Parameters.java`, `*Supplier.java` classes
  - Authority handling: `Authority`, `AADAuthority`, `B2CAuthority`, `ADFSAuthority`
  - Token request execution: `TokenRequestExecutor`, `OAuthHttpRequest`
  - HTTP layer: `HttpHelper`, `DefaultHttpClient`, `IHttpClient`
  - Exceptions: `MsalException` and subclasses

**Test code:**
- `msal4j-sdk/src/test/java/` - Unit tests
- `msal4j-sdk/src/integrationtest/java/` - Integration tests (require test infrastructure)

**Samples:**
- `msal4j-sdk/src/samples/` - Example applications demonstrating various scenarios

### Key Architectural Patterns

**Builder Pattern**: All application types use fluent builders (e.g., `PublicClientApplication.builder(clientId).authority(...).build()`)

**Request/Parameters Pattern**: Each token acquisition flow has:
- A `*Parameters` class (e.g., `InteractiveRequestParameters`) - User-facing API
- A `*Request` class (e.g., `InteractiveRequest`) - Internal request representation
- A `*Supplier` class (e.g., `AcquireTokenByInteractiveFlowSupplier`) - Executes the flow

**Token Flow Execution**:
1. Application receives `*Parameters` from developer
2. Converts to internal `MsalRequest` (via `*Request` classes)
3. Routes to appropriate `AuthenticationResultSupplier` implementation
4. Supplier checks cache, executes HTTP requests via `TokenRequestExecutor`
5. Returns `AuthenticationResult` with tokens and account info

**Token Cache**: 
- In-memory cache with five entity types: `AccessTokenCacheEntity`, `RefreshTokenCacheEntity`, `IdTokenCacheEntity`, `AccountCacheEntity`, `AppMetadataCacheEntity`
- Implements `ITokenCache` with serialization hooks via `ITokenCacheAccessAspect`
- Thread-safe with read-write locks

**Service Bundle**: Internal `ServiceBundle` class provides shared services (HTTP client, telemetry, executor service) to all requests

---

## Authentication Flows & Public APIs

MSAL4J supports multiple authentication flows, each with a public `*Parameters` class (used by developers) and an internal `*Supplier` class (executes the flow logic). Each flow is available only on specific application types.

### Public Client Flows (PublicClientApplication)

**Interactive Flow**
- **Public API**: `acquireToken(InteractiveRequestParameters)`
- **Parameters**: `InteractiveRequestParameters` - Opens system browser for user authentication
- **Internal**: `InteractiveRequest` → `AcquireTokenByInteractiveFlowSupplier`
- **Key Classes**: `HttpListener`, `AuthorizationResponseHandler`, `SystemBrowserOptions`

**Device Code Flow**
- **Public API**: `acquireToken(DeviceCodeFlowParameters)`
- **Parameters**: `DeviceCodeFlowParameters` - For devices without a browser (IoT, CLI tools)
- **Internal**: `DeviceCodeFlowRequest` → `AcquireTokenByDeviceCodeFlowSupplier`
- **Key Classes**: `DeviceCode`, device code consumer callback

**Username/Password (Deprecated)**
- **Public API**: `acquireToken(UserNamePasswordParameters)`
- **Parameters**: `UserNamePasswordParameters` - Resource Owner Password Credentials (ROPC) flow
- **Internal**: `UserNamePasswordRequest` → `AcquireTokenByAuthorizationGrantSupplier`
- **Note**: Deprecated and insecure, will be removed in future versions

**Integrated Windows Authentication**
- **Public API**: `acquireToken(IntegratedWindowsAuthenticationParameters)`
- **Parameters**: `IntegratedWindowsAuthenticationParameters` - Uses Windows authentication
- **Internal**: `IntegratedWindowsAuthenticationRequest` → `AcquireTokenByAuthorizationGrantSupplier`
- **Key Classes**: `WSTrustRequest`, `WSTrustResponse`

### Confidential Client Flows (ConfidentialClientApplication)

**Client Credentials**
- **Public API**: `acquireToken(ClientCredentialParameters)`
- **Parameters**: `ClientCredentialParameters` - App-only authentication (daemon apps)
- **Internal**: `ClientCredentialRequest` → `AcquireTokenByClientCredentialSupplier`
- **Key Classes**: `IClientCredential`, `ClientSecret`, `ClientCertificate`, `ClientAssertion`
- **mTLS Proof-of-Possession (PoP)**: Opt in with `ClientCredentialParameters.builder(...).mtlsProofOfPossession()` to obtain an mTLS-bound PoP token (`token_type=mtls_pop`). The app's SN/I `IClientCertificate` is presented as the client TLS certificate to a rewritten `mtlsauth.*` endpoint (no `client_assertion` on the direct path) instead of signing an x5c assertion (the existing SNI+Bearer path is unchanged). Requires a tenanted authority and an ESTS allow-listed resource; region is optional (global `mtlsauth.microsoft.com` when absent). The result exposes `metadata().tokenType()` and `metadata().bindingCertificate()` (public material only — x5c chain + `x5t#S256`). For 2-leg FIC over mTLS PoP, attach the binding cert to an assertion-authenticated app with `ConfidentialClientApplication.Builder.mtlsBindingCertificate(IClientCertificate)`.
  - **Key Classes**: `TokenType`, `BindingCertificate`, `MtlsClientCertificateHelper`, `MtlsEndpointHelper`; internal `AuthScheme`. Not supported: US Gov / China clouds, and user-scoped (`user_fic`) FIC over mTLS.

**On-Behalf-Of (OBO)**
- **Public API**: `acquireToken(OnBehalfOfParameters)`
- **Parameters**: `OnBehalfOfParameters` - Middle-tier service calls downstream API on behalf of user
- **Internal**: `OnBehalfOfRequest` → `AcquireTokenByOnBehalfOfSupplier`
- **Key Classes**: `UserAssertion` (incoming access token from user)

### Managed Identity Flow (ManagedIdentityApplication)

**Managed Identity**
- **Public API**: `acquireTokenForManagedIdentity(ManagedIdentityParameters)`
- **Parameters**: `ManagedIdentityParameters` - For Azure resources (VMs, App Service, Functions)
- **Internal**: `ManagedIdentityRequest` → `AcquireTokenByManagedIdentitySupplier`
- **Key Classes**: `ManagedIdentitySource` implementations (`IMDSManagedIdentitySource`, `AppServiceManagedIdentitySource`, etc.)

### Common Flows (All Application Types)

**Authorization Code**
- **Public API**: `acquireToken(AuthorizationCodeParameters)`
- **Parameters**: `AuthorizationCodeParameters` - Exchanges authorization code for tokens
- **Internal**: `AuthorizationCodeRequest` → `AcquireTokenByAuthorizationGrantSupplier`
- **Use Case**: Second step after authorization endpoint redirect

**Silent (Token Cache)**
- **Public API**: `acquireTokenSilently(SilentParameters)`
- **Parameters**: `SilentParameters` - Gets tokens from cache or refresh token
- **Internal**: `SilentRequest` → `AcquireTokenSilentSupplier`
- **Key Classes**: `TokenCache`, cache entity classes, `SilentRequestHelper`

**Refresh Token (Migration)**
- **Public API**: `acquireToken(RefreshTokenParameters)`
- **Parameters**: `RefreshTokenParameters` - Direct use of refresh token (for ADAL migration)
- **Internal**: `RefreshTokenRequest` → `AcquireTokenByAuthorizationGrantSupplier`
- **Use Case**: Migration scenarios from ADAL to MSAL

### Flow Execution Pattern

All flows follow this pattern:
1. Developer creates `*Parameters` object using builder
2. Calls `application.acquireToken(*Parameters)` or `application.acquireTokenSilently(*Parameters)`
3. Application creates `*Request` from parameters (contains `MsalRequest`)
4. Routes to appropriate `*Supplier` via `getAuthenticationResultSupplier()` in `AbstractApplicationBase`
5. Supplier checks cache, executes HTTP requests via `TokenRequestExecutor`
6. Returns `CompletableFuture<IAuthenticationResult>` with tokens

---

## Maintaining These Instructions

**IMPORTANT**: When you implement changes to the MSAL Java codebase, you must review and update this `copilot-instructions.md` file to keep it accurate and useful for future agents.

### When to Update This File

Update this file whenever you make changes that affect:

1. **Architecture & Structure**
   - Adding/removing application types or major classes
   - Changing the class hierarchy or inheritance structure
   - Modifying key architectural patterns
   - Reorganizing directory structure

2. **Authentication Flows & Public APIs**
   - Adding a new authentication flow (new `*Parameters` and `*Supplier` classes)
   - Deprecating or removing an existing flow
   - Changing the flow execution pattern
   - Modifying which application types support which flows

3. **Project Overview**
   - Updating version numbers (in pom.xml)
   - Adding/removing core capabilities
   - Changing supported Java versions or dependencies
   - Adding/removing modules from the repository

4. **Important Implementation Details**
   - Adding new key classes that are central to how the library works
   - Changing token cache architecture or entity types
   - Modifying HTTP layer or retry behavior
   - Updating exception handling patterns

### How to Update

1. **After making your changes**, review each section of this file
2. **Check for accuracy**: Do class names, file paths, and descriptions still match your changes?
3. **Add new information**: If you added a new flow or major component, document it following the existing format
4. **Mark deprecations**: If you deprecated a feature, note it clearly with a deprecation warning
5. **Keep it concise**: This file is for Copilot agents - prioritize clarity and navigation over comprehensive detail
6. **Test your updates**: Ensure the instructions would help another agent understand your changes

### Example Scenarios

- **Added a new auth flow?** → Add it to the "Authentication Flows & Public APIs" section with its Parameters class, Supplier class, and key classes
- **Changed the version in the pom.xml?** → Update the version in "Project Overview"
- **Refactored cache entities?** → Update the "Token Cache" description in "Key Architectural Patterns"
- **Added a new application type?** → Update "Application Hierarchy" and relevant flow sections
- **Moved files to new directories?** → Update the "Important Directories" section

By keeping these instructions current, you help ensure that future Copilot agents (and developers) can quickly understand and work with the MSAL Java library effectively.

---

