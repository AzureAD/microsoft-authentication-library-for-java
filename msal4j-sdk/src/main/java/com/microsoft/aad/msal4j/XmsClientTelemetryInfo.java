// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class XmsClientTelemetryInfo {

    private static final String EXPECTED_HEADER_VERSION = "1";
    private static final int ERROR_CODE_INDEX = 1;
    private static final int SUB_ERROR_CODE_INDEX = 2;
    private static final int TOKEN_AGE_INDEX = 3;
    private static final int SPE_INFO_INDEX = 4;

    private String serverErrorCode;
    private String serverSubErrorCode;
    private String tokenAge;
    private String speInfo;

    static XmsClientTelemetryInfo parseXmsTelemetryInfo(String headerValue) {
        if (StringHelper.isBlank(headerValue)) {
            return null;
        }

        String[] headerSegments = headerValue.split(",");
        if (headerSegments.length == 0) {
            return null;
        }

        String headerVersion = headerSegments[0];
        XmsClientTelemetryInfo xmsClientTelemetryInfo = new XmsClientTelemetryInfo();

        if (!headerVersion.equals(EXPECTED_HEADER_VERSION)) {
            return null;
        }

        Matcher matcher = matchHeaderToExpectedFormat(headerValue);
        if (!matcher.matches()) {
            return xmsClientTelemetryInfo;
        }

        headerSegments = headerValue.split(",", 5);

        xmsClientTelemetryInfo.serverErrorCode = headerSegments[ERROR_CODE_INDEX];
        xmsClientTelemetryInfo.serverSubErrorCode = headerSegments[SUB_ERROR_CODE_INDEX];
        xmsClientTelemetryInfo.tokenAge = headerSegments[TOKEN_AGE_INDEX];
        xmsClientTelemetryInfo.speInfo = headerSegments[SPE_INFO_INDEX];

        return xmsClientTelemetryInfo;
    }

    private static Matcher matchHeaderToExpectedFormat(String header) {
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
