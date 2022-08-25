package com.microsoft.aad.msal4j;

import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

//TODO: javadocs, explaining this class is the layer to communicate with MSALRuntime
public interface MSALRuntimeLibrary extends Library {

       MSALRuntimeLibrary MSALRuntime_INSTANCE = Native.load(
               "msalruntime.dll",
               MSALRuntimeLibrary.class);

       //TODO: Just used for easier references in methods, can probably replace the callback interfaces with JNA's 'pointerType' to keep things generic but readable
       interface AuthResultCallbackData { }
       interface AccountResultCallbackData { }
       interface SignOutResultCallbackData { }
       interface LogCallBackData { }

       public interface AuthResultCallbackInterface extends Callback {
              void callback(LongByReference authResult, AuthResultCallbackData callbackData);
       }

       public interface AccountResultCallbackInterface extends Callback {
              void callback(LongByReference authResult, AccountResultCallbackData callbackData);
       }

       public interface SignOutResultCallbackInterface extends Callback {
              void callback(LongByReference authResult, SignOutResultCallbackData callbackData);
       }

       public interface LogCallbackInterface extends Callback {
              void callback(LongByReference authResult, LogCallBackData callbackData);
       }

       interface AsyncHandleInterface extends Callback {
       }


       //TODO: need to figure out best way to use JNA to convert Java enum <-> MSALRuntime ResponsStatus enum
       @Structure.FieldOrder({"unused"})
       public class MSALRUNTIME_RESPONSE_STATUS extends Structure {
              public int unused;
       }


       //MSALRuntime.h
       LongByReference MSALRUNTIME_Startup();
       void MSALRUNTIME_Shutdown();
       LongByReference MSALRUNTIME_ReadAccountByIdAsync(String accountId, String correlationId, AccountResultCallbackInterface callback, String callbackData, AsyncHandleInterface asyncHandle);
       LongByReference MSALRUNTIME_SignInAsync(long parentHwnd, LongByReference authParameters, String correlationId, String accountHint, AuthResultCallbackInterface callback, String callbackData, AsyncHandleInterface asyncHandle);
       LongByReference MSALRUNTIME_SignInSilentlyAsync(long authParameters, WString correlationId, AuthResultCallbackInterface callback, Pointer callbackData, IntByReference asyncHandle);
       LongByReference MSALRUNTIME_SignInInteractivelyAsync(long parentHwnd, LongByReference authParameters, String correlationId, String accountHint, AuthResultCallbackInterface callback, AuthResultCallbackData callbackData, AsyncHandleInterface asyncHandle);
       LongByReference MSALRUNTIME_AcquireTokenSilentlyAsync(long authParameters, WString correlationId, Long account, AuthResultCallbackInterface callback, Pointer callbackData, IntByReference asyncHandle);
       LongByReference MSALRUNTIME_AcquireTokenInteractivelyAsync(long parentHwnd, LongByReference authParameters, String correlationId, long account, AuthResultCallbackInterface callback, AuthResultCallbackData callbackData, AsyncHandleInterface asyncHandle);
       LongByReference MSALRUNTIME_SignOutSilentlyAsync(String clientId, String correlationId, long account, SignOutResultCallbackInterface callback, String callbackData, AsyncHandleInterface asyncHandle);
       LongByReference MSALRUNTIME_DiscoverAccountsAsync(String clientId, String correlationId, AccountResultCallbackInterface callback, String callbackData, AsyncHandleInterface asyncHandle);

       //MSALRuntimeAccount.h
       LongByReference MSALRUNTIME_ReleaseAccount(long account);
       LongByReference MSALRUNTIME_GetAccountId(long account, MSALRuntimeUtils.StringByReference accountId, IntByReference bufferSize);
       LongByReference MSALRUNTIME_GetClientInfo(long account, MSALRuntimeUtils.StringByReference clientInfo, IntByReference bufferSize);
       LongByReference MSALRUNTIME_GetAccountProperty(long account, String key, String value, IntByReference bufferSize);

       //MSALRuntimeAuthParameters.h
       LongByReference MSALRUNTIME_CreateAuthParameters(WString clientId, WString authority, LongByReference authParameters);
       LongByReference MSALRUNTIME_ReleaseAuthParameters(long authParameters);
       LongByReference MSALRUNTIME_SetRequestedScopes(long authParameters, WString scopes);
       LongByReference MSALRUNTIME_SetRedirectUri(long authParameters, WString redirectUri);
       LongByReference MSALRUNTIME_SetDecodedClaims(long authParameters, WString claims);
       LongByReference MSALRUNTIME_SetAccessTokenToRenew(LongByReference authParameters, String accessTokenToRenew);

