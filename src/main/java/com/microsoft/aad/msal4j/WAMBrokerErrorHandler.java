package com.microsoft.aad.msal4j;//TODO: will be move to JavaMSALRuntime package, must not reference MSAL Java directly

import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

//TODO: just move to MSALRuntimeUtils?
class WAMBrokerErrorHandler {
    static class MSALRuntimeError {
        int tag;
        long errorCode;
        long responseStatus;
        String context;

        MSALRuntimeError (int tag, long errorCode, long responseStatus, String context) {
             this.tag = tag;
             this.errorCode = errorCode;
             this.responseStatus = responseStatus;
             this.context = context;
        }
    }

    //Checks if a LongByReference could be pointing to an MSALRUNTIME_ERROR_HANDLE, and if so retrieves error info from it
    //  After retrieving the error info, store it in an MSALRuntimeError object and release the original error
    //TODO: use result of this to throw exceptions elsewhere
    static MSALRuntimeError parseAndReleaseError(LongByReference error, MsalRuntimeLibrary msalRuntimeLibraryInstance) {
        if (checkMSALRuntimeError(error)) {
            IntByReference tagRef = new IntByReference();
            LongByReference errorCodeRef = new LongByReference();
            LongByReference responseStatusRef = new LongByReference();//TODO: enums might be tricky

            ignoreAndReleaseError(msalRuntimeLibraryInstance.MSALRUNTIME_GetTag(error.getValue(), tagRef), msalRuntimeLibraryInstance);
            ignoreAndReleaseError(msalRuntimeLibraryInstance.MSALRUNTIME_GetErrorCode(error, errorCodeRef), msalRuntimeLibraryInstance);
            ignoreAndReleaseError(msalRuntimeLibraryInstance.MSALRUNTIME_GetStatus(error, responseStatusRef), msalRuntimeLibraryInstance);
            WAMBrokerUtils.StringByReference contextRef = WAMBrokerUtils.getString(error, msalRuntimeLibraryInstance, msalRuntimeLibraryInstance::MSALRUNTIME_GetContext);

            MSALRuntimeError parsedError = new MSALRuntimeError(tagRef.getValue(), errorCodeRef.getValue(), responseStatusRef.getValue(), contextRef.getValue());
            ignoreAndReleaseError(error, msalRuntimeLibraryInstance);

            return parsedError;
        } else {
            return null;
        }
    }

    // Used when result of MSALRUNTIM_ERROR_HANDLE can be ignored (such as the error handles returned when parsing other error handles)
    //TODO: determine if a 'release memory' call from JNA is needed to remove the pointer or any other refs
    static void ignoreAndReleaseError(LongByReference error, MsalRuntimeLibrary MSALRuntime_INSTANCE) {
        if (error != null) {
            MSALRuntime_INSTANCE.MSALRUNTIME_ReleaseError(error);
        }
    }

    //Most MSALRuntime APIs return a '0' if there were no errors, and the MSALRuntime library methods in our interop layer
    //  expect a LongByReference return type for all of those APIs
    //If the value of a pointer is 0 or null, MSALRuntime was successful or at least did not return any error info
    static boolean checkMSALRuntimeError(LongByReference error) {
        return error != null && error.getValue() != 0;
    }

    //Releases all objects created by JNA (TODO: maybe not needed? Java's garbarge collection should definitely handle JNA's stuff)
    static void releaseAllNativeMemory(){
        Memory.disposeAll();//TODO: need to ensure that this does what it says it does
    }
}
