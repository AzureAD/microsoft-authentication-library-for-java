// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

public class AppCredentialProvider {

    private final String clientId;
    private final String oboClientId;
    private final String oboAppPassword;
    private final String oboAppIdURI;

    AppCredentialProvider() {
        KeyVaultSecretsProvider keyVaultSecretsProvider = new KeyVaultSecretsProvider();

        clientId = "54a2d933-8bf8-483b-a8f8-0a31924f3c1f";
        oboClientId = "23c64cd8-21e4-41dd-9756-ab9e2c23f58c";
        oboAppIdURI = "api://23c64cd8-21e4-41dd-9756-ab9e2c23f58c";
        oboAppPassword = keyVaultSecretsProvider.getSecretByName("IdentityDivisionDotNetOBOServiceSecret").getValue();
    }

    public String getAppId() {
        return clientId;
    }

    public String getOboAppId() {
        return oboClientId;
    }

    public String getOboAppPassword() {
        return oboAppPassword;
    }

    public String getOboAppIdURI() {
        return oboAppIdURI;
    }
}
