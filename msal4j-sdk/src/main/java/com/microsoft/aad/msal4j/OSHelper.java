package com.microsoft.aad.msal4j;

/**
 * TODO: Add class description
 */
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

    /**
     * Gets the os.
     * 
     * @return the os
     */
    public static String getOs(){
        return OS;
    }

    /**
     * Checks if mac.
     * 
     * @return true if mac, false otherwise
     */
    public static boolean isMac(){
        return OSType.MAC.equals(osType);
    }

    /**
     * Checks if windows.
     * 
     * @return true if windows, false otherwise
     */
    public static boolean isWindows(){
        return OSType.WINDOWS.equals(osType);
    }

    /**
     * Checks if linux.
     * 
     * @return true if linux, false otherwise
     */
    public static boolean isLinux(){
        return OSType.LINUX.equals(osType);
    }
}
