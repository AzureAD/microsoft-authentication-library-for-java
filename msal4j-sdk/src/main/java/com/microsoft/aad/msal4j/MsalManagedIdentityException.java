// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.List;
import java.util.Map;

public class MsalManagedIdentityException extends MsalServiceException{

    public ManagedIdentitySourceType managedIdentitySourceType;

    public MsalManagedIdentityException(String errorCode, String errorMessage, ManagedIdentitySourceType sourceType ){
        super(errorMessage, errorCode);
        this.managedIdentitySourceType = sourceType;
    }

    public MsalManagedIdentityException(String errorCode, ManagedIdentitySourceType sourceType) {
        this(errorCode, "", sourceType);
    }
}
