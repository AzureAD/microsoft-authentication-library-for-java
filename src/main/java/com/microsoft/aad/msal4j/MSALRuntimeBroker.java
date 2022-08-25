package com.microsoft.aad.msal4j;//TODO: will be moved to the Broker package, can reference MSAL Java and JavaMSALRuntime

import com.nimbusds.jwt.JWTParser;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of IBroker, allows use of MSALRuntime to authenticate via WAM
 */
public class MSALRuntimeBroker implements IBroker {
    MSALRuntimeLibrary MSALRUNTIME_INSTANCE;

    public void initializeBroker() {
        MSALRuntimeUtils.setMSALRuntimeDLLPath();
        MSALRUNTIME_INSTANCE = MSALRuntimeUtils.setMSALRuntimeLibrary();
        //TODO: exception handling for when the above two variables can't be set
    }

    public Account signInSilently(PublicClientApplication clientApplication, InteractiveRequestParameters interactiveRequestParameters) {
        MSALRuntimeAuthResult authResult = new MSALRuntimeAuthResult();
        authResult.authParamsHandle = new LongByReference();

        authResult.initializeAuthParameters(clientApplication.clientId(), clientApplication.authority(),
                interactiveRequestParameters.scopes().toString(), interactiveRequestParameters.redirectUri().toString(), interactiveRequestParameters.claims().toString());

        final MSALRuntimeAuthResult.authResultCallback authResultCallback = new MSALRuntimeAuthResult.authResultCallback();

        //TODO: need to figure out if AsyncHandle can be handled entirely by MSAL Java, or if we need to get it from customers like WindowHandle

        MSALRUNTIME_INSTANCE.MSALRUNTIME_SignInSilentlyAsync(
                authResult.authParamsHandle.getValue(),
                new WString(java.util.UUID.randomUUID().toString()),
                authResultCallback,
                authResult.getPointer(),
                new IntByReference(0)
        );

        return parseMSALRuntimeAccountResult(authResult, clientApplication.authenticationAuthority);
    }


    public Account signInInteractively(PublicClientApplication clientApplication, InteractiveRequestParameters interactiveRequestParameters) {
        //TODO: blocked by windowHandle and messaging issues
        return  null;
    }

    //AuthenticationResult acquireTokenSilently(String clientId, String authority, String scopes, String redirectUri, String claims, String accessToken, String additionalParams, String correlationId, String accountId) {
    public AuthenticationResult acquireTokenSilently(PublicClientApplication clientApplication, InteractiveRequestParameters interactiveRequestParameters, Account account) {
        MSALRuntimeAuthResult authResult = new MSALRuntimeAuthResult();
        authResult.authParamsHandle = new LongByReference();

        authResult.initializeAuthParameters(clientApplication.clientId(), clientApplication.authority(),
                interactiveRequestParameters.scopes().toString(), interactiveRequestParameters.redirectUri().toString(), interactiveRequestParameters.claims().toString());

        final MSALRuntimeAuthResult.authResultCallback authResultCallback = new MSALRuntimeAuthResult.authResultCallback();

        //TODO: need to figure out if AsyncHandle can be handled entirely by MSAL Java, or if we need to get it from customers like WindowHandle

        MSALRUNTIME_INSTANCE.MSALRUNTIME_AcquireTokenSilentlyAsync(
                authResult.authParamsHandle.getValue(),
                new WString(java.util.UUID.randomUUID().toString()),
                null,//Need simple account class
                authResultCallback,
                authResult.getPointer(),
                new IntByReference(0)
        );

        return parseMSALRuntimeAuthResult(authResult, clientApplication.authenticationAuthority);
    }

    //AuthenticationResult acquireTokenInteractively(int windowHandle, String clientId, String authority, String scopes, String redirectUri, String claims, String accessToken, String additionalParams, String correlationId, String accountId) {
    public AuthenticationResult acquireTokenInteractively(PublicClientApplication clientApplication, InteractiveRequestParameters interactiveRequestParameters, Account account) {
        //TODO: blocked by windowHandle and messaging issues
        return  null;
    }

    //void signOutSilently(String clientId, String correlationId, String accountId) {
    public void signOutSilently(PublicClientApplication clientApplication, InteractiveRequestParameters interactiveRequestParameters) {

    }

