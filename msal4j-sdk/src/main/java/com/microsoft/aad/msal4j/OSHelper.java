package com.microsoft.aad.msal4j;

public class OSHelper {
    enum OSType{
        MAC,
        WINDOWS,
        LINUX
    }

    private static final String OS;
    private static OSType osType;

    static{
        OS = System.getProperty("os.name").toLowerCase();
        if(OS.contains("windows")){
            osType = OSType.WINDOWS;
        }else if (OS.contains("mac")){
            osType = OSType.MAC;
        }else if (OS.contains("nux") || OS.contains("nix")){
            osType = OSType.LINUX;
        }
    }

    public static String getOs(){
        return OS;
    }

    public static boolean isMac(){
        return OSType.MAC.equals(osType);
    }

    public static boolean isWindows(){
        return OSType.WINDOWS.equals(osType);
    }

    public static boolean isLinux(){
        return OSType.LINUX.equals(osType);
    }
}
