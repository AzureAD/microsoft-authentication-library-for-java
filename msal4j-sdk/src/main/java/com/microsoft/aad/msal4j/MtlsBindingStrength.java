// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Strength with which an access token can be bound to a cryptographic key.
 */
public enum MtlsBindingStrength {
    NONE(0),
    SOFTWARE(1),
    KEY_GUARD(3);

    private final int value;

    MtlsBindingStrength(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    boolean meets(MtlsBindingStrength required) {
        return value >= required.value;
    }
}
