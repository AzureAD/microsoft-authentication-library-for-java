// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * An enumerator representing common HTTP request methods.
 */
public enum HttpMethod {

    /**
     * The HTTP CONNECT method.
     */
    CONNECT("CONNECT"),

    /**
     * The HTTP DELETE method.
     */
    DELETE("DELETE"),

    /**
     * The HTTP GET method.
     */
    GET("GET"),

    /**
     * The HTTP HEAD method.
     */
    HEAD("HEAD"),

    /**
     * The HTTP OPTIONS method.
     */
    OPTIONS("OPTIONS"),

    /**
     * The HTTP POST method.
     */
    POST("POST"),

    /**
     * The HTTP PUT method.
     */
    PUT("PUT"),

    /**
     * The HTTP TRACE method.
     */
    TRACE("TRACE");

    public final String methodName;

    HttpMethod(String methodName) {
        this.methodName = methodName;
    }
}
