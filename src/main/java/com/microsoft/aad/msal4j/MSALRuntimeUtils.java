package com.microsoft.aad.msal4j;//TODO: will be moved to JavaMSALRuntime package, must not reference MSAL Java directly

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.ptr.ByReference;

import java.util.concurrent.atomic.AtomicInteger;

public class MSALRuntimeUtils {
    static AtomicInteger MSALRuntimeRefs = new AtomicInteger(0);

    //TODO: GetString method to make the string buffer stuff simpler

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

    static void incrementRefsCounter(MSALRuntimeLibrary MSALRuntime_INSTANCE, MSALRuntimeError errorHandler) {
        //Once we get our first reference to the msalruntime.dll, call the startup method
        if (MSALRuntimeRefs.incrementAndGet() == 1) {
            errorHandler.printAndReleaseError(MSALRuntime_INSTANCE.MSALRUNTIME_Startup());
        }
    }

    static void decrementRefsCounter(MSALRuntimeLibrary MSALRuntime_INSTANCE) {
        //When there are no more refs to the msalruntime.dll, call the shutdown method
        if (MSALRuntimeRefs.decrementAndGet() == 0) {
            MSALRuntime_INSTANCE.MSALRUNTIME_Shutdown();
        }
    }

    static void setMSALRuntimeDLLPath (){
        //Assumes msalruntime.dll is in same packages as JavaMSALRuntime
        //TODO: Should we allow customer to change it? Any use case where they want a different mdsalruntime.dll
        if (System.getProperty("jna.library.path") != null) {
            System.setProperty("jna.library.path", System.getProperty("user.dir"));
        }
    }

    static MSALRuntimeLibrary setMSALRuntimeLibrary(){
        //Will look for dll at jna.library.path, plus some other common dll/Java paths specified by JNA
        return Native.load("msalruntime.dll", MSALRuntimeLibrary.class);
    }
}
