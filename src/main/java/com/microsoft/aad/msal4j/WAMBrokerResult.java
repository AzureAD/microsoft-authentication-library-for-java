package com.microsoft.aad.msal4j;//TODO: will be move to JavaMSALRuntime package, must not reference MSAL Java directly

import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.ptr.LongByReference;

import java.util.concurrent.CompletableFuture;

//FieldOrder needs to explicitly state the public fields that should be in the C structure JNA creates
@Structure.FieldOrder({"authParamsHandle", "accountHandle", "authResultHandle", "idToken", "accessToken", "accessTokenExpirationTime", "accountId", "accountClientInfo"})
public class WAMBrokerResult extends Structure implements MsalRuntimeLibrary.AuthResultCallbackData {
    public LongByReference authParamsHandle;
    public LongByReference accountHandle;
    public LongByReference authResultHandle;
    public WAMBrokerUtils.StringByReference idToken = new WAMBrokerUtils.StringByReference(new WString(""));
    public WAMBrokerUtils.StringByReference accessToken = new WAMBrokerUtils.StringByReference(new WString(""));
    public LongByReference accessTokenExpirationTime;
    public WAMBrokerUtils.StringByReference accountId = new WAMBrokerUtils.StringByReference(new WString(""));//TODO: split AuthenticationResult into SignInResult (id token/account) and TokenResult (access tokens)? Or keep it simple all-in-one like MSAL Java's AuthenticationResult?
    public WAMBrokerUtils.StringByReference accountClientInfo = new WAMBrokerUtils.StringByReference(new WString(""));
    private MsalRuntimeLibrary msalRuntimeLibrary;
    private WAMBroker brokerInstance;

    WAMBrokerResult(WAMBroker broker) {
        this.brokerInstance = broker;
        this.msalRuntimeLibrary = brokerInstance.msalRuntimeLibraryInstance;
    }

    void initializeAuthParameters(String clientId, String authority, String scopes, String redirectUri, String claims) {

        WAMBrokerErrorHandler.parseAndReleaseError(msalRuntimeLibrary.MSALRUNTIME_CreateAuthParameters(new WString(clientId),
                new WString(authority),
                authParamsHandle), msalRuntimeLibrary);
        if (authParamsHandle != null) {
            brokerInstance.incrementRefsCounter();//reference to an MSALRUNTIME_AUTH_PARAMETERS_HANDLE

            //TODO: need better error handling for required/optional parameters
            WAMBrokerErrorHandler.parseAndReleaseError(msalRuntimeLibrary.MSALRUNTIME_SetRequestedScopes(authParamsHandle.getValue(), new WString(scopes)), msalRuntimeLibrary);
            if (redirectUri != null) {
                WAMBrokerErrorHandler.parseAndReleaseError(msalRuntimeLibrary.MSALRUNTIME_SetRedirectUri(authParamsHandle.getValue(), new WString(redirectUri)), msalRuntimeLibrary);
            }
            if (claims != null) {
                WAMBrokerErrorHandler.parseAndReleaseError(msalRuntimeLibrary.MSALRUNTIME_SetDecodedClaims(authParamsHandle.getValue(), new WString(claims)), msalRuntimeLibrary);
            }
        }
    }

    void parseResult() {
        LongByReference error = new LongByReference();

        if (authResultHandle != null) {
            brokerInstance.incrementRefsCounter();//reference to an MSALRUNTIME_AUTH_RESULT_HANDLE

            WAMBrokerErrorHandler.parseAndReleaseError(msalRuntimeLibrary.MSALRUNTIME_GetError(authResultHandle, error), msalRuntimeLibrary);
            WAMBrokerErrorHandler.parseAndReleaseError(error, msalRuntimeLibrary); // TODO: can shortcut and not bother trying to parse authResult?
        }
        // Retrieve any tokens from auth result
        accessToken = WAMBrokerUtils.getString(error, msalRuntimeLibrary, msalRuntimeLibrary::MSALRUNTIME_GetAccessToken);
        idToken = WAMBrokerUtils.getString(error, msalRuntimeLibrary, msalRuntimeLibrary::MSALRUNTIME_GetRawIdToken);

        WAMBrokerErrorHandler.parseAndReleaseError(msalRuntimeLibrary.MSALRUNTIME_GetAccount(authResultHandle, accountHandle), msalRuntimeLibrary);

        // Retrieve any account info from auth result
        if (accountHandle != null) {
            brokerInstance.incrementRefsCounter();//reference to an MSALRUNTIME_ACCOUNT_HANDLE

            accountId = WAMBrokerUtils.getString(error, msalRuntimeLibrary, msalRuntimeLibrary::MSALRUNTIME_GetAccountId);
            accountClientInfo = WAMBrokerUtils.getString(error, msalRuntimeLibrary, msalRuntimeLibrary::MSALRUNTIME_GetClientInfo);
        }
    }

    static class authResultCallback implements MsalRuntimeLibrary.AuthResultCallbackInterface {
        @Override
        public void callback(LongByReference authResult, MsalRuntimeLibrary.AuthResultCallbackData callbackData) {
            ((WAMBrokerResult) callbackData).authResultHandle = authResult;
            //TODO: JNA may be setting up threads in a weird way, may need to replace with just logic around the ref counter in MSALRuntimeUtils
            CompletableFuture<Void> future = CompletableFuture.runAsync(((WAMBrokerResult) callbackData)::parseResult);
            future.join();
        }
    }

    //Helper method to call any relevant handle release functions in MSALRuntime
    //TODO: ensure all handles are getting released
    void releaseAllHandles (){
        releaseAuthParams();
        releaseAuthResult();
        releaseAccountResult();
    }

    void releaseAuthParams() {
        if (authParamsHandle != null) {
            WAMBrokerErrorHandler.parseAndReleaseError(msalRuntimeLibrary.MSALRUNTIME_ReleaseAuthParameters(authParamsHandle.getValue()), msalRuntimeLibrary);
            brokerInstance.decrementRefsCounter();
        }
    }

    void releaseAuthResult() {
        if (authResultHandle != null) {
            WAMBrokerErrorHandler.parseAndReleaseError(msalRuntimeLibrary.MSALRUNTIME_ReleaseAuthResult(authResultHandle.getValue()), msalRuntimeLibrary);
            brokerInstance.decrementRefsCounter();
        }
    }

    void releaseAccountResult() {
        if (accountHandle != null) {
            WAMBrokerErrorHandler.parseAndReleaseError(msalRuntimeLibrary.MSALRUNTIME_ReleaseAccount(accountHandle.getValue()), msalRuntimeLibrary);
            brokerInstance.decrementRefsCounter();
        }

    }
}

