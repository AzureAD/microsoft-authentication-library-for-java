package com.microsoft.aad.msal4j;

import java.util.List;
import java.util.Map;

public class MsalManagedIdentityException extends MsalServiceException{

    ManagedIdentitySourceType managedIdentitySourceType;

    public MsalManagedIdentityException(String errorCode, String errorMessage, ManagedIdentitySourceType sourceType ){
        super(errorMessage, errorCode);
        this.managedIdentitySourceType = sourceType;
    }
    public MsalManagedIdentityException(String message, String error) {
        super(message, error);
    }

    public MsalManagedIdentityException(ErrorResponse errorResponse, Map<String, List<String>> httpHeaders) {
        super(errorResponse, httpHeaders);
    }

    public MsalManagedIdentityException(AadInstanceDiscoveryResponse discoveryResponse) {
        super(discoveryResponse);
    }
}
