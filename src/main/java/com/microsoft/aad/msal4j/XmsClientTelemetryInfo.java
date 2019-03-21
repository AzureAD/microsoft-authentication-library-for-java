package com.microsoft.aad.msal4j;

import com.google.common.base.Strings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class XmsClientTelemetryInfo {

    private final static String EXPECTED_HEADER_VERSION = "1";
    private final static int ERROR_CODE_INDEX = 1;
    private final static int SUB_ERROR_CODE_INDEX = 2;
    private final static int TOKEN_AGE_INDEX = 3;
    private final static int SPE_INFO_INDEX = 4;

    private String version;
    private String serverErrorCode;
    private String serverSubErrorCode;
    private String tokenAge;
    private String speInfo;


    private XmsClientTelemetryInfo(String headerVersion){
        this.version = headerVersion;
    }

    static XmsClientTelemetryInfo parseXmsTelemetryInfo(String headerValue){
        if(Strings.isNullOrEmpty(headerValue)){
            return null;
        }

        String[] headerSegments = headerValue.split(",");
        if(headerSegments.length == 0){
            return null;
        }

        String headerVersion = headerSegments[0];
        XmsClientTelemetryInfo xmsClientTelemetryInfo = new XmsClientTelemetryInfo(headerVersion);

        if(!headerVersion.equals(EXPECTED_HEADER_VERSION)){
            return  xmsClientTelemetryInfo;
        }

        Matcher matcher = matchHeaderToExpectedFormat(headerValue);
        if(!matcher.matches()){
            return xmsClientTelemetryInfo;
        }

        headerSegments = headerValue.split(",", 5 );

        xmsClientTelemetryInfo.serverErrorCode = headerSegments[ERROR_CODE_INDEX];
        xmsClientTelemetryInfo.serverSubErrorCode = headerSegments[SUB_ERROR_CODE_INDEX];
        xmsClientTelemetryInfo.tokenAge = headerSegments[TOKEN_AGE_INDEX];
        xmsClientTelemetryInfo.speInfo = headerSegments[SPE_INFO_INDEX];

        return xmsClientTelemetryInfo;
    }

    private static Matcher matchHeaderToExpectedFormat(String header){
        String regexp = "^[1-9]+\\.?[0-9|\\.]*,[0-9|\\.]*,[0-9|\\.]*,[^,]*[0-9\\.]*,[^,]*$";
        Pattern pattern = Pattern.compile(regexp);
        return pattern.matcher(header);
    }

    public String getServerErrorCode() {
        return serverErrorCode;
    }

    public String getServerSubErrorCode() {
        return serverSubErrorCode;
    }

    public String getTokenAge() {
        return tokenAge;
    }

    public String getSpeInfo() {
        return speInfo;
    }
}
