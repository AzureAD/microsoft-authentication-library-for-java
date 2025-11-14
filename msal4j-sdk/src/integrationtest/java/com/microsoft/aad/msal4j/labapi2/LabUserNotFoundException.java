// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

/**
 * Exception thrown when a lab user matching the query cannot be found.
 */
public class LabUserNotFoundException extends RuntimeException {

    private final UserQuery query;

    public LabUserNotFoundException(UserQuery query, String message) {
        super(message);
        this.query = query;
    }

    public UserQuery getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return String.format("LabUserNotFoundException{query=%s, message=%s}",
                query, getMessage());
    }
}