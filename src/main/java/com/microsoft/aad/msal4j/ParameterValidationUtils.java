// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Set;

class ParameterValidationUtils {

    static void validateNotBlank(String name, String value) {
        if (StringHelper.isBlank(value)) {
            throw new IllegalArgumentException(name + " is null or empty");
        }
    }

    static void validateNotNull(String name, Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException(name + " is null");
        }
    }

    static void validateNotEmpty(String name, Set<String> set) {
        if (set == null || set.isEmpty()) {
            throw new IllegalArgumentException(name + " is null or empty");
        }
    }

    static void validateNotEmpty(String name, char[] arr) {
        if (arr == null || arr.length == 0) {
            throw new IllegalArgumentException(name + " is null or empty");
        }
    }
}
