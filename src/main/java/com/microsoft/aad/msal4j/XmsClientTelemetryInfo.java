// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

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

    private String serverErrorCode;
    private String serverSubErrorCode;
    private String tokenAge;
    private String speInfo;

    static XmsClientTelemetryInfo parseXmsTelemetryInfo(String headerValue){
        if(Strings.isNullOrEmpty(headerValue)){
            return null;
        }

        String[] headerSegments = headerValue.split(",");
        if(headerSegments.length == 0){
            return null;
        }

        String headerVersion = headerSegments[0];
        XmsClientTelemetryInfo xmsClientTelemetryInfo = new XmsClientTelemetryInfo();

        if(!headerVersion.equals(EXPECTED_HEADER_VERSION)){
            return  null;
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
