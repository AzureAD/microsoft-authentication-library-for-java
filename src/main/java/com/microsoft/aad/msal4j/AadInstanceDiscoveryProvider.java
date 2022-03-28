// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    private final static String INSTANCE_DISCOVERY_ENDPOINT_TEMPLATE = "https://{host}:{port}/common/discovery/instance";
    private final static String INSTANCE_DISCOVERY_ENDPOINT_TEMPLATE_WITH_REGION = "https://{region}.{host}:{port}/common/discovery/instance";
    private final static String INSTANCE_DISCOVERY_REQUEST_PARAMETERS_TEMPLATE = "?api-version=1.1&authorization_endpoint={authorizeEndpoint}";
    private final static String REGION_NAME = "REGION_NAME";
    private final static int PORT_NOT_SET = -1;
    // For information of the current api-version refer: https://docs.microsoft.com/en-us/azure/virtual-machines/windows/instance-metadata-service#versioning
    private final static String DEFAULT_API_VERSION = "2020-06-01";
    private final static String IMDS_ENDPOINT = "https://169.254.169.254/metadata/instance/compute/location?" + DEFAULT_API_VERSION + "&format=text";

    final static TreeSet<String> TRUSTED_HOSTS_SET = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    private final static Logger log = LoggerFactory.getLogger(AadInstanceDiscoveryProvider.class);

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

        InstanceDiscoveryMetadataEntry result = cache.get(authorityUrl.getHost());

        if (result == null) {
            doInstanceDiscoveryAndCache(authorityUrl, validateAuthority, msalRequest, serviceBundle);
        }

        return cache.get(authorityUrl.getHost());
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
            aadInstanceDiscoveryResponse = AadInstanceDiscoveryResponse.convertJsonToObject(
                    instanceDiscoveryJson
            );

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

    private static String getInstanceDiscoveryEndpointWithRegion(URL authorityUrl, String region) {

        String discoveryHost = TRUSTED_HOSTS_SET.contains(authorityUrl.getHost()) ?
                authorityUrl.getHost() :
                DEFAULT_TRUSTED_HOST;

        int port = authorityUrl.getPort() == PORT_NOT_SET ?
                authorityUrl.getDefaultPort() :
                authorityUrl.getPort();

        return INSTANCE_DISCOVERY_ENDPOINT_TEMPLATE_WITH_REGION.
                replace("{region}", region).
                replace("{host}", discoveryHost).
                replace("{port}", String.valueOf(port));
    }


    private static AadInstanceDiscoveryResponse sendInstanceDiscoveryRequest(URL authorityUrl,
                                                                             MsalRequest msalRequest,
                                                                             ServiceBundle serviceBundle) {

        IHttpResponse httpResponse = null;
        String providedRegion = msalRequest.application().azureRegion();
        String detectedRegion = null;
        int regionOutcomeTelemetryValue = 0;
        String regionToUse = null;

        //If a region was provided by a developer or they set the autoDetectRegion parameter,
        // attempt to discover the region and set telemetry info based on the outcome
        if (providedRegion != null) {
            detectedRegion = discoverRegion(msalRequest, serviceBundle);
            regionToUse = providedRegion;
            regionOutcomeTelemetryValue = determineRegionOutcome(detectedRegion, providedRegion, msalRequest.application().autoDetectRegion());
        } else if (msalRequest.application().autoDetectRegion()) {
            detectedRegion = discoverRegion(msalRequest, serviceBundle);

            if (detectedRegion != null) {
                regionToUse = detectedRegion;
            }

            regionOutcomeTelemetryValue = determineRegionOutcome(detectedRegion, providedRegion, msalRequest.application().autoDetectRegion());
        }

        //If the region is known, attempt to make instance discovery request with region endpoint
        if (regionToUse != null) {
            String instanceDiscoveryRequestUrl = getInstanceDiscoveryEndpointWithRegion(authorityUrl, regionToUse) +
                    formInstanceDiscoveryParameters(authorityUrl);

            try {
                httpResponse = executeRequest(instanceDiscoveryRequestUrl, msalRequest.headers().getReadonlyHeaderMap(), msalRequest, serviceBundle);
            } catch (MsalClientException ex) {
                log.warn("Could not retrieve regional instance discovery metadata, falling back to global endpoint");
            }
        }

        //If the region is unknown or the instance discovery failed at the region endpoint, try the global endpoint
        if ((detectedRegion == null && providedRegion == null) || httpResponse == null || httpResponse.statusCode() != HTTPResponse.SC_OK) {

            String instanceDiscoveryRequestUrl = getInstanceDiscoveryEndpoint(authorityUrl) +
                    formInstanceDiscoveryParameters(authorityUrl);

            httpResponse = executeRequest(instanceDiscoveryRequestUrl, msalRequest.headers().getReadonlyHeaderMap(), msalRequest, serviceBundle);
        }

        if (httpResponse.statusCode() != HttpHelper.HTTP_STATUS_200) {
            throw MsalServiceExceptionFactory.fromHttpResponse(httpResponse);
        }

        serviceBundle.getServerSideTelemetry().getCurrentRequest().regionOutcome(regionOutcomeTelemetryValue);

        try {
            return AadInstanceDiscoveryResponse.convertJsonToObject(httpResponse.body());
        } catch (IOException e) {
            throw new MsalClientException(e);
        }
    }

    private static int determineRegionOutcome(String detectedRegion, String providedRegion, boolean autoDetect) {
        int regionOutcomeTelemetryValue = 0;
        if (providedRegion != null) {
            if (detectedRegion == null) {
                regionOutcomeTelemetryValue = RegionTelemetry.REGION_OUTCOME_DEVELOPER_AUTODETECT_FAILED.telemetryValue;
            } else if (providedRegion.equals(detectedRegion)) {
                regionOutcomeTelemetryValue = RegionTelemetry.REGION_OUTCOME_DEVELOPER_AUTODETECT_MATCH.telemetryValue;
            } else {
                regionOutcomeTelemetryValue = RegionTelemetry.REGION_OUTCOME_DEVELOPER_AUTODETECT_MISMATCH.telemetryValue;
            }
        } else if (autoDetect) {
            if (detectedRegion != null) {
                regionOutcomeTelemetryValue = RegionTelemetry.REGION_OUTCOME_AUTODETECT_SUCCESS.telemetryValue;
            } else {
                regionOutcomeTelemetryValue = RegionTelemetry.REGION_OUTCOME_AUTODETECT_FAILED.telemetryValue;
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
