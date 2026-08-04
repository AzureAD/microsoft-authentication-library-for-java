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
import java.util.concurrent.*;

class AadInstanceDiscoveryProvider {

    private static final String DEFAULT_TRUSTED_HOST = "login.microsoftonline.com";
    private static final String AUTHORIZE_ENDPOINT_TEMPLATE = "https://{host}/{tenant}/oauth2/v2.0/authorize";
    private static final String INSTANCE_DISCOVERY_ENDPOINT_TEMPLATE = "https://{host}/common/discovery/instance";
    private static final String INSTANCE_DISCOVERY_REQUEST_PARAMETERS_TEMPLATE = "?api-version=1.1&authorization_endpoint={authorizeEndpoint}";
    private static final String HOST_TEMPLATE_WITH_REGION = "{region}.login.microsoft.com";
    private static final String SOVEREIGN_HOST_TEMPLATE_WITH_REGION = "{region}.{host}";
    private static final String REGION_NAME = "REGION_NAME";
    private static final int PORT_NOT_SET = -1;

    // For information of the current api-version refer: https://docs.microsoft.com/en-us/azure/virtual-machines/windows/instance-metadata-service#versioning
    private static final String DEFAULT_API_VERSION = "2021-02-01";
    private static final String IMDS_ENDPOINT = "http://169.254.169.254/metadata/instance/compute?api-version=" + DEFAULT_API_VERSION;

    private static final int IMDS_TIMEOUT = 2;
    private static final TimeUnit IMDS_TIMEOUT_UNIT = TimeUnit.SECONDS;
    static final TreeSet<String> TRUSTED_HOSTS_SET = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    static final TreeSet<String> TRUSTED_SOVEREIGN_HOSTS_SET = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    private static final Logger LOG = LoggerFactory.getLogger(AadInstanceDiscoveryProvider.class);

    static ConcurrentHashMap<String, InstanceDiscoveryMetadataEntry> cache = new ConcurrentHashMap<>();

    static {
        TRUSTED_SOVEREIGN_HOSTS_SET.addAll(Arrays.asList(
                "login.chinacloudapi.cn",
                "login.partner.microsoftonline.cn",
                "login-us.microsoftonline.com",
                "login.microsoftonline.de",
                "login.microsoftonline.us",
                "login.usgovcloudapi.net",
                "login.sovcloud-identity.fr",
                "login.sovcloud-identity.de",
                "login.sovcloud-identity.sg"));

        TRUSTED_HOSTS_SET.addAll(Arrays.asList(
                DEFAULT_TRUSTED_HOST,
                "login.windows.net",
                "login.microsoft.com",
                "sts.windows.net"));

        TRUSTED_HOSTS_SET.addAll(TRUSTED_SOVEREIGN_HOSTS_SET);
    }

