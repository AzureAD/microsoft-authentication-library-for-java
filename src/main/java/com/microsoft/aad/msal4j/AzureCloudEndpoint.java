// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * All the national clouds authenticate users separately in each environment and have separate authentication endpoints.
 * AzureCloudEndpoint is an utility enum containing URLs for each of the national clouds endpoints, as well as the public cloud endpoint
 *
 * For more details see: https://aka.ms/msal4j-national-cloud
 */
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
