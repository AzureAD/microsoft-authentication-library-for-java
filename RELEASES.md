# Microsoft Identity SDK Versioning and Servicing FAQ

We have adopted the semantic versioning flow that is industry standard for OSS projects. It gives the maximum amount of control on what risk you take with what versions.

## Semantic Versioning and API stability promises

Microsoft Authentication Libraries are independent open source libraries that are used by  both internal and external Microsoft partners. As with the rest of Microsoft, we have moved to a rapid iteration model where bugs are fixed daily and new versions are produced as required. To communicate these frequent changes to external partners and customers, we follow the practices of other open source libraries and use semantic versioning for all our public Microsoft Authentication SDK libraries. This allows us to support our downstream partners which will lock on certain versions for stability purposes, as well as providing for the distribution over NuGet, CocoaPods, and Maven.

The semantics are: MAJOR.MINOR.PATCH (example 1.1.5)

We will update our code distributions to use the latest PATCH semantic version number in order to make sure our customers and partners get the latest bug fixes. Downstream partner needs to pull the latest PATCH version. Most partners should try lock on the latest MINOR version number in their builds and accept any updates in the PATCH number.

Example:
Using Maven, this ensures all 1.1.0 to 1.1.x updates are included when building your code, but not 1.2. 

```
<dependency>
    <groupId>com.microsoft.azure</groupId>
    <artifactId>msal4j</artifactId>
    <version>[1.1.0,1.2.0)</version>
</dependency>
```

| Version |                                                                                                                                                                                                                                                             Description                                                                                                                                                                                                                                                              |
|:-------:|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| x.x.x   | PATCH version number. Incrementing these numbers is for bug fixes and updates but do not introduce new features. This is used for close partners who build on our platform release (ex. Microsoft Entra Fabric, Office, etc.). In addition, Cocoapods, NuGet, and Maven use this number to deliver the latest release to customers. This will update frequently (sometimes within the same day). There are no new features, and no regressions or API surface changes. Code will continue to work unless affected by a particular code fix. |
| x.x     |                                                          MINOR version numbers. These are for new feature additions that do not impact existing features or introduce regressions. They are purely additive, but may require testing to ensure nothing is impacted. All x.x.x bug fixes will also roll up in to this number. There is no regressions or API surface changes. Code will continue to work unless affected by a particular code fix or needs this new feature.                                                          |
| x       |                                                   MAJOR version numbers. This should be considered a new, supported version of Microsoft Authentication SDK and begins the Azure one year support cycle anew. Major new features are introduced and API changes can occur. This should only be used after a large amount of testing and used only if those features are needed. We will continue to service MAJOR version numbers with bug fixes up to the one year support cycle.                                                   |

## Serviceability

When we release a new MINOR version, the previous MINOR versions shipped within one year MAY still accept bug report and receive patches. MINOR versions more than one year old will not receive any support. If you suspect there is an issue in a 1+ year old version that you are using, please upgrade to latest MINOR version and retry, before you send out a bug report.

When we release a new MAJOR version, we will continue to apply bug fixes to the existing features in the previous MAJOR version for up to the 1-year support cycle for Azure.

## Microsoft Authentication SDKs and Microsoft Entra

Microsoft Authentication SDKs major versions will maintain backwards compatibility with Microsoft Entra web services through the support period. This means that the API surface area defined in a MAJOR version will continue to work for at least 1 year after release.

We will respond to bugs quickly from our partners and customers submitted through GitHub and through our private alias (tellaad@microsoft.com) for security issues and update the PATCH version number. We will also submit a change summary for each PATCH number.
Occasionally, there will be security bugs or breaking bugs from our partners that will require an immediate fix and an update to all partners and customers. When this occurs, we will do an emergency roll up to a PATCH version number and update all our distribution methods to the latest.