    static InstanceDiscoveryMetadataEntry getMetadataEntry(URL authorityUrl,
                                                           boolean validateAuthority,
                                                           MsalRequest msalRequest,
                                                           ServiceBundle serviceBundle) {
        String host = authorityUrl.getHost();

        //If instanceDiscovery flag set to false OR this is a managed identity scenario, cache a basic instance metadata entry to skip this and future lookups
        if (msalRequest.application() instanceof ManagedIdentityApplication || !((AbstractClientApplicationBase) msalRequest.application()).instanceDiscovery()) {
            if (cache.get(host) == null) {
                LOG.debug("Instance discovery set to false, caching a default entry.");
                cacheInstanceDiscoveryMetadata(host);
            }
            return cache.get(host);
        }

        //If a region was set by an app developer or previously found through autodetection, adjust the authority host to use it
        if (shouldUseRegionalEndpoint(msalRequest) && ((AbstractClientApplicationBase) msalRequest.application()).azureRegion() != null) {
            host = getRegionalizedHost(authorityUrl.getHost(), ((AbstractClientApplicationBase) msalRequest.application()).azureRegion());
        }

        //If there is no cached instance metadata, do instance discovery cache the result
        if (cache.get(host) == null) {
            LOG.debug("No cached instance metadata, will attempt instance discovery.");

            if (shouldUseRegionalEndpoint(msalRequest)) {
                LOG.debug("Region API used, will attempt to discover Azure region.");

                //Server side telemetry requires the result from region discovery when any part of the region API is used
                String detectedRegion = discoverRegion(msalRequest, serviceBundle);

                //If region autodetection is enabled and a specific region was not already set, set the application's
                // region to the discovered region so that future requests can skip the IMDS endpoint call
                if (((AbstractClientApplicationBase) msalRequest.application()).azureRegion() == null
                        && ((AbstractClientApplicationBase) msalRequest.application()).autoDetectRegion()
                        && detectedRegion != null) {
                    LOG.debug("Region autodetection found {}, this region will be used for future calls.", detectedRegion);

                    ((AbstractClientApplicationBase) msalRequest.application()).azureRegion = detectedRegion;
                    host = getRegionalizedHost(authorityUrl.getHost(), ((AbstractClientApplicationBase) msalRequest.application()).azureRegion());
                }

                cacheRegionInstanceMetadata(authorityUrl.getHost(), host);
                serviceBundle.getServerSideTelemetry().getCurrentRequest().regionOutcome(
                        determineRegionOutcome(detectedRegion, ((AbstractClientApplicationBase) msalRequest.application()).azureRegion(), ((AbstractClientApplicationBase) msalRequest.application()).autoDetectRegion()));
            }

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

        try {
            return JsonHelper.convertJsonStringToJsonSerializableObject(instanceDiscoveryJson, AadInstanceDiscoveryResponse::fromJson);
        } catch (Exception ex) {
            throw new MsalClientException("Error parsing instance discovery response. Data must be " +
                    "in valid JSON format. For more information, see https://aka.ms/msal4j-instance-discovery",
                    AuthenticationErrorCode.INVALID_INSTANCE_DISCOVERY_METADATA);
        }
    }

    static void cacheInstanceDiscoveryResponse(String host,
                                               AadInstanceDiscoveryResponse aadInstanceDiscoveryResponse) {

        if (aadInstanceDiscoveryResponse != null && aadInstanceDiscoveryResponse.metadata() != null) {
            for (InstanceDiscoveryMetadataEntry entry : aadInstanceDiscoveryResponse.metadata()) {
                for (String alias : entry.aliases()) {
                    cache.put(alias, entry);
                }
            }
        }

        cacheInstanceDiscoveryMetadata(host);
    }

    static void cacheInstanceDiscoveryMetadata(String host) {
        InstanceDiscoveryMetadataEntry knownEntry = KnownMetadataProvider.getMetadataEntry(host);
        if (knownEntry != null) {
            for (String alias : knownEntry.aliases()) {
                cache.putIfAbsent(alias, knownEntry);
            }
        } else {
            cache.putIfAbsent(host, new InstanceDiscoveryMetadataEntry(host, host, Collections.singleton(host)));
        }
    }

    private static boolean shouldUseRegionalEndpoint(MsalRequest msalRequest){
        if (((AbstractClientApplicationBase) msalRequest.application()).azureRegion() != null
                || ((AbstractClientApplicationBase) msalRequest.application()).autoDetectRegion()){
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
                    LOG.warn("Regional endpoints are only available for client credential flow, request will fall back to using the global endpoint. See here for more information about supported scenarios: https://aka.ms/msal4j-azure-regions");
                }
                return false;
            }
        }
        return false;
    }

    static void cacheRegionInstanceMetadata(String originalHost, String regionalHost) {

        Set<String> aliases = new HashSet<>();
        aliases.add(originalHost);

        cache.putIfAbsent(regionalHost,  new InstanceDiscoveryMetadataEntry(regionalHost, originalHost, aliases));
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
                    replace("{region}", region);

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

    static String getInstanceDiscoveryEndpoint(URL authorityUrl) {

        String discoveryHost = TRUSTED_HOSTS_SET.contains(authorityUrl.getHost()) ?
                authorityUrl.getHost() :
                DEFAULT_TRUSTED_HOST;

        if (discoveryHost.equalsIgnoreCase(authorityUrl.getHost())) {
            int port = authorityUrl.getPort() == PORT_NOT_SET ?
                    authorityUrl.getDefaultPort() :
                    authorityUrl.getPort();

            discoveryHost += ":" + port;
        }

        return INSTANCE_DISCOVERY_ENDPOINT_TEMPLATE.
                replace("{host}", discoveryHost);
    }

     static AadInstanceDiscoveryResponse sendInstanceDiscoveryRequest(URL authorityUrl,
                                                                             MsalRequest msalRequest,
                                                                             ServiceBundle serviceBundle) {

        String instanceDiscoveryRequestUrl = getInstanceDiscoveryEndpoint(authorityUrl) +
                formInstanceDiscoveryParameters(authorityUrl);

        IHttpResponse httpResponse = executeRequest(instanceDiscoveryRequestUrl, msalRequest.headers().getReadonlyHeaderMap(), msalRequest, serviceBundle);

        AadInstanceDiscoveryResponse response = JsonHelper.convertJsonStringToJsonSerializableObject(httpResponse.body(), AadInstanceDiscoveryResponse::fromJson);

        if (httpResponse.statusCode() != HttpStatus.HTTP_OK) {
            if (httpResponse.statusCode() == HttpStatus.HTTP_BAD_REQUEST && response.error().equals(AuthenticationErrorCode.INVALID_INSTANCE)) {
                // instance discovery failed due to an invalid authority, throw an exception.
                throw MsalServiceExceptionFactory.fromHttpResponse(httpResponse);
            }
            // instance discovery failed due to reasons other than an invalid authority, do not perform instance discovery again in this environment.
            LOG.debug("Instance discovery failed due to an unknown error, no more instance discovery attempts will be made.");
            cacheInstanceDiscoveryMetadata(authorityUrl.getHost());
        }

        return response;
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

        return serviceBundle.getHttpHelper().executeHttpRequest(
                httpRequest,
                msalRequest.requestContext(),
                serviceBundle);
    }

    static String discoverRegion(MsalRequest msalRequest, ServiceBundle serviceBundle) {

        CurrentRequest currentRequest = serviceBundle.getServerSideTelemetry().getCurrentRequest();

        //Check if the REGION_NAME environment variable has a value for the region
        if (System.getenv(REGION_NAME) != null) {
            LOG.info("Region found in environment variable: {}", System.getenv(REGION_NAME));
            currentRequest.regionSource(RegionTelemetry.REGION_SOURCE_ENV_VARIABLE.telemetryValue);

            return System.getenv(REGION_NAME);
        }

        //Check the IMDS endpoint to retrieve current region (will only work if application is running in an Azure VM)
        Map<String, String> headers = new HashMap<>();
        headers.put("Metadata", "true");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<IHttpResponse> future = executor.submit(() -> executeRequest(IMDS_ENDPOINT, headers, msalRequest, serviceBundle));

        try {
            LOG.info("Starting call to IMDS endpoint.");
            IHttpResponse httpResponse = future.get(IMDS_TIMEOUT, IMDS_TIMEOUT_UNIT);
            //If call to IMDS endpoint was successful, parse the region from the JSON response body
            if (httpResponse.statusCode() == HttpStatus.HTTP_OK) {
                String region = parseRegionFromImdsResponse(httpResponse.body());
                if (!StringHelper.isBlank(region)) {
                    LOG.info("Region retrieved from IMDS endpoint: {}", region);
                    currentRequest.regionSource(RegionTelemetry.REGION_SOURCE_IMDS.telemetryValue);

                    return region;
                }
            }
            LOG.warn("Call to local IMDS failed with status code: {}, or response was empty or missing a region", httpResponse.statusCode());
            currentRequest.regionSource(RegionTelemetry.REGION_SOURCE_FAILED_AUTODETECT.telemetryValue);
        } catch (Exception ex) {
            // handle other exceptions
            //IMDS call failed, cannot find region
            //The IMDS endpoint is only available from within an Azure environment, so the most common cause of this
            //  exception will likely be java.net.SocketException: Network is unreachable: connect
            LOG.warn("Exception during call to local IMDS endpoint: {}", ex.getMessage());
            currentRequest.regionSource(RegionTelemetry.REGION_SOURCE_FAILED_AUTODETECT.telemetryValue);
            future.cancel(true);

        } finally {
            executor.shutdownNow();
        }

        return null;
    }

    /**
     * Parses the region from the IMDS {@code /compute} JSON response body, reading the
     * {@code location} field. Returns {@code null} when the body is blank, the
     * {@code location} field is missing/null, or the body is not valid JSON, so that all
     * unusable responses funnel into the same failed-autodetection path.
     */
    static String parseRegionFromImdsResponse(String responseBody) {
        if (StringHelper.isBlank(responseBody)) {
            return null;
        }

        try {
            ImdsComputeResponse computeResponse = JsonHelper.convertJsonStringToJsonSerializableObject(
                    responseBody, ImdsComputeResponse::fromJson);

            return computeResponse == null ? null : computeResponse.location();
        } catch (Exception ex) {
            //Malformed JSON: treat as an unusable response (region stays null) so the failure
            //  is consistent with the empty/missing-location cases instead of surfacing a parse error
            LOG.warn("Failed to parse IMDS compute response: {}", ex.getMessage());
            return null;
        }
    }

    private static void doInstanceDiscoveryAndCache(URL authorityUrl,
                                                    boolean validateAuthority,
                                                    MsalRequest msalRequest,
                                                    ServiceBundle serviceBundle) {

        AadInstanceDiscoveryResponse aadInstanceDiscoveryResponse = null;

        if (msalRequest.application().authenticationAuthority.authorityType.equals(AuthorityType.AAD)) {
            try {
                aadInstanceDiscoveryResponse = sendInstanceDiscoveryRequest(authorityUrl, msalRequest, serviceBundle);
            } catch (MsalServiceException ex) {
                // Throw "invalid_instance" errors: this means the authority itself is invalid.
                // All other HTTP-level errors (500, 502, 404, etc.) should fall through to the fallback path.
                if (ex.errorCode().equals(AuthenticationErrorCode.INVALID_INSTANCE)) {
                    throw ex;
                }
                LOG.warn("Instance discovery request failed with a service error. " +
                         "MSAL will use fallback instance metadata for {}. Error: {}", authorityUrl.getHost(), ex.getMessage());
                cacheInstanceDiscoveryMetadata(authorityUrl.getHost());
                return;
            } catch (Exception e) {
                // Network failures (timeout, DNS, connection refused) — cache a fallback
                // entry so subsequent calls don't retry the failing network call.
                LOG.warn("Instance discovery network request failed. " +
                         "MSAL will use fallback instance metadata for {}. Exception: {}", authorityUrl.getHost(), e.getMessage());
                cacheInstanceDiscoveryMetadata(authorityUrl.getHost());
                return;
            }

            if (validateAuthority) {
                validate(aadInstanceDiscoveryResponse);
            }
        }

        cacheInstanceDiscoveryResponse(authorityUrl.getHost(), aadInstanceDiscoveryResponse);
    }

    private static void validate(AadInstanceDiscoveryResponse aadInstanceDiscoveryResponse) {
        if (StringHelper.isBlank(aadInstanceDiscoveryResponse.tenantDiscoveryEndpoint())) {
            throw new MsalServiceException(aadInstanceDiscoveryResponse);
        }
    }
}