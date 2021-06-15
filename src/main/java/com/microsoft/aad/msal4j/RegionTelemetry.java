// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Telemetry values covering the use of regions in the library
 */
enum RegionTelemetry {
    /**
     * These values represent the source of the region info: either from the cache (2), environment variables (3), or the IMDS endpoint (4)
     * One value is used for the failure cause when region autodetection was requested but a region could not be found (1)
     */
    REGION_SOURCE_FAILED_AUTODETECT(1),
    REGION_SOURCE_CACHE(2),
    REGION_SOURCE_ENV_VARIABLE(3),
    REGION_SOURCE_IMDS(4),
    /**
     * These values represent the result of the attempt to find region info
     * Three values cover cases where developer provided a region and either it matches the autodetected region (1),
     *   autodetection failed (2), or the autodetected region does not match the developer provided region (3)
     * Two values cover cases where developer just requested autodetection, and we either detected the region (4) or failed (5)
     */
    REGION_OUTCOME_DEVELOPER_AUTODETECT_MATCH(1),
    REGION_OUTCOME_DEVELOPER_AUTODETECT_FAILED(2),
    REGION_OUTCOME_DEVELOPER_AUTODETECT_MISMATCH(3),
    REGION_OUTCOME_AUTODETECT_SUCCESS(4),
    REGION_OUTCOME_AUTODETECT_FAILED(5);

    final int telemetryValue;

    RegionTelemetry(int telemetryValue){
        this.telemetryValue = telemetryValue;
    }
}