    public void discoverAccounts(PublicClientApplication clientApplication, InteractiveRequestParameters interactiveRequestParameters) { }

    //Methods for converting results of MSALRuntime calls to MSAL Java objects
    AuthenticationResult parseMSALRuntimeAuthResult(MSALRuntimeAuthResult msalRuntimeResult, Authority authority) {

        AuthenticationResult.AuthenticationResultBuilder builder = AuthenticationResult.builder();
        try {
            if (msalRuntimeResult.idToken != null) {
                builder.idToken(msalRuntimeResult.idToken.toString());
                if (msalRuntimeResult.accountId != null) {//TODO: this code is a shame in almost every way. Needs a lot better error handling
                    String idTokenJson = JWTParser.parse(msalRuntimeResult.idToken.toString()).getParsedParts()[1].decodeToString();

                    //TODO: check if policies can come from MSALRuntime
                    builder.accountCacheEntity(AccountCacheEntity.create(msalRuntimeResult.accountClientInfo.toString(), authority, JsonHelper.convertJsonToObject(idTokenJson, IdToken.class), null));
                }
            }

            if (msalRuntimeResult.accessToken != null) {
                builder.accessToken(msalRuntimeResult.accessToken.toString());
                builder.expiresOn(msalRuntimeResult.accessTokenExpirationTime.getValue());
            }

        } catch (Exception e) {
            parseMSALRuntimeError();
        }

        return builder.build();
    }


    Account parseMSALRuntimeAccountResult(MSALRuntimeAuthResult msalRuntimeResult, Authority authority) {
        try {
            AuthenticationResult.AuthenticationResultBuilder builder = AuthenticationResult.builder();
            String idTokenJson = JWTParser.parse(msalRuntimeResult.idToken.toString()).getParsedParts()[1].decodeToString();

            builder.accountCacheEntity(AccountCacheEntity.create(msalRuntimeResult.accountClientInfo.toString(), authority, JsonHelper.convertJsonToObject(idTokenJson, IdToken.class), null));
            AuthenticationResult result = builder.build();
            result.account();

            msalRuntimeResult.releaseAuthParams();
            msalRuntimeResult.releaseAuthResult();
            return (Account) result.account();
        } catch (Exception e) {
            return null;
        }

    }

    void parseMSALRuntimeError() {
        //TODO: send msalexceptions back to MSAL Java
    }


    //TODO: purely used for manual testing during early development, will be removed when there is automatic testing and can be completely ignored if seen in code reviews
    public static void main(String args[]) {
        MSALRuntimeBroker broker = new MSALRuntimeBroker();
        broker.initializeBroker();

        MSALRuntimeError errorHandler = new MSALRuntimeError();

        //Used to halt the Java program to get time to set up C++/msalruntime.dll debugger in Visual Studio, not needed if just testing Java side only
        Scanner myObj = new Scanner(System.in);
        myObj.nextLine();

        System.out.println("===Set up auth result");
        MSALRuntimeAuthResult authResult = new MSALRuntimeAuthResult();
        authResult.authParamsHandle = new LongByReference();
        authResult.setMSALRuntimeLibraryInstance(broker.MSALRUNTIME_INSTANCE);
        authResult.setMSALRuntimeErrorHandler(broker.MSALRUNTIME_INSTANCE);

        //ClientID taken from MSAL Python's PyMSALRuntime sample
        authResult.initializeAuthParameters("903c8a8a-9e74-415e-9921-711a293d90cb", "https://login.microsoftonline.com/common",
                "https://graph.microsoft.com/.default", "https://login.microsoftonline.com/common/oauth2/nativeclient", null);

        System.out.println("===Set up callback");
        final MSALRuntimeAuthResult.authResultCallback authResultCallback = new MSALRuntimeAuthResult.authResultCallback();


        System.out.println("===Sign in silently");
        errorHandler.printAndReleaseError(broker.MSALRUNTIME_INSTANCE.MSALRUNTIME_SignInSilentlyAsync(
                authResult.authParamsHandle.getValue(),
                new WString(java.util.UUID.randomUUID().toString()),
                authResultCallback,
                authResult.getPointer(),
                new IntByReference(0)
        ));
    }
}
