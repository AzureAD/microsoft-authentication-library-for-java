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

    /**
     * Gets the value.
     * 
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value.
     * 
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the url.
     * 
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the url.
     * 
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Sets the version.
     * 
     * @param version the version to set
     */
    public void setVersion(WSTrustVersion version) {
        this.version = version;
    }

    /**
     * Gets the version.
     * 
     * @return the version
     */
    public WSTrustVersion getVersion() {
        return this.version;
    }
}