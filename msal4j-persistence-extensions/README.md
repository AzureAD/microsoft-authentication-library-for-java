# Microsoft Authentication Extensions for Java

The Microsoft Authentication Extensions for Java offers secure mechanisms for client applications to perform cross-platform token cache serialization and persistence. It gives additional support to the [Microsoft Authentication Library for Java (MSAL)](https://github.com/AzureAD/microsoft-authentication-library-for-java). 

MSAL Java supports an in-memory cache by default and provides the `ITokenCacheAccessAspect` interface to perform cache serialization. You can read more about this in the MSAL Java [documentation](https://docs.microsoft.com/en-us/azure/active-directory/develop/msal-java-token-cache-serialization). Developers are required to implement their own cache persistance across multiple platforms and Microsoft Authentication Extensions makes this simpler.
The extensions library supports persistence of the cache to disk in encrypted form where the platform supports this.

This is available for Windows, Mac and Linux.
- Windows - [DPAPI](https://docs.microsoft.com/en-us/dotnet/standard/security/how-to-use-data-protection) is used for encryption.
- MAC - The MAC KeyChain is used and encryption is built in.
- Linux - [LibSecret](https://wiki.gnome.org/Projects/Libsecret) is used for encryption. Any place where libsecret is not available encryption is not supported.

> Note: It is recommended to use this library for cache persistance support for Public client applications such as Desktop apps only. In web applications, this may lead to scale and performance issues. Web applications are recommended to persist the cache in session. Take a look at this [webapp sample](https://github.com/Azure-Samples/ms-identity-java-webapp).

## Installation

You can find the latest pacakge in the [Maven repository](https://mvnrepository.com/artifact/com.microsoft.azure/msal4j-persistence-extension).

Add the dependecy to the pom file.

```
<dependency>
    <groupId>com.microsoft.azure</groupId>
    <artifactId>msal4j-persistence-extension</artifactId>
    <version>1.2.0</version>
</dependency>
```

## Versions

This library follows [Semantic Versioning](http://semver.org/).

## Usage

[![javadoc](https://javadoc.io/badge2/com.microsoft.azure/msal4j-persistence-extension/javadoc.svg)](https://javadoc.io/doc/com.microsoft.azure/msal4j-persistence-extension)

The Microsoft Authentication Extensions library provides the `PersistenceTokenCacheAccessAspect` which is an implementation of the `ITokenCacheAccessAspect` interface defined in MSAL Java. After configuring this token cache, it can then be used to instantiate the client application in MSAL Java. 

The token cache includes a file lock, and auto-reload behavior under the hood.

Here is the usage pattern for multiple platforms:

1. Configure the `PersistenceSettings`.

    ```java
    private PersistenceSettings createPersistenceSettings() throws IOException {

        Path path = Paths.get(System.getProperty("user.home"), "MSAL", "testCache");

        return PersistenceSettings.builder("testCacheFile", path)
                .setMacKeychain("MsalTestService", "MsalTestAccount")
                .setLinuxKeyring(null,
                        "MsalTestSchema",
                        "MsalTestSecretLabel",
                        "MsalTestAttribute1Key",
                        "MsalTestAttribute1Value",
                        "MsalTestAttribute2Key",
                        "MsalTestAttribute2Value")
                .setLockRetry(1000, 50)
                .build();
    }
    ```

1. Create the `PersistenceTokenCacheAccessAspect`.

    ```java
    private ITokenCacheAccessAspect createPersistenceAspect() throws IOException {
        return new PersistenceTokenCacheAccessAspect(createPersistenceSettings());
    }
    ```
1. Now you can use `PersistenceTokenCacheAccessAspect` to configure Msal client application:

    ```java
    return PublicClientApplication.builder(PUBLIC_CLIENT_ID)
        .authority(AUTHORITY)
        .setTokenCacheAccessAspect(createPersistenceAspect())
        .build();
    ```

## Community Help and Support

We leverage Stack Overflow to work with the community on supporting Azure Active Directory and its SDKs, including this one!
We highly recommend you ask your questions on Stack Overflow (we're all on there!).
Also browse existing issues to see if someone has had your question before.

We recommend you use the "msal" tag so we can see it!
Here is the latest Q&A on Stack Overflow for MSAL:
[http://stackoverflow.com/questions/tagged/msal](http://stackoverflow.com/questions/tagged/msal)


## Contributing

All code is licensed under the MIT license and we triage actively on GitHub.

This project welcomes contributions and suggestions.  Most contributions require you to agree to a
Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us
the rights to use your contribution. For details, visit https://cla.microsoft.com.

When you submit a pull request, a CLA-bot will automatically determine whether you need to provide
a CLA and decorate the PR appropriately (e.g., label, comment). Simply follow the instructions
provided by the bot. You will only need to do this once across all repos using our CLA.


## We value and adhere to the Microsoft Open Source Code of Conduct

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
