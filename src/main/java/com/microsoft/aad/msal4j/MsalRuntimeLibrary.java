package com.microsoft.aad.msal4j;

import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

//TODO: javadocs, explaining this class is the layer to communicate with MSALRuntime
public interface MsalRuntimeLibrary extends Library {

       MsalRuntimeLibrary MSALRuntime_INSTANCE = Native.load(
               "msalruntime.dll",
               MsalRuntimeLibrary.class);

       //TODO: Just used for easier references in methods, can probably replace the callback interfaces with JNA's 'pointerType' to keep things generic but readable
       interface AuthResultCallbackData { }
       interface AccountResultCallbackData { }
       interface SignOutResultCallbackData { }
       interface LogCallBackData { }

       interface AuthResultCallbackInterface extends Callback {
              void callback(LongByReference authResult, AuthResultCallbackData callbackData);
       }

       interface SignOutResultCallbackInterface extends Callback {
              void callback(LongByReference authResult, SignOutResultCallbackData callbackData);
       }

       interface LogCallbackInterface extends Callback {
              void callback(LongByReference authResult, LogCallBackData callbackData);
       }

       interface AsyncHandleInterface extends Callback {
              void callback(LongByReference authResult, LogCallBackData callbackData);
       }


       //TODO: need to figure out best way to use JNA to convert Java enum <-> MSALRuntime ResponsStatus enum
       @Structure.FieldOrder({"unused"})
       public class MSALRUNTIME_RESPONSE_STATUS extends Structure {
              public int unused;
       }


       //MSALRuntime.h
       LongByReference MSALRUNTIME_Startup();
       void MSALRUNTIME_Shutdown();
       LongByReference MSALRUNTIME_ReadAccountByIdAsync(WString accountId, WString correlationId, AuthResultCallbackInterface callback, Pointer callbackData, IntByReference asyncHandle);
       LongByReference MSALRUNTIME_SignInAsync(long parentHwnd, long authParameters, WString correlationId, WString accountHint, AuthResultCallbackInterface callback, Pointer callbackData, IntByReference asyncHandle);
       LongByReference MSALRUNTIME_SignInSilentlyAsync(long authParameters, WString correlationId, AuthResultCallbackInterface callback, Pointer callbackData, IntByReference asyncHandle);
       LongByReference MSALRUNTIME_SignInInteractivelyAsync(long parentHwnd, long authParameters, WString correlationId, WString accountHint, AuthResultCallbackInterface callback, Pointer callbackData, IntByReference asyncHandle);
       LongByReference MSALRUNTIME_AcquireTokenSilentlyAsync(long authParameters, WString correlationId, long account, AuthResultCallbackInterface callback, Pointer callbackData, IntByReference asyncHandle);
       LongByReference MSALRUNTIME_AcquireTokenInteractivelyAsync(long parentHwnd, long authParameters, WString correlationId, long account, AuthResultCallbackInterface callback, Pointer callbackData, IntByReference asyncHandle);
       LongByReference MSALRUNTIME_SignOutSilentlyAsync(WString clientId, WString correlationId, long account, SignOutResultCallbackInterface callback, WString callbackData, AsyncHandleInterface asyncHandle);
       LongByReference MSALRUNTIME_DiscoverAccountsAsync(WString clientId, WString correlationId, AuthResultCallbackInterface callback, WString callbackData, AsyncHandleInterface asyncHandle);

       //MSALRuntimeAccount.h
       LongByReference MSALRUNTIME_ReleaseAccount(long account);
       LongByReference MSALRUNTIME_GetAccountId(LongByReference account, WAMBrokerUtils.StringByReference accountId, IntByReference bufferSize);
       LongByReference MSALRUNTIME_GetClientInfo(LongByReference account, WAMBrokerUtils.StringByReference clientInfo, IntByReference bufferSize);
       LongByReference MSALRUNTIME_GetAccountProperty(long account, WString key, WAMBrokerUtils.StringByReference value, IntByReference bufferSize);

       //MSALRuntimeAuthParameters.h
       LongByReference MSALRUNTIME_CreateAuthParameters(WString clientId, WString authority, LongByReference authParameters);
       LongByReference MSALRUNTIME_ReleaseAuthParameters(long authParameters);
       LongByReference MSALRUNTIME_SetRequestedScopes(long authParameters, WString scopes);
       LongByReference MSALRUNTIME_SetRedirectUri(long authParameters, WString redirectUri);
       LongByReference MSALRUNTIME_SetDecodedClaims(long authParameters, WString claims);
       LongByReference MSALRUNTIME_SetAccessTokenToRenew(LongByReference authParameters, WString accessTokenToRenew);

