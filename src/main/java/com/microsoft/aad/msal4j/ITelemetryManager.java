// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

interface ITelemetryManager {
    String generateRequestId();

    TelemetryHelper createTelemetryHelper(String requestId,
                                          String clientId,
                                          Event event,
                                          Boolean shouldFlush);
}
