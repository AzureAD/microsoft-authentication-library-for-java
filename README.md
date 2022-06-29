# Microsoft Authentication Library (MSAL) for Java

`main` branch    | `dev` branch    | Reference Docs
--------------------|-----------------|---------------
[![Build status](https://identitydivision.visualstudio.com/IDDP/_apis/build/status/CI/Java/MSAL%20Java%20CI%20Build?branchName=main)](https://identitydivision.visualstudio.com/IDDP/_build/latest?definitionId=762) | [![Build status](https://identitydivision.visualstudio.com/IDDP/_apis/build/status/CI/Java/MSAL%20Java%20CI%20Build?branchName=dev)](https://identitydivision.visualstudio.com/IDDP/_build/latest?definitionId=762)| [![Javadocs](http://javadoc.io/badge/com.microsoft.azure/msal4j.svg)](http://javadoc.io/doc/com.microsoft.azure/msal4j)

The Microsoft Authentication Library for Java (MSAL4J) enables applications to integrate with the [Microsoft identity platform](https://docs.microsoft.com/en-us/azure/active-directory/develop/). It allows you to sign in users or apps with Microsoft identities (Azure AD, Microsoft accounts and Azure AD B2C accounts) and obtain tokens to call Microsoft APIs such as [Microsoft Graph](https://graph.microsoft.io/) or your own APIs registered with the Microsoft identity platform. It is built using industry standard OAuth2 and OpenID Connect protocols.

Quick links:

| [Getting Started](https://docs.microsoft.com/en-us/azure/active-directory/develop/web-app-quickstart?pivots=devlang-java) | [Home](https://github.com/AzureAD/microsoft-authentication-library-for-java/wiki) | [Samples](https://github.com/Azure-Samples/ms-identity-msal-java-samples) | [Support](README.md#community-help-and-support) | [Feedback](https://forms.office.com/r/6AhHwQp3pe)
| --- | --- | --- | --- | --- |

## Install

The library supports the following Java environments:
- Java 8 (or higher)

Current version - 1.13.0

You can find the changes for each version in the [change log](https://github.com/AzureAD/microsoft-authentication-library-for-java/blob/master/changelog.txt).

You can get the msal4j package through Maven or Gradle.

### Maven
Find [the latest package in the Maven repository](https://mvnrepository.com/artifact/com.microsoft.azure/msal4j).
```
<dependency>
    <groupId>com.microsoft.azure</groupId>
    <artifactId>msal4j</artifactId>
    <version>1.13.0</version>
</dependency>
```
### Gradle

compile group: 'com.microsoft.azure', name: 'msal4j', version: '1.13.0'

## Usage

MSAL4J supports multiple [application types and authentication scenarios](https://docs.microsoft.com/azure/active-directory/develop/authentication-flows-app-scenarios).

Refer the [Wiki](https://github.com/AzureAD/microsoft-authentication-library-for-java/wiki) pages for more details on the usage of MSAL Java and the supported scenarios.

## Migrating from ADAL
If your application is using ADAL for Java (ADAL4J), we recommend you to update to use MSAL4J. No new feature work will be done in ADAL4J.

See the [ADAL to MSAL migration](https://github.com/AzureAD/microsoft-authentication-library-for-java/wiki/Migrate-to-MSAL-Java) guide.

## Roadmap

You can follow the latest updates and plans for MSAL Java in the [Roadmap](https://github.com/AzureAD/microsoft-authentication-library-for-java/wiki#roadmap) published on our Wiki.

## Contribution

This project welcomes contributions and suggestions. Most contributions require you to agree to a Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us the rights to use your contribution. For details, visit https://cla.microsoft.com.
When you submit a pull request, a CLA-bot will automatically determine whether you need to provide a CLA and decorate the PR appropriately (e.g., label, comment). Simply follow the instructions provided by the bot. You will only need to do this once across all repos using our CLA.
This project has adopted the Microsoft Open Source Code of Conduct. For more information see the Code of Conduct FAQ or contact opencode@microsoft.com with any additional questions or comments.

## Samples and Documentation

We provide a [full suite of sample applications](https://aka.ms/aaddevsamplesv2) and [documentation](https://docs.microsoft.com/en-us/azure/active-directory/develop/) to help you get started with learning the Microsoft identity platform.

## Community Help and Support

We leverage [Stack Overflow](http://stackoverflow.com/) to work with the community on supporting Azure Active Directory and its SDKs, including this one! We highly recommend you ask your questions on Stack Overflow (we're all on there!) Also browser existing issues to see if someone has had your question before.

We recommend you use the "msal" tag so we can see it! Here is the latest Q&A on Stack Overflow for MSAL: [http://stackoverflow.com/questions/tagged/msal](http://stackoverflow.com/questions/tagged/msal)

## Submit Feedback
We'd like your thoughts on this library. Please complete [this short survey.](https://forms.office.com/r/6AhHwQp3pe)

## Security Reporting

If you find a security issue with our libraries or services please report it to [secure@microsoft.com](mailto:secure@microsoft.com) with as much detail as possible. Your submission may be eligible for a bounty through the [Microsoft Bounty](http://aka.ms/bugbounty) program. Please do not post security issues to GitHub Issues or any other public site. We will contact you shortly upon receiving the information. We encourage you to get notifications of when security incidents occur by visiting [this page](https://technet.microsoft.com/security/dd252948) and subscribing to Security Advisory Alerts.

## We Value and Adhere to the Microsoft Open Source Code of Conduct

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