       LongByReference MSALRUNTIME_SetPopParams(LongByReference authParameters, WString httpMethod, WString uriHost, WString uriPath, WString nonce);

       LongByReference MSALRUNTIME_SetAdditionalParameter(LongByReference authParameters, WString key, WString value);

       //MSALRuntimeAuthResult.h
       LongByReference MSALRUNTIME_ReleaseAuthResult(long authResult);
       LongByReference MSALRUNTIME_GetAccount(LongByReference authResult, LongByReference account);
       LongByReference MSALRUNTIME_GetIdToken(LongByReference authResult, WAMBrokerUtils.StringByReference idToken, IntByReference bufferSize);
       LongByReference MSALRUNTIME_GetRawIdToken(LongByReference authResult, WAMBrokerUtils.StringByReference rawIdToken, IntByReference bufferSize);
       LongByReference MSALRUNTIME_GetAccessToken(LongByReference authResult, WAMBrokerUtils.StringByReference accessToken, IntByReference bufferSize);
       LongByReference MSALRUNTIME_GetGrantedScopes(long authResult, WAMBrokerUtils.StringByReference grantedScopes, IntByReference bufferSize);
       LongByReference MSALRUNTIME_GetAuthorizationHeader(long authResult, WAMBrokerUtils.StringByReference authHeader, IntByReference bufferSize);
       LongByReference MSALRUNTIME_IsPopAuthorization(long authResult, boolean isPopAuthorization);
       LongByReference MSALRUNTIME_GetExpiresOn(long authResult, LongByReference accessTokenExpirationTime);
       LongByReference MSALRUNTIME_GetError(LongByReference authResult, LongByReference responseError);
       LongByReference MSALRUNTIME_GetTelemetryData(long authResult, WAMBrokerUtils.StringByReference telemetryData, IntByReference bufferSize);

       //MSALRuntimeReadAccountResult.h
       LongByReference MSALRUNTIME_ReleaseReadAccountResult(long readAccountResult);
       LongByReference MSALRUNTIME_GetReadAccount(long readAccountResult, long account);
       LongByReference MSALRUNTIME_GetReadAccountError(long readAccountResult, LongByReference responseError);
       LongByReference MSALRUNTIME_GetReadAccountTelemetryData(long readAccountResult, WAMBrokerUtils.StringByReference telemetryData, IntByReference bufferSize);

       //MSALRuntimeSignoutResult.h
       LongByReference MSALRUNTIME_ReleaseSignOutResult(long signoutResult);
       LongByReference MSALRUNTIME_GetSignOutError(long signoutResult, LongByReference responseError);
       LongByReference MSALRUNTIME_GetSignOutTelemetryData(long signoutResult, WAMBrokerUtils.StringByReference telemetryData, IntByReference bufferSize);

       //MSALRuntimeDiscoverAccountsResult.h
       LongByReference MSALRUNTIME_ReleaseDiscoverAccountsResult(long discoverAccountsResult);
       LongByReference MSALRUNTIME_GetDiscoverAccountsAt(long discoverAccountsResult, int index, long account);
       LongByReference MSALRUNTIME_GetDiscoverAccountsError(long discoverAccountsResult, LongByReference responseError);
       LongByReference MSALRUNTIME_GetDiscoverAccountsTelemetryData(long discoverAccountsResult, WAMBrokerUtils.StringByReference telemetryData, IntByReference bufferSize);

       //MSALRuntimeError.h
       LongByReference MSALRUNTIME_ReleaseError(LongByReference error);
       LongByReference MSALRUNTIME_GetStatus(LongByReference error, LongByReference responseStatus);
       LongByReference MSALRUNTIME_GetErrorCode(LongByReference error, LongByReference responseErrorCode);
       LongByReference MSALRUNTIME_GetTag(long error, IntByReference responseErrorTag);
       LongByReference MSALRUNTIME_GetContext(LongByReference error, WAMBrokerUtils.StringByReference context, IntByReference bufferSize);

       //MSALRuntimeCancel.h
       LongByReference MSALRUNTIME_ReleaseAsyncHandle(AsyncHandleInterface asyncHandle);
       LongByReference MSALRUNTIME_CancelAsyncOperation(AsyncHandleInterface asyncHandle);

       //MSALRuntimeLogging.h
       LongByReference MSALRUNTIME_RegisterLogCallback(LogCallbackInterface callback, WString callbackData, long callbackHandle);
       LongByReference MSALRUNTIME_ReleaseLogCallbackHandle(long callbackHandle);
}




