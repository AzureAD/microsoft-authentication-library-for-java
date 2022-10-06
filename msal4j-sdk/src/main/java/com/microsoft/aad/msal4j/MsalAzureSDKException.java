package com.microsoft.aad.msal4j;

/**
 * Exception type thrown when Azure SDK returns an error response.
 */
public class MsalAzureSDKException extends MsalException{
    public MsalAzureSDKException(Throwable throwable) {
        super(throwable);
    }

    public MsalAzureSDKException(String message, String errorCode) {
        super(message, errorCode);
    }
}
