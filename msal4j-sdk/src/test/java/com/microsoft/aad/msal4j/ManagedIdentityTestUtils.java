// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.lang.reflect.Field;
import java.util.Map;

public class ManagedIdentityTestUtils {

    public static void setEnvironmentVariables(ManagedIdentitySourceType managedIdentitySource, String endpoint, String secret)
    {
        if(StringHelper.isNullOrBlank(secret)){
            secret = "secret";
        }
        switch (managedIdentitySource)
        {
            case AppService:
                setEnvironmentVariable("IDENTITY_ENDPOINT", endpoint);
                setEnvironmentVariable("IDENTITY_HEADER", secret);
                break;

            case Imds:
                setEnvironmentVariable("AZURE_POD_IDENTITY_AUTHORITY_HOST", endpoint);
                break;

            case AzureArc:
                setEnvironmentVariable("IDENTITY_ENDPOINT", endpoint);
                setEnvironmentVariable("IMDS_ENDPOINT", "http://localhost:40342");
                break;

            case CloudShell:
                setEnvironmentVariable("MSI_ENDPOINT", endpoint);
                break;

            case ServiceFabric:
                setEnvironmentVariable("IDENTITY_ENDPOINT", endpoint);
                setEnvironmentVariable("IDENTITY_HEADER", secret);
                setEnvironmentVariable("IDENTITY_SERVER_THUMBPRINT", "thumbprint");
                break;
        }
    }

    public static void setEnvironmentVariables(ManagedIdentitySourceType managedIdentitySource, String endpoint)
    {
        setEnvironmentVariables(managedIdentitySource,endpoint,"secret");
    }

    /// Sets the Environment Variables
    public static void setEnvironmentVariables(Map<String, String> envVariables)
    {
        //Set the environment variables
        for(String key : envVariables.keySet())
        {
            setEnvironmentVariable(key, envVariables.get(key));
        }
    }



    private static void setEnvironmentVariable(String key, String value){
        try {
            Map<String, String> env = System.getenv();
            Class<?> cl = env.getClass();
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            writableEnv.put(key, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set environment variable", e);
        }
    }
}
