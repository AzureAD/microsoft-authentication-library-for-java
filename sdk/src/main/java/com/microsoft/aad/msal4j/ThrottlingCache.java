// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache to hold requests to be throttled
 */
class ThrottlingCache {

    static final int MAX_THROTTLING_TIME_SEC = 3600;
    static int DEFAULT_THROTTLING_TIME_SEC = 120;
    static final int CACHE_SIZE_LIMIT_TO_TRIGGER_EXPIRED_ENTITIES_REMOVAL = 100;

    // request hash to expiration timestamp
    static Map<String, Long> requestsToThrottle = new ConcurrentHashMap<>();

    static void set(String requestHash, Long expirationTimestamp) {
        removeInvalidCacheEntities();

        requestsToThrottle.put(requestHash, expirationTimestamp);
    }

    static long retryInMs(String requestHash) {
        removeInvalidCacheEntities();

        if (requestsToThrottle.containsKey(requestHash)) {
            long expirationTimestamp = requestsToThrottle.get(requestHash);
            long currentTimestamp = System.currentTimeMillis();

            if (isCacheEntryValid(currentTimestamp, expirationTimestamp)) {
                return expirationTimestamp - currentTimestamp;
            } else {
                requestsToThrottle.remove(requestHash);
            }
        }
        return 0;
    }

    private static boolean isCacheEntryValid(long currentTimestamp, long expirationTimestamp) {
        return currentTimestamp < expirationTimestamp &&
                currentTimestamp >= expirationTimestamp - MAX_THROTTLING_TIME_SEC * 1000;
    }

    private static void removeInvalidCacheEntities() {
        long currentTimestamp = System.currentTimeMillis();

        if (requestsToThrottle.size() > CACHE_SIZE_LIMIT_TO_TRIGGER_EXPIRED_ENTITIES_REMOVAL) {
            requestsToThrottle.values().removeIf(value -> !isCacheEntryValid(value, currentTimestamp));
        }
    }

    static void clear() {
        requestsToThrottle.clear();
    }
}