       LongByReference MSALRUNTIME_SetPopParams(LongByReference authParameters, String httpMethod, String uriHost, String uriPath, String nonce);

       LongByReference MSALRUNTIME_SetAdditionalParameter(LongByReference authParameters, String key, String value);

       //MSALRuntimeAuthResult.h
       LongByReference MSALRUNTIME_ReleaseAuthResult(long authResult);
       LongByReference MSALRUNTIME_GetAccount(LongByReference authResult, LongByReference account);
       LongByReference MSALRUNTIME_GetIdToken(LongByReference authResult, MSALRuntimeUtils.StringByReference idToken, IntByReference bufferSize);
       LongByReference MSALRUNTIME_GetRawIdToken(LongByReference authResult, MSALRuntimeUtils.StringByReference rawIdToken, IntByReference bufferSize);
       LongByReference MSALRUNTIME_GetAccessToken(LongByReference authResult, MSALRuntimeUtils.StringByReference accessToken, IntByReference bufferSize);
       LongByReference MSALRUNTIME_GetGrantedScopes(long authResult, String grantedScopes, IntByReference bufferSize);
       LongByReference MSALRUNTIME_GetAuthorizationHeader(long authResult, String authHeader, IntByReference bufferSize);
       LongByReference MSALRUNTIME_IsPopAuthorization(long authResult, boolean isPopAuthorization);
       LongByReference MSALRUNTIME_GetExpiresOn(long authResult, LongByReference accessTokenExpirationTime);
       LongByReference MSALRUNTIME_GetError(LongByReference authResult, LongByReference responseError);
       LongByReference MSALRUNTIME_GetTelemetryData(long authResult, String telemetryData, IntByReference bufferSize);

       //MSALRuntimeReadAccountResult.h
       LongByReference MSALRUNTIME_ReleaseReadAccountResult(long readAccountResult);
       LongByReference MSALRUNTIME_GetReadAccount(long readAccountResult, long account);
       LongByReference MSALRUNTIME_GetReadAccountError(long readAccountResult, LongByReference responseError);
       LongByReference MSALRUNTIME_GetReadAccountTelemetryData(long readAccountResult, String telemetryData, IntByReference bufferSize);

       //MSALRuntimeSignoutResult.h
       LongByReference MSALRUNTIME_ReleaseSignOutResult(long signoutResult);
       LongByReference MSALRUNTIME_GetSignOutError(long signoutResult, LongByReference responseError);
       LongByReference MSALRUNTIME_GetSignOutTelemetryData(long signoutResult, String telemetryData, IntByReference bufferSize);

       //MSALRuntimeDiscoverAccountsResult.h
       LongByReference MSALRUNTIME_ReleaseDiscoverAccountsResult(long discoverAccountsResult);
       LongByReference MSALRUNTIME_GetDiscoverAccountsAt(long discoverAccountsResult, int index, long account);
       LongByReference MSALRUNTIME_GetDiscoverAccountsError(long discoverAccountsResult, LongByReference responseError);
       LongByReference MSALRUNTIME_GetDiscoverAccountsTelemetryData(long discoverAccountsResult, String telemetryData, IntByReference bufferSize);

       //MSALRuntimeError.h
       //bool_t instead of error handle
       LongByReference MSALRUNTIME_ReleaseError(LongByReference error);
       LongByReference MSALRUNTIME_GetStatus(LongByReference error, LongByReference responseStatus);
       LongByReference MSALRUNTIME_GetErrorCode(LongByReference error, LongByReference responseErrorCode);
       LongByReference MSALRUNTIME_GetTag(long error, IntByReference responseErrorTag);
       LongByReference MSALRUNTIME_GetContext(LongByReference error, MSALRuntimeUtils.StringByReference context, IntByReference bufferSize);

       //MSALRuntimeCancel.h
       LongByReference MSALRUNTIME_ReleaseAsyncHandle(AsyncHandleInterface asyncHandle);
       LongByReference MSALRUNTIME_CancelAsyncOperation(AsyncHandleInterface asyncHandle);

       //MSALRuntimeLogging.h
       LongByReference MSALRUNTIME_RegisterLogCallback(LogCallbackInterface callback, String callbackData, long callbackHandle);
       LongByReference MSALRUNTIME_ReleaseLogCallbackHandle(long callbackHandle);
}




