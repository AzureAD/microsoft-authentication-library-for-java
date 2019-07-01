// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

public enum AuthenticationErrorCode {

    UNKNOWN ("unknown"),
    AUTHORIZATION_PENDING ("authorization_pending"),
    INTERACTION_REQUIRED ("interaction_required");

    private String errorCode;

    AuthenticationErrorCode(String errorCode){
        this.errorCode = errorCode;
    }

    @Override
    public String toString(){
        return errorCode;
    }
}
