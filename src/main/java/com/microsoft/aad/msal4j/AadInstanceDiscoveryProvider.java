// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

class AadInstanceDiscoveryProvider {

    private final static String DEFAULT_TRUSTED_HOST = "login.microsoftonline.com";
    private final static String AUTHORIZE_ENDPOINT_TEMPLATE = "https://{host}/{tenant}/oauth2/v2.0/authorize";
    private final static String INSTANCE_DISCOVERY_ENDPOINT_TEMPLATE = "https://{host}:{port}/common/discovery/instance";
    private final static String INSTANCE_DISCOVERY_REQUEST_PARAMETERS_TEMPLATE = "?api-version=1.1&authorization_endpoint={authorizeEndpoint}";
    private final static String HOST_TEMPLATE_WITH_REGION = "{region}.r.{host}";
    private final static String SOVEREIGN_HOST_TEMPLATE_WITH_REGION = "{region}.{host}";
    private final static String REGION_NAME = "REGION_NAME";
    private final static int PORT_NOT_SET = -1;
    // For information of the current api-version refer: https://docs.microsoft.com/en-us/azure/virtual-machines/windows/instance-metadata-service#versioning
    private final static String DEFAULT_API_VERSION = "2020-06-01";
    private final static String IMDS_ENDPOINT = "https://169.254.169.254/metadata/instance/compute/location?" + DEFAULT_API_VERSION + "&format=text";

    final static TreeSet<String> TRUSTED_HOSTS_SET = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    final static TreeSet<String> TRUSTED_SOVEREIGN_HOSTS_SET = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    private final static Logger log = LoggerFactory.getLogger(HttpHelper.class);

    static ConcurrentHashMap<String, InstanceDiscoveryMetadataEntry> cache = new ConcurrentHashMap<>();

    static {
        TRUSTED_SOVEREIGN_HOSTS_SET.addAll(Arrays.asList(
                "login.chinacloudapi.cn",
                "login-us.microsoftonline.com",
                "login.microsoftonline.de",
                "login.microsoftonline.us"));

        TRUSTED_HOSTS_SET.addAll(Arrays.asList(
                "login.windows.net",
                "login.microsoftonline.com"));

        TRUSTED_HOSTS_SET.addAll(TRUSTED_SOVEREIGN_HOSTS_SET);
    }

    static InstanceDiscoveryMetadataEntry getMetadataEntry(URL authorityUrl,
                                                           boolean validateAuthority,
                                                           MsalRequest msalRequest,
                                                           ServiceBundle serviceBundle) {
        String host = authorityUrl.getHost();

        if (shouldUseRegionalEndpoint(msalRequest)) {
            //Server side telemetry requires the result from region discovery when any part of the region API is used
            String detectedRegion = discoverRegion(msalRequest, serviceBundle);

            if (msalRequest.application().azureRegion() != null) {
                host = getRegionalizedHost(authorityUrl.getHost(), msalRequest.application().azureRegion());
            }

            //If region autodetection is enabled and a specific region not already set,
            // set the application's region to the discovered region so that future requests can skip the IMDS endpoint call
            if (msalRequest.application().azureRegion() == null && msalRequest.application().autoDetectRegion()) {
                if (detectedRegion != null) {
                    msalRequest.application().azureRegion = detectedRegion;
                }
            }
            cacheRegionInstanceMetadata(authorityUrl.getHost(), msalRequest.application().azureRegion());
            serviceBundle.getServerSideTelemetry().getCurrentRequest().regionOutcome(
                    determineRegionOutcome(detectedRegion, msalRequest.application().azureRegion(), msalRequest.application().autoDetectRegion()));
        }

        InstanceDiscoveryMetadataEntry result = cache.get(host);

        if (result == null) {
            doInstanceDiscoveryAndCache(authorityUrl, validateAuthority, msalRequest, serviceBundle);
        }

        return cache.get(host);
    }

    static Set<String> getAliases(String host) {
        if (cache.containsKey(host)) {
            return cache.get(host).aliases();
        } else {
            return Collections.singleton(host);
        }
    }

