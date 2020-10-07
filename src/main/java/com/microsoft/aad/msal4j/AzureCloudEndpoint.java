package com.microsoft.aad.msal4j;

public enum AzureCloudEndpoint {
    /**
     * Microsoft Azure public cloud, https://login.microsoftonline.com
     */
    AzurePublic("https://login.microsoftonline.com/"),
    /**
     * Microsoft Chinese national cloud, https://login.chinacloudapi.cn
     */
    AzureChina("https://login.chinacloudapi.cn/"),
    /**
     * Microsoft German national cloud ("Black Forest"), https://login.microsoftonline.de
     */
    AzureGermany("https://login.microsoftonline.de/"),
    /**
     * US Government cloud, https://login.microsoftonline.us
     */
    AzureUsGovernment("https://login.microsoftonline.us/");

    public final String endpoint;

    AzureCloudEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
