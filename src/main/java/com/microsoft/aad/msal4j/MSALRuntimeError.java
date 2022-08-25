package com.microsoft.aad.msal4j;//TODO: will be move to JavaMSALRuntime package, must not reference MSAL Java directly

import com.sun.jna.Memory;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

//TODO: just move to MSALRuntimeUtils?
public class MSALRuntimeError {
    public MSALRuntimeLibrary MSALRuntime_INSTANCE;

    //ParseError(Tag, Code, Status, Context)
    //CheckError TODO: currently used for more manual testing, will be replaced with error handling to create MSAL exceptions
    public void printAndReleaseError(LongByReference error) {
        if (error != null && error.getValue() != 0) {
            System.out.println("--Some error");
            IntByReference tagRef = new IntByReference();
            LongByReference errorCodeRef = new LongByReference();
            LongByReference responseStatusRef = new LongByReference();//TODO: enums might be tricky
            MSALRuntimeUtils.StringByReference contextRef = new MSALRuntimeUtils.StringByReference(new WString(""));
            IntByReference bufferSize = new IntByReference(0);
            ignoreAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_GetTag(error.getValue(), tagRef));
            ignoreAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_GetErrorCode(error, errorCodeRef));
            ignoreAndReleaseError( MSALRuntime_INSTANCE.MSALRUNTIME_GetStatus(error, responseStatusRef));
            //TODO: Better error handling for getting strings
            //To get a string from a handle, MSALRuntime requires us to send a blank string and a buffer of 0,
            // get an exception saying it failed because the buffer was 0, and then just try it again since they've now set the buffer to the right size
            ignoreAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_GetContext(error, null, bufferSize));
            ignoreAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_GetContext(error, contextRef, bufferSize));

            System.out.println("Tag: " + tagRef.getValue());
            System.out.println("Code: " + errorCodeRef.getValue());
            System.out.println("Status: " + responseStatusRef.getValue());
            System.out.println("Context: " + contextRef.getValue());

            System.out.println("===Releasing error");
            ignoreAndReleaseError(error);
        }
    }

    // Used when result of MSALRUNTIM_ERROR_HANDLE can be ignored (such as the error handles returned when parsing other error handles)
    //TODO: determine if a 'release memory' call from JNA is needed to remove the pointer or any other refs
    public void ignoreAndReleaseError(LongByReference error) {
        if (error != null) {
            MSALRuntime_INSTANCE.MSALRUNTIME_ReleaseError(error);
        }
    }

    //Releases all objects created by JNA (TODO: maybe not needed? Java's garbarge collection should definitely handle JNA's stuff)
    void releaseAllNativeMemory(){
        Memory.disposeAll();//TODO: need to ensure that this does what it says it does
    }
}