    static AadInstanceDiscoveryResponse parseInstanceDiscoveryMetadata(String instanceDiscoveryJson) {

        AadInstanceDiscoveryResponse aadInstanceDiscoveryResponse;
        try {
            aadInstanceDiscoveryResponse = JsonHelper.convertJsonToObject(
                    instanceDiscoveryJson,
                    AadInstanceDiscoveryResponse.class);

        } catch (Exception ex) {
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
                for (String alias : entry.aliases()) {
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


    private static boolean shouldUseRegionalEndpoint(MsalRequest msalRequest){
        if (msalRequest.application().azureRegion() != null || msalRequest.application().autoDetectRegion()){
            //This class type check is a quick and dirty fix to accommodate changes to the internal workings of the region API
            //
            //ESTS-R only supports a small, but growing, number of scenarios, and the original design failed silently whenever
            //  regions could not be used. To avoid a breaking change this check will allow supported flows to use regions,
            //  and unsupported flows will continue to fall back to global, but with an added warning and a link to more info
            if (msalRequest.getClass() == ClientCredentialRequest.class) {
                return true;
            } else {
                //Avoid unnecessary warnings when looking for cached tokens by checking if request was a silent call
                if (msalRequest.getClass() != SilentRequest.class) {
                    log.warn("Regional endpoints are only available for client credential flow, request will fall back to using the global endpoint. See here for more information about supported scenarios: https://aka.ms/msal4j-azure-regions");
                }
                return false;
            }
        }
        return false;
    }

    static void cacheRegionInstanceMetadata(String host, String region) {

        Set<String> aliases = new HashSet<>();
        aliases.add(host);
        String regionalHost = getRegionalizedHost(host, region);

        cache.putIfAbsent(regionalHost, InstanceDiscoveryMetadataEntry.builder().
                preferredCache(host).
                preferredNetwork(regionalHost).
                aliases(aliases).
                build());
    }

    private static String getRegionalizedHost(String host, String region) {
        String regionalizedHost;

        if (region == null){
            return host;
        }

        //Cached calls may already have the regionalized authority, so if region info is already in the host just return as-is
        if (host.contains(region)) {
            return host;
        }

        //Basic Microsoft authority hosts (login.microsoftonline.com, login.windows.net, etc.) follow one regional URL template,
        //  whereas sovereign cloud endpoints and any non-Microsoft authorities are assumed to follow another template
        if (TRUSTED_HOSTS_SET.contains(host) && !TRUSTED_SOVEREIGN_HOSTS_SET.contains(host)){
            regionalizedHost = HOST_TEMPLATE_WITH_REGION.
                    replace("{region}", region).
                    replace("{host}", host);

        } else {
            regionalizedHost = SOVEREIGN_HOST_TEMPLATE_WITH_REGION.
                    replace("{region}", region).
                    replace("{host}", host);
        }

        return regionalizedHost;
    }

    private static String getAuthorizeEndpoint(String host, String tenant) {
        return AUTHORIZE_ENDPOINT_TEMPLATE.
                replace("{host}", host).
                replace("{tenant}", tenant);
    }

    private static String getInstanceDiscoveryEndpoint(URL authorityUrl) {

        String discoveryHost = TRUSTED_HOSTS_SET.contains(authorityUrl.getHost()) ?
                authorityUrl.getHost() :
                DEFAULT_TRUSTED_HOST;

        int port = authorityUrl.getPort() == PORT_NOT_SET ?
                authorityUrl.getDefaultPort() :
                authorityUrl.getPort();

        return INSTANCE_DISCOVERY_ENDPOINT_TEMPLATE.
                replace("{host}", discoveryHost).
                replace("{port}", String.valueOf(port));
    }

    private static AadInstanceDiscoveryResponse sendInstanceDiscoveryRequest(URL authorityUrl,
                                                                             MsalRequest msalRequest,
                                                                             ServiceBundle serviceBundle) {

        IHttpResponse httpResponse = null;

        String instanceDiscoveryRequestUrl = getInstanceDiscoveryEndpoint(authorityUrl) +
                formInstanceDiscoveryParameters(authorityUrl);

        httpResponse = executeRequest(instanceDiscoveryRequestUrl, msalRequest.headers().getReadonlyHeaderMap(), msalRequest, serviceBundle);

        if (httpResponse.statusCode() != HttpHelper.HTTP_STATUS_200) {
            throw MsalServiceExceptionFactory.fromHttpResponse(httpResponse);
        }


        return JsonHelper.convertJsonToObject(httpResponse.body(), AadInstanceDiscoveryResponse.class);
    }

    private static int determineRegionOutcome(String detectedRegion, String providedRegion, boolean autoDetect) {
        int regionOutcomeTelemetryValue = 0;//By default, assume region API was not used
        if (providedRegion != null) {//Developer provided a region
            if (detectedRegion == null) {//Region autodetection failed
                regionOutcomeTelemetryValue = RegionTelemetry.REGION_OUTCOME_DEVELOPER_AUTODETECT_FAILED.telemetryValue;
            } else if (providedRegion.equals(detectedRegion)) {//Provided and detected regions match
                regionOutcomeTelemetryValue = RegionTelemetry.REGION_OUTCOME_DEVELOPER_AUTODETECT_MATCH.telemetryValue;
            } else {//Mismatch between provided and detected regions
                regionOutcomeTelemetryValue = RegionTelemetry.REGION_OUTCOME_DEVELOPER_AUTODETECT_MISMATCH.telemetryValue;
            }
        } else if (autoDetect) {//Developer enabled region autodetection
            if (detectedRegion == null) {//Region autodetection failed
                regionOutcomeTelemetryValue = RegionTelemetry.REGION_OUTCOME_AUTODETECT_FAILED.telemetryValue;
            } else {//Region autodetection succeeded
                regionOutcomeTelemetryValue = RegionTelemetry.REGION_OUTCOME_AUTODETECT_SUCCESS.telemetryValue;
            }
        }

        return regionOutcomeTelemetryValue;
    }

    private static String formInstanceDiscoveryParameters(URL authorityUrl) {
        return INSTANCE_DISCOVERY_REQUEST_PARAMETERS_TEMPLATE.replace("{authorizeEndpoint}",
                getAuthorizeEndpoint(authorityUrl.getHost(),
                        Authority.getTenant(authorityUrl, Authority.detectAuthorityType(authorityUrl))));
    }

    private static IHttpResponse executeRequest(String requestUrl, Map<String, String> headers, MsalRequest msalRequest, ServiceBundle serviceBundle) {
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

        CurrentRequest currentRequest = serviceBundle.getServerSideTelemetry().getCurrentRequest();

        //Check if the REGION_NAME environment variable has a value for the region
        if (System.getenv(REGION_NAME) != null) {
            log.info("Region found in environment variable: " + System.getenv(REGION_NAME));
            currentRequest.regionSource(RegionTelemetry.REGION_SOURCE_ENV_VARIABLE.telemetryValue);

            return System.getenv(REGION_NAME);
        }

        try {
            //Check the IMDS endpoint to retrieve current region (will only work if application is running in an Azure VM)
            Map<String, String> headers = new HashMap<>();
            headers.put("Metadata", "true");
            IHttpResponse httpResponse = executeRequest(IMDS_ENDPOINT, headers, msalRequest, serviceBundle);

            //If call to IMDS endpoint was successful, return region from response body
            if (httpResponse.statusCode() == HttpHelper.HTTP_STATUS_200 && !httpResponse.body().isEmpty()) {
                log.info("Region retrieved from IMDS endpoint: " + httpResponse.body());
                currentRequest.regionSource(RegionTelemetry.REGION_SOURCE_IMDS.telemetryValue);

                return httpResponse.body();
            }

            log.warn(String.format("Call to local IMDS failed with status code: %s, or response was empty", httpResponse.statusCode()));
            currentRequest.regionSource(RegionTelemetry.REGION_SOURCE_FAILED_AUTODETECT.telemetryValue);

            return null;
        } catch (Exception e) {
            //IMDS call failed, cannot find region
            //The IMDS endpoint is only available from within an Azure environment, so the most common cause of this
            //  exception will likely be java.net.SocketException: Network is unreachable: connect
            log.warn(String.format("Exception during call to local IMDS endpoint: %s", e.getMessage()));
            currentRequest.regionSource(RegionTelemetry.REGION_SOURCE_FAILED_AUTODETECT.telemetryValue);

            return null;
        }
    }

    private static void doInstanceDiscoveryAndCache(URL authorityUrl,
                                                    boolean validateAuthority,
                                                    MsalRequest msalRequest,
                                                    ServiceBundle serviceBundle) {

        AadInstanceDiscoveryResponse aadInstanceDiscoveryResponse = null;

        if (msalRequest.application().authenticationAuthority.authorityType.equals(AuthorityType.AAD)) {
            aadInstanceDiscoveryResponse = sendInstanceDiscoveryRequest(authorityUrl, msalRequest, serviceBundle);

            if (validateAuthority) {
                validate(aadInstanceDiscoveryResponse);
            }
        }

        cacheInstanceDiscoveryMetadata(authorityUrl.getHost(), aadInstanceDiscoveryResponse);
    }

    private static void validate(AadInstanceDiscoveryResponse aadInstanceDiscoveryResponse) {
        if (StringHelper.isBlank(aadInstanceDiscoveryResponse.tenantDiscoveryEndpoint())) {
            throw new MsalServiceException(aadInstanceDiscoveryResponse);
        }
    }
}
