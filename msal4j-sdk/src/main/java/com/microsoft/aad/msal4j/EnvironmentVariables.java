package com.microsoft.aad.msal4j;

public class EnvironmentVariables {

    private static String IDENTITY_ENDPOINT;
    private static String IDENTITY_HEADER;
    private static String AZURE_POD_IDENTITY_AUTHORITY_HOST;
    private static String IMDS_ENDPOINT;
    private static String MSI_ENDPOINT;
    private static String IDENTITY_SERVER_THUMBPRINT;

    public static String getIdentityEndpoint() {
        return System.getenv("IDENTITY_ENDPOINT");
    }

    public static String getIdentityHeader() {
        return System.getenv("IDENTITY_HEADER");
    }

    public static String getAzurePodIdentityAuthorityHost() {
        return System.getenv("AZURE_POD_IDENTITY_AUTHORITY_HOST");
    }

    public static String getImdsEndpoint() {
        return System.getenv("IMDS_ENDPOINT");
    }

    public static String getMsiEndpoint() {
        return System.getenv("MSI_ENDPOINT");
    }

    public static String getIdentityServerThumbprint() {
        return System.getenv("IDENTITY_SERVER_THUMBPRINT");
    }
}
