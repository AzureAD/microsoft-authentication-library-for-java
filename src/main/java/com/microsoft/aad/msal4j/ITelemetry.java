// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

interface ITelemetry {
    void startEvent(String requestId, Event eventToStart);

    void stopEvent(String requestId, Event eventToEnd);

    void flush(String requestId, String clientId);
}
