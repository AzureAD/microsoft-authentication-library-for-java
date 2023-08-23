package com.microsoft.aad.msal4j;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentVariablesHelper implements IEnvironmentVariables {
    Map<String, String> mockedEnvironmentVariables;

    EnvironmentVariablesHelper(ManagedIdentitySourceType source, String endpoint) {
        mockedEnvironmentVariables = new HashMap<>();

        switch (source) {
            case AppService:
                mockedEnvironmentVariables.put(IEnvironmentVariables.IDENTITY_ENDPOINT, endpoint);
                mockedEnvironmentVariables.put(IEnvironmentVariables.IDENTITY_HEADER, "secret");
                break;

            case Imds:
                mockedEnvironmentVariables.put(EnvironmentVariables.IMDS_ENDPOINT, endpoint);
                break;

            case ServiceFabric:
                mockedEnvironmentVariables.put(IEnvironmentVariables.IDENTITY_ENDPOINT, endpoint);
                mockedEnvironmentVariables.put(IEnvironmentVariables.IDENTITY_HEADER, "secret");
                mockedEnvironmentVariables.put(IEnvironmentVariables.IDENTITY_SERVER_THUMBPRINT, "thumbprint");
                break;

            case CloudShell:
                mockedEnvironmentVariables.put(IEnvironmentVariables.MSI_ENDPOINT, endpoint);
                break;

            case AzureArc:
                mockedEnvironmentVariables.put(IEnvironmentVariables.IDENTITY_ENDPOINT, endpoint);
                mockedEnvironmentVariables.put(IEnvironmentVariables.IMDS_ENDPOINT, endpoint);
                break;
        }
    }

    @Override
    public String getEnvironmentVariable(String envVariable) {
        return mockedEnvironmentVariables.get(envVariable);
    }
}
