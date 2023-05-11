package com.microsoft.aad.msal4j;

public class OSHelper {

    private static String os;
    private static boolean mac;
    private static boolean windows;
    private static boolean linux;

    static{
        os = System.getProperty("os.name").toLowerCase();
        if(os.contains("windows")){
            windows = true;
        }else if (os.contains("mac")){
            mac = true;
        }else if (os.contains("nux") || os.contains("nix")){
            linux = true;
        }
    }

    public static String getOs(){
        return os;
    }

    public static boolean isMac(){
        return mac;
    }

    public static boolean isWindows(){
        return windows;
    }

    public static boolean isLinux(){
        return linux;
    }


}
