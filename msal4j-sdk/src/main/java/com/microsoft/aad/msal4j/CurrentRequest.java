// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

class CurrentRequest {

    private final PublicApi publicApi;
    private CacheRefreshReason cacheInfo = CacheRefreshReason.NOT_APPLICABLE;
    private String regionUsed = StringHelper.EMPTY_STRING;
    private int regionSource = 0;
    private int regionOutcome = 0;

    CurrentRequest(PublicApi publicApi) {
        this.publicApi = publicApi;
    }

    PublicApi publicApi() {
        return this.publicApi;
    }

    CacheRefreshReason cacheInfo() {
        return this.cacheInfo;
    }

    String regionUsed() {
        return this.regionUsed;
    }

    int regionSource() {
        return this.regionSource;
    }

    int regionOutcome() {
        return this.regionOutcome;
    }

    void cacheInfo(CacheRefreshReason cacheInfo) {
        this.cacheInfo = cacheInfo;
    }

    void regionUsed(String regionUsed) {
        this.regionUsed = regionUsed;
    }

    void regionSource(int regionSource) {
        this.regionSource = regionSource;
    }

    void regionOutcome(int regionOutcome) {
        this.regionOutcome = regionOutcome;
    }
}