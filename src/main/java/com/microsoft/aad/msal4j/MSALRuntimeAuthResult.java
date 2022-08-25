package com.microsoft.aad.msal4j;//TODO: will be move to JavaMSALRuntime package, must not reference MSAL Java directly

import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import java.util.concurrent.CompletableFuture;

//FieldOrder needs to explicitly state the public fields that should be in the C structure JNA creates
@Structure.FieldOrder({"authParamsHandle", "accountHandle", "authResultHandle", "idToken", "accessToken", "accessTokenExpirationTime", "accountId", "accountClientInfo"})
public class MSALRuntimeAuthResult extends Structure implements MSALRuntimeLibrary.AuthResultCallbackData {
    public LongByReference authParamsHandle;
    public LongByReference accountHandle;
    public LongByReference authResultHandle;
    public MSALRuntimeUtils.StringByReference idToken = new MSALRuntimeUtils.StringByReference(new WString(""));
    public MSALRuntimeUtils.StringByReference accessToken = new MSALRuntimeUtils.StringByReference(new WString(""));
    public LongByReference accessTokenExpirationTime;
    public MSALRuntimeUtils.StringByReference accountId = new MSALRuntimeUtils.StringByReference(new WString(""));//TODO: split AuthenticationResult into SignInResult (id token/account) and TokenResult (access tokens)? Or keep it simple all-in-one like MSAL Java's AuthenticationResult?
    public MSALRuntimeUtils.StringByReference accountClientInfo = new MSALRuntimeUtils.StringByReference(new WString(""));
    private MSALRuntimeLibrary MSALRuntime_INSTANCE;
    private MSALRuntimeError MSALRuntimeErrorHandler;

    public void setMSALRuntimeLibraryInstance (MSALRuntimeLibrary MSALRuntime_INSTANCE) {
        this.MSALRuntime_INSTANCE = MSALRuntime_INSTANCE;
    }

    public void setMSALRuntimeErrorHandler (MSALRuntimeLibrary MSALRuntime_INSTANCE) {
        this.MSALRuntimeErrorHandler = new MSALRuntimeError();
        this.MSALRuntimeErrorHandler.MSALRuntime_INSTANCE = MSALRuntime_INSTANCE;
    }

    public void initializeAuthParameters(String clientId, String authority, String scopes, String redirectUri, String claims) {

        System.out.println("===Creating auth parameters");
        MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_CreateAuthParameters(new WString(clientId),
                new WString(authority),
                authParamsHandle));
        MSALRuntimeUtils.incrementRefsCounter(MSALRuntime_INSTANCE, this.MSALRuntimeErrorHandler);//reference to an MSALRUNTIME_AUTH_PARAMETERS_HANDLE

        System.out.println("===Setting parameters");
        MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_SetRedirectUri(authParamsHandle.getValue(), new WString("https://login.microsoftonline.com/common/oauth2/nativeclient")));
        MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_SetRequestedScopes(authParamsHandle.getValue(), new WString(scopes)));
        MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_SetRedirectUri(authParamsHandle.getValue(), new WString(redirectUri)));
        if (claims != null) {
            MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_SetDecodedClaims(authParamsHandle.getValue(), new WString(claims)));
        }
    }

    public void parseAndReleaseAuthResult_SignInSilently() {
        System.out.println("==Parse sign in");
        LongByReference error = new LongByReference();
        MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_GetError(authResultHandle, error));//TODO: parse and return any errors to MSAL Java
        MSALRuntimeErrorHandler.printAndReleaseError(error); // TODO: can shortcut and not bother trying to parse authResult?
        MSALRuntimeUtils.incrementRefsCounter(MSALRuntime_INSTANCE, this.MSALRuntimeErrorHandler);//reference to an MSALRUNTIME_AUTH_RESULT_HANDLE

        // Retrieve any tokens from auth result
        IntByReference bufferSize = new IntByReference(0);
        MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_GetAccessToken(authResultHandle, null, bufferSize));//TODO: the thing about calling it twice to get the buffer size
        MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_GetAccessToken(authResultHandle, accessToken, bufferSize));

        bufferSize = new IntByReference(0);
        MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_GetRawIdToken(authResultHandle, null, bufferSize));
        MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_GetRawIdToken(authResultHandle, idToken, bufferSize));

        MSALRuntime_INSTANCE.MSALRUNTIME_GetAccount(authResultHandle, accountHandle);

        // Retrieve any account info from auth result
        if (accountHandle != null) {
            MSALRuntimeUtils.incrementRefsCounter(MSALRuntime_INSTANCE, this.MSALRuntimeErrorHandler);//reference to an MSALRUNTIME_ACCOUNT_HANDLE
            bufferSize = new IntByReference(0);
            MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_GetAccountId(accountHandle.getValue(), accountId, bufferSize));
            MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_GetAccountId(accountHandle.getValue(), accountId, bufferSize));

            bufferSize = new IntByReference(0);
            MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_GetClientInfo(accountHandle.getValue(), accountClientInfo, bufferSize));
            MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_GetClientInfo(accountHandle.getValue(), accountClientInfo, bufferSize));

            MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_ReleaseAccount(accountHandle.getValue()));
        }
    }



    public static class authResultCallback implements MSALRuntimeLibrary.AuthResultCallbackInterface {
        @Override
        public void callback(LongByReference authResult, MSALRuntimeLibrary.AuthResultCallbackData callbackData) {
            System.out.println("!!!Callback");
            ((MSALRuntimeAuthResult) callbackData).authResultHandle = authResult;
            //TODO: JNA may be setting up threads in a weird way, may need to replace with just logic around the ref counter in MSALRuntimeUtils
            CompletableFuture<Void> future = CompletableFuture.runAsync(((MSALRuntimeAuthResult) callbackData)::parseAndReleaseAuthResult_SignInSilently);
            future.join();
            System.out.println("!!!Completed callback");
        }
    }

    public void releaseAuthParams() {
        MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_ReleaseAuthParameters(authParamsHandle.getValue()));
        MSALRuntimeUtils.decrementRefsCounter(MSALRuntime_INSTANCE);
    }

    public void releaseAuthResult() {
        MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_ReleaseAuthResult(authResultHandle.getValue()));
        MSALRuntimeUtils.decrementRefsCounter(MSALRuntime_INSTANCE);
    }

    public void releaseAccountResult() {
        MSALRuntimeErrorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_ReleaseAuthResult(accountHandle.getValue()));
        MSALRuntimeUtils.decrementRefsCounter(MSALRuntime_INSTANCE);
    }
}

