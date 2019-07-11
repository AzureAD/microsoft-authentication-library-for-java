// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

class BindingPolicy {

    private String value;
    private String url;
    private WSTrustVersion version;

    public BindingPolicy(String value) {
        this.value = value;
    }

    public BindingPolicy(String url, WSTrustVersion version) {
        this.url = url;
        this.version = version;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setVersion(WSTrustVersion version) {
        this.version = version;
    }

    public WSTrustVersion getVersion() {
        return this.version;
    }
}