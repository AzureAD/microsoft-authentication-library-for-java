// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache to hold MsalInteractionRequiredException responses for Silent and RefreshToken API requests
 */
class InteractionRequiredCache {

    static int DEFAULT_CACHING_TIME_SEC = 120;
    static final int CACHE_SIZE_LIMIT_TO_TRIGGER_EXPIRED_ENTITIES_REMOVAL = 10;

    static Map<String, CachedEntity> requestsToCache = new ConcurrentHashMap<>();

    static void set(String requestHash, MsalInteractionRequiredException ex) {
        removeInvalidCacheEntities();

        long currentTimestamp = System.currentTimeMillis();

        requestsToCache.put(requestHash,
                new CachedEntity(ex,
                        currentTimestamp + DEFAULT_CACHING_TIME_SEC * 1000));
    }

    static MsalInteractionRequiredException getCachedInteractionRequiredException(String requestHash) {
        removeInvalidCacheEntities();

        if (requestsToCache.containsKey(requestHash)) {
            CachedEntity cachedEntity = requestsToCache.get(requestHash);

            if (isCacheEntityValid(cachedEntity)) {
                return cachedEntity.exception;
            } else {
                requestsToCache.remove(requestHash);
            }
        }
        return null;
    }

    private static boolean isCacheEntityValid(CachedEntity cachedEntity) {
        long expirationTimestamp = cachedEntity.expirationTimestamp;
        long currentTimestamp = System.currentTimeMillis();

        if (currentTimestamp < expirationTimestamp &&
                currentTimestamp >= expirationTimestamp - DEFAULT_CACHING_TIME_SEC * 1000) {
            return true;
        }
        return false;
    }

    private static class CachedEntity {
        MsalInteractionRequiredException exception;

        long expirationTimestamp;

        public CachedEntity(MsalInteractionRequiredException exception, long expirationTimestamp) {
            this.exception = exception;
            this.expirationTimestamp = expirationTimestamp;
        }
    }

    private static void removeInvalidCacheEntities(){
        if(requestsToCache.size() > CACHE_SIZE_LIMIT_TO_TRIGGER_EXPIRED_ENTITIES_REMOVAL){
            requestsToCache.values().removeIf(value -> !isCacheEntityValid(value));
        }
    }

    static void clear(){
        requestsToCache.clear();
    }
}
