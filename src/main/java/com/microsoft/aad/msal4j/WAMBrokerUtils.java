package com.microsoft.aad.msal4j;//TODO: will be moved to JavaMSALRuntime package, must not reference MSAL Java directly

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.ptr.ByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;


public class WAMBrokerUtils {

    //StringByReference is a class to act as an MSALRuntime os_char**
    public static class StringByReference extends ByReference {
        public StringByReference() {
            this(0);
        }

        public StringByReference(int size) {
            super(size < 4 ? 4 : size);
            getPointer().clear(size < 4 ? 4 : size);
        }

        public StringByReference(WString str) {
            super(str.length() < 4 ? 4 : str.length() + 1);
            setValue(str);
        }

        private void setValue(WString str) {
            getPointer().setString(0, str);
        }

        public String getValue() {
            return getPointer().getWideString(0);
        }
    }

    //Utilities for finding the correct msalruntime.dll to use on a given platform architecture
    static void setMSALRuntimeDLLPath (){
        //Assumes msalruntime.dll is in same packages as JavaMSALRuntime
        //TODO: Should we allow customer to change it? Any use case where they want a different mdsalruntime.dll
        if (System.getProperty("jna.library.path") != null) {
            System.setProperty("jna.library.path", System.getProperty("user.dir"));
        }
    }

    static MsalRuntimeLibrary loadMsalRuntimeLibrary(){
        //Will look for dll at jna.library.path, plus some other common dll/Java paths specified by JNA
        //TODO: logic for platform type
        return Native.load("msalruntime.dll", MsalRuntimeLibrary.class);
    }

    //Utilities for getting strings from various MSALRuntime data objects
    static StringByReference getString(LongByReference error, MsalRuntimeLibrary msalRuntimeLibraryInstance, GetMSALRuntimeString getMSALRuntimeString) {
        WAMBrokerUtils.StringByReference contextRef = new WAMBrokerUtils.StringByReference(new WString(""));
        IntByReference bufferSize = new IntByReference(0);

        //To get a string from a handle, MSALRuntime requires us to send a blank string and a buffer of 0,
        // get an exception saying it failed because the buffer was 0, and then just try it again since they've now set the buffer to the right size
        WAMBrokerErrorHandler.MSALRuntimeError parseError =  WAMBrokerErrorHandler.parseAndReleaseError(getMSALRuntimeString.getString(error, null, bufferSize), msalRuntimeLibraryInstance);
        if (parseError != null) {
            //If the attempt to get a string resulted in an 'insufficient buffer' error, the bufferSize will have been set with the correct size and we can get the string
            //TODO: needs to align with the MSALRuntimeTypes.MSALRUNTIME_RESPONSE_STATUS enum, which is currently option 10
            if (parseError.responseStatus == 10) {
                WAMBrokerErrorHandler.ignoreAndReleaseError(getMSALRuntimeString.getString(error, contextRef, bufferSize), msalRuntimeLibraryInstance);
            } else {
                //TODO: throw exception, since we couldn't get the string for an unknown reason
            }
        } else {
            //TODO: throw exception, since we couldn't get the string for an unknown reason
        }

        return contextRef;
    }

    interface GetMSALRuntimeString {
        LongByReference getString(LongByReference MSALRuntimeData, WAMBrokerUtils.StringByReference stringReference, IntByReference bufferSize);
    }
}
