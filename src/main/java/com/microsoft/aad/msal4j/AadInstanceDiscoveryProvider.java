// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

class AadInstanceDiscoveryProvider {

    private final static String DEFAULT_TRUSTED_HOST = "login.microsoftonline.com";
    private final static String AUTHORIZE_ENDPOINT_TEMPLATE = "https://{host}/{tenant}/oauth2/v2.0/authorize";
    private final static String INSTANCE_DISCOVERY_ENDPOINT_TEMPLATE = "https://{host}/common/discovery/instance";
    private final static String INSTANCE_DISCOVERY_ENDPOINT_TEMPLATE_WITH_REGION = "https://{region}.{host}/common/discovery/instance";
    private final static String INSTANCE_DISCOVERY_REQUEST_PARAMETERS_TEMPLATE = "?api-version=1.1&authorization_endpoint={authorizeEndpoint}";
    private final static String REGION_NAME = "REGION_NAME";
    // For information of the current api-version refer: https://docs.microsoft.com/en-us/azure/virtual-machines/windows/instance-metadata-service#versioning
    private final static String DEFAULT_API_VERSION = "2020-06-01";
    private final static String IMDS_ENDPOINT = "https://169.254.169.254/metadata/instance/compute/location?" + DEFAULT_API_VERSION + "&format=text";

    final static TreeSet<String> TRUSTED_HOSTS_SET = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    static ConcurrentHashMap<String, InstanceDiscoveryMetadataEntry> cache = new ConcurrentHashMap<>();

    static {
        TRUSTED_HOSTS_SET.addAll(Arrays.asList(
                "login.windows.net",
                "login.chinacloudapi.cn",
                "login-us.microsoftonline.com",
                "login.microsoftonline.de",
                "login.microsoftonline.com",
                "login.microsoftonline.us"));
    }

    static InstanceDiscoveryMetadataEntry getMetadataEntry(URL authorityUrl,
                                                           boolean validateAuthority,
                                                           MsalRequest msalRequest,
                                                           ServiceBundle serviceBundle) {

        InstanceDiscoveryMetadataEntry result = cache.get(authorityUrl.getAuthority());

        if (result == null) {
            doInstanceDiscoveryAndCache(authorityUrl, validateAuthority, msalRequest, serviceBundle);
        }

        return cache.get(authorityUrl.getAuthority());
    }

    static Set<String> getAliases(String host){
        if(cache.containsKey(host)){
            return cache.get(host).aliases();
        }
        else{
            return Collections.singleton(host);
        }
    }

    static AadInstanceDiscoveryResponse parseInstanceDiscoveryMetadata(String instanceDiscoveryJson) {

        AadInstanceDiscoveryResponse aadInstanceDiscoveryResponse;
        try {
            aadInstanceDiscoveryResponse = JsonHelper.convertJsonToObject(
                    instanceDiscoveryJson,
                    AadInstanceDiscoveryResponse.class);

        } catch(Exception ex){
            throw new MsalClientException("Error parsing instance discovery response. Data must be " +
                    "in valid JSON format. For more information, see https://aka.ms/msal4j-instance-discovery",
                    AuthenticationErrorCode.INVALID_INSTANCE_DISCOVERY_METADATA);
        }

        return aadInstanceDiscoveryResponse;
    }

    static void cacheInstanceDiscoveryMetadata(String host,
                                               AadInstanceDiscoveryResponse aadInstanceDiscoveryResponse) {

        if (aadInstanceDiscoveryResponse != null && aadInstanceDiscoveryResponse.metadata() != null) {
            for (InstanceDiscoveryMetadataEntry entry : aadInstanceDiscoveryResponse.metadata()) {
                for (String alias: entry.aliases()) {
                    cache.put(alias, entry);
                }
            }
        }
        cache.putIfAbsent(host, InstanceDiscoveryMetadataEntry.builder().
                preferredCache(host).
                preferredNetwork(host).
                aliases(Collections.singleton(host)).
                build());
    }

    private static String getAuthorizeEndpoint(String host, String tenant) {
        return AUTHORIZE_ENDPOINT_TEMPLATE.
                replace("{host}", host).
                replace("{tenant}", tenant);
    }

    private static String getInstanceDiscoveryEndpoint(String host) {

        String discoveryHost = TRUSTED_HOSTS_SET.contains(host) ? host : DEFAULT_TRUSTED_HOST;

        return INSTANCE_DISCOVERY_ENDPOINT_TEMPLATE.
                replace("{host}", discoveryHost);
    }

    private static String getInstanceDiscoveryEndpointWithRegion(String host, String region) {

        String discoveryHost = TRUSTED_HOSTS_SET.contains(host) ? host : DEFAULT_TRUSTED_HOST;

        return INSTANCE_DISCOVERY_ENDPOINT_TEMPLATE_WITH_REGION.
                replace("{region}", region).
                replace("{host}", discoveryHost);
    }

