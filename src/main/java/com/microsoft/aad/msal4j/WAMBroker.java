package com.microsoft.aad.msal4j;//TODO: will be moved to the Broker package, can reference MSAL Java and JavaMSALRuntime

import com.nimbusds.jwt.JWTParser;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of IBroker, allows use of MSALRuntime to authenticate via WAM
 */
public class WAMBroker implements IBroker {
    MsalRuntimeLibrary msalRuntimeLibraryInstance;
    AtomicInteger MSALRuntimeRefs = new AtomicInteger(0);

    //Set up whatever is needed for the broker to function
    public void initializeBroker() {
        WAMBrokerUtils.setMSALRuntimeDLLPath();
        msalRuntimeLibraryInstance = WAMBrokerUtils.loadMsalRuntimeLibrary();
        //TODO: exception handling for when the above two variables can't be set
    }

    public AuthenticationResult acquireToken(SilentRequest request) {
        WAMBrokerResult result = new WAMBrokerResult(this);

        result.authParamsHandle = new LongByReference();

        result.initializeAuthParameters(request.application().clientId(),
                request.application().authority(),
                request.parameters().scopes().toString(),
                null,//No redirect url in a silent request
                request.parameters().claims().toString());

        //If request has an account ID, try to get account info from MSALRuntime
        if (request.parameters().account() != null) {
            readAccountById(request.parameters().account().homeAccountId(), result);
        }

        //If request did not have an account ID or MSALRuntime did not return an account, attempt a silent sign in
        if (result.accountId == null) {
            signInSilently(result);
        }

        //Account information populated, attempt to acquire access token
        acquireTokenSilently(result);

        //Parse the results of the MSALRuntime calls into an MSAL Java AuthenticationResult, release handles, and return
        AuthenticationResult parsedResult = parseMSALRuntimeAuthResult(result, request.requestAuthority());
        result.releaseAllHandles();

        return parsedResult;
    }

    public AuthenticationResult acquireToken(RefreshTokenRequest request) {
        //TODO: likely can't do this without adding access token to RefreshTokenRequest, might need to do a check for expires_on in acquireToken methods
        return null;
    }

    public AuthenticationResult acquireToken(InteractiveRequest request) {
        WAMBrokerResult result = new WAMBrokerResult(this);

        result.authParamsHandle = new LongByReference();

        result.initializeAuthParameters(request.application().clientId(),
                request.application().authority(),
                request.interactiveRequestParameters().scopes().toString(),
                request.interactiveRequestParameters().redirectUri().toString(),
                request.interactiveRequestParameters().claims().toString());

        //Perform an interactive sign in to get the user information
        signInInteractively(result, request);

        //Account information populated, attempt to acquire access token
        acquireTokenInteractively(result, request);

        //Parse the results of the MSALRuntime calls into an MSAL Java AuthenticationResult, release handles, and return
        AuthenticationResult parsedResult = parseMSALRuntimeAuthResult(result, request.application().authenticationAuthority);
        result.releaseAllHandles();

        return parsedResult;
    }

    public AuthenticationResult acquireToken(UserNamePasswordParameters request) {
        //TODO: not actually supported by MSALRuntime? Might only be for their internal testing?
        return null;
    }

    public Account getAccount(String id) {
        return null;
    }

    public Set<Account> getAccounts() {
        return null;
    }

    WAMBrokerErrorHandler.MSALRuntimeError readAccountById(String accountId, WAMBrokerResult result) {

        return WAMBrokerErrorHandler.parseAndReleaseError(msalRuntimeLibraryInstance.MSALRUNTIME_ReadAccountByIdAsync(
                new WString(accountId),
                new WString(java.util.UUID.randomUUID().toString()),
                new WAMBrokerResult.authResultCallback(),
                result.getPointer(),
                new IntByReference(0) //TODO: need to figure out if AsyncHandle can be handled entirely by MSAL Java, or if we need to get it from customers like WindowHandle
        ), msalRuntimeLibraryInstance);
    }

    WAMBrokerErrorHandler.MSALRuntimeError signInSilently(WAMBrokerResult result) {
        return WAMBrokerErrorHandler.parseAndReleaseError(msalRuntimeLibraryInstance.MSALRUNTIME_SignInSilentlyAsync(
                result.authParamsHandle.getValue(),
                new WString(java.util.UUID.randomUUID().toString()),
                new WAMBrokerResult.authResultCallback(),
                result.getPointer(),
                new IntByReference(0) //TODO: AsyncHandle
        ), msalRuntimeLibraryInstance);
    }


    WAMBrokerErrorHandler.MSALRuntimeError signInInteractively(WAMBrokerResult result, InteractiveRequest request) {
        return WAMBrokerErrorHandler.parseAndReleaseError(msalRuntimeLibraryInstance.MSALRUNTIME_SignInInteractivelyAsync(
                request.interactiveRequestParameters().windowHandle(),
                result.authParamsHandle.getValue(),
                new WString(java.util.UUID.randomUUID().toString()),
                new WString(request.interactiveRequestParameters().loginHint()),
                new WAMBrokerResult.authResultCallback(),
                result.getPointer(),
                new IntByReference(0) //TODO: AsyncHandle
        ), msalRuntimeLibraryInstance);
    }

