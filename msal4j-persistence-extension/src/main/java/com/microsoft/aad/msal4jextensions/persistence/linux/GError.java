// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence.linux;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * Error returned by libsecret library if saving or retrieving fails
 * https://developer.gnome.org/glib/stable/glib-Error-Reporting.html
 */
class GError extends Structure {

    public int domain;

    public int code;

    public String message;

    GError(Pointer p) {
        super(p);
        read();
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("domain", "code", "message");
    }
}
