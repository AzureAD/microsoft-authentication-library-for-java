// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentVariablesHelper implements IEnvironmentVariables {
    Map<String, String> mockedEnvironmentVariables;

    EnvironmentVariablesHelper(ManagedIdentitySourceType source, String endpoint) {
        mockedEnvironmentVariables = new HashMap<>();

        switch (source) {
            case AppService:
                mockedEnvironmentVariables.put(Constants.IDENTITY_ENDPOINT, endpoint);
                mockedEnvironmentVariables.put(Constants.IDENTITY_HEADER, "secret");
                break;

            case Imds:
                mockedEnvironmentVariables.put(Constants.IMDS_ENDPOINT, endpoint);
                break;

            case ServiceFabric:
                mockedEnvironmentVariables.put(Constants.IDENTITY_ENDPOINT, endpoint);
                mockedEnvironmentVariables.put(Constants.IDENTITY_HEADER, "secret");
                mockedEnvironmentVariables.put(Constants.IDENTITY_SERVER_THUMBPRINT, "thumbprint");
                break;

            case CloudShell:
                mockedEnvironmentVariables.put(Constants.MSI_ENDPOINT, endpoint);
                break;

            case AzureArc:
                mockedEnvironmentVariables.put(Constants.IDENTITY_ENDPOINT, endpoint);
                mockedEnvironmentVariables.put(Constants.IMDS_ENDPOINT, endpoint);
                break;
        }
    }

    @Override
    public String getEnvironmentVariable(String envVariable) {
        return mockedEnvironmentVariables.get(envVariable);
    }
}
