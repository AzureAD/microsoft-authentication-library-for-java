// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentVariablesHelper implements IEnvironmentVariables {
    Map<String, String> mockedEnvironmentVariables;

    EnvironmentVariablesHelper(ManagedIdentitySourceType source, String endpoint) {
        mockedEnvironmentVariables = new HashMap<>();

        mockedEnvironmentVariables.put("SourceType", source.toString());
        switch (source) {
            case APP_SERVICE:
                mockedEnvironmentVariables.put(Constants.IDENTITY_ENDPOINT, endpoint);
                mockedEnvironmentVariables.put(Constants.IDENTITY_HEADER, "secret");
                break;

            case IMDS:
                mockedEnvironmentVariables.put(Constants.IMDS_ENDPOINT, endpoint);
                break;

            case SERVICE_FABRIC:
                mockedEnvironmentVariables.put(Constants.IDENTITY_ENDPOINT, endpoint);
                mockedEnvironmentVariables.put(Constants.IDENTITY_HEADER, "secret");
                mockedEnvironmentVariables.put(Constants.IDENTITY_SERVER_THUMBPRINT, "thumbprint");
                break;

            case CLOUD_SHELL:
                mockedEnvironmentVariables.put(Constants.MSI_ENDPOINT, endpoint);
                break;

            case AZURE_ARC:
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
