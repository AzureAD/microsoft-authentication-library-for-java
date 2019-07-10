// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

class TelemetryConstants {
    final static String EVENT_NAME_PREFIX = "msal.";
    final static String DEFAULT_EVENT_NAME_KEY = EVENT_NAME_PREFIX + "default_event";
    final static String API_EVENT_NAME_KEY = EVENT_NAME_PREFIX + "api_event";
    final static String HTTP_EVENT_NAME_KEY = EVENT_NAME_PREFIX + "http_event";
    final static String CACHE_EVENT_NAME_KEY = EVENT_NAME_PREFIX + "cache_event";
}
