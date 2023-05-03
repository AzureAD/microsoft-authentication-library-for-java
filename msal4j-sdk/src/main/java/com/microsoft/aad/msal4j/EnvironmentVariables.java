package com.microsoft.aad.msal4j;

public class EnvironmentVariables {

    public static String IDENTITY_ENDPOINT = System.getenv("IDENTITY_ENDPOINT");
    public static String IDENTITY_HEADER = System.getenv("IDENTITY_HEADER");
    public static String AZURE_POD_IDENTITY_AUTHORITY_HOST = System.getenv("AZURE_POD_IDENTITY_AUTHORITY_HOST");
    public static String IMDS_ENDPOINT = System.getenv("IMDS_ENDPOINT");
    public static String MSI_ENDPOINT = System.getenv("MSI_ENDPOINT");
    public static String IDENTITY_SERVER_THUMBPRINT = System.getenv("IDENTITY_SERVER_THUMBPRINT");

}
