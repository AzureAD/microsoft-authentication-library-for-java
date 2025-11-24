// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi;

/**
 * Exception thrown when a lab user matching the query cannot be found.
 */
class LabUserNotFoundException extends RuntimeException {

    private final UserQueryHelper query;

    LabUserNotFoundException(UserQueryHelper query, String message) {
        super(message);
        this.query = query;
    }

    @Override
    public String toString() {
        return String.format("LabUserNotFoundException{query=%s, message=%s}",
                query, getMessage());
    }
}