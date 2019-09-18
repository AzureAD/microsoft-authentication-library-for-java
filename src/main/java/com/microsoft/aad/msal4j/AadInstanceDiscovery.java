// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

class AadInstanceDiscovery {
    private static final Logger log = LoggerFactory.getLogger(AadInstanceDiscovery.class);

    final static TreeSet<String> TRUSTED_HOSTS_SET = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    static {
        TRUSTED_HOSTS_SET.addAll(Arrays.asList(
                "login.windows.net",
                "login.chinacloudapi.cn",
                "login-us.microsoftonline.com",
                "login.microsoftonline.de",
                "login.microsoftonline.com",
                "login.microsoftonline.us"));
    }

    private final static String DEFAULT_TRUSTED_HOST = "login.microsoftonline.com";

    private final static String AUTHORIZE_ENDPOINT_TEMPLATE = "https://{host}/{tenant}/oauth2/v2.0/authorize";
    private final static String INSTANCE_DISCOVERY_ENDPOINT_TEMPLATE = "https://{host}/common/discovery/instance";
    private final static String INSTANCE_DISCOVERY_REQUEST_PARAMETERS_TEMPLATE =
            "?api-version=1.1&authorization_endpoint={authorizeEndpoint}";

    static ConcurrentHashMap<String, InstanceDiscoveryMetadataEntry> cache = new ConcurrentHashMap<>();

    static Set<String> getAliases(String host){
        if(cache.containsKey(host)){
            return cache.get(host).aliases();
        }
        else{
            return Collections.singleton(host);
        }
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

    private static InstanceDiscoveryResponse sendInstanceDiscoveryRequest
            (URL authorityUrl, MsalRequest msalRequest,
             ServiceBundle serviceBundle) throws Exception {

        String instanceDiscoveryRequestUrl = getInstanceDiscoveryEndpoint(authorityUrl.getAuthority()) +
                INSTANCE_DISCOVERY_REQUEST_PARAMETERS_TEMPLATE.replace("{authorizeEndpoint}",
                        getAuthorizeEndpoint(authorityUrl.getAuthority(),
                                Authority.getTenant(authorityUrl, Authority.detectAuthorityType(authorityUrl))));

        String json = HttpHelper.executeHttpRequest
                (log, HttpMethod.GET, instanceDiscoveryRequestUrl, msalRequest.headers().getReadonlyHeaderMap(),
                        null, msalRequest.requestContext(), serviceBundle);

        return JsonHelper.convertJsonToObject(json, InstanceDiscoveryResponse.class);
    }

    private static void validate(InstanceDiscoveryResponse instanceDiscoveryResponse) {
        if (StringHelper.isBlank(instanceDiscoveryResponse.tenantDiscoveryEndpoint())) {
            throw new MsalServiceException(instanceDiscoveryResponse);
        }
    }

    private static void doInstanceDiscoveryAndCache
            (URL authorityUrl, boolean validateAuthority, MsalRequest msalRequest, ServiceBundle serviceBundle) throws Exception {

        InstanceDiscoveryResponse instanceDiscoveryResponse =
                sendInstanceDiscoveryRequest(authorityUrl, msalRequest, serviceBundle);

        if (validateAuthority) {
            validate(instanceDiscoveryResponse);
        }

        cacheInstanceDiscoveryMetadata(authorityUrl.getAuthority(), instanceDiscoveryResponse);
    }

    static InstanceDiscoveryMetadataEntry GetMetadataEntry
            (URL authorityUrl, boolean validateAuthority, MsalRequest msalRequest, ServiceBundle serviceBundle) throws Exception {

        InstanceDiscoveryMetadataEntry result = cache.get(authorityUrl.getAuthority());

        if (result == null) {
            doInstanceDiscoveryAndCache(authorityUrl, validateAuthority, msalRequest, serviceBundle);
        }

        return cache.get(authorityUrl.getAuthority());
    }

    private static void cacheInstanceDiscoveryMetadata(String host, InstanceDiscoveryResponse instanceDiscoveryResponse) {
        if (instanceDiscoveryResponse.metadata() != null) {
            for (InstanceDiscoveryMetadataEntry entry : instanceDiscoveryResponse.metadata()) {
                for (String alias : entry.aliases) {
                    cache.put(alias, entry);
                }
            }
        }
        cache.putIfAbsent(host, InstanceDiscoveryMetadataEntry.builder().
                preferredCache(host).
                preferredNetwork(host).build());
    }
}