    private static AadInstanceDiscoveryResponse sendInstanceDiscoveryRequest(URL authorityUrl,
                                                                             MsalRequest msalRequest,
                                                                             ServiceBundle serviceBundle) {

        String region = StringHelper.EMPTY_STRING;
        IHttpResponse httpResponse = null;

        //If the autoDetectRegion parameter in the request is set, attempt to discover the region
        if (msalRequest.application().autoDetectRegion()) {
            region = discoverRegion(msalRequest, serviceBundle);
        }

        //If the region is known, attempt to make instance discovery request with region endpoint
        if (!region.isEmpty()) {
            String instanceDiscoveryRequestUrl = getInstanceDiscoveryEndpointWithRegion(authorityUrl.getAuthority(), region) +
                    formInstanceDiscoveryParameters(authorityUrl);

            httpResponse = httpRequest(instanceDiscoveryRequestUrl, msalRequest.headers().getReadonlyHeaderMap(), msalRequest, serviceBundle);
        }

        //If the region is unknown or the instance discovery failed at the region endpoint, try the global endpoint
        if (region.isEmpty() || httpResponse == null || httpResponse.statusCode() != HTTPResponse.SC_OK) {
            String instanceDiscoveryRequestUrl = getInstanceDiscoveryEndpoint(authorityUrl.getAuthority()) +
                    formInstanceDiscoveryParameters(authorityUrl);

            httpResponse = httpRequest(instanceDiscoveryRequestUrl, msalRequest.headers().getReadonlyHeaderMap(), msalRequest, serviceBundle);
        }

        return JsonHelper.convertJsonToObject(httpResponse.body(), AadInstanceDiscoveryResponse.class);
    }

    private static String formInstanceDiscoveryParameters(URL authorityUrl) {
        return INSTANCE_DISCOVERY_REQUEST_PARAMETERS_TEMPLATE.replace("{authorizeEndpoint}",
                getAuthorizeEndpoint(authorityUrl.getAuthority(),
                        Authority.getTenant(authorityUrl, Authority.detectAuthorityType(authorityUrl))));
    }

    private static IHttpResponse httpRequest(String requestUrl, Map<String, String> headers, MsalRequest msalRequest, ServiceBundle serviceBundle) {
        HttpRequest httpRequest = new HttpRequest(
                HttpMethod.GET,
                requestUrl,
                headers);

        return HttpHelper.executeHttpRequest(
                httpRequest,
                msalRequest.requestContext(),
                serviceBundle);
    }

    private static String discoverRegion(MsalRequest msalRequest, ServiceBundle serviceBundle) {

        //Check if the REGION_NAME environment variable has a value for the region
        if (System.getenv(REGION_NAME) != null) {
            return System.getenv(REGION_NAME);
        }

        try {
            //Check the IMDS endpoint to retrieve current region (will only work if application is running in an Azure VM)
            Map<String, String> headers = new HashMap<>();
            headers.put("Metadata", "true");
            IHttpResponse httpResponse = httpRequest(IMDS_ENDPOINT, headers, msalRequest, serviceBundle);

            //If call to IMDS endpoint was successful, return region from response body
            if (httpResponse.statusCode() == HTTPResponse.SC_OK && !httpResponse.body().isEmpty()) {
                return httpResponse.body();
            }
        } catch (Exception e) {
            //IMDS call failed, cannot find region
            return StringHelper.EMPTY_STRING;
        }

        return StringHelper.EMPTY_STRING;
    }

    private static void doInstanceDiscoveryAndCache(URL authorityUrl,
                                                    boolean validateAuthority,
                                                    MsalRequest msalRequest,
                                                    ServiceBundle serviceBundle) {

        AadInstanceDiscoveryResponse aadInstanceDiscoveryResponse = null;

        if(msalRequest.application().authenticationAuthority.authorityType.equals(AuthorityType.AAD)) {
            aadInstanceDiscoveryResponse = sendInstanceDiscoveryRequest(authorityUrl, msalRequest, serviceBundle);

            if (validateAuthority) {
                validate(aadInstanceDiscoveryResponse);
            }
        }

        cacheInstanceDiscoveryMetadata(authorityUrl.getAuthority(), aadInstanceDiscoveryResponse);
    }

    private static void validate(AadInstanceDiscoveryResponse aadInstanceDiscoveryResponse) {
        if (StringHelper.isBlank(aadInstanceDiscoveryResponse.tenantDiscoveryEndpoint())) {
            throw new MsalServiceException(aadInstanceDiscoveryResponse);
        }
    }
}