    WAMBrokerErrorHandler.MSALRuntimeError acquireTokenSilently(WAMBrokerResult result) {

        return WAMBrokerErrorHandler.parseAndReleaseError(msalRuntimeLibraryInstance.MSALRUNTIME_AcquireTokenSilentlyAsync(
                result.authParamsHandle.getValue(),
                new WString(java.util.UUID.randomUUID().toString()),
                result.accountHandle.getValue(),
                new WAMBrokerResult.authResultCallback(),
                result.getPointer(),
                new IntByReference(0) //TODO: AsyncHandle
        ), msalRuntimeLibraryInstance);
    }

    WAMBrokerErrorHandler.MSALRuntimeError  acquireTokenInteractively(WAMBrokerResult result, InteractiveRequest request) {
        return WAMBrokerErrorHandler.parseAndReleaseError(msalRuntimeLibraryInstance.MSALRUNTIME_AcquireTokenInteractivelyAsync(
                request.interactiveRequestParameters().windowHandle(),
                result.authParamsHandle.getValue(),
                new WString(java.util.UUID.randomUUID().toString()),
                result.accountHandle.getValue(),
                new WAMBrokerResult.authResultCallback(),
                result.getPointer(),
                new IntByReference(0) //TODO: AsyncHandle
        ), msalRuntimeLibraryInstance);
    }

    //TODO: add logic for this call to MSAL Java's removeAccount
    void signOutSilently() { }

    //TODO: determine if this is needed (.NET considered it optional/based on customer requesting it?)
    void discoverAccounts() { }

    //Methods for converting results of MSALRuntime calls to MSAL Java objects
    AuthenticationResult parseMSALRuntimeAuthResult(WAMBrokerResult msalRuntimeResult, Authority authority) {

        //TODO: using a builder like this feels weird and I don't know why. Better way to handle creating the AuthenticationResult? Needs error handling at least
        AuthenticationResult.AuthenticationResultBuilder builder = AuthenticationResult.builder();
        try {
            if (msalRuntimeResult.idToken != null) {
                builder.idToken(msalRuntimeResult.idToken.toString());
                if (msalRuntimeResult.accountId != null) {
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
            //TODO: send msalexception back to MSAL Java
        }

        return builder.build();
    }

    //Utilities for incrementing/decrementing a counter to references to msalruntime.dll
    //  When the first reference is made, call MSALRuntime's startup method
    //  When the last reference is released, call MSALRuntime's shutdown method
    void incrementRefsCounter() {
        //Once we get our first reference to the msalruntime.dll, call the startup method
        if (MSALRuntimeRefs.incrementAndGet() == 1) {
            WAMBrokerErrorHandler.parseAndReleaseError(msalRuntimeLibraryInstance.MSALRUNTIME_Startup(), msalRuntimeLibraryInstance);
        }
    }

    void decrementRefsCounter() {
        //When there are no more refs to the msalruntime.dll, call the shutdown method
        if (MSALRuntimeRefs.decrementAndGet() == 0) {
            msalRuntimeLibraryInstance.MSALRUNTIME_Shutdown();
        }
    }


    //TODO: purely used for manual testing during early development, will be removed when there is automatic testing and can be completely ignored if seen in code reviews
    public static void main(String args[]) {
        WAMBroker broker = new WAMBroker();
        broker.initializeBroker();

        //Used to halt the Java program to get time to set up C++/msalruntime.dll debugger in Visual Studio, not needed if just testing Java side only
        Scanner myObj = new Scanner(System.in);
        myObj.nextLine();

        System.out.println("===Set up auth result");
        WAMBrokerResult authResult = new WAMBrokerResult(broker);
        authResult.authParamsHandle = new LongByReference();

        //ClientID taken from MSAL Python's PyMSALRuntime sample
        authResult.initializeAuthParameters("903c8a8a-9e74-415e-9921-711a293d90cb", "https://login.microsoftonline.com/common",
                "https://graph.microsoft.com/.default", "https://login.microsoftonline.com/common/oauth2/nativeclient", null);

        System.out.println("===Set up callback");
        final WAMBrokerResult.authResultCallback authResultCallback = new WAMBrokerResult.authResultCallback();


        System.out.println("===Sign in silently");
        WAMBrokerErrorHandler.parseAndReleaseError(broker.msalRuntimeLibraryInstance.MSALRUNTIME_SignInSilentlyAsync(
                authResult.authParamsHandle.getValue(),
                new WString(java.util.UUID.randomUUID().toString()),
                authResultCallback,
                authResult.getPointer(),
                new IntByReference(0)
        ), broker.msalRuntimeLibraryInstance);
    }
}
